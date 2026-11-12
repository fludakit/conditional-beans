package io.github.fludakit.conditional.full.support;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;

/**
 * The conditions used by the test suite.
 */
public final class Conditions {

    private Conditions() {
    }

    /** A condition that always matches. */
    public static class AlwaysTrue implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return true;
        }
    }

    /** A condition that never matches. */
    public static class AlwaysFalse implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return false;
        }
    }

    /** A condition driven by the {@code app.country} configuration entry. */
    public static class CountryIsBrazil implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return "BR".equals(context.getEnvironmentValue("app.country").orElse(null));
        }
    }

    /** A condition driven by the {@code app.country} configuration entry. */
    public static class CountryIsPortugal implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return "PT".equals(context.getEnvironmentValue("app.country").orElse(null));
        }
    }

    /** A condition that detects a class that is always on the classpath. */
    public static class HasPresentClass implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return context.hasClass("java.lang.String");
        }
    }

    /** A condition that detects a class that is never on the classpath. */
    public static class HasMissingClass implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return context.hasClass("com.github.fludakit.NotOnTheClasspath");
        }
    }

    /** A condition that requires another bean to be part of the deployment. */
    public static class HasMarker implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return isRegistered(context, Fixtures.Marker.class);
        }
    }

    /** A condition that requires a bean of a type of its own to be part of the deployment. */
    public static class HasBlueMarker implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return isRegistered(context, Fixtures.BlueMarker.class);
        }
    }

    /** A condition that always fails, to prove that failures abort the deployment. */
    public static class Throwing implements Condition<Conditional> {

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            throw new IllegalStateException("condition failed on purpose");
        }
    }

    /** A condition without a no-argument constructor. */
    public static class WithoutDefaultConstructor implements Condition<Conditional> {

        private final String value;

        public WithoutDefaultConstructor(String value) {
            this.value = value;
        }

        @Override
        public boolean matches(ConditionContext context, Conditional annotation) {
            return value != null;
        }
    }

    /**
     * Whether a bean of the given type is part of the deployment.
     */
    private static boolean isRegistered(ConditionContext context, Class<?> type) {
        return context.getRegisteredBeanTypes().anyMatch(registered -> registered.equals(type));
    }
}
