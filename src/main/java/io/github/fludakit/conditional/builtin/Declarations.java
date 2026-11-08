package io.github.fludakit.conditional.builtin;

import jakarta.enterprise.inject.spi.DefinitionException;

import java.lang.annotation.Annotation;

/**
 * Reads the annotation that declares a built-in condition.
 */
final class Declarations {

    private Declarations() {
    }

    /**
     * The declaration of a built-in condition is always the annotation that declares it.
     *
     * @param declaration the annotation that declared the condition
     * @param type the expected annotation type
     * @param <A> the annotation type
     * @return the declaration
     */
    static <A extends Annotation> A require(Annotation declaration, Class<A> type) {
        if (type.isInstance(declaration)) {
            return type.cast(declaration);
        }
        throw new DefinitionException("Expected the declaration " + type.getName()
                + " but found " + declaration.annotationType().getName());
    }
}
