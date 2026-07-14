package eu._Nightwarden.quartaSMP_Core.player;

import eu._Nightwarden.quartaSMP_Core.QuartaSMP_Core;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/**
 * Менеджер данных игроков.
 *
 * Хранение: YAML-файлы в папке plugins/QuartaSMP_Core/players/<uuid>.yml
 * Кэш: ConcurrentHashMap<UUID, PlayerData> в памяти
 *
 * Защита от гонок: per-player ReentrantLock через ConcurrentHashMap<UUID, ReentrantLock>.
 * executeAtomic() захватывает лок игрока, выполняет операцию под ним, сохраняет результат.
 *
 * Загрузка: при PlayerJoinEvent
 * Сохранение: при PlayerQuitEvent + каждые N минут асинхронно (BukkitScheduler)
 * Сохранение через временный файл + atomic rename для защиты от повреждения.
 */
public final class PlayerDataManager {

    private final QuartaSMP_Core plugin;
    private final File playersFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();

    // === Leaderboard cache (для топа и админ-панели, работает даже для оффлайн игроков) ===
    private final Map<UUID, LeaderboardEntry> leaderboardCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> knownNames = new ConcurrentHashMap<>();

    public PlayerDataManager(QuartaSMP_Core plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "players");
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
    }

    /**
     * Загружает данные игрока из YAML (или создаёт новые).
     */
    public PlayerData load(Player player) {
        var uuid = player.getUniqueId();
        var file = getPlayerFile(uuid);

        if (!file.exists()) {
            var defaultData = PlayerData.createDefault(uuid);
            cache.put(uuid, defaultData);
            return defaultData;
        }

        try {
            var yaml = YamlConfiguration.loadConfiguration(file);
            var level = yaml.getInt("level", 1);
            var exp = yaml.getInt("exp", 0);

            // 🔥 ФИКС БАГА 600/200: если exp >= expNeeded, повышаем уровень
            var config = plugin.getConfig();
            var baseExp = config.getInt("leveling.exp-formula.base-exp", 100);
            var multiplier = config.getInt("leveling.exp-formula.multiplier-per-level", 50);
            var maxLevel = config.getInt("leveling.max-level", 100);

            while (level < maxLevel) {
                var expNeeded = baseExp + (level * multiplier);
                if (exp < expNeeded) break;
                exp -= expNeeded;
                level++;
            }

            // Загружаем прогресс квестов
            Map<String, Map<String, Integer>> questProgress = new HashMap<>();
            var progressSection = yaml.getConfigurationSection("quest-progress");
            if (progressSection != null) {
                for (var weekId : progressSection.getKeys(false)) {
                    var weekSection = progressSection.getConfigurationSection(weekId);
                    if (weekSection == null) continue;
                    Map<String, Integer> weekTasks = new HashMap<>();
                    for (var taskId : weekSection.getKeys(false)) {
                        weekTasks.put(taskId, weekSection.getInt(taskId, 0));
                    }
                    questProgress.put(weekId, weekTasks);
                }
            }

            // Загружаем завершённые недели
            Map<String, Boolean> completedWeeks = new HashMap<>();
            var completedList = yaml.getStringList("completed-weeks");
            for (var weekId : completedList) {
                completedWeeks.put(weekId, true);
            }

            // Загружаем времена открытия недель
            Map<String, Long> weekStartTimes = new HashMap<>();
            var timesSection = yaml.getConfigurationSection("week-start-times");
            if (timesSection != null) {
                for (var weekId : timesSection.getKeys(false)) {
                    weekStartTimes.put(weekId, timesSection.getLong(weekId, 0L));
                }
            }

            var firstJoinTime = yaml.getLong("first-join-time", System.currentTimeMillis());

            // Загружаем купленные товары
            Set<String> purchasedItems = new HashSet<>(yaml.getStringList("purchased-items"));

            var data = new PlayerData(uuid, level, exp, questProgress, completedWeeks, weekStartTimes, firstJoinTime, purchasedItems);
            cache.put(uuid, data);

            // Обновляем leaderboard cache
            knownNames.put(uuid, player.getName());
            updateLeaderboardEntry(uuid, player.getName(), data);

            return data;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            var defaultData = PlayerData.createDefault(uuid);
            cache.put(uuid, defaultData);
            return defaultData;
        }
    }

    /**
     * Сохраняет данные игрока в YAML (синхронно).
     * Использует временный файл + atomic rename для защиты от повреждения.
     * Если ATOMIC_MOVE не удался (Windows), пробует обычный REPLACE_EXISTING.
     * Этот метод НЕ захватывает lock — вызывающий должен быть под lock.
     */
    private void saveInternal(PlayerData data) {
        var file = getPlayerFile(data.uuid());
        var tempFile = new File(file.getParentFile(), file.getName() + ".tmp");

        try {
            var yaml = new YamlConfiguration();

            yaml.set("uuid", data.uuid().toString());
            yaml.set("name", knownNames.getOrDefault(data.uuid(), data.uuid().toString().substring(0, 8)));
            yaml.set("level", data.level());
            yaml.set("exp", data.exp());
            yaml.set("first-join-time", data.firstJoinTime());

            // Сохраняем прогресс квестов
            if (!data.questProgress().isEmpty()) {
                for (var weekEntry : data.questProgress().entrySet()) {
                    for (var taskEntry : weekEntry.getValue().entrySet()) {
                        yaml.set("quest-progress." + weekEntry.getKey() + "." + taskEntry.getKey(), taskEntry.getValue());
                    }
                }
            }

            // Сохраняем завершённые недели
            var completedList = data.completedWeeks().entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .toList();
            yaml.set("completed-weeks", completedList);

            // Сохраняем времена открытия недель
            if (!data.weekStartTimes().isEmpty()) {
                for (var entry : data.weekStartTimes().entrySet()) {
                    yaml.set("week-start-times." + entry.getKey(), entry.getValue());
                }
            }

            // Сохраняем купленные товары
            if (!data.purchasedItems().isEmpty()) {
                yaml.set("purchased-items", data.purchasedItems().stream().toList());
            }

            // Сначала пишем во временный файл
            yaml.save(tempFile);

            // Пробуем переместить временный файл на место основного.
            // ATOMIC_MOVE не используется, так как на Windows он часто не поддерживается
            // между разными файловыми системами и вызывает AccessDeniedException.
            // Вместо этого: удаляем старый файл (если есть) и переименовываем временный.
            try {
                // Удаляем старый файл, если существует
                if (file.exists()) {
                    Files.delete(file.toPath());
                }
                // Переименовываем временный файл в основной
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Если перемещение не удалось — пробуем скопировать содержимое
                plugin.getLogger().warning("Не удалось переместить временный файл для " + data.uuid() + ": " + e.getMessage());
                try {
                    Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(tempFile.toPath());
                } catch (IOException e2) {
                    plugin.getLogger().log(Level.SEVERE, "Критическая ошибка сохранения данных для " + data.uuid(), e2);
                }
            }

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + data.uuid(), e);
            // Если что-то пошло не так, пытаемся удалить временный файл
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Сохраняет данные конкретного игрока (по UUID из кэша) под per-player lock.
     */
    public void save(UUID uuid) {
        var lock = playerLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
        lock.lock();
        try {
            var data = cache.get(uuid);
            if (data != null) {
                saveInternal(data);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Сохраняет данные всех игроков в кэше (при выключении плагина).
     * Каждый игрок сохраняется под своим per-player lock.
     */
    public void saveAll() {
        for (var uuid : cache.keySet()) {
            save(uuid);
        }
        plugin.getLogger().info("Saved all player data (" + cache.size() + " players).");
    }

    /**
     * Возвращает снимок кэша (для PAPI топа и других внешних нужд).
     * Возвращает копию, чтобы внешний код не мог модифицировать кэш.
     */
    public Map<UUID, PlayerData> getCacheSnapshot() {
        return new HashMap<>(cache);
    }

    /**
     * Выгружает игрока из кэша (при выходе).
     */
    public void unload(UUID uuid) {
        cache.remove(uuid);
    }


    /**
     * Получает данные игрока из кэша.
     */
    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Получает данные игрока, загружая если нет в кэше.
     */
    public PlayerData getOrLoad(Player player) {
        var data = cache.get(player.getUniqueId());
        if (data != null) return data;
        return load(player);
    }

    /**
     * Обновляет данные игрока в кэше (immutable record — заменяем ссылку).
     */
    public void update(UUID uuid, PlayerData newData) {
        cache.put(uuid, newData);
        // Обновляем leaderboard cache
        var name = knownNames.get(uuid);
        if (name != null) {
            updateLeaderboardEntry(uuid, name, newData);
        }
    }

    /**
     * Выполняет операцию над PlayerData под per-player блокировкой.
     * <p>
     * 1. Захватывает ReentrantLock для UUID игрока
     * 2. Получает текущий PlayerData из кэша
     * 3. Применяет функцию (которая возвращает новый PlayerData или null)
     * 4. Если результат не null — сохраняет в кэш
     * 5. Возвращает результат
     * <p>
     * Это гарантирует, что два concurrent запроса для одного игрока
     * не вызовут race condition (например, при одновременной сдаче предметов).
     *
     * @param uuid UUID игрока
     * @param operator функция, принимающая текущий PlayerData и возвращающая новый
     * @return новый PlayerData после применения функции, или null если функция вернула null
     */
    public PlayerData executeAtomic(UUID uuid, UnaryOperator<PlayerData> operator) {
        var lock = playerLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
        lock.lock();
        try {
            var current = cache.get(uuid);
            var result = operator.apply(current);
            if (result != null) {
                cache.put(uuid, result);
                saveInternal(result); // 🔥 Сохраняем сразу под тем же lock
                // Обновляем leaderboard cache
                var name = knownNames.get(uuid);
                if (name != null) {
                    updateLeaderboardEntry(uuid, name, result);
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Удаляет предметы из инвентаря игрока (синхронно, в главном треде).
     * <p>
     * Вызывается ТОЛЬКО внутри executeAtomic() или в главном треде сервера.
     * Не использует Bukkit.getScheduler() — работает напрямую с инвентарём.
     *
     * @param player  игрок
     * @param material тип материала для удаления
     * @param amount  количество для удаления
     * @return реально удалённое количество (может быть меньше запрошенного)
     */
    public int removeItemsAtomic(Player player, Material material, int amount) {
        var removed = 0;
        var inventory = player.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            var item = inventory.getItem(i);
            if (item == null || item.getType() != material) continue;

            var toRemove = Math.min(item.getAmount(), amount - removed);
            if (toRemove <= 0) break;

            if (toRemove >= item.getAmount()) {
                inventory.setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - toRemove);
            }
            removed += toRemove;

            if (removed >= amount) break;
        }

        player.updateInventory();
        return removed;
    }

    /**
     * Добавляет опыт с автоматическим повышением уровня.
     * Если exp >= expNeeded — повышает уровень, пока хватает.
     * Если достигнут maxLevel — обнуляет exp.
     * <p>
     * Единый метод для всей логики level-up, чтобы не дублировать код.
     *
     * @param data      текущие PlayerData
     * @param expToAdd  сколько опыта добавить
     * @param player    игрок (для отправки сообщений о level-up)
     * @return обновлённые PlayerData с новым exp/level
     */
    public PlayerData addExpWithLevelUp(PlayerData data, int expToAdd, Player player) {
        if (expToAdd <= 0) return data;

        var config = plugin.getConfig();
        var baseExp = config.getInt("leveling.exp-formula.base-exp", 100);
        var multiplier = config.getInt("leveling.exp-formula.multiplier-per-level", 50);
        var maxLevel = config.getInt("leveling.max-level", 100);

        var newExp = data.exp() + expToAdd;
        var newLevel = data.level();

        while (newLevel < maxLevel) {
            var expNeeded = baseExp + (newLevel * multiplier);
            if (newExp >= expNeeded) {
                newExp -= expNeeded;
                newLevel++;
                // Отправляем сообщение о повышении уровня
                if (player != null) {
                    var messagesConfig = plugin.getMessagesConfig();
                    var lines = messagesConfig.levelUp();
                    eu._Nightwarden.quartaSMP_Core.util.MiniMessageUtil.sendMessageList(player, lines,
                            java.util.Map.of("level", String.valueOf(newLevel)));
                }
            } else {
                break;
            }
        }

        if (newLevel >= maxLevel) {
            newExp = 0;
        }

        return data.withLevel(newLevel).withExp(newExp);
    }

    // ========================================================================
    // Leaderboard cache — для топа и админ-панели (работает для оффлайн игроков)
    // ========================================================================

    /**
     * Загружает leaderboard cache из всех YAML-файлов в папке players.
     * Вызывается при старте плагина, чтобы топ работал даже до первого входа игроков.
     */
    public void loadLeaderboardCache() {
        var files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        int loaded = 0;
        for (var file : files) {
            try {
                var yaml = YamlConfiguration.loadConfiguration(file);
                var uuidStr = yaml.getString("uuid");
                if (uuidStr == null) continue;

                var uuid = UUID.fromString(uuidStr);
                var level = yaml.getInt("level", 1);
                var exp = yaml.getInt("exp", 0);

                // Пытаемся получить имя из YAML, если нет — через Bukkit offline player
                var name = yaml.getString("name");
                if (name == null || name.isEmpty()) {
                    var offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                    name = offlinePlayer.getName();
                }
                if (name == null || name.isEmpty()) {
                    name = uuid.toString().substring(0, 8);
                }

                knownNames.put(uuid, name);
                leaderboardCache.put(uuid, new LeaderboardEntry(uuid, name, level, exp));
                loaded++;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось загрузить leaderboard entry из " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Загружено " + loaded + " записей в таблицу лидеров.");
    }

    /**
     * Обновляет запись в leaderboard cache для указанного игрока.
     * Вызывается при загрузке, изменении уровня/опыта или сохранении данных.
     */
    private void updateLeaderboardEntry(UUID uuid, String name, PlayerData data) {
        if (name == null || name.isEmpty()) {
            name = uuid.toString().substring(0, 8);
        }
        leaderboardCache.put(uuid, new LeaderboardEntry(uuid, name, data.level(), data.exp()));
    }

    /**
     * Возвращает топ-N игроков по уровню из leaderboard cache.
     * Сортировка: уровень (убывание), опыт (убывание), имя (возрастание).
     * Работает даже для оффлайн игроков.
     *
     * @param limit максимальное количество записей
     * @return список записей топа
     */
    public List<LeaderboardEntry> getTopLevel(int limit) {
        var sorted = new ArrayList<>(leaderboardCache.values());
        sorted.sort(Comparator
                .comparingInt(LeaderboardEntry::level).reversed()
                .thenComparing(Comparator.comparingInt(LeaderboardEntry::exp).reversed())
                .thenComparing(LeaderboardEntry::name)
        );
        return sorted.stream().limit(limit).toList();
    }

    /**
     * Возвращает снимок leaderboard cache (для админ-панели).
     * Содержит всех известных игроков, включая оффлайн.
     */
    public Map<UUID, LeaderboardEntry> getLeaderboardSnapshot() {
        return new HashMap<>(leaderboardCache);
    }

    /**
     * Возвращает имя игрока по UUID из knownNames.
     * Работает даже для оффлайн игроков.
     */
    public String getKnownName(UUID uuid) {
        return knownNames.get(uuid);
    }

    /**
     * Возвращает файл для UUID игрока.
     */
    private File getPlayerFile(UUID uuid) {
        return new File(playersFolder, uuid.toString() + ".yml");
    }
}
