package eu._Nightwarden.quartaSMP_Core.gui;

import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.config.ShopConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.shop.ShopAccessService;
import eu._Nightwarden.quartaSMP_Core.shop.ShopAccessService.ShopItemState;
import eu._Nightwarden.quartaSMP_Core.shop.ShopItem;
import eu._Nightwarden.quartaSMP_Core.shop.ShopManager;
import eu._Nightwarden.quartaSMP_Core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Меню магазина — стиль "QuartaSMP Shop".
 * <p>
 * 🎨 Дизайн:
 * - Тёмная неоновая рамка (BLACK + MAGENTA + PURPLE стекло)
 * - Верхняя панель: статус игрока (уровень, куплено товаров)
 * - Категорийные разделители: ПРЕМИУМ, ФЛАГИ, ПРЕДМЕТЫ
 * - Карточки товаров с прогресс-барами и цветовой индикацией
 * - Нижняя панель: навигация (назад)
 * <p>
 * Состояния товара:
 * - 🔒 ЗАКРЫТО (уровень ниже required-level) — скрытый предмет (BARRIER, ???)
 * - ⚠ НЕ ХВАТАЕТ УРОВНЕЙ (уровень есть, но не хватает на price-level)
 * - ◆ ДОСТУПНО (можно купить)
 * - ✔ КУПЛЕНО (уже куплено)
 */
public final class ShopMenu extends BaseMenu {

    private static final int MENU_SIZE = 54;

    private final ShopConfig shopConfig;
    private final MessagesConfig messagesConfig;
    private final PlayerDataManager playerDataManager;
    private final ShopManager shopManager;
    private final ShopAccessService accessService;

    public ShopMenu(Player player, ShopConfig shopConfig, MessagesConfig messagesConfig,
                    PlayerDataManager playerDataManager, ShopManager shopManager,
                    ShopAccessService accessService, FileConfiguration config) {
        super(
                player,
                "<gradient:#FF00FF:#00FFFF>✦ QuartaSMP Shop</gradient>",
                MENU_SIZE,
                config
        );
        this.shopConfig = shopConfig;
        this.messagesConfig = messagesConfig;
        this.playerDataManager = playerDataManager;
        this.shopManager = shopManager;
        this.accessService = accessService;
    }

    @Override
    protected String getMenuId() {
        return "shop";
    }

    @Override
    protected void buildInventory() {
        var items = shopConfig.getItemsList();
        var data = playerDataManager.get(player.getUniqueId());
        int playerLevel = (data != null) ? data.level() : 0;
        int purchasedCount = (data != null) ? data.getPurchasedItems().size() : 0;

        // ==========================================
        // 1. СТАТУС-ПАНЕЛЬ (верхняя строка)
        // ==========================================
        buildStatusPanel(playerLevel, purchasedCount);

        // ==========================================
        // 2. КАТЕГОРИЙНЫЕ РАЗДЕЛИТЕЛИ
        // ==========================================
        buildCategoryHeaders();

        // ==========================================
        // 3. ТОВАРЫ
        // ==========================================
        placeItems(items, data, playerLevel);

        // ==========================================
        // 4. НЕОНОВЫЙ ФОН
        // ==========================================
        fillNeonBackground();

        // ==========================================
        // 5. НАВИГАЦИЯ (нижняя строка)
        // ==========================================
        addBackButton("main");
    }

    // ========================================================================
    // СТАТУС-ПАНЕЛЬ
    // ========================================================================

