package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

/**
 * The condition behind {@link ConditionalOnBean}.
 */
public final class OnBeanCondition implements Condition<ConditionalOnBean> {

    @Override
    public boolean matches(ConditionContext context, ConditionalOnBean annotation) {

        for (Class<?> beanType : annotation.value()) {
            if (!context.hasBean(beanType, annotation.qualifiers())) {
                return false;
            }
        }
        return true;
    }
}
