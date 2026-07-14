package eu._Nightwarden.quartaSMP_Core.shop;

import org.bukkit.Material;

import java.util.List;

/**
 * Данные одного товара из shop.yml.
 *
 * @param id             уникальный ID товара
 * @param displayName    название (MiniMessage)
 * @param material       материал иконки
 * @param priceLevel     цена в уровнях (сколько списывается при покупке)
 * @param requiredLevel  минимальный уровень для просмотра/покупки (если 0 = priceLevel)
 * @param slot           слот в GUI магазина (-1 = авто-размещение)
 * @param lore           дополнительный лор (список MiniMessage-строк)
 * @param commands       список консольных команд при покупке (%player% = имя игрока)
 * @param rewardType     тип награды: COMMANDS (дефолт), TOP_SWORD и т.д.
 */
public record ShopItem(
        String id,
        String displayName,
        Material material,
        int priceLevel,
        int requiredLevel,
        int slot,
        List<String> lore,
        List<String> commands,
        String rewardType
) {
}
