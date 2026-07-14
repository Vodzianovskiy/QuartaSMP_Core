package eu._Nightwarden.quartaSMP_Core.shop;

import eu._Nightwarden.quartaSMP_Core.player.PlayerData;

/**
 * Единый сервис для проверки доступа к товарам магазина.
 * <p>
 * Содержит всю бизнес-логику видимости и доступности товаров,
 * чтобы она не дублировалась в GUI, ShopManager и других местах.
 * <p>
 * Правила:
 * - effectiveRequiredLevel = max(requiredLevel, priceLevel)
 * - Если игрок не достиг effectiveRequiredLevel → LOCKED (не видит)
 * - Если игрок достиг, но не хватает на priceLevel → NOT_ENOUGH_LEVEL
 * - Если игрок достиг и хватает → AVAILABLE
 * - Если уже куплено → PURCHASED
 */
public final class ShopAccessService {

    /**
     * Эффективный уровень для просмотра товара.
     * Берётся максимум из requiredLevel и priceLevel,
     * чтобы товар не показывался раньше, чем его можно купить.
     */
    public int effectiveRequiredLevel(ShopItem item) {
        return Math.max(item.requiredLevel(), item.priceLevel());
    }

    /**
     * Может ли игрок видеть товар в магазине.
     */
    public boolean canView(PlayerData data, ShopItem item) {
        if (data == null) return false;
        return data.level() >= effectiveRequiredLevel(item);
    }

    /**
     * Может ли игрок купить товар (есть ли уровни).
     */
    public boolean canBuy(PlayerData data, ShopItem item) {
        if (data == null) return false;
        if (data.isItemPurchased(item.id())) return false;
        return data.level() >= item.priceLevel();
    }

    /**
     * Куплен ли товар игроком.
     */
    public boolean isPurchased(PlayerData data, ShopItem item) {
        if (data == null) return false;
        return data.isItemPurchased(item.id());
    }

    /**
     * Получить полное состояние товара для игрока.
     */
    public ShopItemState getState(PlayerData data, ShopItem item) {
        if (data == null) return ShopItemState.LOCKED;

        if (data.isItemPurchased(item.id())) {
            return ShopItemState.PURCHASED;
        }

        if (data.level() < effectiveRequiredLevel(item)) {
            return ShopItemState.LOCKED;
        }

        if (data.level() < item.priceLevel()) {
            return ShopItemState.NOT_ENOUGH_LEVEL;
        }

        return ShopItemState.AVAILABLE;
    }

    /**
     * Состояние товара в магазине.
     */
    public enum ShopItemState {
        /** Игрок не достиг нужного уровня — товар скрыт (BARRIER, ???) */
        LOCKED,
        /** Игрок видит товар, но не хватает уровней на покупку */
        NOT_ENOUGH_LEVEL,
        /** Можно купить */
        AVAILABLE,
        /** Уже куплено */
        PURCHASED
    }
}
