package eu._Nightwarden.quartaSMP_Core.player;

import java.util.UUID;

/**
 * Лёгкая запись для таблицы лидеров (топ по уровню).
 * Хранит только то, что нужно для отображения в топе и админ-панели.
 * Не зависит от полного PlayerData, поэтому может жить в кэше
 * даже когда игрок оффлайн и его полные данные выгружены.
 *
 * @param uuid  UUID игрока
 * @param name  последний известный ник игрока
 * @param level уровень
 * @param exp   опыт
 */
public record LeaderboardEntry(
        UUID uuid,
        String name,
        int level,
        int exp
) {}
