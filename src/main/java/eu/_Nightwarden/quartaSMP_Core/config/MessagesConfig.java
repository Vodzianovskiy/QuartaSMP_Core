package eu._Nightwarden.quartaSMP_Core.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Обёртка над messages.yml.
 * Все тексты — сырые MiniMessage-строки, готовые к передаче в MiniMessageUtil.
 *
 * ВАЖНО: Ни один метод не возвращает строки с тегом {@code <prefix>}.
 * Для сообщений с префиксом используй {@link #prefixed(String)} или
 * {@link eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil#sendPrefixedMessage}.
 */
public final class MessagesConfig {

    private final FileConfiguration yaml;

    public MessagesConfig(FileConfiguration yaml) {
        this.yaml = yaml;
    }

    public String prefix() {
        return yaml.getString("prefix", "<gradient:#FF6B6B:#4ECDC4>[QuartaSMP]</gradient> ");
    }

    /**
     * Конкатенирует префикс с сообщением.
     * Используй этот метод вместо ручной конкатенации.
     */
    public String prefixed(String message) {
        return prefix() + message;
    }

    // --- Команды ---
    public String noPermission() {
        return yaml.getString("command.no-permission", "<red>Нет прав.</red>");
    }

    public String playerOnly() {
        return yaml.getString("command.player-only", "<red>Только для игроков.</red>");
    }

    public String reloadSuccess() {
        return yaml.getString("command.reload-success", "<green>Конфиг перезагружен.</green>");
    }

    public String reloadFail() {
        return yaml.getString("command.reload-fail", "<red>Ошибка перезагрузки.</red>");
    }

    public String unlockSuccess() {
        return yaml.getString("command.unlock-success", "<green>Неделя <yellow>%week%</yellow> открыта для <yellow>%player%</yellow>!</green>");
    }

    public String unlockUsage() {
        return yaml.getString("command.unlock-usage", "<red>Использование: /quartasmp unlockweek <weekId> [player]</red>");
    }

    public String weekNotFound() {
        return yaml.getString("command.week-not-found", "<red>Неделя <yellow>%week%</yellow> не найдена.</red>");
    }

    public String playerNotFound() {
        return yaml.getString("command.player-not-found", "<red>Игрок <yellow>%player%</yellow> не найден.</red>");
    }

    public String setweekSuccess() {
        return yaml.getString("command.setweek-success", "<green>Текущая неделя установлена: <yellow>%week%</yellow> для <yellow>%player%</yellow>!</green>");
    }

    public String setweekUsage() {
        return yaml.getString("command.setweek-usage", "<red>Использование: /quartasmp setweek <weekId> [player]</red>");
    }

    public String helpHeader() {
        return yaml.getString("command.help-header", "<gold>=== QuartaSMP Core Help ===");
    }

    public String helpMain() {
        return yaml.getString("command.help-main", "<yellow>/quartasmp</yellow> — открыть главное меню");
    }

    public String helpHelp() {
        return yaml.getString("command.help-help", "<yellow>/quartasmp help</yellow> — эта справка");
    }

    public String helpReload() {
        return yaml.getString("command.help-reload", "<yellow>/quartasmp reload</yellow> — перезагрузить конфиги (admin)");
    }

    public String helpUnlockweek() {
        return yaml.getString("command.help-unlockweek", "<yellow>/quartasmp unlockweek <weekId> [player]</yellow> — открыть неделю (admin)");
    }

    public String helpSetweek() {
        return yaml.getString("command.help-setweek", "<yellow>/quartasmp setweek <weekId> [player]</yellow> — установить текущую неделю (admin)");
    }

    public String helpAddexp() {
        return yaml.getString("command.help-addexp", "<yellow>/quartasmp addexp <amount> [player]</yellow> — добавить опыт (admin)");
    }

    public String addexpSuccess() {
        return yaml.getString("command.addexp-success", "<green>Добавлено <yellow>%amount%</yellow> опыта для <yellow>%player%</yellow>!</green>");
    }

    public String addexpUsage() {
        return yaml.getString("command.addexp-usage", "<red>Использование: /quartasmp addexp <amount> [player]</red>");
    }

    public String addexpInvalidAmount() {
        return yaml.getString("command.addexp-invalid-amount", "<red>Укажи положительное число.</red>");
    }

    // --- Уровни ---
    public List<String> levelUp() {
        return yaml.getStringList("level-up");
    }

    // --- Квесты ---
    public List<String> weekCompleted() {
        return yaml.getStringList("quest.week-completed");
    }

    public String taskCompleted() {
        return yaml.getString("quest.task-completed", "<green>Задача выполнена: <yellow>%task%</yellow>!</green>");
    }

    public String taskProgress() {
        return yaml.getString("quest.task-progress", "<gray>Прогресс: <green>%progress%</green>/<yellow>%target%</yellow></gray>");
    }

    public String weekLocked() {
        return yaml.getString("quest.week-locked", "<red>❌ Неделя недоступна.</red>");
    }

    public String weekLockedLore() {
        return yaml.getString("quest.week-locked-lore", "<dark_red>❌ Неделя закрыта</dark_red>");
    }

    public String weekExpired() {
        return yaml.getString("quest.week-expired", "<red>⏰ Время истекло! Прогресс сброшен.</red>");
    }

    public String noTasks() {
        return yaml.getString("quest.no-tasks", "<gray>Нет задач</gray>");
    }

    // --- DELIVER ---
    public String deliverButtonName() {
        return yaml.getString("quest.deliver-button-name", "<green>✔ Сдать предметы</green>");
    }

    public List<String> deliverButtonLore() {
        return yaml.getStringList("quest.deliver-button-lore");
    }

    public String deliverSuccess() {
        return yaml.getString("quest.deliver-success", "<green>Предметы сданы!</green>");
    }

    public String deliverFail() {
        return yaml.getString("quest.deliver-fail", "<red>Недостаточно предметов.</red>");
    }

    // --- HAVE_IN_INVENTORY ---
    public String checkButtonName() {
        return yaml.getString("quest.check-button-name", "<aqua>🔍 Проверить</aqua>");
    }

    public List<String> checkButtonLore() {
        return yaml.getStringList("quest.check-button-lore");
    }

    public String checkSuccess() {
        return yaml.getString("quest.check-success", "<green>Все предметы есть!</green>");
    }

    public String checkFail() {
        return yaml.getString("quest.check-fail", "<red>Не хватает <yellow>%missing%</yellow>.</red>");
    }

    // --- Магазин ---
    public String purchaseSuccess() {
        return yaml.getString("shop.purchase-success", "<green>Куплено <yellow>%item%</yellow> за <gold>%price% уровней</gold>!</green>");
    }

    public String purchaseFailLevel() {
        return yaml.getString("shop.purchase-fail-level", "<red>Не хватает уровней! Нужно <gold>%price%</gold>.</red>");
    }

    public String purchaseFailUnknown() {
        return yaml.getString("shop.purchase-fail-unknown", "<red>Ошибка покупки.</red>");
    }

    public String purchaseProcessing() {
        return yaml.getString("shop.purchase-processing", "<red>Подождите, предыдущая покупка ещё обрабатывается...</red>");
    }

    public String purchaseAlreadyOwned() {
        return yaml.getString("shop.purchase-already-owned", "<red>Ты уже приобрёл <yellow>%item%</yellow>!</red>");
    }

    public String notAvailableLore() {
        return yaml.getString("shop.not-available-lore", "<dark_red>✖ Недоступно</dark_red>");
    }

    public String priceLore() {
        return yaml.getString("shop.price-lore", "<gold>Цена: %price% уровней</gold>");
    }

    public String lockedLore() {
        return yaml.getString("shop.locked-lore", "<gray>Получи <yellow><bold>%level%</bold></yellow> уровень для разблокировки</gray>");
    }

    // --- Русские названия материалов и сущностей ---

    /**
     * Возвращает русское название материала из messages.yml.
     * Если не найдено — форматирует английское название.
     */
    public String getMaterialName(org.bukkit.Material material) {
        if (material == null) return "???";
        var key = material.name();
        var name = yaml.getString("materials." + key);
        if (name != null) return name;
        // Fallback: форматируем английское название
        var formatted = key.toLowerCase().replace("_", " ");
        var words = formatted.split(" ");
        var sb = new StringBuilder();
        for (var word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Возвращает русское название сущности из messages.yml.
     * Если не найдено — форматирует английское название.
     */
    public String getEntityName(org.bukkit.entity.EntityType entityType) {
        if (entityType == null) return "???";
        var key = entityType.name();
        var name = yaml.getString("entities." + key);
        if (name != null) return name;
        // Fallback: форматируем английское название
        var formatted = key.toLowerCase().replace("_", " ");
        var words = formatted.split(" ");
        var sb = new StringBuilder();
        for (var word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Возвращает описание задачи для GUI из messages.yml (раздел task-descriptions).
     * Подставляет плейсхолдеры: %target%, %item%, %entity%, %block%, %effect%
     *
     * @param task задача
     * @return строка описания с подставленными значениями
     */
    public String getTaskDescription(eu._Nightwarden.quartaSMP_Core.quest.QuestTask task) {
        var typeName = task.type().name();
        var template = yaml.getString("task-descriptions." + typeName);
        if (template == null) {
            // Fallback на английский
            return "<white>" + typeName + ": <green>" + task.targetAmount() + "</green></white>";
        }

        var result = template.replace("%target%", String.valueOf(task.targetAmount()));

        if (task.itemMaterial() != null) {
            result = result.replace("%item%", getMaterialName(task.itemMaterial()));
        } else if (task.craftMaterial() != null) {
            result = result.replace("%item%", getMaterialName(task.craftMaterial()));
        } else {
            result = result.replace("%item%", "???");
        }

        if (task.blockMaterial() != null) {
            result = result.replace("%block%", getMaterialName(task.blockMaterial()));
        } else {
            result = result.replace("%block%", "???");
        }

        if (task.entityType() != null) {
            result = result.replace("%entity%", getEntityName(task.entityType()));
        } else {
            result = result.replace("%entity%", "???");
        }

        if (task.potionEffectType() != null) {
            result = result.replace("%effect%", task.potionEffectType().getName());
        } else {
            result = result.replace("%effect%", "???");
        }

        return result;
    }
}

