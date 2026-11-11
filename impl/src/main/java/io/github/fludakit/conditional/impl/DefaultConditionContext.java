package io.github.fludakit.conditional.impl;

import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.ConditionDefinition;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The {@link ConditionContext} handed to a condition while it is evaluated.
 */
final class DefaultConditionContext implements ConditionContext {

    private final DeploymentModel model;

    private final Annotation declaration;

    DefaultConditionContext(DeploymentModel model, Annotation declaration) {
        this.model = model;
        this.declaration = declaration;
    }

    @Override
    public boolean hasBean(Class<?> type, Class<?>... qualifierTypes) {
        return model.hasQualifiedBean(type, Arrays.asList(qualifierTypes));
    }

    @Override
    public boolean hasQualifiedBean(Class<?> type, Class<?>... qualifierTypes) {
        return model.hasQualifiedBean(type, Arrays.asList(qualifierTypes));
    }

    @Override
    public ConditionDefinition getAnnotation() {
        return new ReflectiveConditionDefinition(declaration);
    }

    @Override
    public boolean hasClass(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        try {
            Class.forName(className, false, classLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public <T> boolean hasClass(Class<T> type) {
        return type != null;
    }

    @Override
    public Optional<String> getProperty(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String value = System.getProperty(name);
        if (value != null) {
            return Optional.of(value);
        }
        value = System.getenv(name);
        if (value != null) {
            return Optional.of(value);
        }
        String relaxed = name.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        value = System.getenv(relaxed);
        return value == null ? Optional.empty() : Optional.of(value);
    }

    private static ClassLoader classLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = DefaultConditionContext.class.getClassLoader();
        }
        return classLoader;
    }
}
