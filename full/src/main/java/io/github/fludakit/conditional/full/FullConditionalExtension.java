package io.github.fludakit.conditional.full;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Conditionals;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
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
    ) {
    }

    private final Set<BeanCandidate> discoveredCandidates = ConcurrentHashMap.newKeySet();

    /**
     * Step 1: Record all non-vetoed bean classes and @Produces methods/fields during ProcessAnnotatedType,
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
                beanTypes(annotatedType),
                extractQualifiers(annotatedType.getAnnotations())
        ));

        // Record any @Produces methods declared on this class
        for (AnnotatedMethod<? super T> method : annotatedType.getMethods()) {
            if (method.isAnnotationPresent(Produces.class)) {
                discoveredCandidates.add(new BeanCandidate(
                        method,
                        beanTypes(method),
                        extractQualifiers(method.getAnnotations())
                ));
            }
        }

        // Record any @Produces fields declared on this class
        for (AnnotatedField<? super T> field : annotatedType.getFields()) {
            if (field.isAnnotationPresent(Produces.class)) {
                discoveredCandidates.add(new BeanCandidate(
                        field,
                        beanTypes(field),
                        extractQualifiers(field.getAnnotations())
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

        for (ConditionalEntry entry : collectConditionalEntries(annotated.getAnnotations())) {
            if (!evaluateConditional(entry.conditional(), entry.targetAnnotation(), runtimeContext)) {
                pba.veto();
                return;
            }
        }
    }

    private record ConditionalEntry(Conditional conditional, Annotation targetAnnotation) {
    }

    private List<ConditionalEntry> collectConditionalEntries(Set<Annotation> annotations) {
        List<ConditionalEntry> result = new ArrayList<>();

        for (Annotation ann : annotations) {
            Class<? extends Annotation> type = ann.annotationType();

            // Direct @Conditional usage
            if (type.equals(Conditional.class)) {
                Conditional conditional = (Conditional) ann;
                result.add(new ConditionalEntry(conditional, ann));
                continue;
            }

            // Direct @Conditionals container usage
            if (type.equals(Conditionals.class)) {
                Conditionals conditionals = (Conditionals) ann;
                for (Conditional conditional : conditionals.value()) {
                    result.add(new ConditionalEntry(conditional, ann));
                }
                continue;
            }

            // Meta-annotation / stereotype carrying @Conditional
            if (type.isAnnotationPresent(Conditional.class)) {
                result.add(new ConditionalEntry(type.getAnnotation(Conditional.class), ann));
                continue;
            }

            // Meta-annotation / stereotype carrying @Conditionals
            if (type.isAnnotationPresent(Conditionals.class)) {
                for (Conditional conditional : type.getAnnotation(Conditionals.class).value()) {
                    result.add(new ConditionalEntry(conditional, ann));
                }
                continue;
            }

            // Repeatable container annotations (e.g. some custom repeatables) whose elements hold @Conditional
            try {
                Method valueMethod = type.getDeclaredMethod("value");
                valueMethod.setAccessible(true);
                Class<?> returnType = valueMethod.getReturnType();
                if (returnType.isArray() && Annotation.class.isAssignableFrom(returnType.getComponentType())) {
                    Class<?> component = returnType.getComponentType();
                    if (component.equals(Conditional.class)) {
                        Conditional[] values = (Conditional[]) valueMethod.invoke(ann);
                        for (Conditional conditional : values) {
                            result.add(new ConditionalEntry(conditional, ann));
                        }
                        continue;
                    }
                    if (component.equals(Conditionals.class)) {
                        Conditionals[] values = (Conditionals[]) valueMethod.invoke(ann);
                        for (Conditionals container : values) {
                            for (Conditional conditional : container.value()) {
                                result.add(new ConditionalEntry(conditional, ann));
                            }
                        }
                        continue;
                    }
                    if (component.isAnnotationPresent(Conditional.class)) {
                        Annotation[] values = (Annotation[]) valueMethod.invoke(ann);
                        for (Annotation nested : values) {
                            result.add(new ConditionalEntry(nested.annotationType().getAnnotation(Conditional.class), nested));
                        }
                        continue;
                    }
                    if (component.isAnnotationPresent(Conditionals.class)) {
                        Annotation[] values = (Annotation[]) valueMethod.invoke(ann);
                        for (Annotation nested : values) {
                            for (Conditional conditional : nested.annotationType().getAnnotation(Conditionals.class).value()) {
                                result.add(new ConditionalEntry(conditional, nested));
                            }
                        }
                        continue;
                    }
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new RuntimeException("Failed unwrapping repeatable annotation: " + type.getName(), e);
            }
        }

        return result;
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean evaluateConditional(Conditional conditional, Annotation targetAnnotation, ConditionContext context) {
        for (Class<? extends Condition> condClass : conditional.value()) {
            try {
                Condition condition = condClass.getDeclaredConstructor().newInstance();
                if (!condition.matches(context, targetAnnotation)) {
                    return false;
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Runtime condition evaluation failed for " + condClass.getName(), e);
            }
        }
        return true;
    }

    private ConditionContext createRuntimeContext(Annotated currentTarget) {
        return new ConditionContext() {
            @Override
            public Optional<String> getProperty(String key) {
                String val = System.getProperty(key);
                if (val == null) {
                    val = System.getenv(key);
                    if (val == null && key != null && key.contains(".")) {
                        val = System.getenv(key.replace('.', '_').toUpperCase());
                    }
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
                return discoveredCandidates.stream()
                        .filter(candidate -> !candidate.source().equals(currentTarget))
                        .anyMatch(candidate -> {
                            if (!matchesType(candidate.types(), beanType)) {
                                return false;
                            }
                            if (qualifiers.length == 0) {
                                return true;
                            }
                            Set<Class<? extends Annotation>> required = new HashSet<>(Arrays.asList(qualifiers));
                            return candidate.qualifiers().containsAll(required);
                        });
            }
        };
    }

    private Set<Type> beanTypes(Annotated annotated) {
        Typed typed = annotated.getAnnotation(Typed.class);
        if (typed != null) {
            Set<Type> types = new HashSet<>(Arrays.asList(typed.value()));
            types.add(Object.class);
            return types;
        }
        return new HashSet<>(annotated.getTypeClosure());
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
