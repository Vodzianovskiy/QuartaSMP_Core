package eu._Nightwarden.quartaSMP_Core.quest;

import eu._Nightwarden.quartaSMP_Core.QuartaSMP_Core;
import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;


/**
 * Главный менеджер квестовой системы.
 *
 * Единая точка входа для обновления прогресса задач.
 * Все Listener'ы (CraftListener, KillListener) вызывают updateProgress().
 *
 * Отвечает за:
 * - Обновление прогресса задач (через PlayerDataManager.executeAtomic)
 * - Проверку завершения недели
 * - Выдачу наград
 * - Проверку доступности недель (sequential/all-open)
 * - Сброс прогресса (per-player / global)
 *
 * Защита от race conditions:
 * - updateProgress() использует executeAtomic() с per-player блокировкой
 * - checkWeekCompletion() вызывается внутри той же атомарной операции
 * - Все изменения PlayerData проходят через единый лок
 */
public final class QuestManager {

    private final QuartaSMP_Core plugin;
    private final PlayerDataManager playerDataManager;
    private final QuestsConfig questsConfig;
    private final MessagesConfig messagesConfig;
    private final FileConfiguration config;
    private final TaskHandlerRegistry handlerRegistry;

    public QuestManager(QuartaSMP_Core plugin, PlayerDataManager playerDataManager,
                        QuestsConfig questsConfig, MessagesConfig messagesConfig,
                        FileConfiguration config, TaskHandlerRegistry handlerRegistry) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.questsConfig = questsConfig;
        this.messagesConfig = messagesConfig;
        this.config = config;
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * Единая точка входа для обновления прогресса задачи.
     * Вызывается из всех Listener'ов (CraftListener, KillListener) и из GUI.
     *
     * ВСЕГДА выполняется под per-player блокировкой через executeAtomic().
     *
     * @param player     игрок
     * @param weekId     ID недели
     * @param taskId     ID задачи
     * @param amount     количество (сколько добавить к прогрессу)
     */
    public void updateProgress(Player player, String weekId, String taskId, int amount) {
        if (amount <= 0) return;

        var uuid = player.getUniqueId();

        playerDataManager.executeAtomic(uuid, data -> {
            if (data == null) return null;

            var week = questsConfig.getWeeks().get(weekId);
            if (week == null) return data;

            // 🔥 Проверяем, доступна ли эта неделя игроку
            var weeksList = questsConfig.getWeeksList();
            var weekIndex = getWeekIndex(weekId);
            if (weekIndex < 0 || !isWeekUnlocked(data, weekIndex)) return data;

            // 🔥 Строгая защита: засчитывается только первая незавершённая неделя
            if (!isCurrentActiveWeek(data, weekId)) return data;

            // Находим задачу
            var taskOpt = week.tasks().stream()
                    .filter(t -> t.id().equals(taskId))
                    .findFirst();
            if (taskOpt.isEmpty()) return data;

            var task = taskOpt.get();

            // 🔥 Проверяем, доступна ли задача по цепочке внутри недели
            if (!isTaskUnlocked(data, week, taskId)) return data;

            // Проверяем, не завершена ли уже задача
            var currentProgress = data.getTaskProgress(weekId, taskId);
            if (task.isCompleted(currentProgress)) return data;

            // Новый прогресс (не больше targetAmount)
            var newProgress = Math.min(currentProgress + amount, task.targetAmount());
            var updatedData = data.withTaskProgress(weekId, taskId, newProgress);

            // Если задача только что завершена — начисляем награду за задачу
            if (task.isCompleted(newProgress) && !task.isCompleted(currentProgress)) {
                // Начисляем reward-exp за выполнение задачи
                var rewardExp = task.rewardExp();
                if (rewardExp > 0) {
                    updatedData = playerDataManager.addExpWithLevelUp(updatedData, rewardExp, player);
                }

                // Начисляем предметные награды за задачу
                giveItemRewards(player, task.taskRewards());

                sendTaskCompletedMessage(player, task);
            }

            // Проверяем, завершена ли вся неделя (внутри той же атомарной операции)
            return checkWeekCompletion(player, week, updatedData);

        });
    }

