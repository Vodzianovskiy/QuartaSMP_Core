package eu._Nightwarden.quartaSMP_Core.listener;

import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.player.PlayerData;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.quest.TaskType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Единый класс для всех listener'ов новых типов квестов.
 * Каждый метод слушает своё событие и обновляет прогресс через QuestManager.
 */
public final class QuestListeners implements Listener {

    private final QuestManager questManager;
    private final QuestsConfig questsConfig;
    private final PlayerDataManager playerDataManager;

    public QuestListeners(QuestManager questManager, QuestsConfig questsConfig,
                          PlayerDataManager playerDataManager) {
        this.questManager = questManager;
        this.questsConfig = questsConfig;
        this.playerDataManager = playerDataManager;
    }

    /**
     * Проверяет, доступна ли неделя для игрока (по индексу).
     * Используется во всех listener'ах для защиты от начисления прогресса
     * на ещё не открытые недели.
     */
    private boolean isWeekUnlocked(eu._Nightwarden.quartaSMP_Core.player.PlayerData data, int weekIndex) {
        return questManager.isWeekUnlocked(data, weekIndex);
    }

    // ==================== BLOCK_BREAK ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var brokenType = event.getBlock().getType();
        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.BLOCK_BREAK) continue;
                if (task.blockMaterial() == null || task.blockMaterial() != brokenType) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== BLOCK_PLACE ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        var player = event.getPlayer();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var placedType = event.getBlock().getType();
        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.BLOCK_PLACE) continue;
                if (task.blockMaterial() == null || task.blockMaterial() != placedType) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== ENCHANT ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        var player = event.getEnchanter();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.ENCHANT) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== FISH ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        var caught = event.getCaught();
        if (!(caught instanceof org.bukkit.entity.Item caughtItem)) return;

        var caughtStack = caughtItem.getItemStack();
        var caughtType = caughtStack.getType();

        var player = event.getPlayer();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.FISH) continue;

                // Если у задачи указан конкретный материал рыбы — проверяем его
                if (task.material() != null && task.material() != caughtType) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== BREW ====================

    /**
     * Маппинг базовых PotionType → PotionEffectType для Paper 1.20.5+.
     * В новых версиях PotionType.getPotionEffects() может возвращать пустой список,
     * поэтому используем прямой маппинг.
     */
    private static final java.util.Map<String, PotionEffectType> POTION_TYPE_MAP = new java.util.HashMap<>();

    static {
        POTION_TYPE_MAP.put("SPEED", PotionEffectType.SPEED);
        POTION_TYPE_MAP.put("SLOWNESS", PotionEffectType.SLOWNESS);
        POTION_TYPE_MAP.put("HASTE", PotionEffectType.HASTE);
        POTION_TYPE_MAP.put("STRENGTH", PotionEffectType.STRENGTH);
        POTION_TYPE_MAP.put("HEALING", PotionEffectType.REGENERATION);
        POTION_TYPE_MAP.put("HARMING", PotionEffectType.INSTANT_DAMAGE);
        POTION_TYPE_MAP.put("JUMP_BOOST", PotionEffectType.JUMP_BOOST);
        POTION_TYPE_MAP.put("REGENERATION", PotionEffectType.REGENERATION);
        POTION_TYPE_MAP.put("FIRE_RESISTANCE", PotionEffectType.FIRE_RESISTANCE);
        POTION_TYPE_MAP.put("WATER_BREATHING", PotionEffectType.WATER_BREATHING);
        POTION_TYPE_MAP.put("NIGHT_VISION", PotionEffectType.NIGHT_VISION);
        POTION_TYPE_MAP.put("INVISIBILITY", PotionEffectType.INVISIBILITY);
        POTION_TYPE_MAP.put("POISON", PotionEffectType.POISON);
        POTION_TYPE_MAP.put("WEAKNESS", PotionEffectType.WEAKNESS);
        POTION_TYPE_MAP.put("LUCK", PotionEffectType.LUCK);
        POTION_TYPE_MAP.put("TURTLE_MASTER", PotionEffectType.SLOWNESS); // Turtle Master = Slowness + Resistance
        POTION_TYPE_MAP.put("SLOW_FALLING", PotionEffectType.SLOW_FALLING);
        POTION_TYPE_MAP.put("WIND_CHARGED", PotionEffectType.WIND_CHARGED);
        POTION_TYPE_MAP.put("WEAVING", PotionEffectType.WEAVING);
        POTION_TYPE_MAP.put("OOZING", PotionEffectType.OOZING);
        POTION_TYPE_MAP.put("INFESTED", PotionEffectType.INFESTED);
    }

    /**
     * Получает список PotionEffectType из ItemStack зелья.
     * Учитывает: кастомные эффекты, базовый PotionType (через маппинг), имена эффектов.
     */
    private java.util.List<PotionEffectType> getPotionEffects(ItemStack item) {
        var meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) return java.util.Collections.emptyList();

        var effects = new java.util.ArrayList<PotionEffectType>();

        // 1. Кастомные эффекты (для зелий с изменёнными/добавленными эффектами)
        for (var effect : potionMeta.getCustomEffects()) {
            var type = effect.getType();
            if (type != null && !effects.contains(type)) {
                effects.add(type);
            }
        }

        // 2. Базовый PotionType — используем маппинг по имени
        var baseType = potionMeta.getBasePotionType();
        if (baseType != null) {
            var mapped = POTION_TYPE_MAP.get(baseType.name().toUpperCase());
            if (mapped != null && !effects.contains(mapped)) {
                effects.add(mapped);
            }
        }

        // 3. Если всё ещё пусто — пробуем через getItemMeta() эффекты (для старых версий)
        if (effects.isEmpty()) {
            try {
                // Для Paper 1.20.5+ может быть getBasePotionType().getPotionEffects()
                if (baseType != null) {
                    for (var effect : baseType.getPotionEffects()) {
                        var type = effect.getType();
                        if (type != null && !effects.contains(type)) {
                            effects.add(type);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return effects;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        var inventory = event.getContents();
        var holder = inventory.getHolder();

        // Ищем игрока, который открыл варочную стойку
        if (!(holder instanceof org.bukkit.block.BrewingStand stand)) return;

        // Берём топливо/ингредиент — проверяем что варится
        var ingredient = inventory.getIngredient();
        if (ingredient == null) return;

        // Используем getResults() — это готовые зелья после варки
        var results = event.getResults();
        if (results == null || results.isEmpty()) return;

        // Проходим по результатам варки
        for (var item : results) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (!(item.getItemMeta() instanceof PotionMeta)) continue;

            // Получаем эффекты сваренного зелья
            var effects = getPotionEffects(item);
            if (effects.isEmpty()) continue;

            // Проверяем, есть ли игрок рядом
            var location = stand.getLocation();
            var nearbyPlayers = location.getWorld().getNearbyPlayers(location, 5);
            for (var player : nearbyPlayers) {
                var data = playerDataManager.get(player.getUniqueId());
                if (data == null) continue;

                var weeks = questsConfig.getWeeksList();

                for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
                    var week = weeks.get(weekIndex);
                    if (data.isWeekCompleted(week.id())) continue;
                    if (!isWeekUnlocked(data, weekIndex)) continue;

                    for (var task : week.tasks()) {
                        if (task.type() != TaskType.BREW) continue;

                        // Если у задачи указан конкретный эффект — проверяем его
                        if (task.potionEffectType() != null) {
                            var hasEffect = effects.stream()
                                    .anyMatch(e -> e.equals(task.potionEffectType()));
                            if (!hasEffect) continue;
                        }
                        // Если эффект не указан — засчитываем любое зелье (для обратной совместимости)

                        var currentProgress = data.getTaskProgress(week.id(), task.id());
                        if (task.isCompleted(currentProgress)) continue;

                        questManager.updateProgress(player, week.id(), task.id(), 1);
                    }
                }
            }
        }
    }

    // ==================== EAT ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        var player = event.getPlayer();
        var item = event.getItem();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.EAT) continue;
                if (task.itemMaterial() != null && task.itemMaterial() != item.getType()) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== CONSUME_POTION ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsumePotion(PlayerItemConsumeEvent event) {
        var player = event.getPlayer();
        var item = event.getItem();
        if (item.getType() != Material.POTION && item.getType() != Material.LINGERING_POTION
                && item.getType() != Material.SPLASH_POTION) return;

        var meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) return;

        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        // Получаем эффекты зелья
        var effects = potionMeta.getCustomEffects();
        if (effects.isEmpty()) {
            // Если нет кастомных эффектов, проверяем базовые
            var baseEffect = potionMeta.getBasePotionType();
            if (baseEffect == null || baseEffect.getPotionEffects().isEmpty()) return;
            effects = baseEffect.getPotionEffects().stream()
                    .map(e -> new org.bukkit.potion.PotionEffect(
                            e.getType(), e.getDuration(), e.getAmplifier()))
                    .toList();
        }

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.CONSUME_POTION) continue;
                if (task.potionEffectType() == null) continue;

                // Проверяем, есть ли нужный эффект в зелье
                var hasEffect = effects.stream()
                        .anyMatch(e -> e.getType().equals(task.potionEffectType()));
                if (!hasEffect) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== TAME ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof org.bukkit.entity.Player player)) return;

        var tamedType = event.getEntity().getType();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.TAME) continue;
                if (task.entityType() != null && task.entityType() != tamedType) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== SMELT ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent event) {
        var result = event.getResult();
        var block = event.getBlock();

        // Ищем игроков рядом с печью
        var location = block.getLocation();
        var nearbyPlayers = location.getWorld().getNearbyPlayers(location, 5);
        for (var player : nearbyPlayers) {
            var data = playerDataManager.get(player.getUniqueId());
            if (data == null) continue;

            var weeks = questsConfig.getWeeksList();

            for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
                var week = weeks.get(weekIndex);
                if (data.isWeekCompleted(week.id())) continue;
                if (!isWeekUnlocked(data, weekIndex)) continue;

                for (var task : week.tasks()) {
                    if (task.type() != TaskType.SMELT) continue;
                    if (task.itemMaterial() != null && task.itemMaterial() != result.getType()) continue;

                    var currentProgress = data.getTaskProgress(week.id(), task.id());
                    if (task.isCompleted(currentProgress)) continue;

                    questManager.updateProgress(player, week.id(), task.id(), 1);
                }
            }
        }
    }

    // ==================== BUCKET_FILL ====================
    // Храним UUID игроков, которые только что опустошили ведро (чтобы не читерить)
    private final java.util.Set<java.util.UUID> recentBucketEmpty = new java.util.HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        // Игрок опустошил ведро (поставил лаву/воду) — запоминаем
        recentBucketEmpty.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        var player = event.getPlayer();
        var bucket = event.getItemStack();
        if (bucket == null) return;

        // Проверяем, что игрок реально наполнил ведро, а не просто взял готовое
        // Если игрок не опустошал ведро перед этим — не засчитываем (читерство)
        if (!recentBucketEmpty.remove(player.getUniqueId())) return;

        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.BUCKET_FILL) continue;
                if (task.itemMaterial() != null && task.itemMaterial() != bucket.getType()) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }


    // ==================== SHEAR ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        var player = event.getPlayer();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.SHEAR) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }

    // ==================== TRADE ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(VillagerAcquireTradeEvent event) {
        // Ищем игроков рядом с жителем
        var entity = event.getEntity();
        var location = entity.getLocation();
        var nearbyPlayers = location.getWorld().getNearbyPlayers(location, 5);
        for (var player : nearbyPlayers) {
            var data = playerDataManager.get(player.getUniqueId());
            if (data == null) continue;

            var weeks = questsConfig.getWeeksList();

            for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
                var week = weeks.get(weekIndex);
                if (data.isWeekCompleted(week.id())) continue;
                if (!isWeekUnlocked(data, weekIndex)) continue;

                for (var task : week.tasks()) {
                    if (task.type() != TaskType.TRADE) continue;

                    var currentProgress = data.getTaskProgress(week.id(), task.id());
                    if (task.isCompleted(currentProgress)) continue;

                    questManager.updateProgress(player, week.id(), task.id(), 1);
                }
            }
        }
    }

    // ==================== BREED ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof org.bukkit.entity.Player player)) return;

        var bredType = event.getEntity().getType();
        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return;

        var weeks = questsConfig.getWeeksList();

        for (int weekIndex = 0; weekIndex < weeks.size(); weekIndex++) {
            var week = weeks.get(weekIndex);
            if (data.isWeekCompleted(week.id())) continue;
            if (!isWeekUnlocked(data, weekIndex)) continue;

            for (var task : week.tasks()) {
                if (task.type() != TaskType.BREED) continue;
                if (task.entityType() != null && task.entityType() != bredType) continue;

                var currentProgress = data.getTaskProgress(week.id(), task.id());
                if (task.isCompleted(currentProgress)) continue;

                questManager.updateProgress(player, week.id(), task.id(), 1);
            }
        }
    }
}
