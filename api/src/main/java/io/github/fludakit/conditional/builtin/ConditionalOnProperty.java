package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a bean only when the named configuration entries are present and have the
 * expected values.
 * <p>
 * The entries are read from the system properties and then from the environment variables, exactly like
 * {@code ConditionContext.getEnvironmentValue(String)} does, so a dotted name also matches the relaxed
 * environment variable form.
 * </p>
 * <p>
 * Every named entry has to be present. When {@link #havingValue()} is not empty, the value of the entry has
 * to be equal to it, ignoring the case. All entries have to match.
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * @ConditionalOnProperty(name = "app.country", havingValue = "BR")
 * public class BrazilTaxCalculator implements TaxCalculator {
 * }
 * }</pre>
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
