package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.internal.AnnotationDrivenCondition;

import java.lang.annotation.Annotation;

/**
 * The condition behind {@link ConditionalOnMissingProperty}.
 */
final class OnMissingPropertyCondition implements AnnotationDrivenCondition {

    @Override
    public boolean test(ConditionContext context, Annotation declaration) {
        ConditionalOnMissingProperty annotation = Declarations.require(declaration, ConditionalOnMissingProperty.class);
        for (String name : annotation.name()) {
            if (context.getProperty(name).isPresent()) {
                return false;
            }
        }
        return true;
    }
}
