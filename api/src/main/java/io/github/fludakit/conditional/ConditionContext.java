package io.github.fludakit.conditional;

import java.util.Optional;

/**
 * The CDI-native view available to a {@link Condition} while it is being evaluated.
 * <p>
 * Everything exposed here is usable by both a CDI Full container and a CDI Lite build extension, which is
 * why there is deliberately no {@code BeanManager}, no {@code Annotated} and no reflective
 * {@code Annotation} instance: a condition asks about the beans of the deployment through
 * {@link #hasBean(Class, Class[])} and {@link #hasQualifiedBean(Class, Class[])}, reads the annotation that
 * declared it through {@link #getAnnotation()}, and reaches the deployment configuration as system
 * properties and environment variables.
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
     * Whether a bean of the given type carrying a qualifier of each given type would be part of this
     * deployment.
     * <p>
     * "Would be" means the bean is reported regardless of any {@link Conditional} on it, so conditions never
     * hide one another and the answer never depends on the order in which beans are declared.
     * </p>
     * <p>
     * Qualifiers are given as types, never as instances, so the answer is expressible at build time. Their
     * member values are not compared. Without a qualifier type, the {@code @Default} qualifier is required,
     * exactly like a lookup without qualifiers.
     * </p>
     *
     * @param type the required bean type
     * @param qualifierTypes the required qualifier types; may be empty
     * @return {@code true} when a matching bean is part of the deployment
     */
    boolean hasBean(Class<?> type, Class<?>... qualifierTypes);

    /**
     * As {@link #hasBean(Class, Class[])}; the separate name exists for readability at the call site.
     *
     * @param type the required bean type
     * @param qualifierTypes the required qualifier types; may be empty
     * @return {@code true} when a matching bean is part of the deployment
     */
    boolean hasQualifiedBean(Class<?> type, Class<?>... qualifierTypes);

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
