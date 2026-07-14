package eu._Nightwarden.quartaSMP_Core.quest;

import org.bukkit.entity.Player;

/**
 * Интерфейс обработчика задачи.
 * Каждый тип задачи (DELIVER, CRAFT, KILL, ...) имеет свою реализацию.
 *
 * Паттерн Strategy: обработчики регистрируются в TaskHandlerRegistry
 * и вызываются через единую точку входа.
 */
public interface TaskHandler {

    /**
     * Обрабатывает событие/действие, связанное с задачей.
     *
     * @param player игрок
     * @param task   задача, к которой относится событие
     * @param amount количество (сколько убито/скрафчено/etc.)
     */
    void handle(Player player, QuestTask task, int amount);
}
