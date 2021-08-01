# Seal Config
Seal Config é uma API para criação de arquivos de configuração utilizando [Jankson](https://github.com/falkreon/Jankson).

Para gerar um arquivo de configuração é só criar um novo objeto da classe [SealConfig](https://github.com/Seal-Island/Seal-Config/blob/main/src/main/java/com/focamacho/sealconfig/SealConfig.java) e usar o método getConfig.

## Exemplo
Exemplo de criação de configuração utilizando a API.

Classe da configuração:
```java
//Para evitar conflitos com as mudanças efetuadas, os pacotes
//do Jankson foram realocados.
import com.focamacho.sealconfig.relocated.blue.endless.jankson.Comment;

public class ObjectExample {

    //Anotações de comentário do Jankson podem ser utilizados para
    //definir comentários nas configurações.
    @Comment("Defina seu nome aqui.")
    public String meuNome = "Focamacho";
    
    @Comment("Defina aqui qual número é foda.")
    public int numeroFoda = 0;
    
    //Você pode definir objetos para serem usados como "categorias"
    //por meio da anotação @ConfigObject
    @Comment("Isso aqui é uma categoria :p\n" +
            "E que tal alguns comentários maiorzinhos?\n" +
            "Parece bom pra mim!")
    @ConfigObject
    public CategoryExample categoria = new CategoryExample();
    
    static class CategoryExample {
        
        @Comment("Resultado do que?")
        public int resultado = 20;
        
        @Comment("Oi!")
        public String oi = "olá!";
        
    }

}
```
Classe para carregamento e manipulação da configuração:
```java
import java.io.File;

public class ConfigExample {

    private static SealConfig sealConfig;
    public static ObjectExample config;
    
    public static void loadConfig() {
        //Cria uma instância da Seal Config
        sealConfig = new SealConfig();
        //Cria ou carrega a configuração. O primeiro argumento é o arquivo de configuração, e o segundo a classe da configuração.
        config = sealConfig.getConfig(new File("./config/ConfigExemplo.json"), ObjectExample.class);

        //Para acessar a config é só utilizar o objeto criado pelo getConfig.
        System.out.println("Meu nome é " + config.meuNome + " e o número mais foda é o " + config.numeroFoda);

        //Para modificar a configuração é só alterar os valores no objeto.
        config.meuNome = "Foca";
        config.numeroFoda = 10;
        System.out.println("Meu nome é " + config.meuNome + " e o número mais foda é o " + config.numeroFoda);

        //Não esqueça de salvar caso altere os valores!
        saveConfig();
    }
    
    public static void reloadConfig() {
        //Para recarregar a config é só utilizar o método SealConfig#reload.
        sealConfig.reload();
    }
    
    public static void saveConfig() {
        //Para salvar você pode optar por salvar todas as configurações criadas usando SealConfig#save.
        sealConfig.save();
        //ou salvar uma configuração específica passando o objeto dela no método
        sealConfig.save(config);
    }
    
}
```
JSON resultante da classe ObjectExample:
```json5
{
	// Defina seu nome aqui.
	"meuNome": "Focamacho",

	// Defina aqui qual número é foda.
	"numeroFoda": 0,

	/* Isso aqui é uma categoria :p
	   E que tal alguns comentários maiorzinhos?
	   Parece bom pra mim!
	*/
	"categoria": {
		// Resultado do que?
		"resultado": 20,

		// Oi!
		"oi": "olá!"
	}
}
```