package io.github.fludakit.conditional.internal;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Conditionals;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects the conditions that apply to a bean.
 * <p>
 * The conditions are taken from the {@link Conditional} annotation on the bean class, producer method or
 * producer field, from the {@link Conditional} annotation on each stereotype of the bean class, and from
 * every annotation that is itself annotated with {@link Conditional}. The last one is what lets a composed
 * annotation such as {@code @ConditionalOnProperty} declare a condition of its own, and what lets an
 * application define an annotation of its own.
 * </p>
 * <p>
 * {@link Conditional} is repeatable, so an element may carry several of them. Java replaces them with a
 * single {@link Conditionals} annotation, and the container is what the resolver finds. The
 * conditions it holds are therefore expanded here.
 * </p>
 * <p>
 * Every condition is kept together with the annotation that declared it, because a condition that is
 * configured by its declaration - the built-in conditions are - needs that declaration to know its
 * requirements.
 * </p>
 */
final class ConditionResolver {

    /** How many levels of stereotypes are followed. */
    private static final int MAX_DEPTH = 3;

    private ConditionResolver() {
    }

    /**
     * A condition together with the annotation that declared it.
     *
     * @param conditionClass the class of the condition
     * @param declaration the annotation that declared the condition
     */
    record Entry(Class<? extends Condition> conditionClass, Annotation declaration) {
    }

    /**
     * Resolves the conditions of a bean.
     *
     * @param annotated the bean class, producer method or producer field
     * @param model the deployment model
     * @return the conditions, in the order they have to be evaluated
     */
    static List<Entry> resolve(Annotated annotated, DeploymentModel model) {
        List<Entry> entries = new ArrayList<>();
        Set<Entry> seen = new LinkedHashSet<>();

        for (Annotation annotation : annotated.getAnnotations()) {
            collect(annotation, entries, seen, 0);
        }

        Class<?> declaringClass = declaringClassOf(annotated);
        if (declaringClass != null) {
            for (Annotation stereotype : model.stereotypesOf(declaringClass)) {
                for (Annotation annotation : stereotype.annotationType().getAnnotations()) {
                    collect(annotation, entries, seen, 0);
                }
            }
        }

        return entries;
    }

    /**
     * Collects the conditions declared by one annotation.
     */
    private static void collect(Annotation annotation, List<Entry> entries, Set<Entry> seen, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }
        Class<? extends Annotation> type = annotation.annotationType();

        if (Conditional.class.equals(type)) {
            add((Conditional) annotation, annotation, entries, seen);
            return;
        }

        if (Conditionals.class.equals(type)) {
            for (Conditional conditional : ((Conditionals) annotation).value()) {
                add(conditional, conditional, entries, seen);
            }
            return;
        }

        if (type.isAnnotationPresent(Stereotype.class)) {
            for (Annotation declared : type.getAnnotations()) {
                collect(declared, entries, seen, depth + 1);
            }
            return;
        }

        Conditional composed = type.getAnnotation(Conditional.class);
        if (composed != null) {
            add(composed, annotation, entries, seen);
        }

        Conditionals composedContainer = type.getAnnotation(Conditionals.class);
        if (composedContainer != null) {
            for (Conditional conditional : composedContainer.value()) {
                add(conditional, annotation, entries, seen);
            }
        }
    }

    private static void add(Conditional conditional, Annotation declaration, List<Entry> entries, Set<Entry> seen) {
        for (Class<? extends Condition> conditionClass : conditional.value()) {
            Entry entry = new Entry(conditionClass, declaration);
            if (seen.add(entry)) {
                entries.add(entry);
            }
        }
    }

    private static Class<?> declaringClassOf(Annotated annotated) {
        if (annotated instanceof AnnotatedType<?> annotatedType) {
            return annotatedType.getJavaClass();
        }
        if (annotated instanceof AnnotatedMember<?> annotatedMember) {
            return annotatedMember.getDeclaringType().getJavaClass();
        }
        return null;
    }
}
