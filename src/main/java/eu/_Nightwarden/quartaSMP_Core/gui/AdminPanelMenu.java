package eu._Nightwarden.quartaSMP_Core.gui;

import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.LeaderboardEntry;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Админ-панель прогресса игроков.
 * Доступна только с правом quartasmp.admin.
 * Показывает список игроков, их уровень, текущую неделю и прогресс.
 * <p>
 * Размер: 54 слота (6 строк).
 * - Верхняя строка: статистика сервера
 * - Строки 2-5: список игроков (с пагинацией)
 * - Нижняя строка: навигация
 */
public final class AdminPanelMenu extends BaseMenu {

    private static final int PLAYERS_PER_PAGE = 28; // 4 строки по 7 слотов (пропуская рамку)

    private final PlayerDataManager playerDataManager;
    private final QuestManager questManager;
    private final QuestsConfig questsConfig;
    private final int page;

    public AdminPanelMenu(Player player, FileConfiguration config, PlayerDataManager playerDataManager,
                          QuestManager questManager, QuestsConfig questsConfig, int page) {
        super(
                player,
                "<gradient:#FF6B6B:#4ECDC4>✦ Админ-панель ✦</gradient>",
                54,
                config
        );
        this.playerDataManager = playerDataManager;
        this.questManager = questManager;
        this.questsConfig = questsConfig;
        this.page = Math.max(0, page);
    }

    @Override
    protected String getMenuId() {
        return "admin_panel";
    }

    @Override
    protected void buildInventory() {
        // Используем leaderboard cache — показывает всех игроков, включая оффлайн
        var leaderboardSnapshot = playerDataManager.getLeaderboardSnapshot();
        var sortedEntries = new ArrayList<>(leaderboardSnapshot.values().stream()
                .sorted(Comparator.<LeaderboardEntry, Integer>comparing(LeaderboardEntry::level, Comparator.reverseOrder())
                        .thenComparing(Comparator.comparingInt(LeaderboardEntry::exp).reversed())
                        .thenComparing(LeaderboardEntry::name))
                .toList());

        // Верхняя строка — статистика сервера
        buildStatsRow(sortedEntries);

        // Список игроков с пагинацией
        buildPlayersList(sortedEntries);

        // Нижняя строка — навигация
        buildNavigationRow(sortedEntries.size());
    }

    /**
     * Строка 0: общая статистика сервера.
     */
    private void buildStatsRow(List<LeaderboardEntry> sortedEntries) {
        var totalPlayers = sortedEntries.size();
        var avgLevel = sortedEntries.stream()
                .mapToInt(LeaderboardEntry::level)
                .average()
                .orElse(0);
        var totalWeeks = questsConfig.getWeeksList().size();

        // Считаем завершённые недели только у онлайн-игроков (у кого есть полные данные)
        long totalCompletedWeeks = 0;
        for (var entry : sortedEntries) {
            var data = playerDataManager.get(entry.uuid());
            if (data != null) {
                totalCompletedWeeks += data.completedWeeks().size();
            }
        }

        var statsItem = ItemBuilder.of(Material.NETHER_STAR)
                .name("<gradient:#FFD700:#FFA500>📊 Статистика сервера</gradient>")
                .lore(List.of(
                        "",
                        "<gray>👥 Всего игроков: <yellow>" + totalPlayers + "</yellow></gray>",
                        "<gray>🏆 Средний уровень: <yellow>" + String.format("%.1f", avgLevel) + "</yellow></gray>",
                        "<gray>📖 Всего недель: <yellow>" + totalWeeks + "</yellow></gray>",
                        "<gray>✅ Всего завершено недель: <yellow>" + totalCompletedWeeks + "</yellow></gray>"
                ))
                .build();

        inventory.setItem(4, statsItem);

        // Заполняем остальные слоты первой строки рамкой
        fillRowBorder(0);
    }

    /**
     * Строки 1-4: список игроков с пагинацией.
     * Каждый игрок — PLAYER_HEAD с информацией.
     */
    private void buildPlayersList(List<LeaderboardEntry> sortedEntries) {
        var startIndex = page * PLAYERS_PER_PAGE;
        var endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, sortedEntries.size());

        if (startIndex >= sortedEntries.size()) {
            // Пустая страница — показываем заглушку
            var emptyItem = ItemBuilder.of(Material.BARRIER)
                    .name("<red>Нет игроков</red>")
                    .build();
            inventory.setItem(22, emptyItem);
            return;
        }

        int slot = 10; // Начальный слот (первый слот второй строки, пропуская рамку)
        for (int i = startIndex; i < endIndex; i++) {
            var lbEntry = sortedEntries.get(i);

            // Пропускаем слоты рамки (col 0, 8 и row 0, 5)
            while (isBorderSlot(slot)) {
                slot++;
                if (slot >= 45) break; // Не выходим за пределы строк 1-4
            }
            if (slot >= 45) break;

            var headItem = createPlayerHead(lbEntry);
            inventory.setItem(slot, headItem);
            slot++;
        }

