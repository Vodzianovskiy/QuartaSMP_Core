package eu._Nightwarden.quartaSMP_Core.config;

import eu._Nightwarden.quartaSMP_Core.quest.ItemReward;
import eu._Nightwarden.quartaSMP_Core.quest.QuestTask;
import eu._Nightwarden.quartaSMP_Core.quest.QuestWeek;
import eu._Nightwarden.quartaSMP_Core.quest.TaskType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



/**
 * Обёртка над quests.yml.
 * Парсит недели и задачи в объекты QuestWeek.
 */
public final class QuestsConfig {

    private final FileConfiguration yaml;
    private final Map<String, QuestWeek> weeks = new LinkedHashMap<>();

    public QuestsConfig(FileConfiguration yaml) {
        this.yaml = yaml;
        loadWeeks();
    }

    /**
     * Парсит все недели из конфига.
     */
    private void loadWeeks() {
        weeks.clear();
        var weeksSection = yaml.getConfigurationSection("weeks");
        if (weeksSection == null) {
            return;
        }

        for (var weekKey : weeksSection.getKeys(false)) {
            var weekSection = weeksSection.getConfigurationSection(weekKey);
            if (weekSection == null) continue;

            var displayName = weekSection.getString("display-name", "Week " + weekKey);
            var questPoints = weekSection.getInt("quest-points", 0);

            var tasks = parseTasks(weekSection.getConfigurationSection("tasks"));
            var weekRewards = parseRewards(weekSection.getConfigurationSection("rewards"));
            var week = new QuestWeek(weekKey, displayName, questPoints, tasks, weekRewards);

            weeks.put(weekKey, week);
        }
    }

    /**
     * Парсит задачи внутри недели.
     */
    private List<QuestTask> parseTasks(ConfigurationSection tasksSection) {
        var tasks = new ArrayList<QuestTask>();
        if (tasksSection == null) return tasks;

        for (var taskKey : tasksSection.getKeys(false)) {
            var taskSection = tasksSection.getConfigurationSection(taskKey);
            if (taskSection == null) continue;

            var task = parseTask(taskKey, taskSection);
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    /**
     * Парсит одну задачу.
     */
    private QuestTask parseTask(String taskKey, ConfigurationSection section) {
        var typeStr = section.getString("type");
        if (typeStr == null) return null;

        TaskType type;
        try {
            type = TaskType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        var displayName = section.getString("display-name", taskKey);
        var materialStr = section.getString("material", "STONE");
        var material = Material.getMaterial(materialStr.toUpperCase());
        if (material == null) material = Material.STONE;

        var targetAmount = section.getInt("target-amount", 1);

        // Парсим специфичные для типа поля
        Material itemMaterial = null;
        if (section.contains("item-material")) {
            var im = section.getString("item-material");
            if (im != null) itemMaterial = Material.getMaterial(im.toUpperCase());
        }

        Material craftMaterial = null;
        if (section.contains("craft-material")) {
            var cm = section.getString("craft-material");
            if (cm != null) craftMaterial = Material.getMaterial(cm.toUpperCase());
        }

        EntityType entityType = null;
        if (section.contains("entity-type")) {
            var et = section.getString("entity-type");
            if (et != null) {
                try {
                    entityType = EntityType.valueOf(et.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Парсим block-material для BLOCK_BREAK / BLOCK_PLACE
        Material blockMaterial = null;
        if (section.contains("block-material")) {
            var bm = section.getString("block-material");
            if (bm != null) blockMaterial = Material.getMaterial(bm.toUpperCase());
        }

        // Парсим potion-effect для CONSUME_POTION
        PotionEffectType potionEffectType = null;
        if (section.contains("potion-effect")) {
            var pe = section.getString("potion-effect");
            if (pe != null) {
                potionEffectType = PotionEffectType.getByName(pe.toUpperCase());
            }
        }

        var rewardExp = section.getInt("reward-exp", 0);
        var taskRewards = parseRewards(section.getConfigurationSection("rewards"));

        return new QuestTask(
                taskKey,
                type,
                displayName,
                material,
                targetAmount,
                itemMaterial,
                craftMaterial,
                entityType,
                blockMaterial,
                potionEffectType,
                rewardExp,
                taskRewards
        );

    }

    /**
     * Парсит список наград из секции rewards.
     * Поддерживает два формата:
     * <p>
     * 1. Предметная награда:
     *   rewards:
     *     reward_1:
     *       material: DIAMOND
     *       amount: 5
     * <p>
     * 2. Командная награда:
     *   rewards:
     *     reward_1:
     *       command: "give %player% minecraft:netherite_sword{Enchantments:[{id:sharpness,lvl:5}]}"
     */
    private List<ItemReward> parseRewards(ConfigurationSection rewardsSection) {
        if (rewardsSection == null) return Collections.emptyList();

        var rewards = new ArrayList<ItemReward>();
        for (var key : rewardsSection.getKeys(false)) {
            var entry = rewardsSection.getConfigurationSection(key);
            if (entry == null) continue;

            // Проверяем, командная ли это награда
            var command = entry.getString("command");
            if (command != null && !command.isBlank()) {
                rewards.add(new ItemReward(command));
                continue;
            }

            // Иначе — предметная награда
            var materialStr = entry.getString("material");
            if (materialStr == null) continue;

            var material = Material.getMaterial(materialStr.toUpperCase());
            if (material == null) continue;

            var amount = entry.getInt("amount", 1);
            if (amount <= 0) continue;

            rewards.add(new ItemReward(material, amount));
        }
        return rewards;
    }


    /**
     * Возвращает все загруженные недели (сохраняет порядок из YAML).
     */
    public Map<String, QuestWeek> getWeeks() {

        return Map.copyOf(weeks);
    }

    /**
     * Возвращает список недель в порядке их объявления в YAML.
     */
    public List<QuestWeek> getWeeksList() {
        return List.copyOf(weeks.values());
    }

    /**
     * Перезагружает недели из YAML.
     */
    public void reload() {
        loadWeeks();
    }
}
