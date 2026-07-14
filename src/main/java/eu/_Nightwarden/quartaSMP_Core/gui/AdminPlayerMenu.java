package eu._Nightwarden.quartaSMP_Core.gui;

import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestWeek;
import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Подробное GUI прогресса конкретного игрока для админа.
 * Показывает профиль игрока и список всех недель с прогрессом задач.
 * <p>
 * Размер: 54 слота (6 строк).
 * - Слот 4: профиль игрока
 * - Строки 1-4: список недель
 * - Строка 5: навигация
 */
public final class AdminPlayerMenu extends BaseMenu {

    private final PlayerDataManager playerDataManager;
    private final QuestManager questManager;
    private final QuestsConfig questsConfig;
    private final UUID targetUuid;

    public AdminPlayerMenu(Player admin, FileConfiguration config, PlayerDataManager playerDataManager,
                           QuestManager questManager, QuestsConfig questsConfig, UUID targetUuid) {
        super(
                admin,
                "<gradient:#FF6B6B:#4ECDC4>✦ Прогресс: " + getPlayerName(targetUuid) + " ✦</gradient>",
                54,
                config
        );
        this.playerDataManager = playerDataManager;
        this.questManager = questManager;
        this.questsConfig = questsConfig;
        this.targetUuid = targetUuid;
    }

    @Override
    protected String getMenuId() {
        return "admin_player";
    }

    @Override
    protected void buildInventory() {
        var data = playerDataManager.get(targetUuid);
        if (data == null) {
            // Игрок не найден в кэше
            var errorItem = ItemBuilder.of(Material.BARRIER)
                    .name("<red>Игрок не найден</red>")
                    .lore(List.of("<gray>Данные игрока отсутствуют в кэше</gray>"))
                    .build();
            inventory.setItem(22, errorItem);
            addBackButton("admin_panel");
            return;
        }

        // Слот 4 — профиль игрока
        buildPlayerProfile(data);

        // Строки 1-4 — список недель
        buildWeeksList(data);

        // Строка 5 — навигация
        buildNavigation();
    }

