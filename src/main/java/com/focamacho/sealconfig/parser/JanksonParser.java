package com.focamacho.sealconfig.parser;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.element.JsonElement;
import blue.endless.jankson.api.element.JsonObject;
import com.focamacho.sealconfig.ConfigParser;
import com.focamacho.sealconfig.annotation.ConfigCategory;
import org.apache.commons.text.translate.UnicodeUnescaper;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "unchecked"})
public class JanksonParser extends ConfigParser {

    private static final UnicodeUnescaper unicodeUnescaper = new UnicodeUnescaper();
    private final Jankson jankson;

    public JanksonParser() {
        this(Jankson.builder().build());
    }

    public JanksonParser(Jankson jankson) {
        this.jankson = jankson;
    }

    @Override
    protected void save(Object configObject) {
        configs.forEach((classe, map) -> map.forEach((file, object) -> {
            if(configObject == object) {
                try {
                    String toSave = unicodeUnescaper.translate(jankson.load(jankson.toJson(configObject).toJson(true, true)).toJson(true, true, 0, 2));

                    if (!file.exists()) {
                        boolean mk = file.getParentFile().mkdirs();
                        boolean nf = file.createNewFile();
                    }

                    Files.write(file.toPath(), toSave.getBytes(StandardCharsets.UTF_8));
                } catch(Exception e) {
                    logger.severe("Error saving a config file:");
                    e.printStackTrace();
                }
            }
        }));
    }

    @Override
    protected <T> T createConfig(File configFile, Class<T> configClass) {
        try {
            JsonObject defaults = jankson.load(jankson.toJson(configClass.getConstructor().newInstance()).toJson(true, true));

            JsonObject configObject;
            if (!configFile.exists()) {
                boolean mk = configFile.getParentFile().mkdirs();
                boolean nf = configFile.createNewFile();
                configObject = defaults;
            } else {
                configObject = jankson.load(configFile);
                configObject = checkValues(defaults, configObject, configClass);
            }

            Files.write(configFile.toPath(), unicodeUnescaper.translate(configObject.toJson(true, true, 0, 2)).getBytes(StandardCharsets.UTF_8));
            T config = jankson.fromJson(configObject.toJson(), configClass);

            Map<File, Object> configs = this.configs.get(configClass);
            if(configs == null) {
                this.configs.put(configClass, new HashMap<>());
                configs = this.configs.get(configClass);
            }

            if(configs.get(configFile) == null) {
                configs.put(configFile, config);
            } else {
                setValues(configs.get(configFile), config);
            }

            removeClassDefaults(config, configObject);
            return config;
        } catch(Exception e) {
            logger.severe("Error loading a config file:");
            e.printStackTrace();
            return null;
        }
    }

    private JsonObject checkValues(JsonObject defaultObject, JsonObject actualObject, Class<?> configClass) {
        for (Map.Entry<String, JsonElement> entry : defaultObject.entrySet()) {
            if(!actualObject.containsKey(entry.getKey())) actualObject = applyDefaults(defaultObject, actualObject);
            //Check if the value is not a JsonObject that also needs to be check
            else if(actualObject.get(entry.getKey()) instanceof JsonObject) {
                try {
                    Field field = configClass.getDeclaredField(entry.getKey());
                    Class<?> fieldType = field.getType();
                    if(fieldType.isAnnotationPresent(ConfigCategory.class)) {
                        actualObject.put(entry.getKey(), checkValues((JsonObject) entry.getValue(), actualObject.getObject(entry.getKey()), field.getType()));
                    }
                } catch(Exception ignored) {}
            }
        }

        for (Map.Entry<String, JsonElement> entry : defaultObject.entrySet()) {
            actualObject.setComment(entry.getKey(), defaultObject.getComment(entry.getKey()));
        }

        return actualObject;
    }

    private JsonObject applyDefaults(JsonObject defaultObject, JsonObject actualObject) {
        JsonObject newObject = defaultObject.clone();
        actualObject.forEach((key, value) -> {
            if(newObject.containsKey(key)) newObject.put(key, value);
        });

        return newObject;
    }

    private void removeClassDefaults(Object config, JsonObject configObject) {
        try {
            Field[] fields = config.getClass().getFields();
            for (Field field : fields) {
                JsonObject jsonObject = configObject.getObject(field.getName());
                if(jsonObject == null) continue;

                Object obj = field.get(config);
                if(!field.getType().isAssignableFrom(Map.class)) {
                    if(configObject.containsKey(field.getName()))
                        removeClassDefaults(obj, configObject.getObject(field.getName()));
                    continue;
                }

                if(obj instanceof Map) {
                    Map<String, ?> map = (Map<String, ?>) obj;
                    map.entrySet().removeIf(entry -> !jsonObject.containsKey(entry.getKey()));
                }
            }
        } catch(Exception ignored) {}
    }

}
