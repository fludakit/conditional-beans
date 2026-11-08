package io.github.fludakit.conditional.internal;

import io.github.fludakit.conditional.ConditionContext;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The {@link ConditionContext} handed to a condition while it is evaluated.
 */
final class DefaultConditionContext implements ConditionContext {

    private final Annotated annotated;

    private final BeanManager beanManager;

    private final DeploymentModel model;

    DefaultConditionContext(Annotated annotated, BeanManager beanManager, DeploymentModel model) {
        this.annotated = annotated;
        this.beanManager = beanManager;
        this.model = model;
    }

    @Override
    public Annotated getAnnotated() {
        return annotated;
    }

    @Override
    public BeanManager getBeanManager() {
        return beanManager;
    }

    @Override
    public <T> boolean hasBean(Class<T> type, Annotation... qualifiers) {
        return model.hasBean(type, qualifiers);
    }

    @Override
    public <T> boolean hasBean(TypeLiteral<T> type, Annotation... qualifiers) {
        return model.hasBean(type.getType(), qualifiers);
    }

    @Override
    public <T> boolean hasQualifiedBean(Class<T> type, Class<? extends Annotation>... qualifierTypes) {
        return model.hasQualifiedBean(type, qualifierTypes(qualifierTypes));
    }

    @Override
    public <T> boolean hasQualifiedBean(TypeLiteral<T> type, Class<? extends Annotation>... qualifierTypes) {
        return model.hasQualifiedBean(type.getType(), qualifierTypes(qualifierTypes));
    }

    private static List<Class<? extends Annotation>> qualifierTypes(Class<? extends Annotation>[] qualifierTypes) {
        return qualifierTypes == null ? List.of() : Arrays.asList(qualifierTypes);
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