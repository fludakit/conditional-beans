package io.github.fludakit.conditional.support;

import io.github.fludakit.conditional.Conditional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The beans and qualifiers used by the test suite.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /** A qualifier used to verify that qualifiers are taken into account. */
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Blue {

        /** The literal instance of the qualifier. */
        final class Literal extends AnnotationLiteral<Blue> implements Blue {

            private static final long serialVersionUID = 1L;

            /** The shared literal instance. */
            public static final Literal INSTANCE = new Literal();
        }
    }

    /** A qualifier that no bean carries. */
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Red {

        /** The literal instance of the qualifier. */
        final class Literal extends AnnotationLiteral<Red> implements Red {

            private static final long serialVersionUID = 1L;

            /** The shared literal instance. */
            public static final Literal INSTANCE = new Literal();
        }
    }

    /** A stereotype that always disables the beans carrying it. */
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @Conditional(Conditions.AlwaysFalse.class)
    public @interface AlwaysDisabled {
    }

    /** A plain bean, without any condition. */
    @ApplicationScoped
    public static class Plain {
    }

    /** A bean that is always enabled. */
    @ApplicationScoped
    @Conditional(Conditions.AlwaysTrue.class)
    public static class Enabled {
    }

    /** A bean that is never enabled. */
    @ApplicationScoped
    @Conditional(Conditions.AlwaysFalse.class)
    public static class Disabled {
    }

    /** A bean enabled by the {@code app.country} configuration entry. */
    @ApplicationScoped
    @Conditional(Conditions.CountryIsBrazil.class)
    public static class BrazilTaxCalculator {

        /** The country this calculator serves. */
        public String country() {
            return "BR";
        }
    }

    /** A bean enabled by the {@code app.country} configuration entry. */
    @ApplicationScoped
    @Conditional(Conditions.CountryIsPortugal.class)
    public static class PortugalTaxCalculator {

        /** The country this calculator serves. */
        public String country() {
            return "PT";
        }
    }

    /** A bean enabled by a condition that requires another bean. */
    @ApplicationScoped
    @Conditional(Conditions.HasMarker.class)
    public static class NeedsMarker {
    }

    /** A bean that is required by {@link NeedsMarker}. */
    @ApplicationScoped
    public static class Marker {
    }

    /** A qualified bean of type {@link Marker}. */
    @ApplicationScoped
    @Blue
    public static class BlueMarker extends Marker {
    }

    /** A bean enabled through a stereotype. */
    @ApplicationScoped
    @AlwaysDisabled
    public static class Stereotyped {
    }

    /** A message produced by an enabled producer method. */
    public static class Message {

        private final String value;

        /** Creates a message. */
        public Message(String value) {
            this.value = value;
        }

        /** The message body. */
        public String value() {
            return value;
        }
    }

    /** A message produced by a disabled producer method. */
    public static class DisabledMessage {

        private final String value;

        /** Creates a message. */
        public DisabledMessage(String value) {
            this.value = value;
        }

        /** The message body. */
        public String value() {
            return value;
        }
    }

    /** A notification produced by an enabled producer field. */
    public static class Notification {

        private final String value;

        /** Creates a notification. */
        public Notification(String value) {
            this.value = value;
        }

        /** The notification body. */
        public String value() {
            return value;
        }
    }

    /** A notification produced by a disabled producer field. */
    public static class DisabledNotification {

        private final String value;

        /** Creates a notification. */
        public DisabledNotification(String value) {
            this.value = value;
        }

        /** The notification body. */
        public String value() {
            return value;
        }
    }

    /** Holds a producer method that is always enabled. */
    @Dependent
    public static class EnabledMethodProducer {

        /** Produces a message. */
        @Produces
        @ApplicationScoped
        @Conditional(Conditions.AlwaysTrue.class)
        public Message produceMessage() {
            return new Message("produced by method");
        }
    }

    /** Holds a producer method that is never enabled. */
    @Dependent
    public static class DisabledMethodProducer {

        /** Produces a message. */
        @Produces
        @ApplicationScoped
        @Conditional(Conditions.AlwaysFalse.class)
        public DisabledMessage produceMessage() {
            return new DisabledMessage("produced by method");
        }
    }

    /** Holds a producer field that is always enabled. */
    @Dependent
    public static class EnabledFieldProducer {

        /** Produces a notification. */
        @Produces
        @Conditional(Conditions.AlwaysTrue.class)
        public Notification notification = new Notification("produced by field");
    }

    /** Holds a producer field that is never enabled. */
    @Dependent
    public static class DisabledFieldProducer {

        /** Produces a notification. */
        @Produces
        @Conditional(Conditions.AlwaysFalse.class)
        public DisabledNotification notification = new DisabledNotification("produced by field");
    }

    /** A named bean, which carries the {@code @Default} qualifier as well. */
    @ApplicationScoped
    @jakarta.inject.Named("namedBean")
    public static class NamedBean {
    }

    /** The only bean type of {@link Restricted}. */
    public interface OnlyInterface {
    }

    /** A bean whose bean types are restricted to {@link OnlyInterface}. */
    @Typed(OnlyInterface.class)
    @ApplicationScoped
    public static class Restricted implements OnlyInterface {
    }

    /** A bean that requires a disabled bean, so that the deployment cannot succeed. */
    @ApplicationScoped
    public static class NeedsDisabledBean {

        /** Injects the bean that is removed by its condition. */
        @jakarta.inject.Inject
        private Disabled disabled;
    }
}