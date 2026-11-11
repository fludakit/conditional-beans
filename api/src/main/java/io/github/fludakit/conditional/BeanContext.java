package io.github.fludakit.conditional;

/**
 * The beans that could be part of the deployment, as seen by a {@link Condition}.
 * <p>
 * "Could be" means a bean is reported regardless of any {@link Conditional} on it, so conditions never
 * hide one another and the answer never depends on the order in which beans are declared.
 * </p>
 * <p>
 * Qualifiers are given as types, never as instances, so the view is usable at build time where a qualifier
 * instance cannot be expressed. The member values of a qualifier are therefore not compared.
 * </p>
 */
public interface BeanContext {

    /**
     * Whether a bean of the given type carrying a qualifier of each given type would be part of this
     * deployment.
     * <p>
     * When no qualifier type is given, the {@code @Default} qualifier is required, exactly like a lookup
     * without qualifiers.
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
}
