package io.github.fludakit.conditional.full;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Condition;

import io.github.fludakit.conditional.full.FullConditionalExtension;
import io.github.fludakit.conditional.full.support.Conditions;
import io.github.fludakit.conditional.full.support.Fixtures;
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
 * A bean may declare several conditions, and all of them have to match.
 */
@ExtendWith(WeldJunit5Extension.class)
class MultipleConditionsTest {

    /** Enabled, because both conditions match. */
    @ApplicationScoped
    @Conditional({ Conditions.AlwaysTrue.class, Conditions.AlwaysTrue.class })
    public static class BothMatch {
    }

    /** Disabled, because the second condition does not match. */
    @ApplicationScoped
    @Conditional({ Conditions.AlwaysTrue.class, Conditions.AlwaysFalse.class })
    public static class OneFails {
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
            FullConditionalExtension.class,
            BothMatch.class,
            OneFails.class).build();

    @Inject
    BeanManager beanManager;

    @Test
    void allConditionsMustMatch() {
        assertThat(beanManager.getBeans(BothMatch.class)).hasSize(1);
        assertThat(beanManager.getBeans(OneFails.class)).isEmpty();
    }
}