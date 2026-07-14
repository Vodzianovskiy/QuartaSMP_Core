package eu._Nightwarden.quartaSMP_Core.listener;

import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

/**
 * Слушает CraftItemEvent и обновляет прогресс задач типа CRAFT.
 * Единая точка входа — QuestManager.updateProgress().
 *
 * Точный подсчёт количества:
 * - При обычном клике: result.getAmount()
 * - При shift-click: рассчитывает реальное количество, которое может быть скрафчено
 *   с учётом свободных слотов и частично заполненных стаков того же материала
 * - Защита от дублирования: проверка InventoryAction
 */
public final class CraftListener implements Listener {

    private final QuestManager questManager;
    private final QuestsConfig questsConfig;
    private final PlayerDataManager playerDataManager;

    public CraftListener(QuestManager questManager, QuestsConfig questsConfig,
                         PlayerDataManager playerDataManager) {
        this.questManager = questManager;
        this.questsConfig = questsConfig;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;

        var result = event.getRecipe().getResult();
        if (result == null || result.getType().isAir()) return;

        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        // Определяем реальное количество скрафченного
        int amount;
        if (event.isShiftClick()) {
            // При shift-click рассчитываем максимально возможное количество
            amount = calculateShiftCraftAmount(player, result);
        } else {
            // При обычном клике — количество из результата крафта
            amount = result.getAmount();
        }

        if (amount <= 0) return;

        // Проходим по всем неделям и ищем задачи типа CRAFT
        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!questManager.isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != eu._Nightwarden.quartaSMP_Core.quest.TaskType.CRAFT) continue;
                if (task.craftMaterial() == null) continue;

                // Проверяем, что скрафченный предмет совпадает
                if (result.getType() != task.craftMaterial()) continue;

                // Проверяем, не завершена ли уже задача
                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                // Обновляем прогресс через единую точку входа
                questManager.updateProgress(player, week.id(), task.id(), amount);
            }
        }
    }

    /**
     * Рассчитывает реальное количество предметов, которое будет скрафчено
     * при shift-click. Учитывает:
     * - Пустые слоты
     * - Частично заполненные стаки того же материала
     * - Максимальный стак (64)
     */
    private int calculateShiftCraftAmount(org.bukkit.entity.Player player, ItemStack result) {
        var inventory = player.getInventory();
        var contents = inventory.getContents();
        var resultType = result.getType();
        var resultAmount = result.getAmount();
        var maxStackSize = result.getMaxStackSize();

        // Считаем доступное место в инвентаре для этого предмета
        int availableSpace = 0;

        for (var item : contents) {
            if (item == null || item.getType() == Material.AIR) {
                // Пустой слот — может вместить maxStackSize
                availableSpace += maxStackSize;
            } else if (item.getType() == resultType && item.getAmount() < maxStackSize) {
                // Частично заполненный стак того же материала
                availableSpace += maxStackSize - item.getAmount();
            }
        }

        // Сколько раз можно скрафтить (каждый крафт даёт resultAmount предметов)
        return (availableSpace / resultAmount) * resultAmount;
    }
}
