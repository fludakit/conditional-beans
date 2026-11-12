package io.github.fludakit.conditional.full;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Condition;

import io.github.fludakit.conditional.full.FullConditionalExtension;
import io.github.fludakit.conditional.full.support.Fixtures;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.inject.spi.BeanManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A bean whose condition matches is deployed exactly like an unannotated bean.
 */
@ExtendWith(WeldJunit5Extension.class)
class ConditionalEnabledTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            Fixtures.Enabled.class,
            Fixtures.Disabled.class).build();

    @Inject
    Fixtures.Enabled enabled;

    @Inject
    BeanManager beanManager;

    @Test
    void enabledBeanIsDeployed() {
        assertThat(enabled).isNotNull();
        assertThat(beanManager.getBeans(Fixtures.Enabled.class)).hasSize(1);
    }

    @Test
    void disabledBeanIsNotDeployed() {
        assertThat(beanManager.getBeans(Fixtures.Disabled.class)).isEmpty();
    }
}