    /**
     * Возвращает индекс недели в списке weeksList по её ID.
     *
     * @param weekId ID недели
     * @return индекс (0-based) или -1 если не найдена
     */
    public int getWeekIndex(String weekId) {
        var weeks = questsConfig.getWeeksList();
        for (int i = 0; i < weeks.size(); i++) {
            if (weeks.get(i).id().equals(weekId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Проверяет, доступна ли задача для выполнения по цепочке внутри недели.
     * <p>
     * Задачи открываются строго последовательно:
     * - Первая задача недели всегда доступна
     * - Каждая следующая задача доступна ТОЛЬКО если все предыдущие выполнены
     * <p>
     * Это серверная защита, которая дублирует визуальную логику из QuestWeekMenu.
     * Используется в updateProgress() и в GUIListener.
     *
     * @param data   данные игрока
     * @param week   неделя
     * @param taskId ID задачи для проверки
     * @return true если задача доступна для выполнения
     */
    public boolean isTaskUnlocked(PlayerData data, QuestWeek week, String taskId) {
        var tasks = week.tasks();
        boolean found = false;

        for (var task : tasks) {
            if (task.id().equals(taskId)) {
                found = true;
                // Первая задача всегда доступна
                // Если мы дошли до искомой задачи и все предыдущие выполнены — true
                return true;
            }

            // Если предыдущая задача не выполнена — следующие недоступны
            var progress = data.getTaskProgress(week.id(), task.id());
            if (!task.isCompleted(progress)) {
                return false;
            }
        }

        // Если задача не найдена в списке — false
        return false;
    }

    /**
     * Проверяет, завершена ли неделя, и если да — выдаёт награду.
     * ДОЛЖНА вызываться внутри executeAtomic().
     *
     * @return обновлённые PlayerData (с наградой, если неделя завершена)
     */
    public PlayerData checkWeekCompletion(Player player, QuestWeek week, PlayerData data) {
        if (data.isWeekCompleted(week.id())) return data;

        // Собираем прогресс по всем задачам недели
        var progressList = week.tasks().stream()
                .map(task -> data.getTaskProgress(week.id(), task.id()))
                .toList();

        if (week.isAllTasksCompleted(progressList)) {
            // Неделя завершена!
            var updatedData = data.withWeekCompleted(week.id());

            // Начисляем quest-points → exp
            var expPerQuestPoint = config.getInt("leveling.exp-per-quest-point", 50);
            var earnedExp = week.questPoints() * expPerQuestPoint;
            updatedData = playerDataManager.addExpWithLevelUp(updatedData, earnedExp, player);

            // Выдаём предметные награды за неделю
            giveItemRewards(player, week.weekRewards());

            // Отправляем сообщение о завершении недели
            sendWeekCompletedMessage(player, week, week.questPoints(), earnedExp);

            return updatedData;

        }

        return data;
    }

    /**
     * Начисляет награду за выполнение задачи (опыт + предметы).
     * Вызывается, когда задача только что завершена.
     * Содержит логику level-up и выдачу предметных наград.
     *
     * @return обновлённые PlayerData с начисленным exp и возможным повышением уровня
     */
    public PlayerData applyTaskReward(Player player, QuestTask task, PlayerData data) {
        var updatedData = data;

        // 1. Начисляем опыт
        var rewardExp = task.rewardExp();
        if (rewardExp > 0) {
            updatedData = playerDataManager.addExpWithLevelUp(updatedData, rewardExp, player);
        }

        // 2. Выдаём предметные награды за задачу
        giveItemRewards(player, task.taskRewards());

        return updatedData;
    }


    /**
     * Проверяет, доступна ли неделя для игрока.
     *
     * @param playerData данные игрока
     * @param weekIndex  индекс недели в списке (0-based)
     * @return true если неделя доступна
     */
    public boolean isWeekUnlocked(PlayerData playerData, int weekIndex) {
        var unlockMode = config.getString("quests.weeks-unlock-mode", "admin-only");

        // global-schedule: недели открываются по глобальному расписанию для всех игроков
        if ("global-schedule".equalsIgnoreCase(unlockMode)) {
            return isWeekUnlockedByGlobalSchedule(playerData, weekIndex);
        }

        if ("all-open".equalsIgnoreCase(unlockMode)) {
            return true;
        }

        // admin-only: недели открываются ТОЛЬКО через /quartasmp unlockweek
        if ("admin-only".equalsIgnoreCase(unlockMode)) {
            if (weekIndex == 0) {
                return true; // Первая неделя всегда доступна
            }
            var weeks = questsConfig.getWeeksList();
            if (weekIndex >= weeks.size()) {
                return false;
            }
            var prevWeek = weeks.get(weekIndex - 1);
            return playerData.isWeekCompleted(prevWeek.id());
        }

        // schedule: недели открываются строго по расписанию от первого входа игрока.
        // Завершение предыдущей недели НЕ открывает следующую раньше срока.
        if ("schedule".equalsIgnoreCase(unlockMode)) {
            var scheduleSection = config.getConfigurationSection("quests.week-unlock-schedule");
            if (scheduleSection == null) {
                return weekIndex == 0;
            }

            var weeks = questsConfig.getWeeksList();
            if (weekIndex >= weeks.size()) {
                return false;
            }

            var weekId = weeks.get(weekIndex).id();

            var delayStr = scheduleSection.getString(weekId, "0d");
            var delayDays = parseDayDelay(delayStr);

            var firstJoin = playerData.firstJoinTime();
            var elapsed = System.currentTimeMillis() - firstJoin;
            var requiredMillis = delayDays * 24L * 60L * 60L * 1000L;

            return elapsed >= requiredMillis;
        }

        // sequential: неделя N доступна, если N-1 завершена
        if (weekIndex == 0) {
            return true;
        }

        var weeks = questsConfig.getWeeksList();
        if (weekIndex >= weeks.size()) {
            return false;
        }

        var prevWeek = weeks.get(weekIndex - 1);
        return playerData.isWeekCompleted(prevWeek.id());
    }

    /**
     * Проверяет доступность недели по глобальному расписанию (global-schedule).
     * <p>
     * Неделя доступна, если:
     * 1. Она уже открыта глобально (прошло достаточно времени от season-start).
     * 2. Предыдущая неделя завершена игроком (sequential в рамках открытых).
     * <p>
     * Это значит, что если глобально открыты week_1..week_4, но игрок прошёл только week_1,
     * его текущая неделя — week_2. Он не может перепрыгнуть на week_4.
     *
     * @param playerData данные игрока
     * @param weekIndex  индекс недели (0-based)
     * @return true если неделя доступна
     */
    private boolean isWeekUnlockedByGlobalSchedule(PlayerData playerData, int weekIndex) {
        var weeks = questsConfig.getWeeksList();
        if (weekIndex >= weeks.size()) return false;

        // 1. Проверяем глобальное расписание
        var seasonStartMillis = getSeasonStartMillis();
        var intervalDays = config.getInt("quests.global-schedule.interval-days", 7);
        var intervalMillis = intervalDays * 24L * 60L * 60L * 1000L;

        var now = System.currentTimeMillis();
        var elapsed = now - seasonStartMillis;

        // Сколько недель уже открыто глобально (0-based)
        // week_1 (index=0) доступна сразу, week_2 (index=1) через intervalDays и т.д.
        var globallyUnlockedCount = (int) (elapsed / intervalMillis) + 1;
        if (globallyUnlockedCount < 1) globallyUnlockedCount = 1;

        // Если неделя ещё не открыта глобально — false
        if (weekIndex >= globallyUnlockedCount) return false;

        // 2. Проверяем последовательность: предыдущая неделя должна быть завершена
        if (weekIndex == 0) return true; // week_1 всегда доступна

        var prevWeek = weeks.get(weekIndex - 1);
        return playerData.isWeekCompleted(prevWeek.id());
    }

    /**
     * Возвращает количество миллисекунд до открытия недели по глобальному расписанию.
     * Если неделя уже открыта — возвращает 0.
     */
    private long getTimeUntilGlobalUnlock(int weekIndex) {
        var seasonStartMillis = getSeasonStartMillis();
        var intervalDays = config.getInt("quests.global-schedule.interval-days", 7);
        var intervalMillis = intervalDays * 24L * 60L * 60L * 1000L;

        var now = System.currentTimeMillis();
        var elapsed = now - seasonStartMillis;

        // Когда должна открыться эта неделя
        var unlockTime = seasonStartMillis + (weekIndex * intervalMillis);

        var remaining = unlockTime - now;
        return Math.max(0, remaining);
    }

    /**
     * Парсит дату старта сезона из конфига.
     * Формат: ISO-8601, например "2026-07-10T00:00:00+02:00".
     * Если не удалось распарсить — возвращает текущее время (сезон стартует сейчас).
     */
    private long getSeasonStartMillis() {
        var dateStr = config.getString("quests.global-schedule.season-start");
        if (dateStr == null || dateStr.isBlank()) return System.currentTimeMillis();

        try {
            var dateTime = OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return dateTime.toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Не удалось распарсить season-start: " + dateStr + ". Используется текущее время.");
            return System.currentTimeMillis();
        }
    }

    /**
     * Возвращает true, если указанная неделя является текущей активной неделей
     * (первой незавершённой в списке).
     *
     * Это строгая защита: даже если schedule открыл week_3, пока week_1 не выполнена,
     * прогресс в week_2/week_3 засчитываться не будет.
     *
     * Для all-open режима: все открытые недели считаются активными,
     * чтобы игрок мог выполнять задачи на любой доступной неделе.
     *
     * @param data   данные игрока
     * @param weekId ID недели для проверки
     * @return true если это текущая активная неделя
     */
    public boolean isCurrentActiveWeek(PlayerData data, String weekId) {
        var unlockMode = config.getString("quests.weeks-unlock-mode", "admin-only");

        // Для all-open: любая незавершённая неделя считается активной
        if ("all-open".equalsIgnoreCase(unlockMode)) {
            return !data.isWeekCompleted(weekId);
        }

        // Для schedule/sequential/admin-only: только первая незавершённая
        var weeks = questsConfig.getWeeksList();
        for (var week : weeks) {
            if (!data.isWeekCompleted(week.id())) {
                return week.id().equals(weekId);
            }
        }
        return false;
    }

    /**
     * Парсит строку задержки вида "7d" в количество дней.
     * Если строка невалидна — возвращает 0.
     */
    private int parseDayDelay(String delayStr) {
        if (delayStr == null || delayStr.isBlank()) return 0;
        try {
            var trimmed = delayStr.trim().toLowerCase();
            if (trimmed.endsWith("d")) {
                return Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
            }
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Возвращает количество миллисекунд до открытия недели по расписанию.
     * Если неделя уже открыта — возвращает 0.
     * <p>
     * Поддерживает:
     * - global-schedule: время до глобального открытия (одинаково для всех)
     * - schedule: время от первого входа игрока (per-player)
     * - остальные режимы: 0 (неделя не открывается по времени)
     */
    public long getTimeUntilUnlock(PlayerData playerData, int weekIndex) {
        var unlockMode = config.getString("quests.weeks-unlock-mode", "admin-only");

        // global-schedule: время до глобального открытия
        if ("global-schedule".equalsIgnoreCase(unlockMode)) {
            return getTimeUntilGlobalUnlock(weekIndex);
        }

        // schedule: время от первого входа игрока
        if ("schedule".equalsIgnoreCase(unlockMode)) {
            var scheduleSection = config.getConfigurationSection("quests.week-unlock-schedule");
            if (scheduleSection == null) return 0;

            var weeks = questsConfig.getWeeksList();
            if (weekIndex >= weeks.size()) return 0;

            var weekId = weeks.get(weekIndex).id();
            var delayStr = scheduleSection.getString(weekId, "0d");
            var delayDays = parseDayDelay(delayStr);

            var firstJoin = playerData.firstJoinTime();
            var elapsed = System.currentTimeMillis() - firstJoin;
            var requiredMillis = delayDays * 24L * 60L * 60L * 1000L;

            var remaining = requiredMillis - elapsed;
            return Math.max(0, remaining);
        }

        // Для sequential/all-open/admin-only время не считается
        return 0;
    }

    /**
     * Проверяет, не истекло ли время на выполнение недели (per-player режим).
     * Если истекло — сбрасывает прогресс.
     */
    public PlayerData checkWeekExpiry(PlayerData data, String weekId) {
        var resetMode = config.getString("quests.reset-mode", "per-player");
        if (!"per-player".equalsIgnoreCase(resetMode)) return data;

        var weekStartTime = data.getWeekStartTime(weekId);
        if (weekStartTime == 0) return data;

        var timeLimitDays = config.getInt("quests.week-time-limit-days", 7);
        var timeLimitMillis = timeLimitDays * 24L * 60L * 60L * 1000L;
        var now = System.currentTimeMillis();

        if (now - weekStartTime > timeLimitMillis) {
            // Время истекло — сбрасываем прогресс
            return data.resetWeekProgress(weekId);
        }

        return data;
    }

    /**
     * Возвращает QuestsConfig (нужно для QuestWeekMenu).
     */
    public QuestsConfig getQuestsConfig() {
        return questsConfig;
    }

    /**
     * Выполняет глобальный сброс всех недель для всех игроков.
     * Вызывается по расписанию (раз в неделю).
     */
    public void globalReset() {
        plugin.getLogger().info("Performing global quest reset...");
        // В реальном коде здесь нужно пройтись по всем PlayerData в кэше
        // и сбросить прогресс. Для MVP — заглушка.
        plugin.getLogger().info("Global quest reset completed.");
    }

    // ====== Отправка сообщений ======

    public void sendTaskProgressMessage(Player player, QuestTask task, int progress) {
        var msg = messagesConfig.taskProgress();
        MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(), msg, Map.of(
                "task", task.displayName(),
                "progress", String.valueOf(progress),
                "target", String.valueOf(task.targetAmount())
        ));
    }

    public void sendTaskCompletedMessage(Player player, QuestTask task) {
        var msg = messagesConfig.taskCompleted();
        MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(), msg, Map.of(
                "task", task.displayName()
        ));
    }

    private void sendWeekCompletedMessage(Player player, QuestWeek week, int questPoints, int exp) {
        var lines = messagesConfig.weekCompleted();
        // Для списка строк префикс добавляем к первой строке
        if (!lines.isEmpty()) {
            var firstLine = messagesConfig.prefix() + lines.get(0);
            var remaining = lines.subList(1, lines.size());
            var allLines = new java.util.ArrayList<String>();
            allLines.add(firstLine);
            allLines.addAll(remaining);
            MiniMessageUtil.sendMessageList(player, allLines, Map.of(
                    "week", MiniMessageUtil.toPlainText(week.displayName()),
                    "quest-points", String.valueOf(questPoints),
                    "exp", String.valueOf(exp)
            ));
        }
    }

    private void sendLevelUpMessage(Player player, int level) {
        var lines = messagesConfig.levelUp();
        MiniMessageUtil.sendMessageList(player, lines, Map.of(
                "level", String.valueOf(level)
        ));
    }

    // ====== Выдача наград ======

    /**
     * Выдаёт игроку список наград.
     * <p>
     * Поддерживает два типа:
     * <ul>
     *   <li><b>Предметные:</b> создаёт ItemStack и кладёт в инвентарь (если полон — на землю)</li>
     *   <li><b>Командные:</b> выполняет команду от имени консоли с плейсхолдером %player%</li>
     * </ul>
     */
    private void giveItemRewards(Player player, java.util.List<ItemReward> rewards) {
        if (rewards == null || rewards.isEmpty()) return;

        var world = player.getWorld();
        var location = player.getLocation();
        var playerName = player.getName();

        for (var reward : rewards) {
            if (reward.isCommand()) {
                // Командная награда — выполняем от консоли
                var command = reward.command().replace("%player%", playerName);
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            } else if (reward.isItem()) {
                // Предметная награда — выдаём в инвентарь
                var itemStack = new ItemStack(reward.material(), reward.amount());
                var remaining = player.getInventory().addItem(itemStack);

                // Если не влезло в инвентарь — выпадает на землю
                for (var entry : remaining.entrySet()) {
                    world.dropItemNaturally(location, entry.getValue());
                }
            }
        }
    }

}
