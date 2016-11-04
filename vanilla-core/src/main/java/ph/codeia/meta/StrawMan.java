package ph.codeia.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A demonstration of how an experimental interface might be used.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface StrawMan {
    /**
     * @return a description of the use case.
     */
    String value() default "";
}
