package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.internal.AnnotationDrivenCondition;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

/**
 * The condition behind {@link ConditionalOnProperty}.
 */
final class OnPropertyCondition implements AnnotationDrivenCondition {

    @Override
    public boolean test(ConditionContext context, Annotation declaration) {
        ConditionalOnProperty annotation = Declarations.require(declaration, ConditionalOnProperty.class);
        for (String name : annotation.name()) {
            Optional<String> value = context.getProperty(name);
            if (value.isEmpty()) {
                if (!annotation.matchIfMissing()) {
                    return false;
                }
                continue;
            }
            if (annotation.value().length > 0 && !Arrays.asList(annotation.value()).contains(value.get())) {
                return false;
            }
        }
        return true;
    }
}
