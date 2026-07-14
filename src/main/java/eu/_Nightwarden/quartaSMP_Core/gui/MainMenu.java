package eu._Nightwarden.quartaSMP_Core.gui;

import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Главное меню плагина (/quartasmp).
 * Содержит кнопки: Уровень, Квесты, Магазин.
 * На кнопке "Квесты" — инфо о текущей и следующей неделе.
 *
 * 🎨 Дизайн:
 * - Тёмная рамка (BLACK_STAINED_GLASS_PANE)
 * - Фон: чередование PURPLE и BLUE стекла для глубины
 * - Кнопки с яркими градиентами и иконками
 * - Заголовок с градиентом
 */
public final class MainMenu extends BaseMenu {

    private final FileConfiguration config;
    private final PlayerDataManager playerDataManager;
    private final QuestManager questManager;
    private final QuestsConfig questsConfig;

    public MainMenu(Player player, FileConfiguration config, PlayerDataManager playerDataManager,
                    QuestManager questManager, QuestsConfig questsConfig) {
        super(
                player,
                config.getString("menu.title", "<gradient:#FF6B6B:#4ECDC4>✦ QuartaSMP Core ✦</gradient>"),
                config.getInt("menu.size", 27),
                config
        );
        this.config = config;
        this.playerDataManager = playerDataManager;
        this.questManager = questManager;
        this.questsConfig = questsConfig;
    }

    @Override
    protected String getMenuId() {
        return "main";
    }

