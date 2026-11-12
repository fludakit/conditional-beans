package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

/**
 * The condition behind {@link ConditionalOnMissingProperty}.
 */
public final class OnMissingPropertyCondition implements Condition<ConditionalOnMissingProperty> {

    @Override
    public boolean matches(ConditionContext context, ConditionalOnMissingProperty annotation) {
        for (String name : annotation.name()) {
            if (context.getProperty(name).isPresent()) {
                return false;
            }
        }
        return true;
    }
}
