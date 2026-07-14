package eu._Nightwarden.quartaSMP_Core.gui;

import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.ItemReward;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestTask;
import eu._Nightwarden.quartaSMP_Core.quest.QuestWeek;
import eu._Nightwarden.quartaSMP_Core.quest.TaskType;
import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


/**
 * Меню задач конкретной недели.
 * Каждая задача — красивая карточка с названием, описанием, прогрессом и статусом.
 * DELIVER-задачи: ПКМ по задаче = сдача ресурсов.
 * HAVE_IN_INVENTORY: ПКМ по задаче = проверка наличия.
 *
 * 🎨 Дизайн:
 * - Чёрная рамка
 * - Фон: чередование синего и голубого стекла (водная/небесная тема)
 * - Задачи — карточки с иконками
 * - Выполненные задачи — с эффектом зачарования
 * - Кнопка назад в главное меню
 *
 * 🔒 Система последовательного открытия задач:
 * - Первая задача недели всегда видна
 * - Следующая задача открывается ТОЛЬКО после выполнения предыдущей
 * - Недоступные задачи показываются как серые "???" (buildHiddenTaskItem)
 * - Это создаёт эффект "цепочки квестов" и мотивирует выполнять по порядку
 *
 * Размер 54 слота — поддерживает до ~28 задач.
 */
public final class QuestWeekMenu extends BaseMenu {

    private static final int MENU_SIZE = 54;

    private final QuestWeek week;
    private final MessagesConfig messagesConfig;
    private final PlayerDataManager playerDataManager;
    private final QuestManager questManager;
    private final QuestsConfig questsConfig;

    public QuestWeekMenu(Player player, QuestWeek week, MessagesConfig messagesConfig,
                         PlayerDataManager playerDataManager, QuestManager questManager,
                         QuestsConfig questsConfig, FileConfiguration config) {
        super(
                player,
                week.displayName(),
                MENU_SIZE,
                config
        );
        this.week = week;
        this.messagesConfig = messagesConfig;
        this.playerDataManager = playerDataManager;
        this.questManager = questManager;
        this.questsConfig = questsConfig;
    }

    @Override
    protected String getMenuId() {
        return "week";
    }

    @Override
    protected void buildInventory() {
        var data = playerDataManager.get(player.getUniqueId());

        // Размещаем задачи, начиная со слота 10, заполняя ряды
        int slot = 10;
        boolean previousTaskCompleted = true; // Первая задача всегда видна

        for (var task : week.tasks()) {
            if (slot >= MENU_SIZE - 9) break; // не залезаем на нижнюю рамку

            var taskProgress = data != null ? data.getTaskProgress(week.id(), task.id()) : 0;
            var isCompleted = task.isCompleted(taskProgress);

            if (previousTaskCompleted) {
                // Задача доступна — показываем её карточку
                var item = buildTaskItem(task, taskProgress, isCompleted);
                inventory.setItem(slot, item);
            } else {
                // Предыдущая задача не выполнена — показываем скрытую иконку
                inventory.setItem(slot, buildHiddenTaskItem());
            }

            // Следующая задача станет видимой, только если эта выполнена
            previousTaskCompleted = isCompleted;

            // Переход к следующему слоту
            slot++;
            // Пропускаем правую рамку (слоты 8, 17, 26, 35, 44)
            if (slot % 9 == 8) slot += 2;
        }

        // 🎨 Красивый дизайн: чёрная рамка + синий/голубой фон
        fillQuestBorder();

        // Кнопка назад в главное меню
        addBackButton("main");
    }

    /**
     * Строит скрытую иконку задачи, которая ещё не доступна.
     * Показывается тёмным мерцающим стеклом с вопросительным знаком,
     * чтобы игрок знал: здесь будет задание, но пока оно секрет.
     * Эффект зачарования (glint) привлекает внимание и создаёт
     * ощущение тайны.
     */
    private ItemStack buildHiddenTaskItem() {
        return ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<dark_gray>❓ Секретное задание</dark_gray>")
                .lore(List.of(
                        "",
                        "<gray>Выполните предыдущее задание,</gray>",
                        "<gray>чтобы раскрыть его.</gray>",
                        "",
                        "<dark_gray>⚔ Продолжайте в том же духе!</dark_gray>"
                ))
                .enchantGlint()
                .build();
    }

    /**
     * Заполняет фон квестов: чёрная рамка, внутри чередование синего и голубого.
     */
    private void fillQuestBorder() {
        int rows = MENU_SIZE / 9;
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

    /**
     * Строит иконку задачи — красивую карточку.
     * ЛКМ = информация (ничего не делает).
     * ПКМ = сдача ресурсов (для DELIVER) / проверка (для HAVE_IN_INVENTORY).
     */
    private ItemStack buildTaskItem(QuestTask task, int progress, boolean isCompleted) {
        var lore = new ArrayList<String>();
        lore.add("");

        // Описание задачи из messages.yml (раздел task-descriptions)
        lore.add(messagesConfig.getTaskDescription(task));



        // Награды за задачу
        var hasExpReward = task.rewardExp() > 0;
        var hasItemRewards = task.taskRewards() != null && !task.taskRewards().isEmpty();

        if (hasExpReward || hasItemRewards) {
            lore.add("");
            lore.add("<gradient:#FFD700:#FFA500>✦═══════════════════✦</gradient>");
            lore.add("<gradient:#FFD700:#FFA500>  🎁 Награды</gradient>");

            if (hasExpReward) {
                lore.add("<gray>  +" + task.rewardExp() + "</gray> <yellow>Опыта</yellow>");
            }

            if (hasItemRewards) {
                for (var reward : task.taskRewards()) {
                    if (reward.isCommand()) {
                        // Командная награда — показываем, что это команда
                        lore.add("<gray>  ⚡</gray> <light_purple>Особая награда</light_purple>");
                    } else if (reward.isItem()) {
                        var matName = messagesConfig.getMaterialName(reward.material());
                        lore.add("<gray>  +" + reward.amount() + "</gray> <white>" + matName + "</white>");
                    }
                }
            }


            lore.add("<gradient:#FFD700:#FFA500>✦═══════════════════✦</gradient>");
        }

        // Прогресс

        lore.add("");
        lore.add("<gray>Прогресс: <yellow>" + progress + "</yellow>/<yellow>" + task.targetAmount() + "</yellow></gray>");

        // Статус
        lore.add("");
        if (isCompleted) {
            lore.add("<green><bold>✔ Задание выполнено</bold></green>");
        } else if (task.type() == TaskType.DELIVER) {
            lore.add("<yellow>ПКМ — отдать ресурсы</yellow>");
        } else if (task.type() == TaskType.HAVE_IN_INVENTORY) {
            lore.add("<yellow>ПКМ — проверить наличие</yellow>");
        } else {
            lore.add("<gray>Отслеживается автоматически</gray>");
        }

        var builder = ItemBuilder.of(task.material())
                .name(task.displayName())
                .lore(lore)
                .pdcString("action", "task_" + task.type().name().toLowerCase())
                .pdcString("task_id", task.id())
                .pdcString("week_id", week.id());

        // Выполненные задачи — оригинальный материал + эффект зачарования
        if (isCompleted) {
            builder.enchantGlint();
        }

        return builder.build();
    }
}
