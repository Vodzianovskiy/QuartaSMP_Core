package eu._Nightwarden.quartaSMP_Core.shop;

import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.config.ShopConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер магазина.
 * Обрабатывает покупки: проверка уровня, списание, выполнение команд.
 *
 * Защита от дюпов:
 * - processingPurchases Set<UUID> блокирует повторные клики во время обработки
 * - Списание валюты происходит под per-player блокировкой PlayerDataManager.executeAtomic()
 * - Награды выдаются через ShopRewardService (безопасно, через Bukkit API)
 * - Проверка доступа через ShopAccessService (единая логика)
 */
public final class ShopManager {

    private final ShopConfig shopConfig;
    private final PlayerDataManager playerDataManager;
    private final MessagesConfig messagesConfig;
    private final ShopAccessService accessService;
    private final ShopRewardService rewardService;
    private final Set<UUID> processingPurchases = ConcurrentHashMap.newKeySet();

    public ShopManager(ShopConfig shopConfig, PlayerDataManager playerDataManager,
                       MessagesConfig messagesConfig, ShopAccessService accessService,
                       ShopRewardService rewardService) {
        this.shopConfig = shopConfig;
        this.playerDataManager = playerDataManager;
        this.messagesConfig = messagesConfig;
        this.accessService = accessService;
        this.rewardService = rewardService;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }

    /**
     * Обрабатывает покупку товара игроком.
     * Защищено от повторного клика через processingPurchases Set.
     *
     * @param player игрок
     * @param item   товар
     * @return true если покупка успешна
     */
    public boolean purchase(Player player, ShopItem item) {
        var uuid = player.getUniqueId();

        // Защита от повторного клика: если игрок уже в обработке — игнорируем
        if (!processingPurchases.add(uuid)) {
            MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(), messagesConfig.purchaseProcessing());
            return false;
        }


        try {
            // Атомарная операция: проверка баланса + списание под per-player локом
            var success = playerDataManager.executeAtomic(uuid, data -> {
                if (data == null) return null;

                // Проверка через единый сервис доступа
                if (!accessService.canBuy(data, item)) {
                    return null;
                }

                // Списываем уровни и отмечаем товар как купленный
                return data.withLevel(data.level() - item.priceLevel())
                        .withPurchasedItem(item.id());
            });

            if (success == null) {
                // Проверяем, может товар уже куплен
                var currentData = playerDataManager.get(uuid);
                if (currentData != null && currentData.isItemPurchased(item.id())) {
                    var msg = messagesConfig.purchaseAlreadyOwned();
                    MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(), msg, Map.of(
                            "item", item.displayName()
                    ));
                    return false;
                }

                // Недостаточно уровней или данных нет
                var msg = messagesConfig.purchaseFailLevel();
                MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(), msg, Map.of(
                        "price", String.valueOf(item.priceLevel()),
                        "current", String.valueOf(playerDataManager.get(uuid) != null ? playerDataManager.get(uuid).level() : "?")
                ));
                return false;
            }

            // ВАЖНО: данные уже сохранены в кэше под локом.
            // Теперь выдаём награду через ShopRewardService (безопасно, через Bukkit API)
            rewardService.giveReward(player, item);

            // Отправляем сообщение об успешной покупке
            var msg = messagesConfig.purchaseSuccess();
            MiniMessageUtil.sendPrefixedMessage(player, messagesConfig.prefix(), msg, Map.of(
                    "item", item.displayName(),
                    "price", String.valueOf(item.priceLevel())
            ));

            return true;

        } finally {
            // ВАЖНО: всегда убираем игрока из processingPurchases, даже если было исключение
            processingPurchases.remove(uuid);
        }
    }

    /**
     * Проверяет, может ли игрок позволить себе товар.
     */
    public boolean canAfford(Player player, ShopItem item) {
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return false;
        return data.level() >= item.priceLevel();
    }
}
