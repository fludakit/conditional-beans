package io.github.fludakit.conditional;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * The CDI-native view available to a {@link Condition} while it is being evaluated.
 * <p>
 * Everything exposed here is a CDI concept. There is deliberately no general-purpose environment, no
 * property resolver and no resource loader: a condition sees the CDI container, the element being
 * evaluated, and the deployment configuration as system properties and environment variables.
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
     * The element under evaluation - the bean class, the producer method or the producer field. Use it to
     * read the annotations that declared the condition.
     *
     * @return the annotated element
     */
    Annotated getAnnotated();

    /**
     * The bean manager of the enclosing deployment.
     * <p>
     * Most {@code BeanManager} methods are not usable while the container is still bootstrapping, so use
     * {@link #hasBean(Class, Annotation...)} rather than {@code getBeans}.
     * </p>
     *
     * @return the bean manager of the current deployment
     */
    BeanManager getBeanManager();

    /**
     * Whether a bean of the given type and qualifiers would be part of this deployment.
     * <p>
     * "Would be" means the bean exists independently of any {@link Conditional} on it, so conditions never
     * hide one another and the answer does not depend on the order in which beans are declared.
     * </p>
     *
     * @param type the required bean type
     * @param qualifiers the required qualifiers; when empty the default qualifier rules apply
     * @param <T> the required bean type
     * @return {@code true} when a matching bean is part of the deployment
     */
    <T> boolean hasBean(Class<T> type, Annotation... qualifiers);

    /**
     * As {@link #hasBean(Class, Annotation...)}, for a parameterized type that a {@code Class} cannot
     * express.
     *
     * @param type the required parameterized bean type
     * @param qualifiers the required qualifiers; when empty the default qualifier rules apply
     * @param <T> the required bean type
     * @return {@code true} when a matching bean is part of the deployment
     */
    <T> boolean hasBean(TypeLiteral<T> type, Annotation... qualifiers);

    /**
     * Whether a bean of the given type would be part of this deployment, where the bean carries a
     * qualifier of each given type.
     * <p>
     * The member values of the qualifiers are not compared, so a qualifier of the given type matches
     * whatever its value is. This is what an annotation can express: an annotation holds the type of a
     * qualifier, never an instance of it.
     * </p>
     * <p>
     * When no qualifier type is given, the {@code @Default} qualifier is required, exactly like
     * {@link #hasBean(Class, Annotation...)} does.
     * </p>
     *
     * @param type the required bean type
     * @param qualifierTypes the required qualifier types
     * @param <T> the required bean type
     * @return {@code true} when a matching bean is part of the deployment
     */
    <T> boolean hasQualifiedBean(Class<T> type, Class<? extends Annotation>... qualifierTypes);

    /**
     * As {@link #hasQualifiedBean(Class, Class[])}, for a parameterized type that a {@code Class} cannot
     * express.
     *
     * @param type the required parameterized bean type
     * @param qualifierTypes the required qualifier types
     * @param <T> the required bean type
     * @return {@code true} when a matching bean is part of the deployment
     */
    <T> boolean hasQualifiedBean(TypeLiteral<T> type, Class<? extends Annotation>... qualifierTypes);

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