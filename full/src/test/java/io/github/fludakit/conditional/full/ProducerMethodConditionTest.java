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
 * A condition declared on a producer method gates the produced bean.
 */
@ExtendWith(WeldJunit5Extension.class)
class ProducerMethodConditionTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            Fixtures.EnabledMethodProducer.class,
            Fixtures.DisabledMethodProducer.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void enabledProducerMethodProducesItsBean() {
        assertThat(beanManager.getBeans(Fixtures.Message.class)).hasSize(1);
    }

    @Test
    void disabledProducerMethodProducesNothing() {
        assertThat(beanManager.getBeans(Fixtures.DisabledMessage.class)).isEmpty();
    }
}