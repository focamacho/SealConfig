package com.focamacho.sealconfig;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.translate.UnicodeUnescaper;

import java.io.File;
import java.lang.reflect.Field;
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

    protected static final Logger logger = Logger.getLogger("SealConfig");
    protected static final UnicodeUnescaper unicodeUnescaper = new UnicodeUnescaper();

    protected final Map<Class<?>, Map<File, Object>> configs = new HashMap<>();

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
                   String toSave = unicodeUnescaper.translate(Jankson.builder().build().load(Jankson.builder().build().toJson(configObject).toJson(true, true, 0)).toJson(true, true, 0, 2));

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
            } else {
                configObject = Jankson.builder().build().load(configFile);
                configObject = checkValues(defaults, configObject, configClass);
            }

            FileUtils.write(configFile, unicodeUnescaper.translate(configObject.toJson(true, true, 0, 2)), StandardCharsets.UTF_8);
            T config = Jankson.builder().build().fromJson(configObject.toJson(), configClass);

            //Remover valores padrões/de exemplo do Map
            Field[] fields = config.getClass().getFields();
            for (Field field : fields) {
                JsonObject jsonObject = configObject.getObject(field.getName());
                if(jsonObject == null) continue;

                Object obj = field.get(config);
                if(field.get(config) instanceof Map) {
                    Map<String, ?> map = (Map<String, ?>) obj;
                    map.entrySet().removeIf(entry -> !jsonObject.containsKey(entry.getKey()));
                }
            }

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

    /**
     * Faz a checagem para que todos os valores padrões
     * sejam definidos corretamente, além de atualizar os
     * comentários que foram adicionados ou modificados e
     * remover os valores que já foram removidos de suas
     * classes de configuração.
     *
     * @param defaultObject o objeto com os valores padrões.
     * @param actualObject o objeto atual para verificação.
     * @param configClass a classe do objeto de configuração.
     * @return um novo objeto com todos os valores padrões definidos,
     * valores obsoletos removidos e comentários atualizados.
     */
    private JsonObject checkValues(JsonObject defaultObject, JsonObject actualObject, Class<?> configClass) {
        for (Map.Entry<String, JsonElement> entry : defaultObject.entrySet()) {
            if(!actualObject.containsKey(entry.getKey())) actualObject = applyDefaults(defaultObject, actualObject);
            //Fazer a verificação se o valor não é um JsonObject com valores que precisam ser verificados também
            else if(actualObject.get(entry.getKey()) instanceof JsonObject) {
                try {
                    Field field = configClass.getDeclaredField(entry.getKey());
                    if(field.isAnnotationPresent(ConfigObject.class)) {
                        actualObject.put(entry.getKey(), checkValues((JsonObject) entry.getValue(), actualObject.getObject(entry.getKey()), field.getType()));
                    }
                } catch(Exception ignored) {}
            }
        }

        //Atualizar os comentários em caso deles terem sido adicionados ou
        //modificados na classe de configuração.
        for (Map.Entry<String, JsonElement> entry : defaultObject.entrySet()) {
            actualObject.setComment(entry.getKey(), defaultObject.getComment(entry.getKey()));
        }

        return actualObject;
    }

    /**
     * Verifica e define os valores padrões que
     * estão faltando, além de remover os valores
     * que já foram removidos da classe de configuração.
     *
     * @param defaultObject o objeto com os valores padrões.
     * @param actualObject o objeto atual para verificação.
     * @return um novo objeto com todos os valores padrões definidos.
     */
    private JsonObject applyDefaults(JsonObject defaultObject, JsonObject actualObject) {
        //Criar um novo JsonObject para que os elementos a serem adicionados sejam colocados
        //na ordem correta.
        JsonObject newObject = defaultObject.clone();
        actualObject.forEach((key, value) -> {
            if(newObject.containsKey(key)) newObject.put(key, value);
        });

        return newObject;
    }

}
