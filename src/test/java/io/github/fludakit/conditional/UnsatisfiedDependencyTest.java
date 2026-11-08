package io.github.fludakit.conditional;

import io.github.fludakit.conditional.internal.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Fixtures;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A disabled bean is removed from the deployment, not merely deactivated. An injection point that requires
 * it therefore fails the deployment.
 */
class UnsatisfiedDependencyTest {

    @Test
    void injectingADisabledBeanFailsTheDeployment() {
        Weld weld = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClass(Fixtures.NeedsDisabledBean.class)
                .addBeanClass(Fixtures.Disabled.class);

        assertThatThrownBy(weld::initialize)
                .isInstanceOf(DeploymentException.class)
                .hasMessageContaining("NeedsDisabledBean");
    }
}