package io.github.fludakit.conditional.full.support;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

import java.lang.annotation.Annotation;

/**
 * The conditions used by the test suite.
 */
public final class Conditions {

    private Conditions() {
    }

    /** A condition that always matches. */
    public static class AlwaysTrue implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return true;
        }
    }

    /** A condition that never matches. */
    public static class AlwaysFalse implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return false;
        }
    }

    /** A condition driven by the {@code app.country} configuration entry. */
    public static class CountryIsBrazil implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return "BR".equals(context.getEnvironmentValue("app.country").orElse(null));
        }
    }

    /** A condition driven by the {@code app.country} configuration entry. */
    public static class CountryIsPortugal implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return "PT".equals(context.getEnvironmentValue("app.country").orElse(null));
        }
    }

    /** A condition that detects a class that is always on the classpath. */
    public static class HasPresentClass implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return context.hasClass("java.lang.String");
        }
    }

    /** A condition that detects a class that is never on the classpath. */
    public static class HasMissingClass implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return context.hasClass("com.github.fludakit.NotOnTheClasspath");
        }
    }

    /** A condition that requires another bean to be part of the deployment. */
    public static class HasMarker implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return isRegistered(context, Fixtures.Marker.class);
        }
    }

    /** A condition that requires a bean of a type of its own to be part of the deployment. */
    public static class HasBlueMarker implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return isRegistered(context, Fixtures.BlueMarker.class);
        }
    }

    /** A condition that always fails, to prove that failures abort the deployment. */
    public static class Throwing implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            throw new IllegalStateException("condition failed on purpose");
        }
    }

    /** A condition without a no-argument constructor. */
    public static class WithoutDefaultConstructor implements Condition<Annotation> {

        private final String value;

        public WithoutDefaultConstructor(String value) {
            this.value = value;
        }

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return value != null;
        }
    }

    /**
     * Whether a bean of the given type is part of the deployment.
     */
    private static boolean isRegistered(ConditionContext context, Class<?> type) {
        return context.hasBean(type);
    }
}
