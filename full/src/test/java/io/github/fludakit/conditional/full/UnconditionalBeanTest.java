package io.github.fludakit.conditional.full;

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
 * Beans without a condition are entirely unaffected by the extension.
 */
@ExtendWith(WeldJunit5Extension.class)
class UnconditionalBeanTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            Fixtures.Plain.class,
            Fixtures.Marker.class,
            Fixtures.BlueMarker.class).build();

    @Inject
    BeanManager beanManager;

    @Inject
    Fixtures.Plain plain;

    @Test
    void unconditionalBeansAreDeployed() {
        assertThat(plain).isNotNull();
        assertThat(beanManager.getBeans(Fixtures.Plain.class)).hasSize(1);
        assertThat(beanManager.getBeans(Fixtures.Marker.class)).isNotEmpty();
    }
}