package eu._Nightwarden.quartaSMP_Core.quest;

import java.util.List;

/**
 * Данные одной недели квестов.
 *
 * @param id          уникальный ID недели (например "week_1")
 * @param displayName название для GUI (MiniMessage)
 * @param questPoints награда quest-points за полное выполнение
 * @param tasks       список задач в этой неделе
 * @param weekRewards список предметных наград за полное прохождение недели
 */
public record QuestWeek(
        String id,
        String displayName,
        int questPoints,
        List<QuestTask> tasks,
        List<ItemReward> weekRewards
) {

    /**
     * Проверяет, все ли задачи недели выполнены.
     */
    public boolean isAllTasksCompleted(List<Integer> progressList) {
        if (tasks.isEmpty()) return false;
        if (progressList.size() != tasks.size()) return false;
        for (int i = 0; i < tasks.size(); i++) {
            if (!tasks.get(i).isCompleted(progressList.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Возвращает количество задач в неделе.
     */
    public int taskCount() {
        return tasks.size();
    }
}
