package io.github.fludakit.conditional.lite;

import io.github.fludakit.conditional.lite.fixtures.LiteFixtures;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CDI Lite path, exercised on Quarkus Arc.
 * <p>
 * Arc is a CDI Lite container, so the build compatible extension of the `lite` module is what decides
 * which beans are part of the deployment. The test checks the same behaviour as the Weld test, on a second
 * container.
 * </p>
 */
@QuarkusTest
class QuarkusArcLiteTest {

    @Inject
    Instance<LiteFixtures.Message> messages;

    @Inject
    Instance<LiteFixtures.Notification> notifications;

    @Inject
    Instance<LiteFixtures.Enabled> enabled;

    @Inject
    Instance<LiteFixtures.Disabled> disabled;

    @Inject
    Instance<LiteFixtures.EnabledByPropertyBean> enabledByProperty;

    @Test
    void aProducerWhoseConditionDoesNotMatchIsNotAProducer() {
        assertThat(messages).isEmpty();
        assertThat(notifications).hasSize(1);
    }

    @Test
    void aBeanWhoseConditionMatchesIsPartOfTheDeployment() {
        assertThat(enabled).hasSize(1);
    }

    @Test
    void aConditionDrivenByAConfigurationEntryIsEvaluatedWhileTheApplicationIsBuilt() {
        assertThat(enabledByProperty).isEmpty();
    }

    @Test
    void aConditionThatDoesNotMatchRemovesTheBean() {
        assertThat(disabled).isEmpty();
    }
}
