package io.github.fludakit.conditional.internal;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

import java.lang.annotation.Annotation;

/**
 * A condition that is configured by the annotation that declares it.
 * <p>
 * A condition is instantiated with a no-argument constructor, so it cannot receive the values of the
 * annotation that declares it through its constructor. An annotation-driven condition is therefore handed
 * the declaration when it is evaluated.
 * </p>
 * <p>
 * The built-in conditions use this interface. It is an extension point: a user may declare an annotation
 * of their own and give it a condition this way.
 * </p>
 */
public interface AnnotationDrivenCondition extends Condition {

    /**
     * Evaluates the condition against the annotation that declared it.
     *
     * @param context the CDI-native view of the deployment
     * @param declaration the annotation that declared the condition
     * @return whether the condition matches
     */
    boolean test(ConditionContext context, Annotation declaration);

    @Override
    default boolean test(ConditionContext context) {
        throw new IllegalStateException(getClass().getName() + " can only be evaluated together with its declaration");
    }
}
