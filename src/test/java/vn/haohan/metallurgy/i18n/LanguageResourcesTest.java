package vn.haohan.metallurgy.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vn.haohan.metallurgy.item.CustomItem;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageResourcesTest {
    @Test
    void everyCustomItemHasCompleteEnglishAndVietnameseText() throws Exception {
        for (String language : new String[]{"en", "vi"}) {
            JsonObject translations = readJson("lang/items/" + language + ".json");
            for (CustomItem item : CustomItem.values()) {
                assertTrue(translations.has(item.getTranslationKey()),
                        language + " missing name for " + item.getId());
                for (int line = 1; line <= item.getLore().size(); line++) {
                    assertTrue(translations.has(item.getTranslationKey() + ".description." + line),
                            language + " missing lore " + line + " for " + item.getId());
                }
            }
        }
    }

    @Test
    void forgeGuideHasCoreKeysInBothLanguages() throws Exception {
        for (String language : new String[]{"en", "vi"}) {
            try (var stream = getClass().getClassLoader().getResourceAsStream(
                    "lang/" + language + ".yml")) {
                assertNotNull(stream);
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                for (String key : new String[]{
                        "guide.title", "guide.headings.recipes", "guide.headings.fluxes",
                        "guide.headings.output-chances", "guide.notes.bellows",
                        "guide.category.info", "guide.button.open-guide"}) {
                    assertTrue(yaml.isString(key), language + " missing " + key);
                }
            }
        }
    }

    @Test
    void oldServerLanguageFileReceivesBundledGuideDefaults() throws Exception {
        YamlConfiguration oldServerFile = new YamlConfiguration();
        oldServerFile.set("language.changed", "old value");

        YamlConfiguration bundledMain = readYaml("lang/en.yml");
        oldServerFile.setDefaults(bundledMain);

        assertEquals("Ancient Forge", oldServerFile.getString("guide.title"));
        assertEquals("FLUX / CATALYSTS",
                oldServerFile.getString("guide.headings.fluxes"));
        assertEquals("old value", oldServerFile.getString("language.changed"));

        YamlConfiguration noDefaultsAttached = new YamlConfiguration();
        assertEquals("Ancient Forge", LanguageManager.resolveText(
                noDefaultsAttached, bundledMain, "guide.title"));
    }

    private JsonObject readJson(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private YamlConfiguration readYaml(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream);
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
