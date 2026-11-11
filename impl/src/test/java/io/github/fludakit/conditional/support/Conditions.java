package io.github.fludakit.conditional.support;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;

/**
 * The conditions used by the test suite.
 */
public final class Conditions {

    private Conditions() {
    }

    /** A condition that always matches. */
    public static class AlwaysTrue implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return true;
        }
    }

    /** A condition that never matches. */
    public static class AlwaysFalse implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return false;
        }
    }

    /** A condition driven by the {@code app.country} configuration entry. */
    public static class CountryIsBrazil implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return "BR".equals(context.getProperty("app.country").orElse(null));
        }
    }

    /** A condition driven by the {@code app.country} configuration entry. */
    public static class CountryIsPortugal implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return "PT".equals(context.getProperty("app.country").orElse(null));
        }
    }

    /** A condition that detects a class that is always on the classpath. */
    public static class HasPresentClass implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.hasClass("java.lang.String");
        }
    }

    /** A condition that detects a class that is never on the classpath. */
    public static class HasMissingClass implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.hasClass("com.github.fludakit.NotOnTheClasspath");
        }
    }

    /** A condition that requires another bean to be part of the deployment. */
    public static class HasMarker implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.hasBean(Fixtures.Marker.class);
        }
    }

    /** A condition that requires a qualified bean to be part of the deployment. */
    public static class HasBlueMarker implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.hasBean(Fixtures.Marker.class, Fixtures.Blue.class);
        }
    }

    /** A condition that always fails, to prove that failures abort the deployment. */
    public static class Throwing implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            throw new IllegalStateException("condition failed on purpose");
        }
    }

    /** A condition without a no-argument constructor. */
    public static class WithoutDefaultConstructor implements Condition {

        private final String value;

        public WithoutDefaultConstructor(String value) {
            this.value = value;
        }

        @Override
        public boolean test(ConditionContext context) {
            return value != null;
        }
    }
}