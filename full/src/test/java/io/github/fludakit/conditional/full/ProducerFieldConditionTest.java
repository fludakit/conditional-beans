package io.github.fludakit.conditional.full;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Condition;

import io.github.fludakit.conditional.full.FullConditionalExtension;
import io.github.fludakit.conditional.full.support.Fixtures;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A condition declared on a producer field gates the produced bean.
 */
@ExtendWith(WeldJunit5Extension.class)
class ProducerFieldConditionTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            Fixtures.EnabledFieldProducer.class,
            Fixtures.DisabledFieldProducer.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void enabledProducerFieldProducesItsBean() {
        assertThat(beanManager.getBeans(Fixtures.Notification.class)).hasSize(1);
    }

    @Test
    void disabledProducerFieldProducesNothing() {
        assertThat(beanManager.getBeans(Fixtures.DisabledNotification.class)).isEmpty();
    }
}