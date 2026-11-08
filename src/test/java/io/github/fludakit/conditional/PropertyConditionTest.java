package io.github.fludakit.conditional;

import io.github.fludakit.conditional.internal.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Fixtures;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A condition can be driven by a configuration entry.
 */
@ExtendWith(WeldJunit5Extension.class)
class PropertyConditionTest {

    @BeforeAll
    static void defineCountry() {
        System.setProperty("app.country", "BR");
    }

    @AfterAll
    static void removeCountry() {
        System.clearProperty("app.country");
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            ConditionalBeansExtension.class,
            Fixtures.BrazilTaxCalculator.class,
            Fixtures.PortugalTaxCalculator.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void onlyTheMatchingBeanIsEnabled() {
        assertThat(beanManager.getBeans(Fixtures.BrazilTaxCalculator.class)).hasSize(1);
        assertThat(beanManager.getBeans(Fixtures.PortugalTaxCalculator.class)).isEmpty();
    }
}