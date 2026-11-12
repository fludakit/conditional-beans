package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.builtin.ConditionalOnClass;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingClass;

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
 * The built-in class conditions decide which beans are part of the deployment.
 */
@ExtendWith(WeldJunit5Extension.class)
class BuiltInClassConditionTest {

    /** Enabled, the class is on the classpath. */
    @ApplicationScoped
    @ConditionalOnClass(name = "java.lang.String")
    public static class PresentClassBean {
    }

    /** Disabled, the class is not on the classpath. */
    @ApplicationScoped
    @ConditionalOnClass(name = "com.github.fludakit.NotOnTheClasspath")
    public static class MissingClassBean {
    }

    /** Enabled, the class is not on the classpath. */
    @ApplicationScoped
    @ConditionalOnMissingClass(value = "com.github.fludakit.NotOnTheClasspath")
    public static class MissingClassAllowedBean {
    }

    /** Disabled, the class is on the classpath. */
    @ApplicationScoped
    @ConditionalOnMissingClass(value = "java.lang.String")
    public static class PresentClassRejectedBean {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            PresentClassBean.class,
            MissingClassBean.class,
            MissingClassAllowedBean.class,
            PresentClassRejectedBean.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void theClasspathDecidesWhichBeansAreDeployed() {
        assertThat(beanManager.getBeans(PresentClassBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(MissingClassBean.class)).isEmpty();
        assertThat(beanManager.getBeans(MissingClassAllowedBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(PresentClassRejectedBean.class)).isEmpty();
    }
}
