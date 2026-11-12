package io.github.fludakit.conditional.full;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Condition;

import io.github.fludakit.conditional.full.FullConditionalExtension;
import io.github.fludakit.conditional.full.support.Conditions;
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
 * A condition can detect whether an optional dependency is on the classpath.
 */
@ExtendWith(WeldJunit5Extension.class)
class HasClassConditionTest {

    /** Enabled, because the class is on the classpath. */
    @ApplicationScoped
    @Conditional(Conditions.HasPresentClass.class)
    public static class NeedsPresentClass {
    }

    /** Disabled, because the class is not on the classpath. */
    @ApplicationScoped
    @Conditional(Conditions.HasMissingClass.class)
    public static class NeedsMissingClass {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            NeedsPresentClass.class,
            NeedsMissingClass.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void classPresenceDecidesTheActivation() {
        assertThat(beanManager.getBeans(NeedsPresentClass.class)).hasSize(1);
        assertThat(beanManager.getBeans(NeedsMissingClass.class)).isEmpty();
    }
}