package io.github.fludakit.conditional.lite.fixtures;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.lang.annotation.Annotation;

/**
 * The beans and the conditions of the CDI Lite deployment.
 */
public final class LiteFixtures {

    private LiteFixtures() {
    }

    /** A condition that always matches. */
    public static class AlwaysEnabled implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return true;
        }
    }

    /** A condition that never matches. */
    public static class NeverEnabled implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return false;
        }
    }

    /** A condition driven by the {@code lite.enabled} configuration property. */
    public static class EnabledByProperty implements Condition<Annotation> {

        @Override
        public boolean matches(ConditionContext context, Annotation annotation) {
            return context.getProperty("lite.enabled").isPresent();
        }
    }

    /** Enabled, the condition matches. */
    @ApplicationScoped
    @Conditional(AlwaysEnabled.class)
    public static class Enabled {
    }

    /** Disabled, the condition does not match. */
    @ApplicationScoped
    @Conditional(NeverEnabled.class)
    public static class Disabled {
    }

    /** Enabled, the configuration entry is present. */
    @ApplicationScoped
    @Conditional(EnabledByProperty.class)
    public static class EnabledByPropertyBean {
    }

    /** Holds a producer method that is enabled and one that is not. */
    @ApplicationScoped
    public static class Producer {

        /** Produces a message that is removed. */
        @Produces
        @Conditional(NeverEnabled.class)
        public Message disabled() {
            return new Message();
        }

        /** Produces a notification. */
        @Produces
        @Conditional(AlwaysEnabled.class)
        public Notification enabled() {
            return new Notification();
        }
    }

    /** A message produced by a disabled producer method. */
    public static class Message {
    }

    /** A notification produced by an enabled producer method. */
    public static class Notification {
    }
}
