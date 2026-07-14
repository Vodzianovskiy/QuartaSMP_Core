package eu._Nightwarden.quartaSMP_Core.command;

import eu._Nightwarden.quartaSMP_Core.QuartaSMP_Core;
import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.gui.AdminPanelMenu;
import eu._Nightwarden.quartaSMP_Core.gui.MainMenu;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Обработчик команды /quartasmp (алиасы: /qsmp, /q).
 * Открывает главное GUI-меню.
 *
 * Подкоманды:
 *   /quartasmp help — справка
 *   /quartasmp reload — перезагрузка конфигов (admin)
 *   /quartasmp unlockweek <weekId> [player] — открыть неделю игроку (admin)
 *   /quartasmp setweek <weekId> [player] — установить текущую неделю для тестов (admin)
 */
public final class QuartaSMPCommand implements CommandExecutor, TabCompleter {

    private final QuartaSMP_Core plugin;
    private final FileConfiguration config;
    private final PlayerDataManager playerDataManager;
    private final MessagesConfig messagesConfig;

    public QuartaSMPCommand(QuartaSMP_Core plugin, FileConfiguration config,
                            PlayerDataManager playerDataManager,
                            MessagesConfig messagesConfig) {
        this.plugin = plugin;
        this.config = config;
        this.playerDataManager = playerDataManager;
        this.messagesConfig = messagesConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            handleConsoleCommand(sender, args);
            return true;
        }

        // Для обычного игрока старое поведение /q не меняем.

