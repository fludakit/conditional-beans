package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.full.support.Conditions;
import io.github.fludakit.conditional.full.support.Fixtures;
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
 * A condition that asks for a bean finds it by its type.
 * <p>
 * The context of a condition reports the bean types of the deployment and nothing else, so the qualifiers of
 * a bean are not part of the question a condition can ask. The qualifiers of the deployment are still the
 * ones the container reports.
 * </p>
 */
@ExtendWith(WeldJunit5Extension.class)
class HasBeanTypeTest {

    /** Enabled, because a bean of that type exists. */
    @ApplicationScoped
    @Conditional(Conditions.HasBlueMarker.class)
    public static class NeedsBlueMarker {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            NeedsBlueMarker.class,
            Fixtures.Marker.class,
            Fixtures.BlueMarker.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void theBeanOfTheTypeIsFound() {
        assertThat(beanManager.getBeans(Fixtures.BlueMarker.class, Fixtures.Blue.Literal.INSTANCE)).hasSize(1);
        assertThat(beanManager.getBeans(NeedsBlueMarker.class)).hasSize(1);
    }

    @Test
    void theQualifierOfTheBeanIsUnrelatedToTheCondition() {
        assertThat(beanManager.getBeans(Fixtures.Marker.class)).isNotEmpty();
        assertThat(beanManager.getBeans(Fixtures.BlueMarker.class, Fixtures.Red.Literal.INSTANCE)).isEmpty();
    }
}
