package io.github.fludakit.conditional.full;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Conditionals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FullConditionalExtension implements Extension {

    /**
     * Handles managed beans, @Produces methods, and @Produces fields uniformly.
     */
    public <T> void evaluateRuntimeConditions(@Observes ProcessBeanAttributes<T> pba, BeanManager bm) {
        ConditionContext runtimeContext = createRuntimeContext(bm);

        for (Annotation customAnn : unwrapConditionalAnnotations(pba.getAnnotated().getAnnotations())) {
            Class<? extends Annotation> customType = customAnn.annotationType();

            if (customType.isAnnotationPresent(Conditional.class)) {
                if (!evaluateConditional(customType.getAnnotation(Conditional.class), customAnn, runtimeContext)) {
                    pba.veto();
                    return;
                }
            }

            if (customType.isAnnotationPresent(Conditionals.class)) {
                for (Conditional conditional : customType.getAnnotation(Conditionals.class).value()) {
                    if (!evaluateConditional(conditional, customAnn, runtimeContext)) {
                        pba.veto();
                        return;
                    }
                }
            }
        }
    }

    private List<Annotation> unwrapConditionalAnnotations(Set<Annotation> annotations) {
        List<Annotation> result = new ArrayList<>();

        for (Annotation ann : annotations) {
            Class<? extends Annotation> type = ann.annotationType();

            if (type.isAnnotationPresent(Conditional.class) || type.isAnnotationPresent(Conditionals.class)) {
                result.add(ann);
                continue;
            }

            // Check for @Repeatable container annotations holding @Conditional elements
            try {
                Method valueMethod = type.getDeclaredMethod("value");
                Class<?> returnType = valueMethod.getReturnType();
                if (returnType.isArray() && Annotation.class.isAssignableFrom(returnType.getComponentType())) {
                    Class<?> component = returnType.getComponentType();
                    if (component.isAnnotationPresent(Conditional.class)
                            || component.isAnnotationPresent(Conditionals.class)) {
                        Collections.addAll(result, (Annotation[]) valueMethod.invoke(ann));
                    }
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new RuntimeException("Failed unwrapping repeatable annotation: " + type.getName(), e);
            }
        }
        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean evaluateConditional(Conditional conditional, Annotation targetAnnotation, ConditionContext context) {
        for (Class<? extends Condition> condClass : conditional.value()) {
            try {
                Condition condition = condClass.getDeclaredConstructor().newInstance();
                if (!condition.matches(context, targetAnnotation)) {
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException("Runtime condition evaluation failed for " + condClass.getName(), e);
            }
        }
        return true;
    }

    private ConditionContext createRuntimeContext(BeanManager bm) {
        return new ConditionContext() {
            @Override
            public Optional<String> getEnvironmentValue(String key) {
                String val = System.getProperty(key);
                if (val == null) {
                    val = System.getenv(key);
                }
                return Optional.ofNullable(val);
            }

            @Override
            public boolean hasClass(String... classNames) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                for (String className : classNames) {
                    try {
                        Class.forName(className, false, loader);
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            @SafeVarargs
            public final boolean hasBean(Class<?> beanType, Class<? extends Annotation>... qualifiers) {
                Set<jakarta.enterprise.inject.spi.Bean<?>> candidates = bm.getBeans(beanType, jakarta.enterprise.inject.Any.Literal.INSTANCE);
                if (candidates.isEmpty()) {
                    return false;
                }

                if (qualifiers.length == 0) {
                    return candidates.stream()
                            .anyMatch(b -> b.getQualifiers().contains(jakarta.enterprise.inject.Default.Literal.INSTANCE));
                }

                for (jakarta.enterprise.inject.spi.Bean<?> bean : candidates) {
                    Set<Class<? extends Annotation>> presentTypes = bean.getQualifiers().stream()
                            .map(Annotation::annotationType)
                            .collect(Collectors.toSet());
                    if (presentTypes.containsAll(java.util.Arrays.asList(qualifiers))) {
                        return true;
                    }
                }
                return false;
            }

        };
    }
}
