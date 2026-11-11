package io.github.fludakit.conditional;

import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
import jakarta.enterprise.inject.spi.Extension;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The extension is registered as a service provider, so that it is active in every deployment that has the
 * artifact on the classpath.
 */
class ServiceLoaderDiscoveryTest {

    @Test
    void extensionIsRegisteredAsAServiceProvider() {
        List<String> providers = ServiceLoader.load(Extension.class).stream()
                .map(provider -> provider.type().getName())
                .toList();
        assertThat(providers).contains(ConditionalBeansExtension.class.getName());
    }

    @Test
    void extensionIsDeclaredInTheServiceFile() throws Exception {
        try (var input = ServiceLoaderDiscoveryTest.class.getClassLoader()
                .getResourceAsStream("META-INF/services/jakarta.enterprise.inject.spi.Extension")) {
            assertThat(input).isNotNull();
            String declaration = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(declaration).contains(ConditionalBeansExtension.class.getName());
        }
    }
}