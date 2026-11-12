package io.github.fludakit.conditional.builtin;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

import java.util.Optional;

/**
 * The condition behind {@link ConditionalOnProperty}.
 */
public final class OnPropertyCondition implements Condition<ConditionalOnProperty> {

    @Override
    public boolean matches(ConditionContext context, ConditionalOnProperty annotation) {
        Optional<String> value = context.getEnvironmentValue(annotation.name());
        if (value.isEmpty()) {
            return false;
        }
        String expected = annotation.havingValue();
        return expected.isEmpty() || value.get().equalsIgnoreCase(expected);
    }
}
