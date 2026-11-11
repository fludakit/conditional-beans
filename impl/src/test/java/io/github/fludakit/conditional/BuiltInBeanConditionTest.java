package io.github.fludakit.conditional;

import io.github.fludakit.conditional.annotation.ConditionalOnBean;
import io.github.fludakit.conditional.annotation.ConditionalOnMissingBean;
import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Fixtures;
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
 * The built-in bean conditions decide which beans are part of the deployment, and they can narrow the
 * requirement down to the qualifiers of the bean.
 */
@ExtendWith(WeldJunit5Extension.class)
class BuiltInBeanConditionTest {

    /** A bean that is always deployed. */
    @ApplicationScoped
    public static class Marker {
    }

    /** A qualified bean of a type of its own. */
    @ApplicationScoped
    @Fixtures.Blue
    public static class QualifiedMarker {
    }

    /** A type that is not a bean. */
    public static class NotABean {
    }

    /** Enabled, because a bean of that type exists. */
    @ApplicationScoped
    @ConditionalOnBean(Marker.class)
    public static class NeedsMarker {
    }

    /** Disabled, because no bean of that type exists. */
    @ApplicationScoped
    @ConditionalOnBean(NotABean.class)
    public static class NeedsMissingBean {
    }

    /** Enabled, because no bean of that type exists. */
    @ApplicationScoped
    @ConditionalOnMissingBean(NotABean.class)
    public static class WithoutMissingBean {
    }

    /** Disabled, because a bean of that type exists. */
    @ApplicationScoped
    @ConditionalOnMissingBean(Marker.class)
    public static class WithoutMarker {
    }

    /** Enabled, because a bean of that type carries the qualifier. */
    @ApplicationScoped
    @ConditionalOnBean(value = QualifiedMarker.class, qualifier = Fixtures.Blue.class)
    public static class NeedsQualifiedMarker {
    }

    /** Disabled, because the bean of that type carries the default qualifier. */
    @ApplicationScoped
    @ConditionalOnBean(value = Marker.class, qualifier = Fixtures.Blue.class)
    public static class NeedsBlueMarker {
    }

    /** Disabled, because a qualified bean of that type exists. */
    @ApplicationScoped
    @ConditionalOnMissingBean(value = QualifiedMarker.class, qualifier = Fixtures.Blue.class)
    public static class WithoutQualifiedMarker {
    }

    /** Enabled, because no qualified bean of that type exists. */
    @ApplicationScoped
    @ConditionalOnMissingBean(value = Marker.class, qualifier = Fixtures.Blue.class)
    public static class WithoutBlueMarker {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            Marker.class,
            QualifiedMarker.class,
            NeedsMarker.class,
            NeedsMissingBean.class,
            WithoutMissingBean.class,
            WithoutMarker.class,
            NeedsQualifiedMarker.class,
            NeedsBlueMarker.class,
            WithoutQualifiedMarker.class,
            WithoutBlueMarker.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void theBeanSetDecidesWhichBeansAreDeployed() {
        assertThat(beanManager.getBeans(NeedsMarker.class)).hasSize(1);
        assertThat(beanManager.getBeans(NeedsMissingBean.class)).isEmpty();
        assertThat(beanManager.getBeans(WithoutMissingBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(WithoutMarker.class)).isEmpty();
    }

    @Test
    void theQualifierNarrowsTheRequirement() {
        assertThat(beanManager.getBeans(NeedsQualifiedMarker.class)).hasSize(1);
        assertThat(beanManager.getBeans(NeedsBlueMarker.class)).isEmpty();
        assertThat(beanManager.getBeans(WithoutQualifiedMarker.class)).isEmpty();
        assertThat(beanManager.getBeans(WithoutBlueMarker.class)).hasSize(1);
    }
}
