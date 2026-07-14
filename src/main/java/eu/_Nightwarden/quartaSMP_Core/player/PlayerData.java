package eu._Nightwarden.quartaSMP_Core.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Данные одного игрока.
 * Хранятся в памяти (кэш PlayerDataManager'а) и периодически сохраняются в YAML.
 *
 * @param uuid              UUID игрока
 * @param level             текущий уровень
 * @param exp               текущий опыт (exp к следующему уровню)
 * @param questProgress     прогресс по задачам: Map<weekId, Map<taskId, currentAmount>>
 * @param completedWeeks    список ID завершённых недель
 * @param weekStartTimes    время (timestamp) когда игрок открыл каждую неделю
 * @param firstJoinTime     timestamp первого входа игрока на сервер (для расписания недель)
 * @param purchasedItems    список ID купленных товаров (защита от повторной покупки)
 */
public record PlayerData(
        UUID uuid,
        int level,
        int exp,
        Map<String, Map<String, Integer>> questProgress,
        Map<String, Boolean> completedWeeks,
        Map<String, Long> weekStartTimes,
        long firstJoinTime,
        Set<String> purchasedItems
) {
    /**
     * Создаёт новые данные игрока с значениями по умолчанию.
     */
    public static PlayerData createDefault(UUID uuid) {
        return new PlayerData(
                uuid,
                1,           // начальный уровень
                0,           // начальный опыт
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                System.currentTimeMillis(),  // firstJoinTime = сейчас
                new HashSet<>()              // purchasedItems пустой
        );
    }

    /**
     * Возвращает копию PlayerData с изменённым уровнем.
     */
    public PlayerData withLevel(int newLevel) {
        return new PlayerData(uuid, newLevel, exp, questProgress, completedWeeks, weekStartTimes, firstJoinTime, purchasedItems);
    }

    /**
     * Возвращает копию PlayerData с изменённым опытом.
     */
    public PlayerData withExp(int newExp) {
        return new PlayerData(uuid, level, newExp, questProgress, completedWeeks, weekStartTimes, firstJoinTime, purchasedItems);
    }

    /**
     * Возвращает копию PlayerData с обновлённым прогрессом задачи.
     */
    public PlayerData withTaskProgress(String weekId, String taskId, int amount) {
        var newProgress = new HashMap<>(questProgress);
        var weekTasks = new HashMap<>(newProgress.getOrDefault(weekId, new HashMap<>()));
        weekTasks.put(taskId, amount);
        newProgress.put(weekId, weekTasks);
        return new PlayerData(uuid, level, exp, newProgress, completedWeeks, weekStartTimes, firstJoinTime, purchasedItems);
    }

    /**
     * Возвращает прогресс по конкретной задаче (0 если нет).
     */
    public int getTaskProgress(String weekId, String taskId) {
        var weekTasks = questProgress.get(weekId);
        if (weekTasks == null) return 0;
        return weekTasks.getOrDefault(taskId, 0);
    }

    /**
     * Отмечает неделю как завершённую.
     */
    public PlayerData withWeekCompleted(String weekId) {
        var newCompleted = new HashMap<>(completedWeeks);
        newCompleted.put(weekId, true);
        return new PlayerData(uuid, level, exp, questProgress, newCompleted, weekStartTimes, firstJoinTime, purchasedItems);
    }

    /**
     * Проверяет, завершена ли неделя.
     */
    public boolean isWeekCompleted(String weekId) {
        return completedWeeks.getOrDefault(weekId, false);
    }

    /**
     * Устанавливает время открытия недели.
     */
    public PlayerData withWeekStartTime(String weekId, long timestamp) {
        var newTimes = new HashMap<>(weekStartTimes);
        newTimes.put(weekId, timestamp);
        return new PlayerData(uuid, level, exp, questProgress, completedWeeks, newTimes, firstJoinTime, purchasedItems);
    }

    /**
     * Получает время открытия недели (0 если не открывал).
     */
    public long getWeekStartTime(String weekId) {
        return weekStartTimes.getOrDefault(weekId, 0L);
    }

    /**
     * Сбрасывает прогресс по всем задачам указанной недели,
     * а также удаляет её из списка завершённых.
     */
    public PlayerData resetWeekProgress(String weekId) {
        var newProgress = new HashMap<>(questProgress);
        newProgress.remove(weekId);
        var newCompleted = new HashMap<>(completedWeeks);
        newCompleted.remove(weekId);
        var newTimes = new HashMap<>(weekStartTimes);
        newTimes.remove(weekId);
        return new PlayerData(uuid, level, exp, newProgress, newCompleted, newTimes, firstJoinTime, purchasedItems);
    }

    /**
     * Возвращает копию PlayerData с добавленным ID купленного товара.
     */
    public PlayerData withPurchasedItem(String itemId) {
        var newPurchased = new HashSet<>(purchasedItems);
        newPurchased.add(itemId);
        return new PlayerData(uuid, level, exp, questProgress, completedWeeks, weekStartTimes, firstJoinTime, newPurchased);
    }

    /**
     * Проверяет, куплен ли уже товар.
     */
    public boolean isItemPurchased(String itemId) {
        return purchasedItems.contains(itemId);
    }

    /**
     * Возвращает неизменяемое множество ID купленных товаров.
     */
    public Set<String> getPurchasedItems() {
        return Set.copyOf(purchasedItems);
    }

}
