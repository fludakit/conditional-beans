package io.github.fludakit.conditional;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.builtin.ConditionalOnMissingProperty;
import io.github.fludakit.conditional.internal.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Conditions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An annotation that declares a condition works the way a meta-annotation works in Spring: the condition is
 * found through the annotation that declares it, and it may be combined with a condition that is declared
 * directly on the bean.
 */
@ExtendWith(WeldJunit5Extension.class)
class ComposedAnnotationTest {

    /** A condition that never matches. */
    public static class NeverEnabled implements Condition {

        @Override
        public boolean test(ConditionContext context) {
            return false;
        }
    }

    /** An annotation of the application that declares a condition. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Conditional(NeverEnabled.class)
    public @interface Never {
    }

    /** A stereotype that declares a condition. */
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Conditional(NeverEnabled.class)
    public @interface NeverStereotype {
    }

    /** A stereotype that declares a built-in condition. */
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @ConditionalOnMissingProperty(name = "app.legacy")
    public @interface WithoutLegacyProperty {
    }

    /** Disabled, the composed annotation declares a condition that does not match. */
    @ApplicationScoped
    @Never
    public static class ComposedBean {
    }

    /** Disabled, the stereotype declares a condition that does not match. */
    @ApplicationScoped
    @NeverStereotype
    public static class StereotypedBean {
    }

    /** Enabled, the stereotype declares a built-in condition that matches. */
    @ApplicationScoped
    @WithoutLegacyProperty
    public static class BuiltInStereotypedBean {
    }

    /** Disabled, the composed annotation and the direct condition are both applied. */
    @ApplicationScoped
    @Never
    @Conditional(Conditions.AlwaysTrue.class)
    public static class BothBean {
    }

    /** Enabled, the direct condition matches. */
    @ApplicationScoped
    @Conditional(Conditions.AlwaysTrue.class)
    public static class DirectBean {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            ComposedBean.class,
            StereotypedBean.class,
            BuiltInStereotypedBean.class,
            BothBean.class,
            DirectBean.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void aComposedAnnotationDeclaresItsCondition() {
        assertThat(beanManager.getBeans(ComposedBean.class)).isEmpty();
        assertThat(beanManager.getBeans(StereotypedBean.class)).isEmpty();
        assertThat(beanManager.getBeans(BuiltInStereotypedBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(BothBean.class)).isEmpty();
        assertThat(beanManager.getBeans(DirectBean.class)).hasSize(1);
    }
}