    /**
     * Верхняя строка (слоты 0-8): статус игрока.
     * Слот 0 — декоративный, слот 4 — уровень, слот 8 — декоративный.
     */
    private void buildStatusPanel(int playerLevel, int purchasedCount) {
        // Левая декорация
        var leftDeco = ItemBuilder.of(Material.MAGENTA_STAINED_GLASS_PANE)
                .name("<gradient:#FF00FF:#AA00FF>◈</gradient>")
                .build();
        inventory.setItem(0, leftDeco);

        // Статус игрока (центр)
        var statusLore = new ArrayList<String>();
        statusLore.add("");
        statusLore.add("<gradient:#FF00FF:#00FFFF>✦ Твой уровень: " + playerLevel + "</gradient>");
        statusLore.add("<gradient:#FFD700:#FF8C00>✦ Куплено товаров: " + purchasedCount + "</gradient>");
        statusLore.add("");
        statusLore.add("<dark_gray>▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰</dark_gray>");

        var statusItem = ItemBuilder.of(Material.NETHER_STAR)
                .name("<gradient:#FF00FF:#00FFFF>✦ ТВОЙ ПРОФИЛЬ</gradient>")
                .lore(statusLore)
                .build();
        inventory.setItem(4, statusItem);

        // Правая декорация
        var rightDeco = ItemBuilder.of(Material.MAGENTA_STAINED_GLASS_PANE)
                .name("<gradient:#AA00FF:#FF00FF>◈</gradient>")
                .build();
        inventory.setItem(8, rightDeco);
    }

    // ========================================================================
    // КАТЕГОРИЙНЫЕ РАЗДЕЛИТЕЛИ
    // ========================================================================

    /**
     * Размещает декоративные разделители категорий.
     * Сейчас: ПРЕМИУМ (слот 9), ФЛАГИ (слот 18), ПРЕДМЕТЫ (слот 27).
     */
    private void buildCategoryHeaders() {
        // Премиум
        var premiumHeader = ItemBuilder.of(Material.PURPLE_STAINED_GLASS_PANE)
                .name("<gradient:#FF6B6B:#EE5A24>✦ ПРЕМИУМ</gradient>")
                .lore(List.of("<dark_gray>Эксклюзивные префиксы и косметика</dark_gray>"))
                .build();
        inventory.setItem(9, premiumHeader);

        // Флаги
        var flagsHeader = ItemBuilder.of(Material.BLUE_STAINED_GLASS_PANE)
                .name("<gradient:#55FFFF:#00AAAA>⚑ ФЛАГИ ПРИВАТ-ЗОН</gradient>")
                .lore(List.of("<dark_gray>Управление приват-зонами</dark_gray>"))
                .build();
        inventory.setItem(18, flagsHeader);

        // Предметы
        var itemsHeader = ItemBuilder.of(Material.CYAN_STAINED_GLASS_PANE)
                .name("<gradient:#AAFFAA:#00AA00>✧ ПРЕДМЕТЫ</gradient>")
                .lore(List.of("<dark_gray>Полезные предметы и ресурсы</dark_gray>"))
                .build();
        inventory.setItem(27, itemsHeader);
    }

    // ========================================================================
    // РАЗМЕЩЕНИЕ ТОВАРОВ
    // ========================================================================

    /**
     * Размещает товары по слотам с улучшенной UX-раскладкой:
     * - Префиксы: центральная витрина (слот 13)
     * - Флаги: сетка 2 ряда (слоты 19-25, 28-34)
     * - Предметы: нижняя витрина (слоты 37-43)
     * Если у товара задан slot (>= 0) — ставит туда.
     */
    private void placeItems(List<ShopItem> items, PlayerData data, int playerLevel) {
        // 🔥 Улучшенная UX-раскладка
        // Префиксы — центральная витрина (1 товар по центру)
        int[] premiumSlots = {13};
        // Флаги — сетка 2 ряда
        int[] flagsSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        // Предметы — нижняя витрина
        int[] itemsSlots = {37, 38, 39, 40, 41, 42, 43};

        int premiumIdx = 0;
        int flagsIdx = 0;
        int itemsIdx = 0;

        for (var shopItem : items) {
            // Используем единый сервис доступа для определения состояния
            var state = accessService.getState(data, shopItem);
            var item = buildShopItem(shopItem, state, playerLevel);

            // Если задан конкретный слот — используем его
            if (shopItem.slot() >= 0 && shopItem.slot() < MENU_SIZE) {
                inventory.setItem(shopItem.slot(), item);
                continue;
            }

            // Авто-размещение по категориям
            int slot = -1;
            if (shopItem.id().startsWith("prefix_")) {
                if (premiumIdx < premiumSlots.length) {
                    slot = premiumSlots[premiumIdx++];
                }
            } else if (shopItem.id().startsWith("flag_")) {
                if (flagsIdx < flagsSlots.length) {
                    slot = flagsSlots[flagsIdx++];
                }
            } else {
                if (itemsIdx < itemsSlots.length) {
                    slot = itemsSlots[itemsIdx++];
                }
            }

            if (slot >= 0) {
                inventory.setItem(slot, item);
            }
        }
    }

