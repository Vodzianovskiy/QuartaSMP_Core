package eu._Nightwarden.quartaSMP_Core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Утилита для работы с MiniMessage.
 * Все тексты в плагине проходят через этот класс — никакого legacy formatting.
 *
 * ВАЖНО: <prefix> НЕ является встроенным тегом MiniMessage.
 * Для сообщений с префиксом используй sendPrefixedMessage() или prefixed().
 */
public final class MiniMessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MiniMessageUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Десериализует MiniMessage-строку в Component.
     */
    public static Component deserialize(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    /**
     * Десериализует строку с заменой плейсхолдеров.
     *
     * @param miniMessage  исходная строка с плейсхолдерами вида %name%
     * @param placeholders карта замен: ключ (без %) -> значение
     */
    public static Component deserialize(String miniMessage, Map<String, String> placeholders) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }
        String resolved = resolvePlaceholders(miniMessage, placeholders);
        return MINI_MESSAGE.deserialize(resolved);
    }

    /**
     * Десериализует список MiniMessage-строк в Component, объединяя их с переносом строки.
     */
    public static Component deserializeList(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Component.empty();
        }
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            result = result.append(deserialize(lines.get(i)));
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    /**
     * Десериализует список строк с плейсхолдерами.
     */
    public static Component deserializeList(List<String> lines, Map<String, String> placeholders) {
        if (lines == null || lines.isEmpty()) {
            return Component.empty();
        }
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            result = result.append(deserialize(lines.get(i), placeholders));
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    // ====== Методы с префиксом ======

    /**
     * Создаёт Component из строки с префиксом.
     * prefix + miniMessage конкатенируются ПЕРЕД deserialize().
     */
    public static Component prefixed(String prefix, String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return deserialize(prefix);
        }
        return deserialize(prefix + miniMessage);
    }

    /**
     * Создаёт Component из строки с префиксом и плейсхолдерами.
     */
    public static Component prefixed(String prefix, String miniMessage, Map<String, String> placeholders) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return deserialize(prefix);
        }
        String resolved = resolvePlaceholders(miniMessage, placeholders);
        return deserialize(prefix + resolved);
    }

    /**
     * Отправляет сообщение с префиксом.
     */
    public static void sendPrefixedMessage(CommandSender sender, String prefix, String miniMessage) {
        sender.sendMessage(prefixed(prefix, miniMessage));
    }

    /**
     * Отправляет сообщение с префиксом и плейсхолдерами.
     */
    public static void sendPrefixedMessage(CommandSender sender, String prefix, String miniMessage, Map<String, String> placeholders) {
        sender.sendMessage(prefixed(prefix, miniMessage, placeholders));
    }

    // ====== Стандартные методы отправки ======

    /**
     * Отправляет игроку/отправителю сообщение в MiniMessage формате.
     */
    public static void sendMessage(CommandSender sender, String miniMessage) {
        sender.sendMessage(deserialize(miniMessage));
    }

    /**
     * Отправляет сообщение с плейсхолдерами.
     */
    public static void sendMessage(CommandSender sender, String miniMessage, Map<String, String> placeholders) {
        sender.sendMessage(deserialize(miniMessage, placeholders));
    }

    /**
     * Отправляет список строк как многострочное сообщение.
     */
    public static void sendMessageList(CommandSender sender, List<String> lines) {
        sender.sendMessage(deserializeList(lines));
    }

    /**
     * Отправляет список строк с плейсхолдерами.
     */
    public static void sendMessageList(CommandSender sender, List<String> lines, Map<String, String> placeholders) {
        sender.sendMessage(deserializeList(lines, placeholders));
    }

    // ====== Утилиты ======

    /**
     * Конвертирует Component в plain text (без форматирования).
     */
    public static String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Конвертирует MiniMessage строку в plain text.
     */
    public static String toPlainText(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(deserialize(miniMessage));
    }

    /**
     * Заменяет плейсхолдеры вида %name% в строке.
     */
    private static String resolvePlaceholders(String input, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }
        String result = input;
        for (var entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