    /**
     * Слот 4: профиль игрока с общей информацией.
     */
    private void buildPlayerProfile(PlayerData data) {
        var playerName = getPlayerName(targetUuid);
        var headItem = ItemBuilder.of(Material.PLAYER_HEAD)
                .name("<gradient:#00FFAA:#00AAFF>👤 " + playerName + "</gradient>");

        var baseExp = config.getInt("leveling.exp-formula.base-exp", 100);
        var multiplier = config.getInt("leveling.exp-formula.multiplier-per-level", 50);
        var expNeeded = baseExp + (data.level() * multiplier);

        var weeks = questsConfig.getWeeksList();
        int currentWeekIndex = -1;
        for (int i = 0; i < weeks.size(); i++) {
            if (!data.isWeekCompleted(weeks.get(i).id())) {
                currentWeekIndex = i;
                break;
            }
        }

        var lore = new ArrayList<String>();
        lore.add("");
        lore.add("<gray>⭐ Уровень: <yellow>" + data.level() + "</yellow></gray>");
        lore.add("<gray>✨ Опыт: <yellow>" + data.exp() + "</yellow><gray>/</gray><yellow>" + expNeeded + "</yellow></gray>");
        lore.add("<gray>🏁 Завершено недель: <yellow>" + data.completedWeeks().size() + "</yellow><gray>/</gray><yellow>" + weeks.size() + "</yellow></gray>");

        if (currentWeekIndex == -1) {
            lore.add("");
            lore.add("<green><bold>★ Все недели пройдены!</bold></green>");
        } else {
            var currentWeek = weeks.get(currentWeekIndex);
            var weekName = MiniMessageUtil.toPlainText(currentWeek.displayName());
            lore.add("<gray>📖 Текущая неделя: <green>" + weekName + "</green></gray>");
        }

        var item = headItem.lore(lore).build();

        // Устанавливаем скин головы
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            var offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);
            skullMeta.setPlayerProfile(offlinePlayer.getPlayerProfile());
            item.setItemMeta(skullMeta);
        }

        inventory.setItem(4, item);

        // Заполняем рамку первой строки
        fillRowBorder(0);
    }

    /**
     * Строки 1-4: список всех недель с прогрессом задач.
     */
    private void buildWeeksList(PlayerData data) {
        var weeks = questsConfig.getWeeksList();
        int slot = 9; // Начало второй строки

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (slot >= 45) break; // Не выходим за пределы строк 1-4

            // Пропускаем слоты рамки
            while (slot < 45 && isBorderSlot(slot)) {
                slot++;
            }
            if (slot >= 45) break;

            var weekItem = createWeekItem(week, data, weekIndex);
            inventory.setItem(slot, weekItem);
            slot++;
        }

        // Заполняем оставшиеся слоты
        for (int s = 9; s < 45; s++) {
            if (inventory.getItem(s) == null) {
                inventory.setItem(s, getFillItem());
            }
        }
    }

    /**
     * Создаёт предмет недели с прогрессом задач.
     */
    private org.bukkit.inventory.ItemStack createWeekItem(QuestWeek week, PlayerData data, int weekIndex) {
        var isCompleted = data.isWeekCompleted(week.id());
        var isUnlocked = questManager.isWeekUnlocked(data, weekIndex);
        var weeks = questsConfig.getWeeksList();

        // Определяем статус недели
        int currentWeekIndex = -1;
        for (int i = 0; i < weeks.size(); i++) {
            if (!data.isWeekCompleted(weeks.get(i).id())) {
                currentWeekIndex = i;
                break;
            }
        }

        boolean isCurrent = (weekIndex == currentWeekIndex);

        Material material;
        String statusColor;
        String statusText;

        if (isCompleted) {
            material = Material.LIME_CONCRETE;
            statusColor = "<green>";
            statusText = "✅ Завершена";
        } else if (isCurrent) {
            material = Material.YELLOW_CONCRETE;
            statusColor = "<yellow>";
            statusText = "🟡 Текущая";
        } else if (isUnlocked) {
            material = Material.ORANGE_CONCRETE;
            statusColor = "<aqua>";
            statusText = "🔓 Открыта";
        } else {
            material = Material.RED_CONCRETE;
            statusColor = "<red>";
            statusText = "🔒 Закрыта";
        }

        var weekName = MiniMessageUtil.toPlainText(week.displayName());
        var lore = new ArrayList<String>();
        lore.add("");
        lore.add(statusColor + statusText + "</" + statusColor.substring(1));

        // Прогресс задач
        var tasks = week.tasks();
        if (!tasks.isEmpty()) {
            int completedTasks = 0;
            for (var task : tasks) {
                var progress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(progress)) {
                    completedTasks++;
                }
            }
            lore.add("<gray>Прогресс: <yellow>" + completedTasks + "</yellow><gray>/</gray><yellow>" + tasks.size() + "</yellow> <gray>задач</gray></gray>");
            lore.add("");

            // Список задач
            for (var task : tasks) {
                var progress = data.getTaskProgress(week.id(), task.id());
                var taskName = MiniMessageUtil.toPlainText(task.displayName());
                if (task.isCompleted(progress)) {
                    lore.add("<green>✅ " + taskName + " — " + progress + "/" + task.targetAmount() + "</green>");
                } else if (progress > 0) {
                    lore.add("<yellow>🟡 " + taskName + " — " + progress + "/" + task.targetAmount() + "</yellow>");
                } else {
                    lore.add("<red>❌ " + taskName + " — 0/" + task.targetAmount() + "</red>");
                }
            }
        }

        return ItemBuilder.of(material)
                .name(statusColor + "<bold>" + weekName + "</bold></" + statusColor.substring(1))
                .lore(lore)
                .build();
    }

    /**
     * Строка 5: навигация.
     */
    private void buildNavigation() {
        // Кнопка "Назад" (в админ-панель)
        var backItem = ItemBuilder.of(Material.ARROW)
                .name("<gradient:#ff6b6b:#ffa500>← Назад к списку</gradient>")
                .lore(List.of("<gray>Вернуться в админ-панель</gray>"))
                .pdcString("action", "back")
                .pdcString("back_target", "admin_panel")
                .build();
        inventory.setItem(45, backItem);

        // Заполняем пустые слоты навигации
        for (int s = 45; s < 54; s++) {
            if (inventory.getItem(s) == null) {
                inventory.setItem(s, getNavFillItem());
            }
        }
    }

    /**
     * Проверяет, является ли слот рамкой.
     */
    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return col == 0 || col == 8 || row == 0 || row == 5;
    }

    /**
     * Заполняет рамку для указанной строки.
     */
    private void fillRowBorder(int row) {
        var borderItem = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<black> </black>")
                .build();
        for (int col = 0; col < 9; col++) {
            int slot = row * 9 + col;
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, borderItem);
            }
        }
    }

    private org.bukkit.inventory.ItemStack getFillItem() {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("<dark_gray> </dark_gray>")
                .build();
    }

    private org.bukkit.inventory.ItemStack getNavFillItem() {
        return ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<black> </black>")
                .build();
    }

    /**
     * Пытается получить имя игрока по UUID.
     */
    private static String getPlayerName(UUID uuid) {
        var player = Bukkit.getPlayer(uuid);
        if (player != null) return player.getName();
        var offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null) return offlinePlayer.getName();
        return uuid.toString().substring(0, 8) + "...";
    }
}
