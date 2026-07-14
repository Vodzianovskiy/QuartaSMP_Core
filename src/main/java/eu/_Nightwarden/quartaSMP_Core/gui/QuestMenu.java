package eu._Nightwarden.quartaSMP_Core.gui;

import eu._Nightwarden.quartaSMP_Core.QuartaSMP_Core;
import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestWeek;
import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * Меню списка недель квестов.
 * При открытии сразу находит первую незавершённую неделю
 * и открывает её задачи (QuestWeekMenu).
 * Если все недели пройдены — показывает сообщение.
 *
 * 🎨 Дизайн:
 * - Чёрная рамка
 * - Фон: чередование синего и голубого стекла
 * - Звезда Незера как символ завершения
 */
public final class QuestMenu extends BaseMenu {

    private static final int MENU_SIZE = 27;

    private final QuartaSMP_Core plugin;
    private final QuestsConfig questsConfig;
    private final MessagesConfig messagesConfig;
    private final PlayerDataManager playerDataManager;
    private final QuestManager questManager;

    public QuestMenu(Player player, QuestsConfig questsConfig, MessagesConfig messagesConfig,
                     PlayerDataManager playerDataManager, QuestManager questManager,
                     FileConfiguration config) {
        super(
                player,
                "<gradient:#00BFFF:#1E90FF>✦ Квесты</gradient>",
                MENU_SIZE,
                config
        );
        this.plugin = (QuartaSMP_Core) player.getServer().getPluginManager().getPlugin("QuartaSMP_Core");
        this.questsConfig = questsConfig;
        this.messagesConfig = messagesConfig;
        this.playerDataManager = playerDataManager;
        this.questManager = questManager;
    }

    @Override
    public void open() {
        var data = playerDataManager.get(player.getUniqueId());
        var weeks = questsConfig.getWeeksList();

        // Ищем первую незавершённую неделю
        QuestWeek firstIncompleteWeek = null;
        int firstIncompleteIndex = -1;

        for (int i = 0; i < weeks.size(); i++) {
            var week = weeks.get(i);
            if (data == null || !data.isWeekCompleted(week.id())) {
                firstIncompleteWeek = week;
                firstIncompleteIndex = i;
                break;
            }
        }

        if (firstIncompleteWeek == null) {
            // Все недели пройдены — показываем заглушку
            super.open();
            return;
        }

        // Если неделя доступна по unlockMode — сразу открываем её задачи
        if (data != null && questManager.isWeekUnlocked(data, firstIncompleteIndex)) {
            var weekMenu = new QuestWeekMenu(player, firstIncompleteWeek, messagesConfig,
                    playerDataManager, questManager, questsConfig, config);
            weekMenu.open();
            return;
        }

        // Если неделя ещё закрыта — показываем заглушку с таймером
        super.open();
    }

    @Override
    protected String getMenuId() {
        return "quests";
    }

    @Override
    protected void buildInventory() {
        var data = playerDataManager.get(player.getUniqueId());
        var weeks = questsConfig.getWeeksList();

        // Ищем первую незавершённую неделю
        QuestWeek firstIncompleteWeek = null;
        int firstIncompleteIndex = -1;

        for (int i = 0; i < weeks.size(); i++) {
            var week = weeks.get(i);
            if (data == null || !data.isWeekCompleted(week.id())) {
                firstIncompleteWeek = week;
                firstIncompleteIndex = i;
                break;
            }
        }

        if (firstIncompleteWeek == null) {
            // Все недели пройдены — показываем заглушку
            buildAllCompleted();
        } else if (data != null && !questManager.isWeekUnlocked(data, firstIncompleteIndex)) {
            // Первая незавершённая неделя ещё закрыта по unlockMode — показываем заглушку
            buildWeekLocked(data, firstIncompleteWeek, firstIncompleteIndex);
        } else {
            // Неделя доступна — показываем стандартную заглушку (не должно сюда попадать,
            // так как open() открывает QuestWeekMenu, но на всякий случай)
            buildAllCompleted();
        }

        // 🎨 Красивый дизайн: чёрная рамка + синий/голубой фон
        fillQuestBorder();
    }

    /**
     * Заглушка — все недели пройдены.
     */
    private void buildAllCompleted() {
        var lore = new ArrayList<String>();
        lore.add("");
        lore.add("<green><bold>★ Все недели пройдены!</bold></green>");
        lore.add("");
        lore.add("<gray>Спасибо за прохождение!</gray>");

        var item = ItemBuilder.of(Material.NETHER_STAR)
                .name("<gradient:#FFD700:#FFA500>✦ Все квесты завершены!</gradient>")
                .lore(lore)
                .build();

        inventory.setItem(13, item);
    }

    /**
     * Заглушка — следующая неделя ещё закрыта по расписанию.
     * Показывает иконку с часами и временем до открытия.
     */
    private void buildWeekLocked(PlayerData data, QuestWeek week, int weekIndex) {
        var timeUntilUnlock = questManager.getTimeUntilUnlock(data, weekIndex);

        var lore = new ArrayList<String>();
        lore.add("");
        lore.add("<gray>Следующая неделя ещё закрыта.</gray>");
        lore.add("");

        if (timeUntilUnlock > 0) {
            var formatted = formatTimeRemaining(timeUntilUnlock);
            lore.add("<yellow>⏳ Осталось: " + formatted + "</yellow>");
            lore.add("");
            lore.add("<dark_gray>Неделя откроется автоматически</dark_gray>");
            lore.add("<dark_gray>по расписанию сервера.</dark_gray>");
        } else {
            lore.add("<red>Неделя недоступна.</red>");
            lore.add("<dark_gray>Обратитесь к администратору.</dark_gray>");
        }

        var item = ItemBuilder.of(Material.CLOCK)
                .name("<gradient:#FFD700:#FFA500>⏳ Следующая неделя закрыта</gradient>")
                .lore(lore)
                .build();

        inventory.setItem(13, item);
    }

    /**
     * Форматирует миллисекунды в читаемый вид: "Xд Yч Zм".
     */
    private String formatTimeRemaining(long millis) {
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        var sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        sb.append(minutes).append("м");
        return sb.toString();
    }

    /**
     * Заполняет фон квестов: чёрная рамка, внутри чередование синего и голубого.
     */
    private void fillQuestBorder() {
        int rows = size / 9;
        var borderItem = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<black> </black>")
                .build();

        var blue = ItemBuilder.of(Material.BLUE_STAINED_GLASS_PANE)
                .name("<dark_blue> </dark_blue>")
                .build();

        var lightBlue = ItemBuilder.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .name("<blue> </blue>")
                .build();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                if (inventory.getItem(slot) != null) continue;

                boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;
                if (isBorder) {
                    inventory.setItem(slot, borderItem);
                } else {
                    // Чередование: чётные ряды — синий, нечётные — голубой
                    inventory.setItem(slot, (row % 2 == 0) ? blue : lightBlue);
                }
            }
        }
    }
}
