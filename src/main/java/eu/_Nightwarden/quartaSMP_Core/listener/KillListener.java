package eu._Nightwarden.quartaSMP_Core.listener;

import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Слушает EntityDeathEvent и обновляет прогресс задач типа KILL.
 * Единая точка входа — QuestManager.updateProgress().
 */
public final class KillListener implements Listener {

    private final QuestManager questManager;
    private final QuestsConfig questsConfig;
    private final PlayerDataManager playerDataManager;

    public KillListener(QuestManager questManager, QuestsConfig questsConfig,
                        PlayerDataManager playerDataManager) {
        this.questManager = questManager;
        this.questsConfig = questsConfig;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        var killer = event.getEntity().getKiller();
        if (killer == null) return;

        var player = killer;
        var killedEntity = event.getEntity();

        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        // Проходим по всем неделям и ищем задачи типа KILL
        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!questManager.isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != eu._Nightwarden.quartaSMP_Core.quest.TaskType.KILL) continue;
                if (task.entityType() == null) continue;

                // Проверяем, что убитый моб совпадает
                if (killedEntity.getType() != task.entityType()) continue;

                // Проверяем, не завершена ли уже задача
                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                // Обновляем прогресс через единую точку входа
                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }
}
