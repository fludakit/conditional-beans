package io.github.fludakit.conditional;

import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated bean is enabled only when every declared condition matches the current
 * deployment.
 * <p>
 * The annotation may be placed on a bean class, a producer method or a producer field. It is
 * <em>repeatable</em>, so several conditions can be declared on one element, one annotation each:
 * </p>
 *
 * <pre>{@code
 * @Conditional(BrazilCondition.class)
 * @Conditional(DatabaseAvailable.class)
 * @ApplicationScoped
 * public class BrazilTaxCalculator implements TaxCalculator {
 * }
 * }</pre>
 *
 * <p>
 * The array form carries the same meaning, so both styles may be mixed: the conditions of
 * {@code @Conditional({ A.class, B.class })} and of two separate {@code @Conditional} annotations are
 * collected together, and all of them have to match.
 * </p>
 * <p>
 * Conditions declared on a stereotype of the bean class are collected in addition to the ones declared on
 * the bean itself.
 * </p>
 * <p>
 * The annotation may also be used as a meta-annotation, which is what the built-in conditions such as
 * {@code @ConditionalOnProperty} do, and what lets an application declare an annotation of its own.
 * </p>
 * <p>
 * All collected conditions must match for the bean to be enabled. When a condition does not match, the
 * bean is vetoed during bootstrap and is not part of the deployment at all: it is not injectable, it does
 * not appear in {@code BeanManager.getBeans(...)} and it produces no {@code ProcessBean} event.
 * </p>
 *
 * @see Conditionals
 * @see Condition
 * @see ConditionContext
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Stereotype
@Repeatable(Conditionals.class)
public @interface Conditional {

    /**
     * The conditions that must all match for the annotated bean to be enabled.
     *
     * @return the required conditions
     */
    Class<? extends Condition>[] value();
}
