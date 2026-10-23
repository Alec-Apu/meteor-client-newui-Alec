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
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/** Display-only localization for the Chinese distribution. Never translate persisted IDs or user input. */
public final class Localization {
    private static final Map<String, String> TRANSLATIONS = load();
    private static final Map<String, String> SOURCES = sourceMap();
    private static final List<Binding<?>> BINDINGS = new ArrayList<>();
    private static Language language = Language.SimplifiedChinese;
    private static int revision;

    public enum Language {
        SimplifiedChinese, English;

        @Override public String toString() {
            return this == SimplifiedChinese ? "zh_cn" : "en_us";
        }
    }

    private Localization() {}

    public static String text(String english) {
        String source = source(english);
        return source == null || language == Language.English ? source : TRANSLATIONS.getOrDefault(source, source);
    }

    /** Recover only strings returned by this translator; never reverse-translate arbitrary user text. */
    public static String source(String display) {
        return SOURCES.getOrDefault(display, display);
    }

    public static String chineseText(String english) {
        return TRANSLATIONS.getOrDefault(source(english), english);
    }

    /** Brigadier's cached literal messages lose string identity when converted to plain text. */
    public static String commandText(String message) {
        if (language == Language.English) {
            for (var entry : TRANSLATIONS.entrySet()) {
                if (entry.getValue().equals(message)) return entry.getKey();
            }
        }
        return text(message);
    }

    private static Map<String, String> sourceMap() {
        Map<String, String> result = new IdentityHashMap<>();
        TRANSLATIONS.forEach((english, chinese) -> result.put(chinese, english));
        return result;
    }

    public static Language language() { return language; }
    public static int revision() { return revision; }

    public static void setLanguage(Language selected) {
        if (selected == null || selected == language) return;
        language = selected;
        BINDINGS.removeIf(binding -> !binding.refresh());
        revision++;
    }

    /** Weak owners prevent deleted HUD elements and discarded settings from being retained. */
    public static <T> void bind(T owner, Consumer<T> refresh) {
        BINDINGS.add(new Binding<>(new WeakReference<>(owner), refresh));
        refresh.accept(owner);
    }

    private record Binding<T>(WeakReference<T> owner, Consumer<T> refreshAction) {
        boolean refresh() {
            T value = owner.get();
            if (value == null) return false;
            refreshAction.accept(value);
            return true;
        }
    }

    /** Context-specific enum labels; enum constants and their serialized strings stay unchanged. */
    public static String value(Object value) {
        if (value instanceof Language selected) return selected == Language.SimplifiedChinese ? "简体中文" : "English";
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
