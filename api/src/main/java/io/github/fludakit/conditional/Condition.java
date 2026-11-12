package io.github.fludakit.conditional;

import java.lang.annotation.Annotation;

/**
 * Determines whether an annotated bean, producer method, or producer field should be active.
 * <p>
 * Implementations are instantiated by the container (not managed by CDI) and must have a public no-argument
 * constructor. Conditions should be deterministic, side-effect free, and rely only on the provided
 * {@link ConditionContext} and declaring annotation.
 * </p>
 *
 * @param <A> the annotation type that declares or parameterizes the condition
 * @see Conditional
 * @see ConditionContext
 */
@FunctionalInterface
public interface Condition<A extends Annotation> {

    /**
     * Evaluates this condition.
     *
     * @param context    deployment context available during bootstrap/build evaluation
     * @param annotation the annotation instance that declared this condition
     * @return {@code true} if the condition matches and the element may remain enabled; {@code false} otherwise
     */
    boolean matches(ConditionContext context, A annotation);
}
