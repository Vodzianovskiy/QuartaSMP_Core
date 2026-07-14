package eu._Nightwarden.quartaSMP_Core.config;

import eu._Nightwarden.quartaSMP_Core.QuartaSMP_Core;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Менеджер конфигурационных файлов.
 * Загружает/сохраняет/перезагружает сырые YAML-конфиги плагина.
 *
 * ConfigManager отвечает ТОЛЬКО за загрузку и перезагрузку сырых FileConfiguration.
 * Объекты-обёртки (MessagesConfig, QuestsConfig, ShopConfig) создаются
 * и хранятся в главном классе плагина (QuartaSMP_Core).
 *
 * При загрузке выполняет синхронизацию с дефолтными значениями из JAR:
 * - Если файла нет — создаётся из resources
 * - Если файл есть — добавляются новые ключи из JAR (если их нет на диске)
 * - Существующие значения НЕ перезаписываются
 * - Ключи на диске, которых нет в JAR, НЕ удаляются
 *
 * ВАЖНО: для config.yml используется Bukkit API (plugin.getConfig()),
 * для остальных — ручная загрузка через YamlConfiguration.
 */

public final class ConfigManager {

    private final QuartaSMP_Core plugin;

    private FileConfiguration config;
    private FileConfiguration messagesFile;
    private FileConfiguration questsFile;
    private FileConfiguration shopFile;

    public ConfigManager(QuartaSMP_Core plugin) {
        this.plugin = plugin;
    }

    /**
     * Загружает или создаёт все конфигурационные файлы.
     * Выполняет полную синхронизацию с дефолтными значениями из JAR.
     */
    public void loadAll() {
        // Главный config.yml — используем Bukkit API
        loadConfigYml();

        // messages.yml, quests.yml, shop.yml — ручная загрузка с синхронизацией
        messagesFile = loadAndSync("messages.yml");
        questsFile = loadAndSync("quests.yml");
        shopFile = loadAndSync("shop.yml");

        plugin.getLogger().info("All configuration files loaded successfully.");
    }

    /**
     * Загружает config.yml через Bukkit API с полной синхронизацией.
     */
    private void loadConfigYml() {
        // saveDefaultConfig() создаёт файл если его нет
        plugin.saveDefaultConfig();
        // reloadConfig() загружает с диска в Bukkit-кэш
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Загружаем дефолтный из resources и синхронизируем
        var defaults = loadDefaultConfig("config.yml");
        if (defaults != null) {
            boolean changed = syncSection(config, defaults);
            if (changed) {
                plugin.saveConfig();
            }
        }
    }

    /**
     * Перезагружает все конфиги из файлов с полной синхронизацией.
     * То же самое, что loadAll(), но без лишних проверок существования файлов.
     */
    public void reloadAll() {
        // Перезагружаем config.yml через Bukkit API
        plugin.reloadConfig();
        config = plugin.getConfig();

        var defaults = loadDefaultConfig("config.yml");
        if (defaults != null) {
            if (syncSection(config, defaults)) {
                plugin.saveConfig();
            }
        }

        // Перезагружаем остальные файлы с синхронизацией
        messagesFile = loadAndSync("messages.yml");
        questsFile = loadAndSync("quests.yml");
        shopFile = loadAndSync("shop.yml");

        plugin.getLogger().info("All configuration files reloaded successfully.");
    }


    /**
     * Загружает файл конфига, создавая если нет.
     * Если файл существует — синхронизирует с дефолтным из resources:
     * - Удаляет ключи, которых нет в JAR
     * - Добавляет новые ключи из JAR
     * - Сохраняет существующие значения
     *
     * 🔥 ФИКС: для shop.yml — полная перезапись с диска из JAR,
     * чтобы удалённые/изменённые товары гарантированно обновились.
     */
    private FileConfiguration loadAndSync(String fileName) {
        var file = new File(plugin.getDataFolder(), fileName);

        // 🔥 ФИКС: shop.yml перезаписываем полностью из JAR
        if (fileName.equals("shop.yml")) {
            if (file.exists()) file.delete();
            plugin.saveResource(fileName, false);
            return YamlConfiguration.loadConfiguration(file);
        }

        if (!file.exists()) {
            // Файла нет — просто сохраняем дефолтный
            plugin.saveResource(fileName, false);
            return YamlConfiguration.loadConfiguration(file);
        }

        // Файл есть — загружаем существующий
        var onDisk = YamlConfiguration.loadConfiguration(file);

        // Загружаем дефолтный из resources
        var defaults = loadDefaultConfig(fileName);
        if (defaults != null) {
            // Полная синхронизация: удаляем лишнее, добавляем новое
            boolean changed = syncSection(onDisk, defaults);
            if (changed) {
                // Сохраняем обратно на диск
                try {
                    onDisk.save(file);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not save synced config: " + fileName + " - " + e.getMessage());
                }
            }
        }

        return onDisk;
    }

    /**
     * Загружает дефолтный конфиг из resources плагина.
     */
    private FileConfiguration loadDefaultConfig(String fileName) {
        try (InputStream in = plugin.getResource(fileName)) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load default config from resources: " + fileName);
            return null;
        }
    }

    /**
     * ДОБАВЛЯЕТ в target ключи из source, которых нет в target.
     * Для вложенных секций — рекурсивный вызов.
     *
     * ВАЖНО: НЕ удаляет ключи из target, которых нет в source.
     * Это позволяет пользователям добавлять свои задачи/недели,
     * не боясь что они будут удалены при перезагрузке.
     *
     * Существующие значения НЕ перезаписываются (если ключ есть в обоих).
     *
     * @return true если были изменения
     */
    private boolean syncSection(ConfigurationSection target, ConfigurationSection source) {
        boolean changed = false;

        // Добавляем/синхронизируем ключи из source
        for (var key : source.getKeys(false)) {
            if (target.isConfigurationSection(key) && source.isConfigurationSection(key)) {
                // Рекурсивно синхронизируем вложенные секции
                changed |= syncSection(target.getConfigurationSection(key), source.getConfigurationSection(key));
            } else if (!target.contains(key)) {
                // Ключа нет в target — добавляем из source
                target.set(key, source.get(key));
                changed = true;
            }
            // Если ключ есть в обоих и это не секция — оставляем значение target как есть
        }

        return changed;
    }


    // ====== Геттеры для сырых FileConfiguration ======

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * @return сырой FileConfiguration из messages.yml
     */
    public FileConfiguration getMessagesFile() {
        return messagesFile;
    }

    /**
     * @return сырой FileConfiguration из quests.yml
     */
    public FileConfiguration getQuestsFile() {
        return questsFile;
    }

    /**
     * @return сырой FileConfiguration из shop.yml
     */
    public FileConfiguration getShopFile() {
        return shopFile;
    }
}