    // ========================================================================
    // ПОСТРОЕНИЕ КАРТОЧКИ ТОВАРА
    // ========================================================================

    /**
     * Строит иконку товара на основе его состояния (через ShopAccessService).
     */
    private ItemStack buildShopItem(ShopItem shopItem, ShopItemState state, int playerLevel) {
        var lore = new ArrayList<String>();
        lore.add("");

        switch (state) {
            // ==========================================
            // КУПЛЕНО
            // ==========================================
            case PURCHASED -> {
                lore.add("<gradient:#FFD700:#FF8C00><bold>✔ КУПЛЕНО</bold></gradient>");
                lore.add("<dark_gray>Ты уже приобрёл этот товар</dark_gray>");
                lore.add("");
                lore.add("<dark_gray>▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰</dark_gray>");

                return ItemBuilder.of(shopItem.material())
                        .name(shopItem.displayName())
                        .lore(lore)
                        .pdcString("action", "shop_owned")
                        .pdcString("shop_id", shopItem.id())
                        .build();
            }

            // ==========================================
            // ЗАКРЫТО (уровень ниже effectiveRequiredLevel)
            // ==========================================
            case LOCKED -> {
                var effectiveLevel = accessService.effectiveRequiredLevel(shopItem);
                lore.add("<dark_gray><bold>🔒 ЗАКРЫТО</bold></dark_gray>");
                lore.add("");
                lore.add("<gray>❌ Требуется уровень: <red>" + effectiveLevel + "</red></gray>");
                lore.add("<gray>📊 Твой уровень: <yellow>" + playerLevel + "</yellow></gray>");

                lore.add("");
                lore.add(buildProgressBar(playerLevel, effectiveLevel));
                lore.add("");
                lore.add("<dark_gray>▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰</dark_gray>");

                return ItemBuilder.of(Material.BARRIER)
                        .name("<gradient:#555555:#888888>✦ ???</gradient>")
                        .lore(lore)
                        .pdcString("action", "shop_locked")
                        .pdcString("shop_id", shopItem.id())
                        .build();
            }

            // ==========================================
            // НЕ ХВАТАЕТ УРОВНЕЙ
            // ==========================================
            case NOT_ENOUGH_LEVEL -> {
                lore.add(messagesConfig.priceLore().replace("%price%", String.valueOf(shopItem.priceLevel())));

                if (shopItem.lore() != null) {
                    lore.addAll(shopItem.lore());
                }

                lore.add("");
                lore.add("<gradient:#FF4444:#CC0000><bold>⚠ НЕ ХВАТАЕТ УРОВНЕЙ</bold></gradient>");
                lore.add("<gray>📊 Твой уровень: <yellow>" + playerLevel + "</yellow> / <red>" + shopItem.priceLevel() + "</red></gray>");

                lore.add("");
                lore.add(buildProgressBar(playerLevel, shopItem.priceLevel()));
                lore.add("");
                lore.add("<dark_gray>▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰</dark_gray>");

                return ItemBuilder.of(shopItem.material())
                        .name(shopItem.displayName())
                        .lore(lore)
                        .pdcString("action", "shop_locked")
                        .pdcString("shop_id", shopItem.id())
                        .build();
            }

            // ==========================================
            // ДОСТУПНО
            // ==========================================
            case AVAILABLE -> {
                lore.add(messagesConfig.priceLore().replace("%price%", String.valueOf(shopItem.priceLevel())));

                if (shopItem.lore() != null) {
                    lore.addAll(shopItem.lore());
                }

                lore.add("");
                lore.add("<gradient:#00FF00:#00CC00><bold>◆ ДОСТУПНО</bold></gradient>");
                lore.add("<gray>📊 Твой уровень: <yellow>" + playerLevel + "</yellow></gray>");
                lore.add("");
                lore.add("<aqua>🖱 Нажми ЛКМ, чтобы купить</aqua>");
                lore.add("");
                lore.add("<dark_gray>▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰</dark_gray>");

                return ItemBuilder.of(shopItem.material())
                        .name(shopItem.displayName())
                        .lore(lore)
                        .pdcString("action", "shop_buy")
                        .pdcString("shop_id", shopItem.id())
                        .build();
            }

            default -> {
                return ItemBuilder.of(Material.BARRIER)
                        .name("<red>ERROR</red>")
                        .build();
            }
        }
    }

