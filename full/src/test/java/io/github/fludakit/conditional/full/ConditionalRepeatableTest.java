package io.github.fludakit.conditional.full;
import io.github.fludakit.conditional.Conditionals;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Condition;

import io.github.fludakit.conditional.full.FullConditionalExtension;
import io.github.fludakit.conditional.full.support.Conditions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Bean;
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
 * {@link Conditional} is repeatable, so several conditions can be declared on one element, one annotation
 * each.
 * <p>
 * The repeated form and the array form mean the same thing, and they may be mixed. All of the declared
 * conditions have to match.
 * </p>
 */
@ExtendWith(WeldJunit5Extension.class)
class ConditionalRepeatableTest {

    /** A stereotype that declares two conditions, one of which never matches. */
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Conditional(Conditions.AlwaysTrue.class)
    @Conditional(Conditions.AlwaysFalse.class)
    public @interface NeverEnabled {
    }

    /** An annotation of the application that declares two conditions. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Conditional(Conditions.AlwaysTrue.class)
    @Conditional(Conditions.AlwaysFalse.class)
    public @interface Never {
    }

    /** Enabled, both conditions match. */
    @ApplicationScoped
    @Conditional(Conditions.AlwaysTrue.class)
    @Conditional(Conditions.AlwaysTrue.class)
    public static class BothMatch {
    }

    /** Disabled, the second condition does not match. */
    @ApplicationScoped
    @Conditional(Conditions.AlwaysTrue.class)
    @Conditional(Conditions.AlwaysFalse.class)
    public static class SecondFails {
    }

    /** Disabled, the first condition does not match. */
    @ApplicationScoped
    @Conditional(Conditions.AlwaysFalse.class)
    @Conditional(Conditions.AlwaysTrue.class)
    public static class FirstFails {
    }

    /** Enabled, the array form and the repeated form are mixed. */
    @ApplicationScoped
    @Conditional(Conditions.AlwaysTrue.class)
    @Conditional({ Conditions.AlwaysTrue.class })
    public static class MixedForms {
    }

    /** Disabled, the stereotype declares a condition that does not match. */
    @ApplicationScoped
    @NeverEnabled
    public static class Stereotyped {
    }

    /** Disabled, the composed annotation declares a condition that does not match. */
    @ApplicationScoped
    @Never
    public static class Composed {
    }

    /** Disabled, a composed annotation and a repeated condition are both applied. */
    @ApplicationScoped
    @Never
    @Conditional(Conditions.AlwaysTrue.class)
    public static class ComposedAndRepeated {
    }

    /** Enabled, a repeated condition on a producer method. */
    @Dependent
    public static class Producer {

        /** Produces a message. */
        @Produces
        @ApplicationScoped
        @Conditional(Conditions.AlwaysTrue.class)
        @Conditional(Conditions.AlwaysTrue.class)
        public String produce() {
            return "produced";
        }
    }

    /** Disabled, a repeated condition on a producer method, one of which fails. */
    @Dependent
    public static class DisabledProducer {

        /** Produces a message that is removed. */
        @Produces
        @ApplicationScoped
        @Conditional(Conditions.AlwaysTrue.class)
        @Conditional(Conditions.AlwaysFalse.class)
        public String produce() {
            return "produced";
        }
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            BothMatch.class,
            SecondFails.class,
            FirstFails.class,
            MixedForms.class,
            Stereotyped.class,
            Composed.class,
            ComposedAndRepeated.class,
            Producer.class,
            DisabledProducer.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void everyRepeatedConditionHasToMatch() {
        assertThat(beanManager.getBeans(BothMatch.class)).hasSize(1);
        assertThat(beanManager.getBeans(SecondFails.class)).isEmpty();
        assertThat(beanManager.getBeans(FirstFails.class)).isEmpty();
    }

    @Test
    void theRepeatedAndTheArrayFormAreEquivalent() {
        assertThat(beanManager.getBeans(MixedForms.class)).hasSize(1);
        assertThat(beanManager.getBeans(ComposedAndRepeated.class)).isEmpty();
    }

    @Test
    void aRepeatedConditionOnAStereotypeOrComposedAnnotationIsEvaluated() {
        assertThat(beanManager.getBeans(Stereotyped.class)).isEmpty();
        assertThat(beanManager.getBeans(Composed.class)).isEmpty();
    }

    @Test
    void aRepeatedConditionOnAProducerMethodIsEvaluated() {
        assertThat(beanManager.getBeans(String.class)).hasSize(1);
    }

    @Test
    void theContainerStaysInvisibleForTheContainer() {
        for (Bean<?> bean : beanManager.getBeans(BothMatch.class)) {
            assertThat(bean.getStereotypes()).contains(Conditional.class);
            assertThat(bean.getStereotypes()).doesNotContain(Conditionals.class);
        }
    }
}
