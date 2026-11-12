package io.github.fludakit.conditional.full;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Condition;

import io.github.fludakit.conditional.full.FullConditionalExtension;
import io.github.fludakit.conditional.full.support.Conditions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A failing condition aborts the deployment instead of silently disabling the bean.
 */
class ConditionFailureTest {

    /** A bean whose condition always throws. */
    @ApplicationScoped
    @Conditional(Conditions.Throwing.class)
    public static class ThrowingBean {
    }

    /** A bean whose condition cannot be instantiated. */
    @ApplicationScoped
    @Conditional(Conditions.WithoutDefaultConstructor.class)
    public static class InvalidConstructorBean {
    }

    @Test
    void throwingConditionFailsTheDeployment() {
        Weld weld = new Weld().disableDiscovery()
                .addExtensions(FullConditionalExtension.class)
                .addBeanClass(ThrowingBean.class);

        assertThatThrownBy(weld::initialize)
                .isInstanceOf(DefinitionException.class)
                .hasMessageContaining("Throwing");
    }

    @Test
    void conditionWithoutNoArgumentConstructorFailsTheDeployment() {
        Weld weld = new Weld().disableDiscovery()
                .addExtensions(FullConditionalExtension.class)
                .addBeanClass(InvalidConstructorBean.class);

        assertThatThrownBy(weld::initialize)
                .isInstanceOf(DefinitionException.class)
                .hasMessageContaining("WithoutDefaultConstructor");
    }
}