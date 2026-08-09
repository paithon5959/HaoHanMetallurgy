package vn.haohan.metallurgy.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vn.haohan.metallurgy.HaoHanMetallurgy;
import vn.haohan.metallurgy.item.CustomItem;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LanguageManager {
    private static final List<String> SUPPORTED = List.of("vi", "en");

    private final HaoHanMetallurgy plugin;
    private YamlConfiguration messages;
    private YamlConfiguration bundledMessages = new YamlConfiguration();
    private Map<String, String> itemMessages = Map.of();
    private String language;

    public LanguageManager(HaoHanMetallurgy plugin) {
        this.plugin = plugin;
        for (String code : SUPPORTED) {
            File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
            if (!file.exists()) plugin.saveResource("lang/" + code + ".yml", false);
        }
        reload();
    }

    public void reload() {
        language = normalize(plugin.getConfig().getString("language", "vi"));
        messages = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "lang/" + language + ".yml"));
        bundledMessages = new YamlConfiguration();
        try (var bundled = plugin.getResource("lang/" + language + ".yml")) {
            if (bundled != null) {
                bundledMessages = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(bundled, StandardCharsets.UTF_8));
                messages.setDefaults(bundledMessages);
            }
        } catch (Exception e) {
            plugin.getPluginLogger().warn("Could not load bundled language defaults: " + e.getMessage());
        }
        itemMessages = loadItemMessages(language);
    }

    public boolean setLanguage(String code) {
        String normalized = normalize(code);
        if (!SUPPORTED.contains(code.toLowerCase(Locale.ROOT))) return false;
        language = normalized;
        plugin.getConfig().set("language", language);
        plugin.saveConfig();
        reload();
        return true;
    }

    public String getLanguage() { return language; }
    public List<String> getSupportedLanguages() { return SUPPORTED; }

    public String text(String key) { return text(key, Map.of()); }

    public String text(String key, Map<String, ?> values) {
        String value = resolveText(messages, bundledMessages, key);
        if (value == null) value = "&cMissing message: " + key;
        for (var entry : values.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    static String resolveText(YamlConfiguration serverMessages,
                              YamlConfiguration bundledMessages,
                              String key) {
        String value = serverMessages == null ? null : serverMessages.getString(key);
        return value != null || bundledMessages == null ? value : bundledMessages.getString(key);
    }

    public List<String> list(String key) {
        return messages.getStringList(key).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .toList();
    }

    public String plain(String key) {
        return plain(key, Map.of());
    }

    public String plain(String key, Map<String, ?> values) {
        String stripped = ChatColor.stripColor(text(key, values));
        return stripped == null ? "" : stripped;
    }

    public String plainOr(String key, String fallback) {
        if (!messages.isString(key) && !bundledMessages.isString(key)) return fallback;
        return plain(key);
    }

    public String itemName(CustomItem item) {
        return itemMessages.getOrDefault(item.getTranslationKey(),
                ChatColor.stripColor(item.getDisplayName()));
    }

    public List<String> itemLore(CustomItem item) {
        List<String> result = new java.util.ArrayList<>();
        for (int index = 1; index <= item.getLore().size(); index++) {
            String key = item.getTranslationKey() + ".description." + index;
            String fallback = ChatColor.stripColor(item.getLore().get(index - 1));
            result.add(itemMessages.getOrDefault(key, fallback == null ? "" : fallback));
        }
        return List.copyOf(result);
    }

    private Map<String, String> loadItemMessages(String code) {
        try (var stream = plugin.getResource("lang/items/" + code + ".json")) {
            if (stream == null) return Map.of();
            JsonObject object = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> result = new LinkedHashMap<>();
            object.entrySet().forEach(entry -> {
                if (entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()) {
                    result.put(entry.getKey(), entry.getValue().getAsString());
                }
            });
            return Map.copyOf(result);
        } catch (Exception e) {
            plugin.getPluginLogger().warn("Could not load item translations for " + code
                    + ": " + e.getMessage());
            return Map.of();
        }
    }

    private String normalize(String code) {
        if (code == null) return "vi";
        String normalized = code.toLowerCase(Locale.ROOT);
        return SUPPORTED.contains(normalized) ? normalized : "vi";
    }
}
