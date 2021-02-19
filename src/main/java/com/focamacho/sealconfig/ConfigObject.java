package com.focamacho.sealconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação que marca um objeto como
 * de configuração. Possibilitando
 * a criação de novos objetos para
 * serem usados como categorias no JSON.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigObject {}