        // Обработка подкоманд
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    if (!player.hasPermission("quartasmp.admin")) {
                        MiniMessageUtil.sendMessage(player, messagesConfig.noPermission());
                        return true;
                    }
                    plugin.reloadConfigs();
                    MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(), messagesConfig.reloadSuccess());
                    return true;
                }
                case "unlockweek" -> {
                    if (!player.hasPermission("quartasmp.admin")) {
                        MiniMessageUtil.sendMessage(player, messagesConfig.noPermission());
                        return true;
                    }
                    handleUnlockWeek(player, args);
                    return true;
                }
                case "setweek" -> {
                    if (!player.hasPermission("quartasmp.admin")) {
                        MiniMessageUtil.sendMessage(player, messagesConfig.noPermission());
                        return true;
                    }
                    handleSetWeek(player, args);
                    return true;
                }
                case "addexp" -> {
                    if (!player.hasPermission("quartasmp.admin")) {
                        MiniMessageUtil.sendMessage(player, messagesConfig.noPermission());
                        return true;
                    }
                    handleAddexp(player, args);
                    return true;
                }
                case "admin" -> {
                    if (!player.hasPermission("quartasmp.admin")) {
                        MiniMessageUtil.sendMessage(player, messagesConfig.noPermission());
                        return true;
                    }
                    var adminPanel = new AdminPanelMenu(player, config, playerDataManager,
                            plugin.getQuestManager(), plugin.getQuestsConfig(), 0);
                    adminPanel.open();
                    return true;
                }
                case "help" -> {
                    sendHelp(player);
                    return true;
                }
            }
        }

        // Открываем главное меню
        var menu = new MainMenu(player, config, playerDataManager,
                plugin.getQuestManager(), plugin.getQuestsConfig());
        menu.open();
        return true;
    }

    /**
     * Обрабатывает вызов из консоли/NPC.
     * Формат для NPC: /npc action Quest any_click add console_command q {player}
     * В итоге консоль выполняет: q <ник>, а плагин открывает этому игроку главное меню.
     */
    private void handleConsoleCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(messagesConfig.playerOnly());
            return;
        }

        var target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = Bukkit.getPlayer(args[0]);
        }

        if (target == null) {
            sender.sendMessage("Player not found or offline: " + args[0]);
            return;
        }

        var menu = new MainMenu(target, config, playerDataManager,
                plugin.getQuestManager(), plugin.getQuestsConfig());
        menu.open();
    }

    /**
     * Обрабатывает /quartasmp unlockweek <weekId> [player]
     * Открывает указанную неделю игроку (или себе, если player не указан).
     *
     * Логика: все недели ДО weekId отмечаются завершёнными,
     * сама weekId НЕ завершается — она становится активной.
     */
    private void handleUnlockWeek(Player sender, String[] args) {
        if (args.length < 2) {
            MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(), messagesConfig.unlockUsage());
            return;
        }

        var weekId = args[1];

        // Проверяем, существует ли такая неделя
        var weeks = plugin.getQuestsConfig().getWeeks();
        if (!weeks.containsKey(weekId)) {
            MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                    messagesConfig.weekNotFound(), Map.of("week", weekId));
            return;
        }

        // Определяем цель
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                        messagesConfig.playerNotFound(), Map.of("player", args[2]));
                return;
            }
        } else {
            target = sender;
        }

        // Открываем неделю: отмечаем предыдущие как завершённые (если есть)
        var weeksList = plugin.getQuestsConfig().getWeeksList();
        var weekIndex = IntStream.range(0, weeksList.size()).filter(i -> weeksList.get(i).id().equals(weekId)).findFirst().orElse(-1);

        var targetUuid = target.getUniqueId();

        playerDataManager.executeAtomic(targetUuid, data -> {
            if (data == null) return null;

            var updated = data;

            // Отмечаем все недели ДО указанной как завершённые
            for (int i = 0; i < weekIndex; i++) {
                var w = weeksList.get(i);
                if (!updated.isWeekCompleted(w.id())) {
                    updated = updated.withWeekCompleted(w.id());
                }
            }

            // 🔥 ФИКС: саму weekId НЕ завершаем, только открываем доступ
            // Если она была случайно завершена — сбрасываем
            if (updated.isWeekCompleted(weekId)) {
                updated = updated.resetWeekProgress(weekId);
            }

            return updated;
        });

        MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                messagesConfig.unlockSuccess(), Map.of(
                        "week", weekId,
                        "player", target.getName()
                ));
    }

    /**
     * Обрабатывает /quartasmp setweek <weekId> [player]
     * Устанавливает указанную неделю как текущую:
     * - Сбрасывает прогресс по всем неделям
     * - Отмечает все недели ДО указанной как завершённые
     * - Указанную неделю НЕ отмечает как завершённую
     * Полезно для тестирования конкретной недели.
     */
    private void handleSetWeek(Player sender, String[] args) {
        if (args.length < 2) {
            MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(), messagesConfig.setweekUsage());
            return;
        }

        var weekId = args[1];

        // Проверяем, существует ли такая неделя
        var weeks = plugin.getQuestsConfig().getWeeks();
        if (!weeks.containsKey(weekId)) {
            MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                    messagesConfig.weekNotFound(), Map.of("week", weekId));
            return;
        }

        // Определяем цель
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                        messagesConfig.playerNotFound(), Map.of("player", args[2]));
                return;
            }
        } else {
            target = sender;
        }

        var weeksList = plugin.getQuestsConfig().getWeeksList();
        var weekIndex = IntStream.range(0, weeksList.size()).filter(i -> weeksList.get(i).id().equals(weekId)).findFirst().orElse(-1);
        var targetUuid = target.getUniqueId();

        playerDataManager.executeAtomic(targetUuid, data -> {
            if (data == null) return null;

            var updated = data;

            // Сбрасываем прогресс по всем неделям
            for (var w : weeksList) {
                updated = updated.resetWeekProgress(w.id());
            }

            // Отмечаем все недели ДО указанной как завершённые
            for (int i = 0; i < weekIndex; i++) {
                var w = weeksList.get(i);
                if (!updated.isWeekCompleted(w.id())) {
                    updated = updated.withWeekCompleted(w.id());
                }
            }

            // Убеждаемся что указанная неделя НЕ завершена
            if (updated.isWeekCompleted(weekId)) {
                updated = updated.resetWeekProgress(weekId);
            }

            // ⚠️ НЕ сдвигаем firstJoinTime — в global-schedule режиме
            // расписание глобальное и не зависит от первого входа игрока.
            // В schedule режиме тоже не сдвигаем, так как setweek — тестовая
            // команда, которая просто устанавливает completedWeeks.

            return updated;
        });
        // 🔥 Принудительное сохранение на диск — гарантирует, что данные
        // не потеряются при plugman reload (который выгружает плагин мгновенно)
        playerDataManager.save(targetUuid);

        MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                messagesConfig.setweekSuccess(), Map.of(
                        "week", weekId,
                        "player", target.getName()
                ));

        // 🔥 Автоматически открываем меню квестов ЦЕЛИ, чтобы она сразу видела результат
        var questMenu = new eu._Nightwarden.quartaSMP_Core.gui.QuestMenu(
                target,
                plugin.getQuestsConfig(),
                messagesConfig,
                playerDataManager,
                plugin.getQuestManager(),
                config
        );
        questMenu.open();
    }

    /**
     * Обрабатывает /quartasmp addexp <amount> [player]
     * Добавляет опыт игроку (или себе, если player не указан).
     * Если опыта хватает для повышения уровня — повышает.
     */
    private void handleAddexp(Player sender, String[] args) {
        if (args.length < 2) {
            MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(), messagesConfig.addexpUsage());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(), messagesConfig.addexpInvalidAmount());
            return;
        }

        // Определяем цель
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                        messagesConfig.playerNotFound(), Map.of("player", args[2]));
                return;
            }
        } else {
            target = sender;
        }

        var targetUuid = target.getUniqueId();

        playerDataManager.executeAtomic(targetUuid, data -> {
            if (data == null) return null;

            return playerDataManager.addExpWithLevelUp(data, amount, target);
        });

        MiniMessageUtil.sendPrefixedMessage(sender, messagesConfig.prefix(),
                messagesConfig.addexpSuccess(), Map.of(
                        "amount", String.valueOf(amount),
                        "player", target.getName()
                ));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            var completions = new ArrayList<String>();
            completions.add("help");
            if (sender.hasPermission("quartasmp.admin")) {
                completions.add("admin");
                completions.add("reload");
                completions.add("unlockweek");
                completions.add("setweek");
                completions.add("addexp");
            }
            return completions;
        }
        if (args.length == 2 && ("unlockweek".equalsIgnoreCase(args[0]) || "setweek".equalsIgnoreCase(args[0]))) {
            // Предлагаем ID недель
            return List.copyOf(plugin.getQuestsConfig().getWeeks().keySet());
        }
        if (args.length == 2 && "addexp".equalsIgnoreCase(args[0])) {
            // Предлагаем количество опыта
            return List.of("100", "500", "1000", "5000");
        }
        if (args.length == 3 && ("unlockweek".equalsIgnoreCase(args[0]) || "setweek".equalsIgnoreCase(args[0]) || "addexp".equalsIgnoreCase(args[0]))) {
            // Предлагаем имена игроков онлайн
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }
        return List.of();
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

    private void sendHelp(Player player) {
        var p = messagesConfig.prefix();
        MiniMessageUtil.sendPrefixedMessage(player, p, messagesConfig.helpHeader());
        MiniMessageUtil.sendPrefixedMessage(player, p, messagesConfig.helpMain());
        MiniMessageUtil.sendPrefixedMessage(player, p, messagesConfig.helpHelp());
        MiniMessageUtil.sendPrefixedMessage(player, p, messagesConfig.helpReload());
        MiniMessageUtil.sendPrefixedMessage(player, p, messagesConfig.helpUnlockweek());
        MiniMessageUtil.sendPrefixedMessage(player, p, messagesConfig.helpSetweek());
        MiniMessageUtil.sendPrefixedMessage(player, p, messagesConfig.helpAddexp());
    }
}
