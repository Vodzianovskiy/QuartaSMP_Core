package eu._Nightwarden.quartaSMP_Core.hook;

import eu._Nightwarden.quartaSMP_Core.player.PlayerDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PlaceholderAPI расширение для QuartaSMP_Core.
 * <p>
 * Поддерживает два identifier: "qcore" и "core".
 * <p>
 * Плейсхолдеры:
 * <pre>
 *   %qcore_level% / %core_level%              — уровень игрока
 *   %qcore_lvl% / %core_lvl%                  — уровень (алиас)
 *   %qcore_player_level% / %core_player_level% — уровень (алиас)
 *   %qcore_exp% / %core_exp%                  — опыт игрока
 *   %qcore_experience% / %core_experience%    — опыт (алиас)
 *   %qcore_level_formatted% / %core_level_formatted% — "12 ур."
 *   %qcore_level_prefix% / %core_level_prefix%       — "&7[&b12 LVL&7]"
 *   %qcore_top_level_1% ... %qcore_top_level_10%     — топ-10 игроков по уровню
 * </pre>
 * <p>
 * Формат топ-плейсхолдера: "Игрок - Уровень" (например "Steve - 15").
 * Если игроков меньше N, возвращает "—".
 */
public final class QuartaSMPExpansion extends PlaceholderExpansion {

    private final String identifier;
    private final PlayerDataManager playerDataManager;

    public QuartaSMPExpansion(String identifier, PlayerDataManager playerDataManager) {
        this.identifier = identifier;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return "_Nightwarden";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // не выгружать при перезагрузке PAPI
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "0";

        var data = playerDataManager.get(player.getUniqueId());
        if (data == null) return "0";

        // %..._level% / %..._lvl% / %..._player_level%
        if (params.equals("level") || params.equals("lvl") || params.equals("player_level")) {
            return String.valueOf(data.level());
        }

        // %..._exp% / %..._experience%
        if (params.equals("exp") || params.equals("experience")) {
            return String.valueOf(data.exp());
        }

        // %..._level_formatted% — "12 ур."
        if (params.equals("level_formatted")) {
            return formatLevel(data.level());
        }

        // %..._level_prefix% — "&7[&b12 LVL&7]"
        if (params.equals("level_prefix")) {
            return formatLevelPrefix(data.level());
        }

        // %..._top_level_1% ... %..._top_level_10%
        if (params.startsWith("top_level_")) {
            try {
                var position = Integer.parseInt(params.substring("top_level_".length()));
                if (position < 1 || position > 10) return "—";
                return getTopPlayer(position);
            } catch (NumberFormatException e) {
                return "—";
            }
        }

        return null;
    }

    /**
     * Форматирует уровень в читаемый вид: "12 ур."
     */
    private String formatLevel(int level) {
        return level + " ур.";
    }

    /**
     * Форматирует уровень в префикс: "&7[&b12 LVL&7]"
     */
    private String formatLevelPrefix(int level) {
        return "&7[&b" + level + " LVL&7]";
    }

    /**
     * Возвращает строку для указанной позиции в топе.
     * Формат: "Игрок - Уровень"
     */
    private String getTopPlayer(int position) {
        var top = getTopPlayers(10);
        if (position > top.size()) return "—";
        var entry = top.get(position - 1);
        return entry.getKey() + " - " + entry.getValue();
    }

    /**
     * Собирает топ-N игроков по уровню из leaderboard cache.
     * Работает даже для оффлайн игроков.
     * Сортировка: уровень (по убыванию), затем опыт (по убыванию).
     */
    private List<Map.Entry<String, Integer>> getTopPlayers(int limit) {
        return playerDataManager.getTopLevel(limit).stream()
                .map(entry -> Map.<String, Integer>entry(entry.name(), entry.level()))
                .collect(Collectors.toList());
    }
}
