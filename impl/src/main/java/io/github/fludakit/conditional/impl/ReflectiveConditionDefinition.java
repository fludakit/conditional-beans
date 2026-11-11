package io.github.fludakit.conditional.impl;

import io.github.fludakit.conditional.ConditionDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A {@link ConditionDefinition} backed by a reflective {@link Annotation} instance.
 */
final class ReflectiveConditionDefinition implements ConditionDefinition {

    private final Annotation annotation;

    ReflectiveConditionDefinition(Annotation annotation) {
        this.annotation = annotation;
    }

    @Override
    public String typeName() {
        return annotation.annotationType().getName();
    }

    @Override
    public String[] strings(String member) {
        return (String[]) memberValue(member);
    }

    @Override
    public boolean bool(String member) {
        return (Boolean) memberValue(member);
    }

    @Override
    public Class<?>[] classes(String member) {
        return (Class<?>[]) memberValue(member);
    }

    private Object memberValue(String member) {
        try {
            Method method = annotation.annotationType().getMethod(member);
            return method.invoke(annotation);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException("No such member " + member + " on " + typeName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Could not read member " + member + " on " + typeName(), e.getCause());
        }
    }
}
