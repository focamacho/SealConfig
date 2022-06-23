package com.focamacho.sealconfig;

import com.focamacho.sealconfig.parser.JanksonParser;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class SealConfig {

    static final Logger logger = Logger.getLogger("SealConfig");
    private final ConfigParser parser;

    /**
     * Default Constructor. Returns a new instance of
     * SealConfig using the Jankson Parser.
     */
    public SealConfig() {
        this(JanksonParser.class);
    }

    /**
     * Creates a new instance of Seal Config.
     *
     * @param parser the Config Parser to be used.
     */
    public SealConfig(Class<? extends ConfigParser> parser) {
        ConfigParser configParser = null;
        try {
            configParser = (ConfigParser) parser.getConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            logger.severe("Invalid Parser class.");
            e.printStackTrace();
        }
        this.parser = configParser;
    }

    /**
     * Load or creates a new config file, and returns it.
     *
     * @param configFile the config file.
     * @param classe the config class
     * @return a new object of the config.
     */
    public <T> T getConfig(File configFile, Class<T> classe) {
        return parser.getConfig(configFile, classe);
    }

    /**
     * Reloads the already existing config objects.
     * The values on the objects will be updated to the new ones.
     */
    public void reload() {
        parser.reload();
    }

    /**
     * Saves all config objects to their files.
     */
    public void save() {
        parser.save();
    }

    /**
     * Saves a specific config object to their file.
     *
     * @param configObject the config object to be saved.
     */
    public void save(Object configObject) {
        parser.save(configObject);
    }

}
