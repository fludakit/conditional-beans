package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.full.serviceloader.ServiceLoaderDisabledBean;
import io.github.fludakit.conditional.full.serviceloader.ServiceLoaderEnabledBean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The extension is registered as a service provider, so a default Weld SE deployment activates it without
 * any explicit registration.
 */
class WeldDiscoveryTest {

    @Test
    void defaultDiscoveryActivatesTheExtension() {
        try (WeldContainer container = new Weld()
                .addPackages(ServiceLoaderEnabledBean.class, ServiceLoaderDisabledBean.class)
                .initialize()) {

            BeanManager beanManager = container.getBeanManager();
            assertThat(beanManager.getBeans(ServiceLoaderEnabledBean.class)).hasSize(1);
            assertThat(beanManager.getBeans(ServiceLoaderDisabledBean.class)).isEmpty();
        }
    }
}