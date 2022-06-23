# Seal Config [![](https://jitpack.io/v/seal-island/sealconfig.svg)](https://jitpack.io/#seal-island/sealconfig)
Seal Config is an API for creating config files. At the moment it supports only [Jankson](https://github.com/falkreon/Jankson).

To create a new config file you only need to create a new instance of the class [SealConfig](https://github.com/Seal-Island/Seal-Config/blob/main/src/main/java/com/focamacho/sealconfig/SealConfig.java), and use the method getConfig.

## First Steps
To start using the API, you'll need to setup the dependency in your project. Here is some examples for Gradle and Maven:
<br>
Do not forget to replace *VERSION* with the desired version of the API. Check what is the latest version in the [releases](https://github.com/Seal-Island/SealConfig/releases) tab.

**Maven**
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>com.github.seal-island</groupId>
    <artifactId>sealconfig</artifactId>
    <version>VERSION</version>
</dependency>
```

**Gradle**
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.seal-island:sealconfig:VERSION'
}
```


## Example
Example of a config file using the API.

Config class:
```java
//To avoid conflicts with the changes made, the Jankson
//packages was relocated.
import com.focamacho.sealconfig.relocated.blue.endless.jankson.Comment;

public class ObjectExample {

    //You can use the @Comment annotation for adding comments
    //to your config file.
    @Comment("Set your name here.")
    public String myName = "Focamacho";
    
    @Comment("Set here a cool number.")
    public int coolNumber = 0;
    
    //You can set objects to be used as "categories"
    //inside your config file.
    @Comment("This is a category :p\n" +
            "And also a multiple lines comment\n" +
            "Do you like it?!")
    public CategoryExample category = new CategoryExample();
    
    //Needs to be public
    public static class CategoryExample {
        
        @Comment("Result of what?")
        public int result = 20;
        
        @Comment("Hi!")
        public String hi = "hello!";
        
    }

}
```
Class for loading the config:
```java
import java.io.File;

public class ConfigExample {

    private static SealConfig sealConfig;
    public static ObjectExample config;
    
    public static void loadConfig() {
        //Create a new instance of the SealConfig. You only need to do it one time.
        sealConfig = new SealConfig();
        //Create or reload a config. The first parameters is the config file, and the second the config class.
        config = sealConfig.getConfig(new File("./config/ConfigExample.json5"), ObjectExample.class);

        //To access the config, you only need to use the object created using getConfig.
        System.out.println("My name is " + config.myName + " and the coolest number is " + config.coolNumber);

        //To modify the config, just change the values.
        config.myName = "Foca";
        config.coolNumber = 10;
        System.out.println("My name is " + config.myName + " and the coolest number is " + config.coolNumber);

        //If you change the values, do not forget to save! You don't need to save otherwise.
        saveConfig();
    }
    
    public static void reloadConfig() {
        //To reload the config, you can use SealConfig#reload.
        sealConfig.reload();
    }
    
    public static void saveConfig() {
        //You can save all the created configs using SealConfig#save.
        sealConfig.save();
        //if you pass a config object to it, it will only save the config inserted
        sealConfig.save(config);
    }
    
}
```
Resulting JSON:
```json5
{
  // Set your name here.
  "myName": "Foca",

  // Set here a cool number.
  "coolNumber": 10,

  /* This is a category :p
     And also a multiple lines comment
     Do you like it?!
  */
  "category": {
    // Result of what?
    "result": 20,

    // Hi!
    "hi": "hello!"
  }
}
```
