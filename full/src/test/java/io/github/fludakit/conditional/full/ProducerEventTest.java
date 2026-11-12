package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.full.FullConditionalExtension;
import io.github.fludakit.conditional.full.support.Fixtures;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessProducerField;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Type;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A producer whose condition does not match is removed from the deployment before the container registers
 * it, so the container never fires a {@code ProcessProducerMethod} or {@code ProcessProducerField} event for
 * it, and the produced bean never produces a {@code ProcessBean} event.
 * <p>
 * This is the producer counterpart of the rule that a vetoed bean produces no {@code ProcessBean} event,
 * and it is what lets another extension trust that a bean it observes is one that is really deployed.
 * </p>
 */
class ProducerEventTest {

    /** The producer methods the container announced. */
    private static final List<String> PRODUCER_METHODS = new ArrayList<>();

    /** The producer fields the container announced. */
    private static final List<String> PRODUCER_FIELDS = new ArrayList<>();

    /** The bean types of every bean the container registered. */
    private static final List<Set<Type>> REGISTERED_BEANS = new ArrayList<>();

    /**
     * Records the events the container fires for producers.
     */
    public static class ProducerEventRecorder implements Extension {

        /**
         * Records the producer method that the container is about to register.
         *
         * @param event the process producer method event
         */
        void producerMethod(@Observes ProcessProducerMethod<?, ?> event) {
            Method method = event.getAnnotatedProducerMethod().getJavaMember();
            PRODUCER_METHODS.add(method.getDeclaringClass().getSimpleName() + "#" + method.getName());
        }

        /**
         * Records the producer field that the container is about to register.
         *
         * @param event the process producer field event
         */
        void producerField(@Observes ProcessProducerField<?, ?> event) {
            Field field = event.getAnnotatedProducerField().getJavaMember();
            PRODUCER_FIELDS.add(field.getDeclaringClass().getSimpleName() + "#" + field.getName());
        }

        /**
         * Records the bean that the container registered.
         *
         * @param event the process bean event
         */
        void bean(@Observes ProcessBean<?> event) {
            Bean<?> bean = event.getBean();
            REGISTERED_BEANS.add(bean.getTypes());
        }
    }

    @BeforeAll
    static void clearRecordedEvents() {
        PRODUCER_METHODS.clear();
        PRODUCER_FIELDS.clear();
        REGISTERED_BEANS.clear();
    }

    @Test
    void onlyEnabledProducersAreAnnouncedByTheContainer() {
        WeldContainer container = new Weld().disableDiscovery()
                .addExtensions(FullConditionalExtension.class, ProducerEventRecorder.class)
                .addBeanClasses(Fixtures.EnabledMethodProducer.class,
                        Fixtures.DisabledMethodProducer.class,
                        Fixtures.EnabledFieldProducer.class,
                        Fixtures.DisabledFieldProducer.class)
                .initialize();
        try {
            assertThat(PRODUCER_METHODS).contains("EnabledMethodProducer#produceMessage");
            assertThat(PRODUCER_METHODS).doesNotContain("DisabledMethodProducer#produceMessage");

            assertThat(PRODUCER_FIELDS).contains("EnabledFieldProducer#notification");
            assertThat(PRODUCER_FIELDS).doesNotContain("DisabledFieldProducer#notification");
        } finally {
            container.shutdown();
        }
    }

    @Test
    void aRemovedProducerNeverRegistersItsBean() {
        WeldContainer container = new Weld().disableDiscovery()
                .addExtensions(FullConditionalExtension.class, ProducerEventRecorder.class)
                .addBeanClasses(Fixtures.EnabledMethodProducer.class,
                        Fixtures.DisabledMethodProducer.class,
                        Fixtures.EnabledFieldProducer.class,
                        Fixtures.DisabledFieldProducer.class)
                .initialize();
        try {
            assertThat(registeredBeansOfType(Fixtures.Message.class)).hasSize(1);
            assertThat(registeredBeansOfType(Fixtures.DisabledMessage.class)).isEmpty();
            assertThat(registeredBeansOfType(Fixtures.Notification.class)).hasSize(1);
            assertThat(registeredBeansOfType(Fixtures.DisabledNotification.class)).isEmpty();
        } finally {
            container.shutdown();
        }
    }

    private static List<Set<Type>> registeredBeansOfType(Class<?> type) {
        List<Set<Type>> beans = new ArrayList<>();
        for (Set<Type> types : REGISTERED_BEANS) {
            if (types.contains(type)) {
                beans.add(types);
            }
        }
        return beans;
    }
}
