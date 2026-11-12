package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The condition behind {@link ConditionalOnMissingBean}.
 */
final class OnMissingBeanCondition implements Condition<ConditionalOnMissingBean> {

    @Override
    public boolean matches(ConditionContext context, ConditionalOnMissingBean annotation) {
        for (Class<?> beanType : annotation.value()) {
            if (context.hasBean(beanType, annotation.qualifiers())) {
                return false;
            }
        }
        return true;
    }
}
