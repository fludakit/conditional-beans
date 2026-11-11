package io.github.fludakit.conditional;

import java.util.function.Predicate;

/**
 * Decides whether a bean participates in the deployment.
 * <p>
 * A condition is a plain class with a no-argument constructor. The container instantiates it directly and
 * never through CDI, because at evaluation time the container is not yet able to inject anything. That is
 * why a condition reaches deployment state through {@link ConditionContext} instead of through injected
 * beans.
 * </p>
 * <p>
 * Extending {@link Predicate} keeps this a functional interface, so a condition may be written as a lambda
 * wherever a named class is not required.
 * </p>
 * <p>
 * Implementations must be side-effect free and deterministic for a given deployment, and must not retain
 * the {@link ConditionContext} for later use on another thread. Conditions are evaluated once, during
 * bootstrap.
 * </p>
 *
 * <pre>{@code
 * public class BrazilCondition implements Condition {
 *
 *     @Override
 *     public boolean test(ConditionContext context) {
 *         return "BR".equals(context.getProperty("app.country").orElse(null));
 *     }
 * }
 * }</pre>
 *
 * @see Conditional
 * @see ConditionContext
 */
@FunctionalInterface
public interface Condition extends Predicate<ConditionContext> {
}