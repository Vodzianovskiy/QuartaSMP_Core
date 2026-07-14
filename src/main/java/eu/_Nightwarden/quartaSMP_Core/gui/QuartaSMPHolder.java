package eu._Nightwarden.quartaSMP_Core.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * InventoryHolder для всех кастомных GUI плагина.
 * Позволяет надёжно идентифицировать наши инвентари через instanceof,
 * вместо проверки holder == null.
 *
 * Хранит menuId, набор фоновых слотов для блокировки кликов,
 * и номер страницы (для меню с постраничной навигацией).
 */
public final class QuartaSMPHolder implements InventoryHolder {

    private final String menuId;
    private final Set<Integer> backgroundSlots;
    private final int page;

    public QuartaSMPHolder(String menuId, Set<Integer> backgroundSlots, int page) {
        this.menuId = menuId;
        this.backgroundSlots = backgroundSlots != null
                ? Collections.unmodifiableSet(backgroundSlots)
                : Collections.emptySet();
        this.page = page;
    }

    public QuartaSMPHolder(String menuId, Set<Integer> backgroundSlots) {
        this(menuId, backgroundSlots, 0);
    }

    public QuartaSMPHolder(String menuId) {
        this(menuId, Collections.emptySet(), 0);
    }

    public String getMenuId() {
        return menuId;
    }

    /**
     * Номер текущей страницы (0-based).
     */
    public int getPage() {
        return page;
    }

    /**
     * Проверяет, является ли слот фоновым (некликабельным).
     */
    public boolean isBackgroundSlot(int slot) {
        return backgroundSlots.contains(slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("QuartaSMPHolder does not provide an Inventory directly. Use BaseMenu.getInventory() instead.");
    }
}

