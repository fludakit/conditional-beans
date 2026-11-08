package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a bean only when the named classes are available to the deployment.
 * <p>
 * Every named class has to be on the classpath. The classes are named rather than referenced, because in
 * CDI an annotation cannot safely refer to a class that is absent: the annotation instance is created, but
 * reading the attribute throws {@link java.lang.TypeNotPresentException}. A name is resolved without
 * loading the class, which is what makes the test possible at all.
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * @ConditionalOnClass(name = "com.example.OptionalIntegration")
 * public class OptionalIntegrationBean {
 * }
 * }</pre>
 *
 * @see ConditionalOnMissingClass
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {

    /**
     * The names of the classes that have to be available.
     *
     * @return the class names
     */
    String[] name() default {};
}
