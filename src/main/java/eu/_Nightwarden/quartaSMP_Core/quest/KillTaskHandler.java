package eu._Nightwarden.quartaSMP_Core.quest;

import org.bukkit.entity.Player;

/**
 * Обработчик для задач типа KILL.
 * Вызывается из KillListener при убийстве моба.
 *
 * Проверяет, что убитый моб соответствует entityType задачи,
 * и обновляет прогресс через QuestManager.updateProgress().
 */
public final class KillTaskHandler implements TaskHandler {

    @Override
    public void handle(Player player, QuestTask task, int amount) {
        if (task.type() != TaskType.KILL) return;
        if (task.entityType() == null) return;

        // QuestManager.updateProgress() уже вызван из KillListener
        // Дополнительная логика (звуки, сообщения) — по желанию
    }
}
