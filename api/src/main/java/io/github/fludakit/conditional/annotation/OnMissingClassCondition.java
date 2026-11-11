package io.github.fludakit.conditional.annotation;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.ConditionDefinition;

/**
 * The condition behind {@link ConditionalOnMissingClass}.
 */
final class OnMissingClassCondition implements Condition {

    @Override
    public boolean test(ConditionContext context) {
        ConditionDefinition annotation = Declarations.require(context.getAnnotation(), ConditionalOnMissingClass.class);
        for (String name : annotation.strings("name")) {
            if (context.hasClass(name)) {
                return false;
            }
        }
        return true;
    }
}
