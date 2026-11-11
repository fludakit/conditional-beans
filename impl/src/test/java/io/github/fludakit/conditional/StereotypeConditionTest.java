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
 * A condition declared on a stereotype applies to every bean carrying that stereotype.
 */
@ExtendWith(WeldJunit5Extension.class)
class StereotypeConditionTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            Fixtures.Stereotyped.class,
            Fixtures.Plain.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void stereotypeConditionDisablesTheBean() {
        assertThat(beanManager.getBeans(Fixtures.Stereotyped.class)).isEmpty();
    }

    @Test
    void beansWithoutTheStereotypeAreUnaffected() {
        assertThat(beanManager.getBeans(Fixtures.Plain.class)).hasSize(1);
    }
}