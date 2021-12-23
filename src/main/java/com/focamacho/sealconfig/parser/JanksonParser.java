package com.focamacho.sealconfig.parser;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.element.JsonElement;
import blue.endless.jankson.api.element.JsonObject;
import com.focamacho.sealconfig.ConfigParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.translate.UnicodeUnescaper;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings({"unused", "unchecked"})
public class JanksonParser extends ConfigParser {

    private static final UnicodeUnescaper unicodeUnescaper = new UnicodeUnescaper();

    public JanksonParser(Logger logger) {
        super(logger);
    }

    @Override
    protected void save(Object configObject) {
        configs.forEach((classe, map) -> map.forEach((file, object) -> {
            if(configObject == object) {
                try {
                    String toSave = unicodeUnescaper.translate(Jankson.builder().build().load(Jankson.builder().build().toJson(configObject).toJson(true, true)).toJson(true, true, 0, 2));

                    if (!file.exists()) {
                        boolean mk = file.getParentFile().mkdirs();
                        boolean nf = file.createNewFile();
                    }

                    FileUtils.write(file, toSave, StandardCharsets.UTF_8);
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
            JsonObject defaults = Jankson.builder().build().load(Jankson.builder().build().toJson(configClass.getConstructor().newInstance()).toJson(true, true));

            JsonObject configObject;
            if (!configFile.exists()) {
                boolean mk = configFile.getParentFile().mkdirs();
                boolean nf = configFile.createNewFile();
                configObject = defaults;
            } else {
                configObject = Jankson.builder().build().load(configFile);
                configObject = checkValues(defaults, configObject, configClass);
            }

            FileUtils.write(configFile, unicodeUnescaper.translate(configObject.toJson(true, true, 0, 2)), StandardCharsets.UTF_8);
            T config = Jankson.builder().build().fromJson(configObject.toJson(), configClass);

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
                    if(fieldType.isAssignableFrom(Object.class) && !fieldType.isAssignableFrom(String.class) && !fieldType.isAssignableFrom(Map.class)) {
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
