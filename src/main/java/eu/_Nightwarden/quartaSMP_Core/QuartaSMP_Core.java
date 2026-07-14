package eu._Nightwarden.quartaSMP_Core;

import eu._Nightwarden.quartaSMP_Core.command.QuartaSMPCommand;
import eu._Nightwarden.quartaSMP_Core.config.ConfigManager;
import eu._Nightwarden.quartaSMP_Core.config.MessagesConfig;
import eu._Nightwarden.quartaSMP_Core.config.QuestsConfig;
import eu._Nightwarden.quartaSMP_Core.config.ShopConfig;
import eu._Nightwarden.quartaSMP_Core.hook.QuartaSMPExpansion;
import eu._Nightwarden.quartaSMP_Core.listener.CraftListener;
import eu._Nightwarden.quartaSMP_Core.listener.GUIListener;
import eu._Nightwarden.quartaSMP_Core.listener.KillListener;
import eu._Nightwarden.quartaSMP_Core.listener.QuestListeners;
import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import eu._Nightwarden.quartaSMP_Core.quest.QuestManager;
import eu._Nightwarden.quartaSMP_Core.quest.TaskHandlerRegistry;
import eu._Nightwarden.quartaSMP_Core.shop.ShopAccessService;
import eu._Nightwarden.quartaSMP_Core.shop.ShopManager;
import eu._Nightwarden.quartaSMP_Core.shop.ShopRewardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;


/**
 * Главный класс плагина QuartaSMP_Core.
 *
 * Отвечает за:
 * - Инициализацию всех менеджеров и конфигов
 * - Регистрацию команд и listener'ов
 * - Сохранение данных при выключении
 * - Периодическое автосохранение данных игроков
 *
 * ConfigManager отвечает ТОЛЬКО за сырые FileConfiguration.
 * Обёртки (MessagesConfig, QuestsConfig, ShopConfig) создаются здесь.
 */
public final class QuartaSMP_Core extends JavaPlugin {

    // Менеджеры
    private ConfigManager configManager;
    private MessagesConfig messagesConfig;
    private QuestsConfig questsConfig;
    private ShopConfig shopConfig;
    private PlayerDataManager playerDataManager;
    private TaskHandlerRegistry taskHandlerRegistry;
    private QuestManager questManager;
    private ShopManager shopManager;
    private ShopAccessService shopAccessService;
    private ShopRewardService shopRewardService;

    @Override
    public void onEnable() {
        var startTime = System.currentTimeMillis();
        getLogger().info("=== QuartaSMP_Core загружается... ===");

        // 1. Загружаем конфиги
        initConfigs();

        // 2. Инициализируем менеджеры
        initManagers();

        // 3. Регистрируем команды
        registerCommands();

        // 4. Регистрируем listener'ы
        registerListeners();

        // 5. Регистрируем PAPI расширение
        registerPlaceholderAPI();

        // 6. Загружаем leaderboard cache из YAML-файлов (для топа и админ-панели)
        playerDataManager.loadLeaderboardCache();

        // 8. Загружаем данные для игроков, которые уже онлайн (после plugman reload)
        //    Обычно PlayerJoinEvent загружает данные, но при reload'е игроки уже на сервере
        for (var player : getServer().getOnlinePlayers()) {
            playerDataManager.load(player);
            getLogger().info("Загружены данные для онлайн-игрока: " + player.getName());
        }

        // 9. Запускаем автосохранение
        startAutoSave();

        var elapsed = System.currentTimeMillis() - startTime;
        getLogger().info("=== QuartaSMP_Core загружен за " + elapsed + "ms ===");

    }

    @Override
    public void onDisable() {
        getLogger().info("=== QuartaSMP_Core выключается... ===");

        // Сохраняем все данные игроков
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        getLogger().info("=== QuartaSMP_Core выключен ===");
    }

    /**
     * Инициализация конфигов.
     * ConfigManager загружает сырые YAML-файлы (создаёт если нет, мержит новые ключи),
     * затем мы создаём обёртки.
     */
    private void initConfigs() {
        // Загружаем сырые FileConfiguration через ConfigManager
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Создаём объекты-обёртки из сырых FileConfiguration
        messagesConfig = new MessagesConfig(configManager.getMessagesFile());
        questsConfig = new QuestsConfig(configManager.getQuestsFile());
        shopConfig = new ShopConfig(configManager.getShopFile());

        getLogger().info("Конфиги загружены.");
    }


