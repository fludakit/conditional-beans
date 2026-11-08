package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a bean only when the named configuration entries are present and have the expected values.
 * <p>
 * The entries are read from the system properties and then from the environment variables, exactly like
 * {@code ConditionContext.getProperty(String)} does, so a dotted name also matches the relaxed environment
 * variable form.
 * </p>
 * <p>
 * Every named entry has to be present. When {@link #value()} is not empty, the value of an entry has to be
 * one of the listed values. All entries have to match.
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * @ConditionalOnProperty(name = "app.country", value = "BR")
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
     * The names of the configuration entries that have to be present.
     *
     * @return the entry names
     */
    String[] name();

    /**
     * The values the entries may have. When empty, any value is accepted.
     *
     * @return the accepted values
     */
    String[] value() default {};

    /**
     * Whether the condition matches when an entry is missing.
     *
     * @return whether a missing entry is accepted
     */
    boolean matchIfMissing() default false;
}