    // ========================================================================
    // ПРОГРЕСС-БАР
    // ========================================================================

    /**
     * Строит визуальный прогресс-бар: ▰▰▰▰▰▰▰▰▰▰
     * Показывает отношение current к target.
     */
    private String buildProgressBar(int current, int target) {
        int totalBars = 10;
        int filled = Math.min(current * totalBars / target, totalBars);
        int empty = totalBars - filled;

        var sb = new StringBuilder();
        sb.append("<dark_gray>[</dark_gray>");

        for (int i = 0; i < filled; i++) {
            if (current >= target) {
                sb.append("<gradient:#00FF00:#00CC00>▰</gradient>");
            } else {
                sb.append("<gradient:#FFAA00:#FF6600>▰</gradient>");
            }
        }
        for (int i = 0; i < empty; i++) {
            sb.append("<dark_gray>▱</dark_gray>");
        }

        sb.append("<dark_gray>]</dark_gray>");
        sb.append(" <gray>").append(current).append("/").append(target).append("</gray>");

        return sb.toString();
    }

    // ========================================================================
    // НЕОНОВЫЙ ФОН
    // ========================================================================

    /**
     * Заполняет фон с неоновым эффектом.
     * Рамка — чёрная с малиновыми углами.
     * Внутренний фон — чередование тёмно-синего и тёмно-фиолетового стекла.
     */
    private void fillNeonBackground() {
        int rows = MENU_SIZE / 9;

        var borderItem = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<black> </black>")
                .build();

        var darkBlue = ItemBuilder.of(Material.BLUE_STAINED_GLASS_PANE)
                .name("<dark_blue> </dark_blue>")
                .build();

        var purple = ItemBuilder.of(Material.PURPLE_STAINED_GLASS_PANE)
                .name("<dark_purple> </dark_purple>")
                .build();

        // Неоновые углы
        var cornerNeon = ItemBuilder.of(Material.MAGENTA_STAINED_GLASS_PANE)
                .name("<gradient:#FF00FF:#AA00FF>◈</gradient>")
                .build();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                if (inventory.getItem(slot) != null) continue;

                boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;

                if (isBorder) {
                    // Углы — неоновые
                    boolean isCorner = (row == 0 || row == rows - 1) && (col == 0 || col == 8);
                    if (isCorner) {
                        inventory.setItem(slot, cornerNeon);
                    } else {
                        inventory.setItem(slot, borderItem);
                    }
                } else {
                    // Чередование: чётные ряды — синий, нечётные — фиолетовый
                    inventory.setItem(slot, (row % 2 == 0) ? darkBlue : purple);
                }
            }
        }
    }
}