    @Override
    protected void buildInventory() {
        var itemsSection = config.getConfigurationSection("menu.items");
        if (itemsSection == null) return;

        var data = playerDataManager.get(player.getUniqueId());

        for (var key : itemsSection.getKeys(false)) {
            var itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            var slot = itemSection.getInt("slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) continue;

            var materialStr = itemSection.getString("material", "STONE");
            var material = Material.getMaterial(materialStr.toUpperCase());
            if (material == null) material = Material.STONE;

            var displayName = itemSection.getString("display-name", key);
            var lore = new ArrayList<>(itemSection.getStringList("lore"));
            var action = itemSection.getString("action", "");

            // Если это кнопка "Квесты" — добавляем инфо о неделях
            if ("open_quests".equals(action)) {
                addQuestInfoToLore(lore, data);
            }

            var placeholders = getPlaceholders(data);

            var item = ItemBuilder.of(material)
                    .name(displayName, placeholders)
                    .lore(lore, placeholders)
                    .pdcString("action", action)
                    .build();

            inventory.setItem(slot, item);
        }

        // 🎨 Красивый дизайн: чёрная рамка + градиентный фон (чередование цветов)
        fillGradientBorder();
    }

    /**
     * Заполняет фон с красивым градиентным эффектом.
     * Рамка — чёрная, внутренний фон — чередование тёмно-синего и тёмно-фиолетового.
     */
    private void fillGradientBorder() {
        int rows = size / 9;
        var borderItem = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<black> </black>")
                .build();

        var darkPurple = ItemBuilder.of(Material.PURPLE_STAINED_GLASS_PANE)
                .name("<dark_purple> </dark_purple>")
                .build();

        var darkBlue = ItemBuilder.of(Material.BLUE_STAINED_GLASS_PANE)
                .name("<dark_blue> </dark_blue>")
                .build();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                if (inventory.getItem(slot) != null) continue;

                boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;
                if (isBorder) {
                    inventory.setItem(slot, borderItem);
                } else {
                    // Чередование: чётные ряды — фиолетовый, нечётные — синий
                    inventory.setItem(slot, (row % 2 == 0) ? darkPurple : darkBlue);
                }
            }
        }
    }

    /**
     * Добавляет в лор кнопки "Квесты" информацию о текущей неделе и таймер.
     * Поддерживает все режимы: global-schedule, schedule, sequential, all-open, admin-only.
     */
    private void addQuestInfoToLore(List<String> lore, PlayerData data) {
        var weeks = questsConfig.getWeeksList();
        if (weeks.isEmpty()) return;

        lore.add("");

        // Ищем первую незавершённую неделю
        int currentWeekIndex = -1;
        for (int i = 0; i < weeks.size(); i++) {
            if (data == null || !data.isWeekCompleted(weeks.get(i).id())) {
                currentWeekIndex = i;
                break;
            }
        }

        if (currentWeekIndex == -1) {
            // Все недели завершены
            lore.add("<green><bold>★ Все недели пройдены!</bold></green>");
            lore.add("<gray>Спасибо за прохождение!</gray>");
            return;
        }

        var currentWeek = weeks.get(currentWeekIndex);
        var currentWeekName = MiniMessageUtil.toPlainText(currentWeek.displayName());

        // 📖 Текущая неделя
        lore.add("<gray>📖 Сейчас: <green>" + currentWeekName + "</green></gray>");

        // ✅ Прогресс задач на текущей неделе
        if (data != null) {
            int completedTasks = 0;
            int totalTasks = currentWeek.tasks().size();
            for (var task : currentWeek.tasks()) {
                var progress = data.getTaskProgress(currentWeek.id(), task.id());
                if (task.isCompleted(progress)) {
                    completedTasks++;
                }
            }
            if (totalTasks > 0) {
                lore.add("<gray>✅ Прогресс: <yellow>" + completedTasks + "</yellow><gray>/</gray><yellow>" + totalTasks + "</yellow> <gray>задач</gray></gray>");
            }
        }

        // Добавляем информацию о следующей неделе и таймере
        var unlockMode = config.getString("quests.weeks-unlock-mode", "admin-only");

        if ("global-schedule".equalsIgnoreCase(unlockMode)) {
            int nextWeekIndex = currentWeekIndex + 1;
            if (nextWeekIndex < weeks.size()) {
                var nextWeek = weeks.get(nextWeekIndex);
                var nextWeekName = MiniMessageUtil.toPlainText(nextWeek.displayName());

                // Проверяем, открыта ли уже следующая неделя глобально
                if (data != null && questManager.isWeekUnlocked(data, nextWeekIndex)) {
                    lore.add("<gray>⏭ Следующая: <aqua>" + nextWeekName + "</aqua> <green>✓ доступна</green></gray>");
                    lore.add("<gray>🔒 Заверши текущую неделю, чтобы перейти дальше</gray>");
                } else {
                    lore.add("<gray>⏭ Следующая: <aqua>" + nextWeekName + "</aqua></gray>");
                    // Таймер до глобального открытия
                    if (data != null) {
                        var remaining = questManager.getTimeUntilUnlock(data, nextWeekIndex);
                        if (remaining > 0) {
                            lore.add("<gray>⏰ До открытия: <yellow>" + formatDuration(remaining) + "</yellow></gray>");
                        } else {
                            lore.add("<gray>⏰ Откроется <green>скоро</green></gray>");
                        }
                    }
                }
            } else {
                lore.add("<green>🏁 Это последняя неделя!</green>");
            }
        } else if ("schedule".equalsIgnoreCase(unlockMode)) {
            // Следующая неделя (если есть)
            int nextWeekIndex = currentWeekIndex + 1;
            if (nextWeekIndex < weeks.size()) {
                var nextWeek = weeks.get(nextWeekIndex);
                var nextWeekName = MiniMessageUtil.toPlainText(nextWeek.displayName());

                // Проверяем, открыта ли уже следующая неделя
                if (data != null && questManager.isWeekUnlocked(data, nextWeekIndex)) {
                    lore.add("<gray>⏭ Следующая: <aqua>" + nextWeekName + "</aqua> <green>✓ доступна</green></gray>");
                } else {
                    lore.add("<gray>⏭ Следующая: <aqua>" + nextWeekName + "</aqua></gray>");

                    // Таймер до открытия
                    if (data != null) {
                        var remaining = questManager.getTimeUntilUnlock(data, nextWeekIndex);
                        if (remaining > 0) {
                            lore.add("<gray>⏰ До открытия: <yellow>" + formatDuration(remaining) + "</yellow></gray>");
                        } else {
                            lore.add("<gray>⏰ Откроется <green>скоро</green></gray>");
                        }
                    }
                }
            } else {
                lore.add("<green>🏁 Это последняя неделя!</green>");
            }
        } else if ("sequential".equalsIgnoreCase(unlockMode)) {
            lore.add("<gray>⏭ Следующая неделя откроется после завершения текущей</gray>");
        } else if ("all-open".equalsIgnoreCase(unlockMode)) {
            lore.add("<green>Все недели доступны</green>");
        }
    }

    /**
     * Форматирует миллисекунды в читаемый вид: Xд Yч Zм
     */
    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        var sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м");
        if (sb.isEmpty()) sb.append("0м");

        return sb.toString().trim();
    }


    private Map<String, String> getPlaceholders(PlayerData data) {
        if (data == null) {
            return Map.of("level", "?", "exp", "?", "exp-needed", "?");
        }
        var baseExp = config.getInt("leveling.exp-formula.base-exp", 100);
        var multiplier = config.getInt("leveling.exp-formula.multiplier-per-level", 50);
        var expNeeded = baseExp + (data.level() * multiplier);
        return Map.of(
                "level", String.valueOf(data.level()),
                "exp", String.valueOf(data.exp()),
                "exp-needed", String.valueOf(expNeeded)
        );
    }
}
