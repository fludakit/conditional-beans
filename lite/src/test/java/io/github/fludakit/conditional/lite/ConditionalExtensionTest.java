package io.github.fludakit.conditional.lite;

import io.github.fludakit.conditional.lite.fixtures.LiteFixtures;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.se.SeContainer;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CDI Lite path, exercised on Weld in its lite mode.
 * <p>
 * The container runs with the discovery of the classpath switched off and with annotated discovery only,
 * which is the situation of a CDI Lite container: there is no portable extension API, and the build
 * compatible extension is what decides which beans are part of the deployment.
 * </p>
 */
class ConditionalExtensionTest {

    @Test
    void aProducerWhoseConditionDoesNotMatchIsNotAProducer() {
        try (SeContainer container = container()) {
            assertThat(container.getBeanContainer().getBeans(LiteFixtures.Message.class)).isEmpty();
            assertThat(container.getBeanContainer().getBeans(LiteFixtures.Notification.class)).hasSize(1);
        }
    }

    @Test
    void aConditionDrivenByAConfigurationEntryIsEvaluatedWhileTheApplicationIsBuilt() {
        System.setProperty("lite.enabled", "true");
        try (SeContainer container = container()) {
            assertThat(container.getBeanContainer().getBeans(LiteFixtures.EnabledByPropertyBean.class)).hasSize(1);
        } finally {
            System.clearProperty("lite.enabled");
        }
    }

    @Test
    void aConditionThatDoesNotMatchRemovesTheAnnotationsOfTheBean() {
        try (SeContainer container = container()) {
            // Weld translates the enhancement phase of a build compatible extension into a
            // ProcessAnnotatedType event, and it keeps a class it has discovered a managed bean of even when
            // its annotations are taken away, because it decides that a class is a managed bean by its
            // constructor and not by its annotations. The bean therefore stays in the deployment, without the
            // scope it was declared with.
            assertThat(container.getBeanContainer().getBeans(LiteFixtures.Disabled.class)).hasSize(1);
            assertThat(container.getBeanContainer().getBeans(LiteFixtures.Disabled.class).iterator().next().getScope())
                    .isEqualTo(Dependent.class);
        }
    }

    private static SeContainer container() {
        return new Weld()
                .disableDiscovery()
                .setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .packages(LiteFixtures.class)
                .initialize();
    }
}
