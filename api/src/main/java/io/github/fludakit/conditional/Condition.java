package io.github.fludakit.conditional;

import java.lang.annotation.Annotation;

/**
 * Decides whether a bean participates in the deployment.
 * <p>
 * A condition is a plain class with a no-argument constructor. The container instantiates it directly and
 * never through CDI, because at evaluation time the container is not yet able to inject anything. That is
 * why a condition reaches deployment state through {@link ConditionContext} instead of through injected
 * beans.
 * </p>
 * <p>
 * A condition is written against the annotation that declares it, so that it can read the values of that
 * annotation without reflection. The annotation is either the annotation that carries the
 * {@link Conditional} meta-annotation - the way the built-in conditions are declared - or the
 * {@link Conditional} annotation itself, when the condition is declared directly on the bean.
 * </p>
 *
 * <pre>{@code
 * public class BrazilCondition implements Condition<ConditionalOnCountry> {
 *
 *     @Override
 *     public boolean matches(ConditionContext context, ConditionalOnCountry annotation) {
 *         return "BR".equals(context.getEnvironmentValue(annotation.country()).orElse(null));
 *     }
 * }
 * }</pre>
 * <p>
 * Implementations must be side-effect free and deterministic for a given deployment, and must not retain
 * the {@link ConditionContext} for later use on another thread. Conditions are evaluated once, during
 * bootstrap.
 * </p>
 *
 * @param <A> the annotation that declares the condition
 * @see Conditional
 * @see ConditionContext
 */
@FunctionalInterface
public interface Condition<A extends Annotation> {

    /**
     * Evaluates the condition against the annotation that declares it.
     *
     * @param context the CDI-native view of the deployment
     * @param annotation the annotation that declares the condition
     * @return {@code true} when the bean satisfies the condition, {@code false} when it has to be vetoed
     */
    boolean matches(ConditionContext context, A annotation);
}
