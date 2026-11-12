package io.github.fludakit.conditional;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * A read-only view of deployment data available to a {@link Condition} during evaluation.
 * <p>
 * The context exposes only the information needed to make bootstrap-time activation decisions: configuration
 * properties, classpath presence, and the potential bean set of the deployment. Implementations are valid
 * only for a single evaluation and must not be retained.
 * </p>
 *
 * @see Condition
 * @see Conditional
 */
public interface ConditionContext {

    /**
     * Returns the value of a configuration property.
     * <p>
     * Implementations typically resolve values from system properties and environment variables. A dotted key
     * may also be matched in a relaxed environment-variable form (for example, {@code app.country} and
     * {@code APP_COUNTRY}).
     * </p>
     *
     * @param key the property name
     * @return the configured value, or {@link Optional#empty()} if absent
     */
    Optional<String> getProperty(String key);

    /**
     * Checks whether all named classes are available to the deployment.
     *
     * @param className fully qualified class names to check
     * @return {@code true} if every class is present; {@code false} otherwise
     */
    boolean hasClass(String... className);

    /**
     * Checks whether a bean of the given type and qualifiers is part of the deployment.
     * <p>
     * The answer is based on the potential deployment model: it must not depend on declaration order, and a
     * bean that is later vetoed by a condition may still be considered present for evaluation purposes.
     * </p>
     *
     * @param beanClass  the bean type to look up
     * @param qualifiers qualifier annotation types required on the matching bean; if empty, the lookup matches
     *                   by type
     * @return {@code true} if a matching bean exists; {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    boolean hasBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers);
}
