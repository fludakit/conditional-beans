package io.github.fludakit.conditional.annotation;

import io.github.fludakit.conditional.ConditionDefinition;

import java.lang.annotation.Annotation;

/**
 * Reads the annotation that declares a built-in condition.
 * <p>
 * The declaration of a built-in condition is always the annotation that declares it, so the condition is
 * handed the declaration through {@code ConditionContext.getAnnotation()} and reads its members through a
 * {@link ConditionDefinition}.
 * </p>
 */
final class Declarations {

    private Declarations() {
    }

    /**
     * The declaration of a built-in condition is always the annotation that declares it.
     *
     * @param definition the definition of the declaring annotation
     * @param type the expected annotation type
     * @return the definition
     */
    static ConditionDefinition require(ConditionDefinition definition, Class<? extends Annotation> type) {
        if (type.getName().equals(definition.typeName())) {
            return definition;
        }
        throw new IllegalStateException("Expected the declaration " + type.getName()
                + " but found " + definition.typeName());
    }
}
