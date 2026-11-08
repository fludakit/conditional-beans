package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.internal.AnnotationDrivenCondition;

import java.lang.annotation.Annotation;

/**
 * The condition behind {@link ConditionalOnMissingBean}.
 */
final class OnMissingBeanCondition implements AnnotationDrivenCondition {

    @Override
    public boolean test(ConditionContext context, Annotation declaration) {
        ConditionalOnMissingBean annotation = Declarations.require(declaration, ConditionalOnMissingBean.class);
        for (Class<?> type : annotation.value()) {
            if (context.hasQualifiedBean(type, annotation.qualifier())) {
                return false;
            }
        }
        return true;
    }
}