    /**
     * Перезагрузка всех конфигов (для /qsmp reload).
     * Сначала перезагружаем сырые файлы через ConfigManager,
     * затем пересоздаём обёртки.
     */
    public void reloadConfigs() {
        configManager.reloadAll();

        // Пересоздаём обёртки из обновлённых сырых FileConfiguration
        messagesConfig = new MessagesConfig(configManager.getMessagesFile());
        questsConfig = new QuestsConfig(configManager.getQuestsFile());
        shopConfig = new ShopConfig(configManager.getShopFile());

        getLogger().info("Конфиги перезагружены.");
    }

    /**
     * Инициализация менеджеров.
     */
    private void initManagers() {
        playerDataManager = new PlayerDataManager(this);
        taskHandlerRegistry = new TaskHandlerRegistry();
        questManager = new QuestManager(this, playerDataManager, questsConfig, messagesConfig, getConfig(), taskHandlerRegistry);
        shopAccessService = new ShopAccessService();
        shopRewardService = new ShopRewardService();
        shopManager = new ShopManager(shopConfig, playerDataManager, messagesConfig, shopAccessService, shopRewardService);

        getLogger().info("Менеджеры инициализированы.");
    }

    /**
     * Регистрация команд.
     */
    private void registerCommands() {
        var command = getCommand("quartasmp");
        if (command != null) {
            var executor = new QuartaSMPCommand(this, getConfig(), playerDataManager, messagesConfig);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        getLogger().info("Команды зарегистрированы.");
    }

    /**
     * Регистрация listener'ов.
     */
    private void registerListeners() {
        var pm = getServer().getPluginManager();

        pm.registerEvents(new GUIListener(playerDataManager, questManager,
                shopManager, messagesConfig, getConfig(), shopAccessService), this);

        pm.registerEvents(new CraftListener(questManager, questsConfig, playerDataManager), this);
        pm.registerEvents(new KillListener(questManager, questsConfig, playerDataManager), this);
        pm.registerEvents(new QuestListeners(questManager, questsConfig, playerDataManager), this);


        // PlayerJoinEvent/PlayerQuitEvent для загрузки/сохранения данных
        pm.registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                playerDataManager.load(event.getPlayer());
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                var player = event.getPlayer();
                var uuid = player.getUniqueId();

                // ВАЖНО: сначала закрываем любые открытые кастомные GUI,
                // чтобы не потерять промежуточное состояние транзакции
                player.closeInventory();

                // Затем сохраняем данные
                playerDataManager.save(uuid);

                // И выгружаем из кэша
                playerDataManager.unload(uuid);
            }
        }, this);

        getLogger().info("Listener'ы зарегистрированы.");
    }

    /**
     * Запускает автосохранение данных игроков каждые 5 минут.
     */
    private void startAutoSave() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerDataManager != null) {
                playerDataManager.saveAll();
            }
        }, 20L * 60 * 5, 20L * 60 * 5); // Каждые 5 минут

        getLogger().info("Автосохранение запущено (каждые 5 минут).");
    }

    /**
     * Регистрирует PAPI расширения, если PlaceholderAPI установлен.
     * Регистрируются два identifier: "qcore" (основной) и "core" (алиас).
     */
    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI не найден. Плейсхолдеры %qcore_*% и %core_*% недоступны.");
            return;
        }

        var registered = 0;

        // Регистрируем "qcore" (основной)
        var expansionQcore = new QuartaSMPExpansion("qcore", playerDataManager);
        if (expansionQcore.register()) {
            registered++;
        } else {
            getLogger().warning("Не удалось зарегистрировать PAPI расширение 'qcore'.");
        }

        // Регистрируем "core" (алиас для удобства)
        var expansionCore = new QuartaSMPExpansion("core", playerDataManager);
        if (expansionCore.register()) {
            registered++;
        } else {
            getLogger().warning("Не удалось зарегистрировать PAPI расширение 'core'.");
        }

        if (registered > 0) {
            getLogger().info("PAPI расширения зарегистрированы: %qcore_level%, %core_level%, %qcore_exp%, %core_exp% и другие.");
        }
    }

    /**
     * Сохраняет ресурс из JAR, если его нет на диске.
     */
    private void saveResourceIfNotExists(String path) {
        var file = new java.io.File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }


    // ====== Геттеры для доступа из других классов ======

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public QuestsConfig getQuestsConfig() {
        return questsConfig;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }
}
