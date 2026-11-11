package io.github.fludakit.conditional;

import io.github.fludakit.conditional.annotation.ConditionalOnClass;
import io.github.fludakit.conditional.annotation.ConditionalOnMissingClass;
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
    @ConditionalOnMissingClass(name = "com.github.fludakit.NotOnTheClasspath")
    public static class MissingClassAllowedBean {
    }

    /** Disabled, the class is on the classpath. */
    @ApplicationScoped
    @ConditionalOnMissingClass(name = "java.lang.String")
    public static class PresentClassRejectedBean {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
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
