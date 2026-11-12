package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.builtin.ConditionalOnMissingProperty;
import io.github.fludakit.conditional.builtin.ConditionalOnProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
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
 * The built-in property conditions decide which beans are part of the deployment.
 */
@ExtendWith(WeldJunit5Extension.class)
class BuiltInPropertyConditionTest {

    @BeforeAll
    static void defineProperties() {
        System.setProperty("app.country", "BR");
        System.setProperty("app.mode", "fast");
    }

    @AfterAll
    static void removeProperties() {
        System.clearProperty("app.country");
        System.clearProperty("app.mode");
    }

    /** Enabled, the entry is present and has the expected value. */
    @ApplicationScoped
    @ConditionalOnProperty(name = "app.country", havingValue = "BR")
    public static class BrazilTaxCalculator {
    }

    /** Disabled, the entry has a different value. */
    @ApplicationScoped
    @ConditionalOnProperty(name = "app.country", havingValue = "PT")
    public static class PortugalTaxCalculator {
    }

    /** Enabled, the entry is present and any value is accepted. */
    @ApplicationScoped
    @ConditionalOnProperty(name = "app.mode")
    public static class ModeBean {
    }

    /** Disabled, the entry is missing. */
    @ApplicationScoped
    @ConditionalOnProperty(name = "app.absent")
    public static class MissingEntryBean {
    }

    /** Enabled, the entry is not configured. */
    @ApplicationScoped
    @ConditionalOnMissingProperty(name = "app.absent")
    public static class AbsentEntryBean {
    }

    /** Disabled, the entry is configured. */
    @ApplicationScoped
    @ConditionalOnMissingProperty(name = "app.country")
    public static class ConfiguredEntryBean {
    }

    /** Holds a producer method that is gated by a property. */
    @ApplicationScoped
    public static class MessageProducer {

        /** Produces a message. */
        @Produces
        @ConditionalOnProperty(name = "app.country", havingValue = "BR")
        public String produce() {
            return "produced";
        }
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            BrazilTaxCalculator.class,
            PortugalTaxCalculator.class,
            ModeBean.class,
            MissingEntryBean.class,
            AbsentEntryBean.class,
            ConfiguredEntryBean.class,
            MessageProducer.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void thePropertyDecidesWhichBeansAreDeployed() {
        assertThat(beanManager.getBeans(BrazilTaxCalculator.class)).hasSize(1);
        assertThat(beanManager.getBeans(PortugalTaxCalculator.class)).isEmpty();
        assertThat(beanManager.getBeans(ModeBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(MissingEntryBean.class)).isEmpty();
        assertThat(beanManager.getBeans(AbsentEntryBean.class)).hasSize(1);
        assertThat(beanManager.getBeans(ConfiguredEntryBean.class)).isEmpty();
    }

    @Test
    void aProducerMethodIsGatedByItsProperty() {
        assertThat(beanManager.getBeans(String.class)).hasSize(1);
    }
}
