package com.focamacho.sealconfig;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.translate.UnicodeUnescaper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Classe para criação de arquivos
 * de configuração de forma fácil.
 */
@SuppressWarnings({"unused", "deprecation", "unchecked"})
public class SealConfig {

    private static final Logger logger = Logger.getLogger("SealConfig");
    private static final UnicodeUnescaper unicodeUnescaper = new UnicodeUnescaper();

    private final Map<Class<?>, Map<File, Object>> configs = new HashMap<>();

    /**
     * Construtor inicial.
     */
    public SealConfig() {}

    /**
     * Carrega ou cria o arquivo de configuração, e
     * retorna seu valor.
     * @param configFile o arquivo de configuração.
     * @param classe a classe de configuração
     * @return um objeto da classe de configuração.
     */
    public <T> T getConfig(File configFile, Class<T> classe) {
        Map<File, Object> configs = this.configs.get(classe);
        if(configs == null) return createConfig(configFile, classe);
        Object config = configs.get(configFile);
        if(config == null) return createConfig(configFile, classe);
        return (T) config;
    }

    /**
     * Recarrega as configurações criadas.
     */
    public void reload() {
        configs.forEach((classe, map) -> map.forEach((file, object) -> createConfig(file, classe)));
    }

    /**
     * Salva todas as configurações criadas.
     */
    public void save() {
        configs.forEach((classe, map) -> map.forEach((file, object) -> save(object)));
    }

    /**
     * Salva a configuração do objeto passado
     * como parâmetro.
     * @param configObject o objeto de configuração para
     *                     ser salvo.
     */
    public void save(Object configObject) {
        configs.forEach((classe, map) -> map.forEach((file, object) -> {
           if(configObject == object) {
               try {
                   String toSave = unicodeUnescaper.translate(Jankson.builder().build().load(Jankson.builder().build().toJson(configObject).toJson(true, true, 0)).toJson(true, true, 0));

                   if (!file.exists()) {
                       boolean mk = file.getParentFile().mkdirs();
                       boolean nf = file.createNewFile();
                   }

                   FileUtils.write(file, toSave, StandardCharsets.UTF_8);
               } catch(Exception e) {
                   logger.severe("Erro ao salvar um arquivo de configuração:");
                   e.printStackTrace();
               }
           }
        }));
    }

    /**
     * Cria o arquivo de configuração, e
     * retorna o seu valor.
     *
     * Esse método é somente utilizado privadamente
     * pelo getConfig().
     *
     * @param configFile o arquivo de configuração.
     * @param configClass a classe de configuração
     * @return um objeto da classe de configuração.
     */
    private <T> T createConfig(File configFile, Class<T> configClass) {
        try {
            JsonObject defaults = Jankson.builder().build().load(Jankson.builder().build().toJson(configClass.newInstance()).toJson(true, true, 0));

            JsonObject configObject;
            if (!configFile.exists()) {
                boolean mk = configFile.getParentFile().mkdirs();
                boolean nf = configFile.createNewFile();
                configObject = defaults;
            } else configObject = Jankson.builder().build().load(configFile);

            defaults.forEach((string, element) -> {
                if(!configObject.containsKey(string)) configObject.putDefault(string, element, defaults.getComment(string));
            });

            FileUtils.write(configFile, unicodeUnescaper.translate(configObject.toJson(true, true, 0)), StandardCharsets.UTF_8);
            T config = Jankson.builder().build().fromJson(configObject.toJson(), configClass);
            Map<File, Object> configs = this.configs.get(configClass);
            if(configs == null) {
                this.configs.put(configClass, new HashMap<>());
                configs = this.configs.get(configClass);
            }
            configs.put(configFile, config);
            return config;
        } catch(Exception e) {
            logger.severe("Erro ao carregar um arquivo de configuração:");
            e.printStackTrace();
            return null;
        }
    }

}
