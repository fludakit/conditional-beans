package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a bean only when the named beans are <em>not</em> part of the deployment.
 * <p>
 * Every named type has to have no bean. The presence is decided by the deployment model, which describes the
 * beans that <em>could</em> be part of the deployment, so a bean that is itself disabled by a condition
 * still counts as present.
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * @ConditionalOnMissingBean(LegacyPaymentService.class)
 * public class PaymentServiceConfiguration {
 * }
 * }</pre>

 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Conditional(OnMissingBeanCondition.class)

public @interface ConditionalOnMissingBean {

    /** Bean types that must be absent from the container. */
    Class<?>[] value();

    /** Qualifier annotation types required when checking for existing beans. */
    Class<? extends Annotation>[] qualifiers() default {};
}
