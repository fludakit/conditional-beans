package io.github.fludakit.conditional;

import io.github.fludakit.conditional.annotation.ConditionalOnClass;
import io.github.fludakit.conditional.annotation.ConditionalOnMissingProperty;
import io.github.fludakit.conditional.annotation.ConditionalOnProperty;
import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
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
 * Runs the same conditional-bean application against a CDI container, treating {@code api} and
 * {@code impl} as external dependencies. This is the seam where a second container (OpenWebBeans) is added
 * later to prove that {@code @Conditional} is a property of the specification, not of one implementation.
 */
@ExtendWith(WeldJunit5Extension.class)
class ConditionalBeansPortabilityTest {

    @ApplicationScoped
    @ConditionalOnProperty(name = "portability.flag")
    public static class EnabledByProperty {
    }

    @ApplicationScoped
    @ConditionalOnMissingProperty(name = "portability.flag")
    public static class EnabledByMissingProperty {
    }

    @ApplicationScoped
    @ConditionalOnClass(name = "java.lang.String")
    public static class EnabledByClass {
    }

    @ApplicationScoped
    @ConditionalOnClass(name = "com.example.DefinitelyNotOnTheClasspath")
    public static class DisabledByClass {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            EnabledByProperty.class,
            EnabledByMissingProperty.class,
            EnabledByClass.class,
            DisabledByClass.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void conditionsEnableAndDisableBeans() {
        // "portability.flag" is absent, so the property condition fails and its inverse passes.
        assertThat(beanManager.getBeans(EnabledByProperty.class)).isEmpty();
        assertThat(beanManager.getBeans(EnabledByMissingProperty.class)).hasSize(1);

        assertThat(beanManager.getBeans(EnabledByClass.class)).hasSize(1);
        assertThat(beanManager.getBeans(DisabledByClass.class)).isEmpty();
    }
}
