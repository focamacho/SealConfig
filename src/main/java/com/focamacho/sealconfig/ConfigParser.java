package com.focamacho.sealconfig;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public abstract class ConfigParser {

    protected final Logger logger;
    protected final Map<Class<?>, Map<File, Object>> configs = new HashMap<>();

    //You can't instantiate this class
    protected ConfigParser(Logger logger) {
        this.logger = logger;
    }

    <T> T getConfig(File configFile, Class<T> classe) {
        Map<File, Object> configs = this.configs.get(classe);
        if(configs == null) return createConfig(configFile, classe);
        Object config = configs.get(configFile);
        if(config == null) return createConfig(configFile, classe);
        return (T) config;
    }

    void reload() {
        configs.forEach((classe, map) -> map.forEach((file, object) -> createConfig(file, classe)));
    }

    void save() {
        configs.forEach((classe, map) -> map.forEach((file, object) -> save(object)));
    }

    protected abstract void save(Object configObject);

    protected abstract <T> T createConfig(File configFile, Class<T> configClass);

    protected void setValues(Object configObject, Object newObject) {
        try {
            for(Field field : configObject.getClass().getFields()) {
                for(Field newField : newObject.getClass().getFields()) {
                    if(field.getName().equalsIgnoreCase(newField.getName())) {
                        if(!field.getType().isAssignableFrom(Map.class)) {
                            setValues(field.get(configObject), newField.get(newObject));
                        } else {
                            field.set(configObject, newField.get(newObject));
                        }
                    }
                }
            }
        } catch(Exception e) {
            logger.severe("Error reloading config object:");
            e.printStackTrace();
        }
    }

}
