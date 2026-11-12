package io.github.fludakit.conditional.full;

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
 * A condition that asks for the presence of a bean must give the same answer no matter in which order the
 * beans are declared. The bean that is required is declared last on purpose.
 */
@ExtendWith(WeldJunit5Extension.class)
class HasBeanConditionTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            Fixtures.NeedsMarker.class,
            Fixtures.Marker.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void requiredBeanIsFoundEvenWhenItIsDeclaredLater() {
        assertThat(beanManager.getBeans(Fixtures.Marker.class)).isNotEmpty();
        assertThat(beanManager.getBeans(Fixtures.NeedsMarker.class)).hasSize(1);
    }
}