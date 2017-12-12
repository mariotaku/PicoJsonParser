package org.mariotaku.pjp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface PicoField {
    String[] value() default {};

    Class<? extends PicoConverter> converter() default PicoConverter.class;

    boolean ignoreNull() default false;

    boolean ignoreInvalid() default false;
}
