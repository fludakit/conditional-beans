package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

/**
 * The condition behind {@link ConditionalOnClass}.
 */
final class OnClassCondition implements Condition<ConditionalOnClass> {

    @Override
    public boolean matches(ConditionContext context, ConditionalOnClass annotation) {
        for (Class<?> type : annotation.value()) {
            if (!context.hasClass(type.getName())) {
                return false;
            }
        }
        for (var name : annotation.name()) {
            if (!context.hasClass(name)) {
                return false;
            }
        }
        return true;
    }
}
