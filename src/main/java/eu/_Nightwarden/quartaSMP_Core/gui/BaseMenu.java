package eu._Nightwarden.quartaSMP_Core.gui;

import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Абстрактный базовый класс для всех GUI-меню.
 * Использует чистый Bukkit Inventory (не библиотечный фреймворк).
 *
 * Поддерживает:
 * - Настраиваемый размер (с валидацией кратности 9)
 * - Кнопку "Назад" с PDC action = "back_<target_menu_id>"
 * - Декоративную окантовку (fillBorder) с разными материалами для рамки и фона
 * - Заполнение фона (fillBackground) с кастомным именем предмета
 */
public abstract class BaseMenu {

    protected final Player player;
    protected Inventory inventory;
    protected final String title;
    protected final int size;
    protected final FileConfiguration config;


    protected BaseMenu(Player player, String title, int size, FileConfiguration config) {
        this.player = player;
        this.title = title;
        this.size = validateSize(size);
        this.config = config;
        this.inventory = createInventory(getMenuId(), 0, title);
    }

    /**
     * Создаёт Inventory с QuartaSMPHolder, хранящим menuId и номер страницы.
     */
    protected Inventory createInventory(String menuId, int page, String title) {
        var holder = new QuartaSMPHolder(menuId, java.util.Collections.emptySet(), page);
        return Bukkit.createInventory(holder, this.size, MiniMessageUtil.deserialize(title));
    }

    /**
     * Возвращает номер страницы из holder'а инвентаря.
     * По умолчанию 0.
     */
    protected int getCurrentPage() {
        if (inventory.getHolder(false) instanceof QuartaSMPHolder qHolder) {
            return qHolder.getPage();
        }
        return 0;
    }



    protected String getMenuId() {
        return "base";
    }

    protected abstract void buildInventory();

    public void open() {
        buildInventory();
        player.openInventory(inventory);
    }

    /**
     * Заполняет все пустые слоты фоновым материалом с кастомным именем.
     */
    protected void fillBackground(Material material, String name) {
        if (name == null) name = "<black> </black>";
        var filler = ItemBuilder.of(material).name(name).build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected void fillBackground(Material material) {
        fillBackground(material, null);
    }

    /**
     * Заполняет окантовку инвентаря.
     * Внешний периметр — borderMaterial, внутренняя часть — fillMaterial.
     */
    protected void fillBorder(Material borderMaterial, String borderName,
                              Material fillMaterial, String fillName) {
        if (size < 27) {
            fillBackground(fillMaterial, fillName);
            return;
        }
        if (borderName == null) borderName = "<black> </black>";
        if (fillName == null) fillName = "<black> </black>";
        var borderItem = ItemBuilder.of(borderMaterial).name(borderName).build();
        var fillItem = ItemBuilder.of(fillMaterial).name(fillName).build();
        int rows = size / 9;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;
                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, isBorder ? borderItem : fillItem);
                }
            }
        }
    }

    protected void fillBorder(Material borderMaterial, Material fillMaterial) {
        fillBorder(borderMaterial, null, fillMaterial, null);
    }

    /**
     * Добавляет кнопку "Назад" с PDC action = "back" и back_target = targetMenuId.
     * targetMenuId — ID меню, в которое нужно вернуться.
     * Слот читается из config.yml (menu.back-button.slot).
     */
    protected void addBackButton(String targetMenuId) {
        var backSlot = config.getInt("menu.back-button.slot", size - 1);
        if (backSlot < 0 || backSlot >= size) backSlot = size - 1;

        var backMaterialStr = config.getString("menu.back-button.material", "ARROW");
        var backMaterial = Material.getMaterial(backMaterialStr.toUpperCase());
        if (backMaterial == null) backMaterial = Material.ARROW;

        var backName = config.getString("menu.back-button.name",
                "<gradient:#ff6b6b:#ffa500>← Назад</gradient>");

        var backItem = ItemBuilder.of(backMaterial)
                .name(backName)
                .pdcString("action", "back")
                .pdcString("back_target", targetMenuId)
                .build();

        inventory.setItem(backSlot, backItem);
    }

    private int validateSize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            Bukkit.getLogger().warning("[QuartaSMP_Core] Invalid inventory size: " + size
                    + ". Must be multiple of 9 between 9-54. Falling back to 27.");
            return 27;
        }
        return size;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public String getPlainTitle() {
        return MiniMessageUtil.toPlainText(title);
    }
}
