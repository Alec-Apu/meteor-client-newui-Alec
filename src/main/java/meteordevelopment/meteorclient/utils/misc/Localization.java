/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Display-only localization for the Chinese distribution. Never translate persisted IDs or user input. */
public final class Localization {
    private static final Map<String, String> TRANSLATIONS = load();

    private Localization() {}

    public static String text(String english) {
        return english == null ? null : TRANSLATIONS.getOrDefault(english, english);
    }

    /** Context-specific enum labels; enum constants and their serialized strings stay unchanged. */
    public static String value(Object value) {
        if (value instanceof Enum<?> option
            && option.getDeclaringClass().getName().equals("meteordevelopment.meteorclient.utils.entity.EntityAgeTest")
            && option.name().equals("Both")) return text("Adults and babies");
        return text(String.valueOf(value));
    }

    private static Map<String, String> load() {
        Map<String, String> result = new HashMap<>();
        var type = new TypeToken<Map<String, String>>() {}.getType();
        try (var en = Localization.class.getResourceAsStream("/assets/meteor-client/lang/en_us.json");
             var zh = Localization.class.getResourceAsStream("/assets/meteor-client/lang/zh_cn.json")) {
            if (en == null || zh == null) throw new IllegalStateException("Missing Meteor language resources");
            Gson gson = new Gson();
            Map<String, String> english = gson.fromJson(new InputStreamReader(en, StandardCharsets.UTF_8), type);
            Map<String, String> chinese = gson.fromJson(new InputStreamReader(zh, StandardCharsets.UTF_8), type);
            english.forEach((key, value) -> {
                String translated = chinese.get(key);
                if (translated != null) result.put(value, translated);
            });
        } catch (Exception e) {
            MeteorClient.LOG.error("Could not load Meteor localization; using English display fallback", e);
        }
        return Map.copyOf(result);
    }
}
