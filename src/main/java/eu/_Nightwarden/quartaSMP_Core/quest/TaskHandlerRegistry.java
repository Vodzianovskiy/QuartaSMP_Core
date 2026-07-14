package eu._Nightwarden.quartaSMP_Core.quest;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр обработчиков задач.
 * Регистрирует реализации TaskHandler для каждого TaskType.
 *
 * Для добавления нового типа задачи:
 * 1. Создай класс implements TaskHandler
 * 2. Зарегистрируй его здесь: register(TaskType.NEW_TYPE, new NewTypeHandler());
 */
public final class TaskHandlerRegistry {

    private final Map<TaskType, TaskHandler> handlers = new HashMap<>();

    public TaskHandlerRegistry() {
        // Регистрируем встроенные обработчики
        register(TaskType.CRAFT, new CraftTaskHandler());
        register(TaskType.KILL, new KillTaskHandler());
        // DELIVER и HAVE_IN_INVENTORY обрабатываются через GUI (QuestWeekMenu)
        // — для них не нужен автоматический обработчик событий
    }

    /**
     * Регистрирует обработчик для типа задачи.
     */
    public void register(TaskType type, TaskHandler handler) {
        handlers.put(type, handler);
    }

    /**
     * Возвращает обработчик для типа задачи, или null.
     */
    public TaskHandler getHandler(TaskType type) {
        return handlers.get(type);
    }

    /**
     * Проверяет, есть ли обработчик для типа.
     */
    public boolean hasHandler(TaskType type) {
        return handlers.containsKey(type);
    }
}
