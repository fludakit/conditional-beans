package io.github.fludakit.conditional;

import java.util.Optional;

/**
 * The CDI-native view available to a {@link Condition} while it is being evaluated.
 * <p>
 * Everything exposed here is usable by both a CDI Full container and a CDI Lite build extension, which is
 * why there is deliberately no {@code BeanManager}, no {@code Annotated} and no reflective
 * {@code Annotation} instance: a condition sees the beans of the deployment through {@link BeanContext},
 * the annotation that declared it through {@link ConditionDefinition}, and the deployment configuration as
 * system properties and environment variables.
 * </p>
 * <p>
 * The context is valid only for the duration of one evaluation and must not be retained.
 * </p>
 *
 * @see Condition
 * @see Conditional
 */
public interface ConditionContext {

    /**
     * The beans that could be part of this deployment.
     * <p>
     * This is the "potential bean set": a bean is reported regardless of whether a {@link Conditional} on
     * it matches, so conditions never hide one another and the answer never depends on the order in which
     * beans are declared.
     * </p>
     *
     * @return the bean view of the current deployment
     */
    BeanContext getBeanContext();

    /**
     * The annotation that declared the condition being evaluated.
     * <p>
     * The annotation is exposed without reflection so the same condition runs at build time and at runtime.
     * For a built-in condition such as {@code @ConditionalOnProperty} the returned definition is that
     * annotation; for a condition declared directly as {@code @Conditional(MyCondition.class)} it is the
     * {@code @Conditional} annotation itself.
     * </p>
     *
     * @return the declaring annotation, never {@code null}
     */
    ConditionDefinition getAnnotation();

    /**
     * Whether the named class is available to the deployment. The name form is the useful one, because it
     * does not force the class to be loaded.
     *
     * @param className the fully qualified class name
     * @return {@code true} when the class is available
     */
    boolean hasClass(String className);

    /**
     * Whether the given class is available to the deployment.
     *
     * @param type the class to check
     * @param <T> the class type
     * @return {@code true} when the class is available
     */
    <T> boolean hasClass(Class<T> type);

    /**
     * The value of a configuration entry, taken from system properties and then from environment variables.
     * <p>
     * A dotted name is also matched against the relaxed environment variable form, so {@code app.country}
     * also resolves {@code APP_COUNTRY}.
     * </p>
     *
     * @param name the configuration entry name
     * @return the value, or {@link Optional#empty()} when the entry is not configured
     */
    Optional<String> getProperty(String name);
}
