package io.github.fludakit.conditional.lite;

import io.github.fludakit.conditional.builtin.ConditionalOnBean;
import io.github.fludakit.conditional.builtin.ConditionalOnClass;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingBean;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingClass;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingProperty;
import io.github.fludakit.conditional.builtin.ConditionalOnProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.se.SeContainer;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for all six built-in conditional annotations in the CDI Lite (BCE) path.
 */
class BuiltInConditionsSmokeTest {

    @ApplicationScoped
    public static class MarkerBean {
    }

    public static class MissingType {
    }

    @ApplicationScoped
    @ConditionalOnProperty(name = "lite.smoke.enabled", havingValue = "true")
    public static class PropertyMatchBean {
    }

    @ApplicationScoped
    @ConditionalOnMissingProperty(name = "lite.smoke.missing")
    public static class MissingPropertyBean {
    }

    @ApplicationScoped
    @ConditionalOnClass(name = "java.lang.String")
    public static class ClassPresentBean {
    }

    @ApplicationScoped
    @ConditionalOnMissingClass("com.example.definitely.missing.LiteSmokeMissingClass")
    public static class MissingClassBean {
    }

    @ApplicationScoped
    @ConditionalOnBean(MarkerBean.class)
    public static class BeanPresentBean {
    }

    @ApplicationScoped
    @ConditionalOnMissingBean(MissingType.class)
    public static class BeanMissingBean {
    }

    @Test
    void propertyAndClassBuiltins() {
        System.setProperty("lite.smoke.enabled", "true");
        try (SeContainer container = container()) {
            assertThat(container.getBeanContainer().getBeans(PropertyMatchBean.class)).hasSize(1);
            assertThat(container.getBeanContainer().getBeans(MissingPropertyBean.class)).hasSize(1);
            assertThat(container.getBeanContainer().getBeans(ClassPresentBean.class)).hasSize(1);
            assertThat(container.getBeanContainer().getBeans(MissingClassBean.class)).hasSize(1);
        } finally {
            System.clearProperty("lite.smoke.enabled");
        }
    }

    @Test
    void beanBuiltins() {
        try (SeContainer container = container()) {
            assertThat(container.getBeanContainer().getBeans(MarkerBean.class)).hasSize(1);
            assertThat(container.getBeanContainer().getBeans(BeanPresentBean.class)).hasSize(1);
            assertThat(container.getBeanContainer().getBeans(BeanMissingBean.class)).hasSize(1);
        }
    }

    private static SeContainer container() {
        return new Weld()
                .disableDiscovery()
                .setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .packages(BuiltInConditionsSmokeTest.class)
                .initialize();
    }
}
