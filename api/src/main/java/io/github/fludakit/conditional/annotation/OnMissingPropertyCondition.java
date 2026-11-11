package io.github.fludakit.conditional.annotation;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.ConditionDefinition;

/**
 * The condition behind {@link ConditionalOnMissingProperty}.
 */
final class OnMissingPropertyCondition implements Condition {

    @Override
    public boolean test(ConditionContext context) {
        ConditionDefinition annotation = Declarations.require(context.getAnnotation(), ConditionalOnMissingProperty.class);
        for (String name : annotation.strings("name")) {
            if (context.getProperty(name).isPresent()) {
                return false;
            }
        }
        return true;
    }
}
