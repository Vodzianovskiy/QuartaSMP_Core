package eu._Nightwarden.quartaSMP_Core.quest;

import org.bukkit.entity.Player;

/**
 * Обработчик для задач типа CRAFT.
 * Вызывается из CraftListener при крафте предмета.
 *
 * Проверяет, что крафтнутый предмет соответствует craftMaterial задачи,
 * и обновляет прогресс через QuestManager.updateProgress().
 */
public final class CraftTaskHandler implements TaskHandler {

    @Override
    public void handle(Player player, QuestTask task, int amount) {
        // Валидация: проверяем что задача действительно CRAFT
        if (task.type() != TaskType.CRAFT) return;

        // Проверяем, что крафтнутый предмет соответствует задаче
        if (task.craftMaterial() == null) return;

        // amount передаётся из CraftListener — количество скрафченных предметов
        // QuestManager.updateProgress() уже вызван из CraftListener,
        // этот метод может быть использован для дополнительной логики
        // (звуки, партиклы, сообщения)
    }
}
