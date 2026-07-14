package eu._Nightwarden.quartaSMP_Core.listener;

import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.gui.AdminPanelMenu;
import eu._Nightwarden.quartaSMP_Core.gui.AdminPlayerMenu;
import eu._Nightwarden.quartaSMP_Core.gui.MainMenu;
import eu._Nightwarden.quartaSMP_Core.gui.QuestMenu;
import eu._Nightwarden.quartaSMP_Core.gui.QuestWeekMenu;
import eu._Nightwarden.quartaSMP_Core.gui.QuartaSMPHolder;
import eu._Nightwarden.quartaSMP_Core.gui.ShopMenu;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.shop.ShopAccessService;
import eu._Nightwarden.quartaSMP_Core.shop.ShopManager;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * Слушатель событий GUI-инвентарей.
 * Блокирует все нежелательные взаимодействия и обрабатывает клики по кнопкам.
 *
 * Защита от дюпов:
 * - Все клики по custom GUI блокируются (cancel)
 * - Drag-события блокируются
 * - MoveItem между инвентарями блокируется
 * - Только ПКМ-клики по задачам DELIVER/HAVE_IN_INVENTORY обрабатываются
 * - Только ЛКМ-клики по товарам магазина обрабатываются
 */
public final class GUIListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final QuestManager questManager;
    private final ShopManager shopManager;
    private final MessagesConfig messagesConfig;
    private final FileConfiguration config;
    private final ShopAccessService accessService;

    public GUIListener(PlayerDataManager playerDataManager, QuestManager questManager,
                       ShopManager shopManager, MessagesConfig messagesConfig,
                       FileConfiguration config, ShopAccessService accessService) {
        this.playerDataManager = playerDataManager;
        this.questManager = questManager;
        this.shopManager = shopManager;
        this.messagesConfig = messagesConfig;
        this.config = config;
        this.accessService = accessService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder(false) instanceof QuartaSMPHolder)) return;

        // Блокируем ВСЕ клики по умолчанию
        event.setCancelled(true);

        var currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType().isAir()) return;

        // Получаем action из PDC
        var meta = currentItem.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        var actionKey = new NamespacedKey("quartasmp", "action");
        var action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "open_quests" -> {
                var questMenu = new QuestMenu(player, questManager.getQuestsConfig(),
                        messagesConfig, playerDataManager, questManager, config);
                questMenu.open();
            }
            case "open_shop" -> {
                var shopMenu = new ShopMenu(player, shopManager.getShopConfig(),
                        messagesConfig, playerDataManager, shopManager, accessService, config);
                shopMenu.open();
            }
            case "open_week" -> {
                var weekIdKey = new NamespacedKey("quartasmp", "week_id");
                var weekId = pdc.get(weekIdKey, PersistentDataType.STRING);
                if (weekId != null) {
                    var week = questManager.getQuestsConfig().getWeeks().get(weekId);
                    if (week != null) {
                        var weekMenu = new QuestWeekMenu(player, week, messagesConfig,
                                playerDataManager, questManager,
                                questManager.getQuestsConfig(), config);
                        weekMenu.open();
                    }
                }
            }
            case "back" -> {

                var targetKey = new NamespacedKey("quartasmp", "back_target");
                var target = pdc.get(targetKey, PersistentDataType.STRING);
                if ("main".equals(target)) {
                    var mainMenu = new MainMenu(player, config, playerDataManager,
                            questManager, questManager.getQuestsConfig());
                    mainMenu.open();
                } else if ("quests".equals(target)) {
                    var questMenu = new QuestMenu(player, questManager.getQuestsConfig(),
                            messagesConfig, playerDataManager, questManager, config);
                    questMenu.open();
                } else if ("admin_panel".equals(target)) {
                    var adminPanel = new AdminPanelMenu(player, config, playerDataManager,
                            questManager, questManager.getQuestsConfig(), 0);
                    adminPanel.open();
                }
            }

            case "admin_open_player" -> {
                if (!player.hasPermission("quartasmp.admin")) return;
                var uuidKey = new NamespacedKey("quartasmp", "admin_player_uuid");
                var uuidStr = pdc.get(uuidKey, PersistentDataType.STRING);
                if (uuidStr != null) {
                    try {
                        var targetUuid = java.util.UUID.fromString(uuidStr);
                        var adminPlayerMenu = new AdminPlayerMenu(player, config, playerDataManager,
                                questManager, questManager.getQuestsConfig(), targetUuid);
                        adminPlayerMenu.open();
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            case "admin_refresh" -> {
                if (!player.hasPermission("quartasmp.admin")) return;
                var adminPanel = new AdminPanelMenu(player, config, playerDataManager,
                        questManager, questManager.getQuestsConfig(), 0);
                adminPanel.open();
            }

            case "admin_next_page" -> {
                if (!player.hasPermission("quartasmp.admin")) return;
                var pageKey = new NamespacedKey("quartasmp", "admin_page");
                var page = pdc.get(pageKey, PersistentDataType.INTEGER);
                if (page != null) {
                    var adminPanel = new AdminPanelMenu(player, config, playerDataManager,
                            questManager, questManager.getQuestsConfig(), page);
                    adminPanel.open();
                }
            }

            case "task_deliver" -> {
                if (event.isRightClick()) {
                    handleDeliver(player, currentItem);
                    reopenCurrentWeekMenu(player, currentItem);
                }
            }
            case "task_have_in_inventory" -> {
                if (event.isRightClick()) {
                    handleCheckInventory(player, currentItem);
                    reopenCurrentWeekMenu(player, currentItem);
                }
            }
            case "shop_buy" -> {
                if (event.isLeftClick()) {
                    handleShopPurchase(player, currentItem);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof QuartaSMPHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder(false) instanceof QuartaSMPHolder
                || event.getDestination().getHolder(false) instanceof QuartaSMPHolder) {
            event.setCancelled(true);
        }
    }

    private void handleDeliver(Player player, ItemStack taskItem) {
        var meta = taskItem.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        var taskIdKey = new NamespacedKey("quartasmp", "task_id");
        var weekIdKey = new NamespacedKey("quartasmp", "week_id");
        var taskId = pdc.get(taskIdKey, PersistentDataType.STRING);
        var weekId = pdc.get(weekIdKey, PersistentDataType.STRING);
        if (taskId == null || weekId == null) return;

        var week = questManager.getQuestsConfig().getWeeks().get(weekId);
        if (week == null) return;

        var taskOpt = week.tasks().stream()
                .filter(t -> t.id().equals(taskId))
                .findFirst();
        if (taskOpt.isEmpty()) return;

        var task = taskOpt.get();
        var itemMaterial = task.itemMaterial();
        if (itemMaterial == null) return;

        var uuid = player.getUniqueId();

        playerDataManager.executeAtomic(uuid, data -> {
            if (data == null) return null;

            // 🔥 Проверяем, доступна ли эта неделя игроку
            var weekIndex = questManager.getWeekIndex(weekId);
            if (weekIndex < 0 || !questManager.isWeekUnlocked(data, weekIndex)) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        "<red>Эта неделя ещё не открыта!</red>");
                return data;
            }

            // 🔥 Проверяем, что это текущая активная неделя
            if (!questManager.isCurrentActiveWeek(data, weekId)) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        "<red>Сначала завершите предыдущие недели!</red>");
                return data;
            }

            // 🔥 Проверяем, доступна ли задача по цепочке внутри недели
            if (!questManager.isTaskUnlocked(data, week, taskId)) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        "<red>Сначала выполните предыдущие задания этой недели!</red>");
                return data;
            }

            var currentProgress = data.getTaskProgress(weekId, taskId);
            if (task.isCompleted(currentProgress)) return data;

            var needed = task.targetAmount() - currentProgress;
            var removed = playerDataManager.removeItemsAtomic(player, itemMaterial, needed);

            if (removed <= 0) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        messagesConfig.deliverFail());
                return data;
            }

            var newProgress = currentProgress + removed;
            var updatedData = data.withTaskProgress(weekId, taskId, newProgress);

            if (task.isCompleted(newProgress)) {
                // Начисляем награду за задачу
                updatedData = questManager.applyTaskReward(player, task, updatedData);
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        messagesConfig.deliverSuccess(), Map.of("task", task.displayName()));
                questManager.sendTaskCompletedMessage(player, task);
            } else {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        messagesConfig.taskProgress(), Map.of(
                                "task", task.displayName(),
                                "progress", String.valueOf(newProgress),
                                "target", String.valueOf(task.targetAmount())
                        ));
            }

            return questManager.checkWeekCompletion(player, week, updatedData);
        });
    }

    private void handleCheckInventory(Player player, ItemStack taskItem) {
        var meta = taskItem.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        var taskIdKey = new NamespacedKey("quartasmp", "task_id");
        var weekIdKey = new NamespacedKey("quartasmp", "week_id");
        var taskId = pdc.get(taskIdKey, PersistentDataType.STRING);
        var weekId = pdc.get(weekIdKey, PersistentDataType.STRING);
        if (taskId == null || weekId == null) return;

        var week = questManager.getQuestsConfig().getWeeks().get(weekId);
        if (week == null) return;

        var taskOpt = week.tasks().stream()
                .filter(t -> t.id().equals(taskId))
                .findFirst();
        if (taskOpt.isEmpty()) return;

        var task = taskOpt.get();
        var itemMaterial = task.itemMaterial();
        if (itemMaterial == null) return;

        var uuid = player.getUniqueId();

        playerDataManager.executeAtomic(uuid, data -> {
            if (data == null) return null;

            // 🔥 Проверяем, доступна ли эта неделя игроку
            var weekIndex = questManager.getWeekIndex(weekId);
            if (weekIndex < 0 || !questManager.isWeekUnlocked(data, weekIndex)) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        "<red>Эта неделя ещё не открыта!</red>");
                return data;
            }

            // 🔥 Проверяем, что это текущая активная неделя
            if (!questManager.isCurrentActiveWeek(data, weekId)) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        "<red>Сначала завершите предыдущие недели!</red>");
                return data;
            }

            // 🔥 Проверяем, доступна ли задача по цепочке внутри недели
            if (!questManager.isTaskUnlocked(data, week, taskId)) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        "<red>Сначала выполните предыдущие задания этой недели!</red>");
                return data;
            }

            var currentProgress = data.getTaskProgress(weekId, taskId);
            if (task.isCompleted(currentProgress)) return data;

            var count = 0;
            for (var item : player.getInventory().getContents()) {
                if (item != null && item.getType() == itemMaterial) {
                    count += item.getAmount();
                }
            }

            var needed = task.targetAmount() - currentProgress;
            var canComplete = Math.min(count, needed);

            if (canComplete <= 0) {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        messagesConfig.checkFail(), Map.of("missing", String.valueOf(needed)));
                return data;
            }

            var newProgress = currentProgress + canComplete;
            var updatedData = data.withTaskProgress(weekId, taskId, newProgress);

            if (task.isCompleted(newProgress)) {
                // Начисляем награду за задачу
                updatedData = questManager.applyTaskReward(player, task, updatedData);
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        messagesConfig.checkSuccess(), Map.of("task", task.displayName()));
                questManager.sendTaskCompletedMessage(player, task);
            } else {
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(),
                        messagesConfig.taskProgress(), Map.of(
                                "task", task.displayName(),
                                "progress", String.valueOf(newProgress),
                                "target", String.valueOf(task.targetAmount())
                        ));
            }

            return questManager.checkWeekCompletion(player, week, updatedData);
        });
    }

    /**
     * Переоткрывает меню недели после обработки задачи,
     * чтобы прогресс отобразился в реальном времени.
     * Читает week_id из PDC предмета задачи.
     */
    private void reopenCurrentWeekMenu(Player player, ItemStack taskItem) {
        var meta = taskItem.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        var weekIdKey = new NamespacedKey("quartasmp", "week_id");
        var weekId = pdc.get(weekIdKey, PersistentDataType.STRING);
        if (weekId == null) return;

        var week = questManager.getQuestsConfig().getWeeks().get(weekId);
        if (week == null) return;

        var weekMenu = new QuestWeekMenu(player, week, messagesConfig,
                playerDataManager, questManager,
                questManager.getQuestsConfig(), config);
        weekMenu.open();
    }

    private void handleShopPurchase(Player player, ItemStack shopItem) {
        var meta = shopItem.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        var shopIdKey = new NamespacedKey("quartasmp", "shop_id");
        var shopId = pdc.get(shopIdKey, PersistentDataType.STRING);
        if (shopId == null) return;

        // Нужно получить ShopItem по shopId
        var shopConfig = shopManager.getShopConfig();
        var item = shopConfig.getItems().get(shopId);
        if (item != null) {
            boolean success = shopManager.purchase(player, item);
            if (success) {
                // 🔥 Переоткрываем магазин с актуальными данными (уровень, купленные товары)
                var shopMenu = new ShopMenu(player, shopManager.getShopConfig(),
                        messagesConfig, playerDataManager, shopManager, accessService, config);
                shopMenu.open();
            }
        }
    }
}
