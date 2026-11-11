package io.github.fludakit.conditional;

import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Fixtures;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The deployment model has to answer the {@code hasBean} question the same way the container answers it.
 * Otherwise a condition would enable a bean that the deployment does not contain, or the other way round.
 * <p>
 * The only documented difference is the bean that is vetoed by a condition: it is still reported as present,
 * because {@code hasBean} describes the beans that <em>could</em> be part of the deployment.
 * </p>
 */
@ExtendWith(WeldJunit5Extension.class)
class HasBeanAgreesWithContainerTest {

    /** The answers collected while the conditions were evaluated. */
    private static final Map<String, Boolean> ANSWERS = new LinkedHashMap<>();

    /** A condition that records what the deployment model answers. */
    public static class Probe implements Condition {

        @Override
        public boolean test(ConditionContext context) {
            ANSWERS.put("plain", context.hasBean(Fixtures.Plain.class));
            ANSWERS.put("marker", context.hasBean(Fixtures.Marker.class));
            ANSWERS.put("blueMarker", context.hasBean(Fixtures.BlueMarker.class));
            ANSWERS.put("markerWithBlue", context.hasBean(Fixtures.Marker.class, Fixtures.Blue.class));
            ANSWERS.put("named", context.hasBean(Fixtures.NamedBean.class));
            ANSWERS.put("produced", context.hasBean(Fixtures.Message.class));
            ANSWERS.put("restricted", context.hasBean(Fixtures.Restricted.class));
            ANSWERS.put("restrictedType", context.hasBean(Fixtures.OnlyInterface.class));
            ANSWERS.put("vetoed", context.hasBean(Fixtures.Disabled.class));
            return true;
        }
    }

    /** A bean that carries the probe condition. */
    @ApplicationScoped
    @Conditional(Probe.class)
    public static class ProbeBean {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            ProbeBean.class,
            Fixtures.Plain.class,
            Fixtures.Marker.class,
            Fixtures.BlueMarker.class,
            Fixtures.NamedBean.class,
            Fixtures.Restricted.class,
            Fixtures.EnabledMethodProducer.class,
            Fixtures.Disabled.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void theDeploymentModelAgreesWithTheContainer() {
        assertThat(beanManager.getBeans(ProbeBean.class)).hasSize(1);
        assertAgrees("plain", beanManager.getBeans(Fixtures.Plain.class));
        assertAgrees("marker", beanManager.getBeans(Fixtures.Marker.class));
        assertAgrees("blueMarker", beanManager.getBeans(Fixtures.BlueMarker.class));
        assertAgrees("named", beanManager.getBeans(Fixtures.NamedBean.class));
        assertAgrees("produced", beanManager.getBeans(Fixtures.Message.class));
        assertAgrees("restricted", beanManager.getBeans(Fixtures.Restricted.class));
        assertAgrees("restrictedType", beanManager.getBeans(Fixtures.OnlyInterface.class));
        assertAgrees("markerWithBlue", beanManager.getBeans(Fixtures.Marker.class, Fixtures.Blue.Literal.INSTANCE));
    }

    @Test
    void aVetoedBeanIsStillReportedAsPresent() {
        assertThat(ANSWERS.get("vetoed")).isTrue();
        assertThat(beanManager.getBeans(Fixtures.Disabled.class)).isEmpty();
    }

    private static void assertAgrees(String key, Set<? extends Bean<?>> containerBeans) {
        assertThat(ANSWERS).containsEntry(key, !containerBeans.isEmpty());
    }
}
