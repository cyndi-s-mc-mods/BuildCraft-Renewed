package buildcraft.lib.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import buildcraft.BuildCraft;

/** Loads BuildCraft's settings from {@code config/buildcraft.properties}. Every public static field of the registered
 * config classes is a setting, named {@code section.field}. The file is rewritten after loading so that it lists every
 * setting with its current value. */
public final class BCConfig {
    private BCConfig() {}

    public static void load(Path file, Map<String, Class<?>> sections) {
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                BuildCraft.LOGGER.warn("Couldn't read {}, using the default settings", file, e);
            }
        }
        Map<String, String> values = new TreeMap<>();
        sections.forEach((section, type) -> {
            for (Field field : type.getFields()) {
                int mods = field.getModifiers();
                if (!Modifier.isStatic(mods) || Modifier.isFinal(mods)) continue;
                String key = section + "." + field.getName();
                String value = props.getProperty(key);
                try {
                    if (value != null) {
                        set(field, value.trim());
                    }
                    values.put(key, String.valueOf(field.get(null)));
                } catch (ReflectiveOperationException | IllegalArgumentException e) {
                    BuildCraft.LOGGER.warn("Invalid value '{}' for {} in {}", value, key, file);
                }
            }
        });
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("# BuildCraft settings. Changes take effect when the game restarts.\n");
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }
            }
        } catch (IOException e) {
            BuildCraft.LOGGER.warn("Couldn't write {}", file, e);
        }
    }

    private static void set(Field field, String value) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type == boolean.class) {
            field.setBoolean(null, Boolean.parseBoolean(value));
        } else if (type == int.class) {
            field.setInt(null, Integer.parseInt(value));
        } else if (type == long.class) {
            field.setLong(null, Long.parseLong(value));
        } else if (type == double.class) {
            field.setDouble(null, Double.parseDouble(value));
        } else if (type == float.class) {
            field.setFloat(null, Float.parseFloat(value));
        } else if (type == String.class) {
            field.set(null, value);
        }
    }
}
