package io.github.fludakit.conditional;

import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Fixtures;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A bean whose condition does not match is removed from the deployment entirely.
 */
@ExtendWith(WeldJunit5Extension.class)
class ConditionalDisabledTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            Fixtures.Disabled.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void disabledBeanIsAbsentFromTheContainer() {
        assertThat(beanManager.getBeans(Fixtures.Disabled.class)).isEmpty();
    }

    @Test
    void disabledBeanDoesNotProduceABeanEvent() {
        assertThat(beanManager.getBeans(Fixtures.Disabled.class)).isEmpty();
    }
}