        // Заполняем оставшиеся слоты рамкой
        for (int s = 9; s < 45; s++) {
            if (inventory.getItem(s) == null) {
                inventory.setItem(s, getFillItem());
            }
        }
    }

    /**
     * Строка 5 (слоты 45-53): навигация.
     */
    private void buildNavigationRow(int totalPlayers) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPlayers / PLAYERS_PER_PAGE));

        // Кнопка "Назад" (в главное меню)
        var backItem = ItemBuilder.of(Material.ARROW)
                .name("<gradient:#ff6b6b:#ffa500>← Назад</gradient>")
                .lore(List.of("<gray>Вернуться в главное меню</gray>"))
                .pdcString("action", "back")
                .pdcString("back_target", "main")
                .build();
        inventory.setItem(45, backItem);

        // Информация о странице
        var pageInfo = ItemBuilder.of(Material.PAPER)
                .name("<aqua>Страница " + (page + 1) + " / " + totalPages + "</aqua>")
                .lore(List.of(
                        "<gray>Всего игроков: <yellow>" + totalPlayers + "</yellow></gray>",
                        "",
                        "<gray>ЛКМ — открыть игрока</gray>"
                ))
                .build();
        inventory.setItem(49, pageInfo);

        // Кнопка "Обновить"
        var refreshItem = ItemBuilder.of(Material.CLOCK)
                .name("<green>🔄 Обновить</green>")
                .lore(List.of("<gray>Обновить список игроков</gray>"))
                .pdcString("action", "admin_refresh")
                .build();
        inventory.setItem(50, refreshItem);

        // Кнопка "Вперёд" (если есть следующая страница)
        if (page + 1 < totalPages) {
            var nextItem = ItemBuilder.of(Material.ARROW)
                    .name("<gradient:#4ECDC4:#FF6B6B>Вперёд →</gradient>")
                    .lore(List.of("<gray>Следующая страница</gray>"))
                    .pdcString("action", "admin_next_page")
                    .pdcInt("admin_page", page + 1)
                    .build();
            inventory.setItem(53, nextItem);
        }

        // Заполняем пустые слоты навигации
        for (int s = 45; s < 54; s++) {
            if (inventory.getItem(s) == null) {
                inventory.setItem(s, getNavFillItem());
            }
        }
    }

    /**
     * Создаёт голову игрока с информацией о прогрессе.
     * Для онлайн-игроков показывает полную информацию (недели, задачи).
     * Для оффлайн — только уровень и опыт из leaderboard cache.
     */
    private org.bukkit.inventory.ItemStack createPlayerHead(LeaderboardEntry lbEntry) {
        var uuid = lbEntry.uuid();
        var playerName = lbEntry.name();
        var headItem = ItemBuilder.of(Material.PLAYER_HEAD)
                .name("<gradient:#00FFAA:#00AAFF>👤 " + playerName + "</gradient>");

        var lore = new ArrayList<String>();
        lore.add("");

        // Уровень и опыт (всегда доступны из leaderboard cache)
        var baseExp = config.getInt("leveling.exp-formula.base-exp", 100);
        var multiplier = config.getInt("leveling.exp-formula.multiplier-per-level", 50);
        var expNeeded = baseExp + (lbEntry.level() * multiplier);
        lore.add("<gray>⭐ Уровень: <yellow>" + lbEntry.level() + "</yellow></gray>");
        lore.add("<gray>✨ Опыт: <yellow>" + lbEntry.exp() + "</yellow><gray>/</gray><yellow>" + expNeeded + "</yellow></gray>");

        // Пытаемся получить полные данные из кэша (только для онлайн-игроков)
        var data = playerDataManager.get(uuid);
        if (data != null) {
            // Текущая неделя и прогресс
            var weeks = questsConfig.getWeeksList();
            int currentWeekIndex = -1;
            for (int i = 0; i < weeks.size(); i++) {
                if (!data.isWeekCompleted(weeks.get(i).id())) {
                    currentWeekIndex = i;
                    break;
                }
            }

            lore.add("");
            if (currentWeekIndex == -1) {
                lore.add("<green><bold>★ Все недели пройдены!</bold></green>");
            } else {
                var currentWeek = weeks.get(currentWeekIndex);
                var weekName = MiniMessageUtil.toPlainText(currentWeek.displayName());

                // Считаем прогресс задач
                int completedTasks = 0;
                int totalTasks = currentWeek.tasks().size();
                for (var task : currentWeek.tasks()) {
                    var progress = data.getTaskProgress(currentWeek.id(), task.id());
                    if (task.isCompleted(progress)) {
                        completedTasks++;
                    }
                }

                lore.add("<gray>📖 Текущая неделя: <green>" + weekName + "</green></gray>");
                if (totalTasks > 0) {
                    lore.add("<gray>✅ Прогресс: <yellow>" + completedTasks + "</yellow><gray>/</gray><yellow>" + totalTasks + "</yellow> <gray>задач</gray></gray>");
                }
                lore.add("<gray>🏁 Завершено недель: <yellow>" + data.completedWeeks().size() + "</yellow><gray>/</gray><yellow>" + weeks.size() + "</yellow></gray>");
            }
        } else {
            lore.add("");
            lore.add("<gray><italic>⚫ Игрок оффлайн</italic></gray>");
        }

        lore.add("");
        lore.add("<gray><italic>ЛКМ — открыть подробности</italic></gray>");

        var item = headItem
                .lore(lore)
                .pdcString("action", "admin_open_player")
                .pdcString("admin_player_uuid", uuid.toString())
                .build();

        // Устанавливаем скин головы
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            var offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            skullMeta.setPlayerProfile(offlinePlayer.getPlayerProfile());
            item.setItemMeta(skullMeta);
        }

        return item;
    }

    /**
     * Проверяет, является ли слот рамкой (col 0, 8 или row 0, 5).
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

    /**
     * Предмет для заполнения пустых слотов в списке игроков.
     */
    private org.bukkit.inventory.ItemStack getFillItem() {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("<dark_gray> </dark_gray>")
                .build();
    }

    /**
     * Предмет для заполнения пустых слотов навигации.
     */
    private org.bukkit.inventory.ItemStack getNavFillItem() {
        return ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<black> </black>")
                .build();
    }
}
