package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

/**
 * The condition behind {@link ConditionalOnMissingClass}.
 */
public final class OnMissingClassCondition implements Condition<ConditionalOnMissingClass> {

    @Override
    public boolean matches(ConditionContext context, ConditionalOnMissingClass annotation) {
        for (String name : annotation.value()) {
            if (context.hasClass(name)) {
                return false;
            }
        }
        return true;
    }
}
