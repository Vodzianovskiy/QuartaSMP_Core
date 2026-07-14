package eu._Nightwarden.quartaSMP_Core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Построитель ItemStack с поддержкой MiniMessage для названий и лора.
 * Все названия и лор проходят через MiniMessageUtil.deserialize().
 *
 * ВАЖНО: Minecraft по умолчанию применяет курсив к displayName предметов.
 * Мы программно отключаем курсив через .decoration(TextDecoration.ITALIC, false)
 * на каждом Component, чтобы избежать использования тега {@code <i:false>}
 * в MiniMessage-строках (который может отображаться буквально).
 */
public final class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    private ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
        if (this.itemMeta == null) {
            throw new IllegalStateException("ItemMeta is null for material: " + material);
        }
    }

    /**
     * Создаёт билдер для указанного материала.
     */
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material, 1);
    }

    /**
     * Создаёт билдер для указанного материала и количества.
     */
    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    /**
     * Создаёт билдер на основе существующего ItemStack.
     */
    public static ItemBuilder of(ItemStack itemStack) {
        var builder = new ItemBuilder(itemStack.getType(), itemStack.getAmount());
        builder.itemStack.setItemMeta(itemStack.getItemMeta().clone());
        return builder;
    }

    /**
     * Устанавливает отображаемое имя через MiniMessage.
     * Курсив автоматически отключается.
     */
    public ItemBuilder name(String miniMessage) {
        itemMeta.displayName(stripItalic(MiniMessageUtil.deserialize(miniMessage)));
        return this;
    }

    /**
     * Устанавливает имя с плейсхолдерами.
     * Курсив автоматически отключается.
     */
    public ItemBuilder name(String miniMessage, Map<String, String> placeholders) {
        itemMeta.displayName(stripItalic(MiniMessageUtil.deserialize(miniMessage, placeholders)));
        return this;
    }

    /**
     * Устанавливает имя как Component напрямую.
     * Курсив автоматически отключается.
     */
    public ItemBuilder name(Component component) {
        itemMeta.displayName(stripItalic(component));
        return this;
    }

    /**
     * Устанавливает лор из списка MiniMessage-строк.
     * Курсив автоматически отключается на каждой строке.
     */
    public ItemBuilder lore(List<String> miniMessageLines) {
        if (miniMessageLines == null || miniMessageLines.isEmpty()) {
            itemMeta.lore(List.of());
            return this;
        }
        var lore = new ArrayList<Component>();
        for (var line : miniMessageLines) {
            lore.add(stripItalic(MiniMessageUtil.deserialize(line)));
        }
        itemMeta.lore(lore);
        return this;
    }

    /**
     * Устанавливает лор с плейсхолдерами.
     * Курсив автоматически отключается на каждой строке.
     */
    public ItemBuilder lore(List<String> miniMessageLines, Map<String, String> placeholders) {
        if (miniMessageLines == null || miniMessageLines.isEmpty()) {
            itemMeta.lore(List.of());
            return this;
        }
        var lore = new ArrayList<Component>();
        for (var line : miniMessageLines) {
            lore.add(stripItalic(MiniMessageUtil.deserialize(line, placeholders)));
        }
        itemMeta.lore(lore);
        return this;
    }

    /**
     * Добавляет строку к существующему лору.
     * Курсив автоматически отключается.
     */
    public ItemBuilder addLoreLine(String miniMessage) {
        var lore = itemMeta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        var newLore = new ArrayList<>(lore);
        newLore.add(stripItalic(MiniMessageUtil.deserialize(miniMessage)));
        itemMeta.lore(newLore);
        return this;
    }

    /**
     * Устанавливает количество предметов.
     */
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * Устанавливает данные в PersistentDataContainer предмета.
     * Используется для идентификации действий в GUI.
     *
     * @param key   ключ (будет преобразован в NamespacedKey через "quartasmp:key")
     * @param value строковое значение
     */
    public ItemBuilder pdcString(String key, String value) {
        var pdc = itemMeta.getPersistentDataContainer();
        var namespacedKey = org.bukkit.NamespacedKey.fromString("quartasmp:" + key);
        if (namespacedKey != null) {
            pdc.set(namespacedKey, PersistentDataType.STRING, value);
        }
        return this;
    }

    /**
     * Устанавливает int-значение в PersistentDataContainer предмета.
     * Используется для пагинации и числовых параметров GUI.
     *
     * @param key   ключ (будет преобразован в NamespacedKey через "quartasmp:key")
     * @param value int-значение
     */
    public ItemBuilder pdcInt(String key, int value) {
        var pdc = itemMeta.getPersistentDataContainer();
        var namespacedKey = NamespacedKey.fromString("quartasmp:" + key);
        if (namespacedKey != null) {
            pdc.set(namespacedKey, PersistentDataType.INTEGER, value);
        }
        return this;
    }

    /**
     * Читает строковое значение из PersistentDataContainer предмета.
     *
     * @param item предмет
     * @param key  ключ (будет преобразован в NamespacedKey через "quartasmp:key")
     * @return значение или null, если ключ не найден
     */
    public static String getPDCString(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        if (meta == null) return null;
        var pdc = meta.getPersistentDataContainer();
        var namespacedKey = NamespacedKey.fromString("quartasmp:" + key);
        if (namespacedKey == null) return null;
        return pdc.get(namespacedKey, PersistentDataType.STRING);
    }

    /**
     * Добавляет эффект зачарования (enchantment glint) без реального зачарования.
     * Используется для визуального выделения выполненных задач и других предметов.
     * Добавляет скрытое зачарование INFINITY уровня 1 и скрывает флаги.
     */
    public ItemBuilder enchantGlint() {
        itemMeta.addEnchant(org.bukkit.enchantments.Enchantment.INFINITY, 1, true);
        itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    /**
     * Позволяет применить произвольные изменения к ItemMeta.
     */
    public ItemBuilder meta(Consumer<ItemMeta> metaConsumer) {
        metaConsumer.accept(itemMeta);
        return this;
    }

    /**
     * Строит ItemStack.
     */
    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /**
     * Программно отключает курсив на Component.
     * Minecraft по умолчанию применяет курсив к displayName предметов,
     * поэтому мы принудительно отключаем его на каждом Component.
     * Это правильный способ, вместо использования тега {@code <i:false>}
     * в MiniMessage-строках.
     */
    private Component stripItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
