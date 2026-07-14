package eu._Nightwarden.quartaSMP_Core.quest;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Данные одной задачи из quests.yml.
 *
 * @param id               уникальный ID задачи внутри недели
 * @param type             тип задачи (DELIVER, HAVE_IN_INVENTORY, CRAFT, KILL, ...)
 * @param displayName      название для GUI (MiniMessage)
 * @param material         материал иконки в GUI
 * @param targetAmount     сколько нужно сделать/принести/убить
 * @param itemMaterial     для DELIVER/HAVE_IN_INVENTORY/EAT/SMELT/BUCKET_FILL — какой предмет
 * @param craftMaterial    для CRAFT — что крафтить
 * @param entityType       для KILL/TAME/BREED — какого моба
 * @param blockMaterial    для BLOCK_BREAK/BLOCK_PLACE — какой блок
 * @param potionEffectType для CONSUME_POTION — какой эффект зелья
 * @param rewardExp        сколько exp даётся за выполнение этой задачи
 * @param taskRewards      список предметных наград за выполнение задачи
 */
public record QuestTask(
        String id,
        TaskType type,
        String displayName,
        Material material,
        int targetAmount,
        Material itemMaterial,
        Material craftMaterial,
        EntityType entityType,
        Material blockMaterial,
        PotionEffectType potionEffectType,
        int rewardExp,
        List<ItemReward> taskRewards
) {

    /**
     * Проверяет, завершена ли задача (достигнут ли targetAmount).
     */
    public boolean isCompleted(int currentProgress) {
        return currentProgress >= targetAmount;
    }
}
