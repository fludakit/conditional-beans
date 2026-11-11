package io.github.fludakit.conditional.annotation;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.ConditionDefinition;

/**
 * The condition behind {@link ConditionalOnBean}.
 */
final class OnBeanCondition implements Condition {

    @Override
    public boolean test(ConditionContext context) {
        ConditionDefinition annotation = Declarations.require(context.getAnnotation(), ConditionalOnBean.class);
        for (Class<?> type : annotation.classes("value")) {
            if (!context.hasQualifiedBean(type, annotation.classes("qualifier"))) {
                return false;
            }
        }
        return true;
    }
}
