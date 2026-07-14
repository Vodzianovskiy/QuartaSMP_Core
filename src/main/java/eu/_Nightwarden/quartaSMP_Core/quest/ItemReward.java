package eu._Nightwarden.quartaSMP_Core.quest;

import org.bukkit.Material;

/**
 * Награда за выполнение квеста.
 * <p>
 * Поддерживает два формата:
 * <ul>
 *   <li><b>Предмет:</b> material + amount — выдача простого ItemStack</li>
 *   <li><b>Команда:</b> command — выполняется через Bukkit.dispatchCommand (с плейсхолдером %player%)</li>
 * </ul>
 *
 * @param material материал (null если используется command)
 * @param amount   количество (0 если используется command)
 * @param command  команда для выполнения (null если используется material+amount)
 */
public record ItemReward(
        Material material,
        int amount,
        String command
) {

    /**
     * Конструктор для предметной награды.
     */
    public ItemReward(Material material, int amount) {
        this(material, amount, null);
    }

    /**
     * Конструктор для командной награды.
     */
    public ItemReward(String command) {
        this(null, 0, command);
    }

    /**
     * @return true если это командная награда
     */
    public boolean isCommand() {
        return command != null && !command.isBlank();
    }

    /**
     * @return true если это предметная награда
     */
    public boolean isItem() {
        return material != null && amount > 0;
    }
}

