package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.internal.AnnotationDrivenCondition;

import java.lang.annotation.Annotation;

/**
 * The condition behind {@link ConditionalOnMissingClass}.
 */
final class OnMissingClassCondition implements AnnotationDrivenCondition {

    @Override
    public boolean test(ConditionContext context, Annotation declaration) {
        ConditionalOnMissingClass annotation = Declarations.require(declaration, ConditionalOnMissingClass.class);
        for (String name : annotation.name()) {
            if (context.hasClass(name)) {
                return false;
            }
        }
        return true;
    }
}
