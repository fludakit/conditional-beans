package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a bean only when the named beans are part of the deployment.
 * <p>
 * Every named type has to have a bean. The presence is decided by the deployment model, which describes the
 * beans that <em>could</em> be part of the deployment, so a bean that is itself disabled by a condition
 * still counts as present. The answer never depends on the order in which the beans are declared.
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * @ConditionalOnBean(PaymentService.class)
 * public class BrazilPaymentService implements PaymentService {
 * }
 * }</pre>
 *
 * @see ConditionalOnMissingBean
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnBean {

    /**
     * The types that have to have a bean in the deployment.
     *
     * @return the required bean types
     */
    Class<?>[] value();

    /**
     * The types of the qualifiers that the bean has to carry.
     * <p>
     * The member values of the qualifiers are not compared, so a qualifier of the given type matches
     * whatever its value is. When no qualifier type is given, the {@code @Default} qualifier is required,
     * exactly like a lookup without qualifiers.
     * </p>
     *
     * @return the required qualifier types
     */
    Class<? extends Annotation>[] qualifier() default {};
}
