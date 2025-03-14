package com.nitros64.nitros_games_backend.constrait;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({PARAMETER}) //Zonas Soportadas
@Retention(RUNTIME) //Funciona en el Runtime
public @interface FileType {
    String message() default "File Format not Supported";
}
