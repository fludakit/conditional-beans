package io.github.fludakit.conditional;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * The CDI-native view available to a {@link Condition} while it is being evaluated.
 * <p>
 * Everything exposed here is a CDI concept. There is deliberately no general-purpose environment, no
 * property resolver and no resource loader: a condition sees the classpath of the deployment, the beans
 * that are part of it, and the configuration of the process as system properties and environment variables.
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
     * The value of a configuration entry, taken from the system properties and then from the environment
     * variables.
     * <p>
     * A dotted name is also matched against the relaxed environment variable form, so {@code app.country}
     * also resolves {@code APP_COUNTRY}.
     * </p>
     *
     * @param key the name of the configuration entry
     * @return the value, or {@link Optional#empty()} when the entry is not configured
     */
    Optional<String> getEnvironmentValue(String key);

    /**
     * Whether a class is available to the deployment. The name form is the useful one, because it does not
     * force the class to be loaded.
     *
     * @param className the fully qualified class name
     * @return {@code true} when the class is available
     */
    boolean hasClass(String... className);


    /**
     * Whether a bean of the given type, carrying the given qualifiers, is part of the deployment.
     * <p>
     * The qualifiers follow the rules of the CDI resolution. A bean that declares no qualifier of its own
     * carries the {@code @Default} qualifier, and every bean carries the {@code @Any} qualifier. Asking
     * for no qualifier at all therefore asks for a bean with the {@code @Default} qualifier, and asking
     * for a qualifier asks for a bean that carries it.
     * </p>
     * <p>
     * The answer does not depend on the order in which the beans are declared: a bean that is itself
     * disabled by a condition still counts as present.
     * </p>
     *
     * @param beanClass  the type of the bean
     * @param qualifiers the qualifiers the bean has to carry
     * @return {@code true} when such a bean is part of the deployment
     */
    @SuppressWarnings("unchecked")
    boolean hasBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers);
}
