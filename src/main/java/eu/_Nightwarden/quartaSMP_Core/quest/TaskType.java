package eu._Nightwarden.quartaSMP_Core.quest;

/**
 * Типы задач в квестах.
 *
 * Для добавления нового типа:
 * 1. Добавь значение в этот enum
 * 2. Создай класс-обработчик implements TaskHandler
 * 3. Зарегистрируй его в TaskHandlerRegistry
 * 4. Добавь нужные поля в QuestTask (с дефолтными значениями)
 * 5. Обнови парсинг в QuestsConfig если нужно новое поле
 * 6. Создай Listener для события
 * 7. Зарегистрируй Listener в QuartaSMP_Core.registerListeners()
 */
public enum TaskType {

    /**
     * Игрок должен иметь предмет в инвентаре и подтвердить сдачу.
     * Предмет забирается из инвентаря.
     * Поля: itemMaterial, targetAmount
     */
    DELIVER,

    /**
     * Проверка наличия N предметов в инвентаре (без изъятия).
     * Поля: itemMaterial, targetAmount
     */
    HAVE_IN_INVENTORY,

    /**
     * Игрок должен скрафтить N предметов.
     * Слушаем CraftItemEvent.
     * Поля: craftMaterial, targetAmount
     */
    CRAFT,

    /**
     * Игрок должен убить N мобов определённого типа.
     * Слушаем EntityDeathEvent.
     * Поля: entityType, targetAmount
     */
    KILL,

    /**
     * Игрок должен сломать N блоков определённого типа.
     * Слушаем BlockBreakEvent.
     * Поля: blockMaterial, targetAmount
     */
    BLOCK_BREAK,

    /**
     * Игрок должен поставить N блоков определённого типа.
     * Слушаем BlockPlaceEvent.
     * Поля: blockMaterial, targetAmount
     */
    BLOCK_PLACE,

    /**
     * Игрок должен зачаровать предмет N раз.
     * Слушаем EnchantItemEvent.
     * Поля: targetAmount
     */
    ENCHANT,

    /**
     * Игрок должен поймать N рыб.
     * Слушаем PlayerFishEvent.
     * Поля: targetAmount
     */
    FISH,

    /**
     * Игрок должен сварить N зелий.
     * Слушаем BrewingStandFuelEvent или BrewEvent.
     * Поля: potionType, targetAmount
     */
    BREW,

    /**
     * Игрок должен съесть N предметов.
     * Слушаем PlayerItemConsumeEvent.
     * Поля: itemMaterial, targetAmount
     */
    EAT,

    /**
     * Игрок должен приручить N животных.
     * Слушаем EntityTameEvent.
     * Поля: entityType, targetAmount
     */
    TAME,

    /**
     * Игрок должен переплавить N предметов в печи.
     * Слушаем FurnaceSmeltEvent.
     * Поля: itemMaterial, targetAmount
     */
    SMELT,

    /**
     * Игрок должен наполнить N вёдер.
     * Слушаем PlayerBucketFillEvent.
     * Поля: itemMaterial, targetAmount
     */
    BUCKET_FILL,

    /**
     * Игрок должен остричь N овец.
     * Слушаем PlayerShearEntityEvent.
     * Поля: targetAmount
     */
    SHEAR,

    /**
     * Игрок должен сделать N сделок с жителями.
     * Слушаем VillagerAcquireTradeEvent.
     * Поля: targetAmount
     */
    TRADE,

    /**
     * Игрок должен размножить N животных.
     * Слушаем EntityBreedEvent.
     * Поля: entityType, targetAmount
     */
    BREED,

    /**
     * Игрок должен выпить зелье с определённым эффектом.
     * Слушаем PlayerItemConsumeEvent + проверка PotionEffectType.
     * Поля: potionEffectType, targetAmount
     */
    CONSUME_POTION
}
