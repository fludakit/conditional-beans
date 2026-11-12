package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a bean only when the named configuration entries are <em>not</em> configured.
 * <p>
 * Every named entry has to be missing. The counterpart of this annotation is
 * {@link ConditionalOnProperty}, which requires the entries to be present.
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * @ConditionalOnMissingProperty(name = "app.legacy")
 * public class ModernTaxCalculator implements TaxCalculator {
 * }
 * }</pre>

 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Conditional(OnMissingPropertyCondition.class)

public @interface ConditionalOnMissingProperty {

    /**
     * The names of the configuration entries that have to be missing.
     *
     * @return the entry names
     */
    String[] name();
}
