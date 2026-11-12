package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Conditionals;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.inject.Qualifier;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FullConditionalExtension implements Extension {

    private record BeanCandidate(
            Annotated source,
            Set<Type> types,
            Set<Class<? extends Annotation>> qualifiers
    ) {}

    private final Set<BeanCandidate> discoveredCandidates = ConcurrentHashMap.newKeySet();

    /**
     * Step 1: Record all non-vetoed bean classes and @Produces methods during ProcessAnnotatedType,
     * which completes completely BEFORE ProcessBeanAttributes begins.
     */
    public <T> void collectCandidates(@Observes ProcessAnnotatedType<T> pat) {
        AnnotatedType<T> annotatedType = pat.getAnnotatedType();
        if (annotatedType.isAnnotationPresent(Vetoed.class)) {
            return;
        }

        // Record the bean class itself
        discoveredCandidates.add(new BeanCandidate(
                annotatedType,
                annotatedType.getTypeClosure(),
                extractQualifiers(annotatedType.getAnnotations())
        ));

        // Record any @Produces methods declared on this class
        for (AnnotatedMethod<? super T> method : annotatedType.getMethods()) {
            if (method.isAnnotationPresent(Produces.class)) {
                discoveredCandidates.add(new BeanCandidate(
                        method,
                        method.getTypeClosure(),
                        extractQualifiers(method.getAnnotations())
                ));
            }
        }
    }

    /**
     * Step 2: Evaluate conditions in ProcessBeanAttributes against the pre-collected candidates.
     * Avoids calling BeanManager.getBeans(), which is illegal during bootstrap.
     */
    public <T> void evaluateRuntimeConditions(@Observes ProcessBeanAttributes<T> pba) {
        Annotated annotated = pba.getAnnotated();
        ConditionContext runtimeContext = createRuntimeContext(annotated);

        for (Annotation customAnn : unwrapConditionalAnnotations(annotated.getAnnotations())) {
            Class<? extends Annotation> customType = customAnn.annotationType();

            if (customType.isAnnotationPresent(Conditional.class)) {
                if (!evaluateConditional(customType.getAnnotation(Conditional.class), customAnn, runtimeContext)) {
                    removeCandidate(annotated);
                    pba.veto();
                    return;
                }
            }

            if (customType.isAnnotationPresent(Conditionals.class)) {
                for (Conditional conditional : customType.getAnnotation(Conditionals.class).value()) {
                    if (!evaluateConditional(conditional, customAnn, runtimeContext)) {
                        removeCandidate(annotated);
                        pba.veto();
                        return;
                    }
                }
            }
        }
    }

    private void removeCandidate(Annotated target) {
        discoveredCandidates.removeIf(c -> c.source().equals(target));
    }

    private Set<Class<? extends Annotation>> extractQualifiers(Set<Annotation> annotations) {
        Set<Class<? extends Annotation>> qualifiers = annotations.stream()
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(Qualifier.class))
                .filter(type -> !type.equals(jakarta.inject.Named.class))
                .collect(Collectors.toSet());

        boolean hasCustomQualifier = qualifiers.stream()
                .anyMatch(q -> !q.equals(Any.class));

        if (!hasCustomQualifier) {
            qualifiers.add(Default.class);
        }
        qualifiers.add(Any.class);
        return qualifiers;
    }

    private List<Annotation> unwrapConditionalAnnotations(Set<Annotation> annotations) {
        List<Annotation> result = new ArrayList<>();

        for (Annotation ann : annotations) {
            Class<? extends Annotation> type = ann.annotationType();

            if (type.isAnnotationPresent(Conditional.class) || type.isAnnotationPresent(Conditionals.class)) {
                result.add(ann);
                continue;
            }

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

    private ConditionContext createRuntimeContext(Annotated currentTarget) {
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
                Set<Class<? extends Annotation>> required = qualifiers.length == 0
                        ? Set.of(Default.class)
                        : new HashSet<>(Arrays.asList(qualifiers));

                return discoveredCandidates.stream()
                        // Exclude the current bean/producer being evaluated so it doesn't match itself
                        .filter(candidate -> !candidate.source().equals(currentTarget))
                        .anyMatch(candidate ->
                                matchesType(candidate.types(), beanType)
                                        && candidate.qualifiers().containsAll(required)
                        );
            }
        };
    }

    private boolean matchesType(Set<Type> candidateTypes, Class<?> targetType) {
        for (Type type : candidateTypes) {
            if (type instanceof Class<?> clazz && targetType.isAssignableFrom(clazz)) {
                return true;
            }
            if (type instanceof java.lang.reflect.ParameterizedType pt
                    && pt.getRawType() instanceof Class<?> rawClazz
                    && targetType.isAssignableFrom(rawClazz)) {
                return true;
            }
        }
        return false;
    }
}
