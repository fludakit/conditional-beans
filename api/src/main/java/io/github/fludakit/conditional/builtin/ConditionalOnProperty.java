package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Activates the annotated element only when a configuration property is present and, optionally, matches a
 * specific value.
 * <p>
 * Property lookup is delegated to {@link io.github.fludakit.conditional.ConditionContext#getProperty(String)}.
 * </p>
 *
 * @see ConditionalOnMissingProperty
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Conditional(OnPropertyCondition.class)

public @interface ConditionalOnProperty {

    /**
     * The name of the configuration entry that has to be present.
     *
     * @return the entry name
     */
    String name();

    /**
     * The value the entry has to have. An empty value accepts any value.
     *
     * @return the expected value
     */
    String havingValue() default "";
}
