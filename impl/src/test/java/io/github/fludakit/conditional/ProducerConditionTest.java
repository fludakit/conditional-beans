package io.github.fludakit.conditional;

import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Conditions;
import io.github.fludakit.conditional.support.Fixtures;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A condition on a producer method or a producer field decides whether the produced bean is part of the
 * deployment.
 * <p>
 * The produced bean behaves like any other bean of the container: it is injected, and an injection point
 * that requires a bean that a condition removed fails the deployment. Only the produced bean is removed -
 * the class that holds the producer stays in the deployment.
 * </p>
 */
@ExtendWith(WeldJunit5Extension.class)
class ProducerConditionTest {

    /** A consumer of the bean that the disabled producer method would have produced. */
    @ApplicationScoped
    public static class NeedsDisabledMessage {

        /** Injects the bean that the condition removed. */
        @jakarta.inject.Inject
        private Fixtures.DisabledMessage message;
    }

    /** A consumer of the bean that the disabled producer field would have produced. */
    @ApplicationScoped
    public static class NeedsDisabledNotification {

        /** Injects the bean that the condition removed. */
        @jakarta.inject.Inject
        private Fixtures.DisabledNotification notification;
    }

    /** Holds two producers of the same type, one enabled and one disabled. */
    @Dependent
    public static class TwoProducers {

        /** Produces the message that is part of the deployment. */
        @Produces
        @ApplicationScoped
        @Conditional(Conditions.AlwaysTrue.class)
        public Fixtures.Message enabled() {
            return new Fixtures.Message("enabled producer");
        }

        /** Produces a message that its condition removes. */
        @Produces
        @ApplicationScoped
        @Conditional(Conditions.AlwaysFalse.class)
        public Fixtures.Message disabled() {
            return new Fixtures.Message("disabled producer");
        }
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            Fixtures.EnabledMethodProducer.class,
            Fixtures.EnabledFieldProducer.class,
            Fixtures.DisabledMethodProducer.class,
            Fixtures.DisabledFieldProducer.class).build();

    @Inject
    BeanManager beanManager;

    @Inject
    Fixtures.Message message;

    @Inject
    Fixtures.Notification notification;

    @Test
    void enabledProducerMethodProducesAnInjectableBean() {
        assertThat(message).isNotNull();
        assertThat(message.value()).isEqualTo("produced by method");
    }

    @Test
    void enabledProducerFieldProducesAnInjectableBean() {
        assertThat(notification).isNotNull();
        assertThat(notification.value()).isEqualTo("produced by field");
    }

    @Test
    void aDisabledProducerMethodMakesTheInjectionPointUnsatisfied() {
        Weld weld = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClasses(NeedsDisabledMessage.class, Fixtures.DisabledMethodProducer.class);

        assertThatThrownBy(weld::initialize)
                .isInstanceOf(DeploymentException.class)
                .hasMessageContaining("DisabledMessage");
    }

    @Test
    void aDisabledProducerFieldMakesTheInjectionPointUnsatisfied() {
        Weld weld = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClasses(NeedsDisabledNotification.class, Fixtures.DisabledFieldProducer.class);

        assertThatThrownBy(weld::initialize)
                .isInstanceOf(DeploymentException.class)
                .hasMessageContaining("DisabledNotification");
    }

    @Test
    void onlyTheProducedBeanIsRemoved() {
        assertThat(beanManager.getBeans(Fixtures.DisabledMessage.class)).isEmpty();
        assertThat(beanManager.getBeans(Fixtures.DisabledNotification.class)).isEmpty();

        assertThat(beanManager.getBeans(Fixtures.DisabledMethodProducer.class)).hasSize(1);
        assertThat(beanManager.getBeans(Fixtures.DisabledFieldProducer.class)).hasSize(1);
    }

    @Test
    void twoProducersOfTheSameTypeLeaveOnlyTheEnabledOne() {
        Weld weld = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClass(TwoProducers.class);

        WeldContainer container = weld.initialize();
        try {
            assertThat(container.getBeanManager().getBeans(Fixtures.Message.class)).hasSize(1);
            assertThat(container.select(Fixtures.Message.class).get().value()).isEqualTo("enabled producer");
        } finally {
            container.shutdown();
        }
    }
}
