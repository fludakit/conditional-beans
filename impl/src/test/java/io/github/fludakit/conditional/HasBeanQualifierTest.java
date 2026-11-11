package io.github.fludakit.conditional;

import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Conditions;
import io.github.fludakit.conditional.support.Fixtures;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A condition that asks for a qualified bean only matches when the qualifier is taken into account.
 */
@ExtendWith(WeldJunit5Extension.class)
class HasBeanQualifierTest {

    /** Enabled, because a bean with the {@code @Blue} qualifier exists. */
    @ApplicationScoped
    @Conditional(Conditions.HasBlueMarker.class)
    public static class NeedsBlueMarker {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            NeedsBlueMarker.class,
            Fixtures.Marker.class,
            Fixtures.BlueMarker.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void qualifiedBeanIsFound() {
        assertThat(beanManager.getBeans(Fixtures.BlueMarker.class, Fixtures.Blue.Literal.INSTANCE)).hasSize(1);
        assertThat(beanManager.getBeans(NeedsBlueMarker.class)).hasSize(1);
    }

    @Test
    void unqualifiedBeanDoesNotSatisfyAQualifiedRequirement() {
        assertThat(beanManager.getBeans(Fixtures.Marker.class)).isNotEmpty();
        assertThat(beanManager.getBeans(Fixtures.BlueMarker.class, Fixtures.Red.Literal.INSTANCE)).isEmpty();
    }
}