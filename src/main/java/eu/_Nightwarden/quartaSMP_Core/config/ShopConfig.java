package eu._Nightwarden.quartaSMP_Core.config;

import eu._Nightwarden.quartaSMP_Core.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Обёртка над shop.yml.
 * Парсит товары в объекты ShopItem.
 */
public final class ShopConfig {

    private final FileConfiguration yaml;
    private final Map<String, ShopItem> items = new LinkedHashMap<>();

    public ShopConfig(FileConfiguration yaml) {
        this.yaml = yaml;
        loadItems();
    }

    /**
     * Парсит все товары из конфига.
     */
    private void loadItems() {
        items.clear();
        var itemsSection = yaml.getConfigurationSection("shop-items");
        if (itemsSection == null) return;

        for (var itemKey : itemsSection.getKeys(false)) {
            var itemSection = itemsSection.getConfigurationSection(itemKey);
            if (itemSection == null) continue;

            var displayName = itemSection.getString("display-name", itemKey);
            var materialStr = itemSection.getString("material", "STONE");
            var material = Material.getMaterial(materialStr.toUpperCase());
            if (material == null) material = Material.STONE;

            var priceLevel = itemSection.getInt("price-level", 1);
            var requiredLevel = itemSection.getInt("required-level", priceLevel);
            var slot = itemSection.getInt("slot", -1);
            var lore = itemSection.getStringList("lore");
            var commands = itemSection.getStringList("commands");
            var rewardType = itemSection.getString("reward-type", "COMMANDS");

            var item = new ShopItem(itemKey, displayName, material, priceLevel, requiredLevel, slot, lore, commands, rewardType);
            items.put(itemKey, item);
        }
    }

    /**
     * Возвращает все товары (сохраняет порядок из YAML).
     */
    public Map<String, ShopItem> getItems() {
        return Map.copyOf(items);
    }

    /**
     * Возвращает список товаров в порядке объявления.
     */
    public List<ShopItem> getItemsList() {
        return List.copyOf(items.values());
    }

    /**
     * Перезагружает товары из YAML.
     */
    public void reload() {
        loadItems();
    }
}
