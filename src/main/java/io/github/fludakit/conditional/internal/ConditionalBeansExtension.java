package io.github.fludakit.conditional.internal;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Portable extension that enables a bean only when every condition declared on it matches the current
 * deployment.
 * <p>
 * The extension observes {@link ProcessBeanAttributes}, which is the only portable point at which a bean can
 * still be removed from the deployment. The set of beans that could be part of the deployment is captured
 * earlier, from the {@link ProcessAnnotatedType} events, so that a condition may ask about bean presence
 * without depending on the order in which beans are declared.
 * </p>
 * <p>
 * The extension is registered through {@code META-INF/services/jakarta.enterprise.inject.spi.Extension} and is
 * therefore active in every deployment that has this artifact on the classpath.
 * </p>
 *
 * @see Conditional
 */
public class ConditionalBeansExtension implements Extension {

    private final DeploymentModel model = new DeploymentModel();

    /**
     * Captures a discovered type, so that the model describes every bean of the deployment before the first
     * bean is deployed.
     *
     * @param event the process annotated type event
     * @param <T> the annotated type
     */
    <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event) {
        model.capture(event.getAnnotatedType());
    }

    /**
     * Evaluates the conditions declared on a bean and vetoes the bean when one of them does not match.
     *
     * @param event the process bean attributes event
     * @param beanManager the bean manager of the current deployment
     * @param <T> the bean type
     */
    <T> void processBeanAttributes(@Observes ProcessBeanAttributes<T> event, BeanManager beanManager) {
        model.record(event.getAnnotated(), event.getBeanAttributes());

        List<ConditionResolver.Entry> entries = ConditionResolver.resolve(event.getAnnotated(), model);
        if (entries.isEmpty()) {
            return;
        }

        ConditionContext context = new DefaultConditionContext(event.getAnnotated(), beanManager, model);
        for (ConditionResolver.Entry entry : entries) {
            Condition condition = instantiate(entry.conditionClass());
            if (!evaluate(condition, context, entry.declaration())) {
                event.veto();
                return;
            }
        }
    }

    private boolean evaluate(Condition condition, ConditionContext context, Annotation declaration) {
        try {
            if (condition instanceof AnnotationDrivenCondition driven) {
                return driven.test(context, declaration);
            }
            return condition.test(context);
        } catch (RuntimeException e) {
            throw new DefinitionException("Condition " + condition.getClass().getName()
                    + " failed while evaluating " + context.getAnnotated().getBaseType(), e);
        }
    }

    /**
     * Instantiates a condition. Conditions are never created by the container, because the container is not
     * yet able to inject anything while it is bootstrapping.
     */
    static Condition instantiate(Class<? extends Condition> conditionClass) {
        Constructor<? extends Condition> constructor;
        try {
            constructor = conditionClass.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
        } catch (NoSuchMethodException e) {
            throw new DefinitionException("Condition class must declare a no-argument constructor: "
                    + conditionClass.getName(), e);
        } catch (RuntimeException e) {
            throw new DefinitionException("Condition class could not be instantiated: " + conditionClass.getName(), e);
        }

        try {
            return constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw new DefinitionException("Condition constructor failed: " + conditionClass.getName(), e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new DefinitionException("Condition class could not be instantiated: " + conditionClass.getName(), e);
        }
    }
}