package io.github.fludakit.conditional.internal;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Derives the qualifiers of a bean from the annotations declared on its class, producer method or producer
 * field, following the rules of the CDI specification.
 */
final class Qualifiers {

    private Qualifiers() {
    }

    /**
     * The qualifiers of a bean are the annotations whose type is meta-annotated with {@link Qualifier}.
     * {@link Any} is always present.
     * <p>
     * {@link Default} is present unless the bean declares a qualifier of its own that excludes it. The
     * {@link Named} qualifier does not exclude {@link Default}, which is why a named bean carries both.
     * </p>
     */
    static Set<Annotation> derive(Collection<Annotation> annotations) {
        Set<Annotation> qualifiers = new LinkedHashSet<>();
        boolean exclusive = false;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                qualifiers.add(annotation);
                if (excludesDefault(annotation.annotationType())) {
                    exclusive = true;
                }
            }
        }
        if (!exclusive) {
            qualifiers.add(Default.Literal.INSTANCE);
        }
        qualifiers.add(Any.Literal.INSTANCE);
        return qualifiers;
    }

    /**
     * Whether a qualifier rules out the {@link Default} qualifier. Only a qualifier that the bean declares
     * for itself does; {@link Any} and {@link Named} do not.
     */
    private static boolean excludesDefault(Class<?> qualifierType) {
        return !qualifierType.equals(Any.class)
                && !qualifierType.equals(Named.class)
                && !qualifierType.equals(Default.class);
    }
}
