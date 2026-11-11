package io.github.fludakit.conditional.annotation;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.ConditionDefinition;

import java.util.Arrays;
import java.util.Optional;

/**
 * The condition behind {@link ConditionalOnProperty}.
 */
final class OnPropertyCondition implements Condition {

    @Override
    public boolean test(ConditionContext context) {
        ConditionDefinition annotation = Declarations.require(context.getAnnotation(), ConditionalOnProperty.class);
        for (String name : annotation.strings("name")) {
            Optional<String> value = context.getProperty(name);
            if (value.isEmpty()) {
                if (!annotation.bool("matchIfMissing")) {
                    return false;
                }
                continue;
            }
            String[] expected = annotation.strings("value");
            if (expected.length > 0 && !Arrays.asList(expected).contains(value.get())) {
                return false;
            }
        }
        return true;
    }
}
