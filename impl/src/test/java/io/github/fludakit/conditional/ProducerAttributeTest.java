package io.github.fludakit.conditional;

import io.github.fludakit.conditional.impl.ConditionalBeansExtension;
import io.github.fludakit.conditional.support.Conditions;
import io.github.fludakit.conditional.support.Fixtures;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A condition on a producer removes the produced bean, but it does not change what the producer produces.
 * <p>
 * The scope and the qualifiers of the produced bean come from the producer method or the producer field,
 * exactly as they do without a condition. A qualifier declared on the class that holds the producer is not
 * inherited by the produced bean, because a producer is a bean of its own.
 * </p>
 */
class ProducerAttributeTest {

    /** A qualified producer method. */
    @Dependent
    public static class QualifiedProducer {

        /** Produces a qualified message. */
        @Produces
        @ApplicationScoped
        @Fixtures.Blue
        @Conditional(Conditions.AlwaysTrue.class)
        public Fixtures.Message produce() {
            return new Fixtures.Message("qualified");
        }
    }

    /** A qualified class whose producer method is not qualified. */
    @ApplicationScoped
    @Fixtures.Blue
    public static class QualifiedHolder {

        /** Produces an unqualified message. */
        @Produces
        @ApplicationScoped
        @Conditional(Conditions.AlwaysTrue.class)
        public Fixtures.Message produce() {
            return new Fixtures.Message("unqualified");
        }
    }

    /** A producer method that declares no scope of its own. */
    @ApplicationScoped
    public static class UnscopedProducer {

        /** Produces a dependent message. */
        @Produces
        @Conditional(Conditions.AlwaysTrue.class)
        public Fixtures.Message produce() {
            return new Fixtures.Message("dependent");
        }
    }

    @Test
    void aQualifiedProducerProducesAQualifiedBean() {
        WeldContainer container = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClass(QualifiedProducer.class)
                .initialize();
        try {
            BeanManager beanManager = container.getBeanManager();
            assertThat(beanManager.getBeans(Fixtures.Message.class)).isEmpty();
            assertThat(beanManager.getBeans(Fixtures.Message.class, Fixtures.Blue.Literal.INSTANCE)).hasSize(1);
        } finally {
            container.shutdown();
        }
    }

    @Test
    void theQualifierOfTheDeclaringClassIsNotInheritedByTheProducedBean() {
        WeldContainer container = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClass(QualifiedHolder.class)
                .initialize();
        try {
            BeanManager beanManager = container.getBeanManager();
            assertThat(beanManager.getBeans(Fixtures.Message.class)).hasSize(1);
            assertThat(beanManager.getBeans(Fixtures.Message.class, Fixtures.Blue.Literal.INSTANCE)).isEmpty();
        } finally {
            container.shutdown();
        }
    }

    @Test
    void theScopeOfTheProducerIsKept() {
        WeldContainer container = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClass(Fixtures.EnabledMethodProducer.class)
                .initialize();
        try {
            BeanManager beanManager = container.getBeanManager();
            assertThat(beanManager.getBeans(Fixtures.Message.class))
                    .hasSize(1)
                    .allSatisfy(bean -> assertThat(bean.getScope()).isEqualTo(ApplicationScoped.class));
        } finally {
            container.shutdown();
        }
    }

    @Test
    void aProducerWithoutAScopeProducesADependentBean() {
        WeldContainer container = new Weld().disableDiscovery()
                .addExtensions(ConditionalBeansExtension.class)
                .addBeanClass(UnscopedProducer.class)
                .initialize();
        try {
            BeanManager beanManager = container.getBeanManager();
            assertThat(beanManager.getBeans(Fixtures.Message.class))
                    .allSatisfy(bean -> assertThat(bean.getScope()).isEqualTo(Dependent.class));
        } finally {
            container.shutdown();
        }
    }
}
