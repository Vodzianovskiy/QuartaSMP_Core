package eu._Nightwarden.quartaSMP_Core.shop;

import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Сервис выдачи наград за покупку в магазине.
 * <p>
 * Вместо выполнения сырых команд (которые могут сломаться при смене версии Minecraft),
 * использует Bukkit API для создания и выдачи предметов.
 * <p>
 * Поддерживаемые типы наград:
 * - COMMANDS — выполнить список консольных команд (для совместимости)
 * - TOP_SWORD — выдать легендарный меч с чарами
 */
public final class ShopRewardService {

    /**
     * Выдаёт награду игроку в зависимости от типа товара.
     *
     * @param player игрок
     * @param item   товар
     */
    public void giveReward(Player player, ShopItem item) {
        var rewardType = item.rewardType() != null ? item.rewardType().toUpperCase() : "COMMANDS";

        switch (rewardType) {
            case "TOP_SWORD" -> giveTopSword(player);
            default -> executeCommands(player, item.commands());
        }
    }

    /**
     * Выдаёт легендарный меч через Bukkit API.
     * Безопасно: не зависит от формата команд Minecraft.
     */
    private void giveTopSword(Player player) {
        var sword = ItemBuilder.of(Material.DIAMOND_SWORD)
                .name("<gold><bold>✦ Клинок Чемпиона</bold></gold>")
                .lore(List.of(
                        "<gray>Легендарный меч для настоящего бойца</gray>",
                        "",
                        "<gold>✦ Острота V</gold>",
                        "<gold>✦ Добыча III</gold>",
                        "<gold>✦ Починка</gold>",
                        "<gold>✦ Разящий клинок III</gold>"
                ))
                .meta(meta -> {
                    meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                    meta.addEnchant(Enchantment.LOOTING, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
                })
                .build();

        // Если инвентарь полный — выпадает рядом
        var leftovers = player.getInventory().addItem(sword);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }
    }

    /**
     * Выполняет список консольных команд.
     * Используется для обычных товаров (префиксы, флаги).
     */
    private void executeCommands(Player player, List<String> commands) {
        if (commands == null) return;

        for (var command : commands) {
            var resolvedCommand = command.replace("%player%", player.getName());
            if (resolvedCommand.startsWith("/")) {
                resolvedCommand = resolvedCommand.substring(1);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolvedCommand);
        }
    }
}
