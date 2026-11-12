package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.builtin.ConditionalOnBean;
import io.github.fludakit.conditional.builtin.ConditionalOnClass;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingBean;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingClass;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingProperty;
import io.github.fludakit.conditional.builtin.ConditionalOnProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests covering all six built-in conditional annotations.
 */
@ExtendWith(WeldJunit5Extension.class)
class BuiltInConditionsSmokeTest {

    @BeforeAll
    static void setUpProperties() {
        System.setProperty("smoke.feature.enabled", "true");
    }

    @AfterAll
    static void clearProperties() {
        System.clearProperty("smoke.feature.enabled");
    }

    /** Marker bean used by bean-presence conditions. */
    @ApplicationScoped
    public static class MarkerBean {
    }

    /** A type that is not deployed as a bean. */
    public static class MissingType {
    }

    @ApplicationScoped
    @ConditionalOnProperty(name = "smoke.feature.enabled", havingValue = "true")
    public static class PropertyMatchBean {
    }

    @ApplicationScoped
    @ConditionalOnMissingProperty(name = "smoke.feature.missing")
    public static class MissingPropertyBean {
    }

    @ApplicationScoped
    @ConditionalOnClass(name = "java.lang.String")
    public static class ClassPresentBean {
    }

    @ApplicationScoped
    @ConditionalOnMissingClass(name = "com.example.definitely.missing.SmokeMissingClass")
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

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            MarkerBean.class,
            PropertyMatchBean.class,
            MissingPropertyBean.class,
            ClassPresentBean.class,
            MissingClassBean.class,
            BeanPresentBean.class,
            BeanMissingBean.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void propertyConditions() {
        assertThat(beanManager.getBeans(PropertyMatchBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(MissingPropertyBean.class)).hasSize(1);
    }

    @Test
    void classConditions() {
        assertThat(beanManager.getBeans(ClassPresentBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(MissingClassBean.class)).hasSize(1);
    }

    @Test
    void beanConditions() {
        assertThat(beanManager.getBeans(BeanPresentBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(BeanMissingBean.class)).hasSize(1);
    }
}
