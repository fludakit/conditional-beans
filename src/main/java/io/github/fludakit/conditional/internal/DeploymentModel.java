package io.github.fludakit.conditional.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A model of the beans that could be part of the deployment.
 * <p>
 * The model is captured from the {@code ProcessAnnotatedType} events, which the container fires for every
 * discovered type before it fires the first {@code ProcessBeanAttributes}. Building it up front is what makes
 * {@code hasBean} independent of the order in which beans are declared.
 * </p>
 * <p>
 * The model is not thread-safe. It is populated and read on the container bootstrap thread only.
 * </p>
 */
final class DeploymentModel {

    private static final Set<Class<?>> BEAN_DEFINING_ANNOTATIONS = Set.of(
            ApplicationScoped.class,
            RequestScoped.class,
            SessionScoped.class,
            ConversationScoped.class,
            Singleton.class,
            Dependent.class);

    private final Map<String, CandidateBean> candidates = new LinkedHashMap<>();

    private final Map<Class<?>, List<Annotation>> stereotypesByClass = new HashMap<>();

    void capture(AnnotatedType<?> annotatedType) {
        if (annotatedType == null) {
            return;
        }
        Class<?> javaClass = annotatedType.getJavaClass();
        stereotypesByClass.putIfAbsent(javaClass, stereotypesOf(annotatedType.getAnnotations()));
        Set<Class<?>> typedRestriction = typedRestrictionOf(annotatedType.getAnnotations());

        // Producer methods and fields only produce a bean when their declaring class is a managed bean.
        boolean managedBean = isManagedBeanCandidate(annotatedType);
        if (managedBean) {
            String key = classKey(javaClass);
            candidates.put(key, new CandidateBean(
                    restrict(beanTypesOfClass(javaClass), typedRestriction),
                    Qualifiers.derive(annotatedType.getAnnotations())));
        }

        for (AnnotatedMethod<?> method : annotatedType.getMethods()) {
            if (managedBean && isProducer(method) && !isDisposer(method)) {
                String key = methodKey(method);
                candidates.put(key, new CandidateBean(
                        restrict(beanTypesOfProducedType(producedTypeOf(method)), typedRestriction),
                        Qualifiers.derive(method.getAnnotations())));
            }
        }

        for (AnnotatedField<?> field : annotatedType.getFields()) {
            if (managedBean && isProducer(field)) {
                String key = fieldKey(field);
                candidates.put(key, new CandidateBean(
                        restrict(beanTypesOfProducedType(producedTypeOf(field)), typedRestriction),
                        Qualifiers.derive(field.getAnnotations())));
            }
        }
    }

    /**
     * Replaces the computed descriptor of a bean with the authoritative attributes reported by the
     * container, so that the model agrees with the container on bean types and qualifiers.
     */
    void record(Annotated annotated, BeanAttributes<?> attributes) {
        String key = keyOf(annotated);
        if (key == null) {
            return;
        }
        Set<Type> beanTypes = new LinkedHashSet<>(attributes.getTypes());
        Set<Annotation> qualifiers = new LinkedHashSet<>(attributes.getQualifiers());
        candidates.put(key, new CandidateBean(beanTypes, qualifiers));
    }

    boolean hasBean(Type requiredType, Annotation[] requiredQualifiers) {
        for (CandidateBean candidate : candidates.values()) {
            if (candidate.matches(requiredType, requiredQualifiers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a bean of the given type, carrying qualifiers of the given types, is part of the deployment.
     */
    boolean hasQualifiedBean(Type requiredType, Collection<Class<? extends Annotation>> requiredQualifierTypes) {
        for (CandidateBean candidate : candidates.values()) {
            if (candidate.matchesType(requiredType) && candidate.matchesQualifierTypes(requiredQualifierTypes)) {
                return true;
            }
        }
        return false;
    }

    List<Annotation> stereotypesOf(Class<?> javaClass) {
        return stereotypesByClass.getOrDefault(javaClass, List.of());
    }

    private static boolean isManagedBeanCandidate(AnnotatedType<?> annotatedType) {
        for (Annotation annotation : annotatedType.getAnnotations()) {
            Class<?> annotationType = annotation.annotationType();
            if (BEAN_DEFINING_ANNOTATIONS.contains(annotationType)) {
                return true;
            }
            if (annotationType.isAnnotationPresent(Scope.class) || annotationType.isAnnotationPresent(NormalScope.class)) {
                return true;
            }
            if (annotationType.isAnnotationPresent(Stereotype.class)) {
                return true;
            }
        }
        return false;
    }

    private static List<Annotation> stereotypesOf(Set<Annotation> annotations) {
        List<Annotation> stereotypes = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Stereotype.class)) {
                stereotypes.add(annotation);
            }
        }
        return stereotypes;
    }

    private static boolean isProducer(Annotated annotated) {
        return annotated.isAnnotationPresent(Produces.class);
    }

    private static boolean isDisposer(Annotated annotated) {
        return annotated.isAnnotationPresent(Disposes.class);
    }

    private static Type producedTypeOf(AnnotatedMethod<?> method) {
        return method.getJavaMember().getGenericReturnType();
    }

    private static Type producedTypeOf(AnnotatedField<?> field) {
        return field.getJavaMember().getGenericType();
    }

    /**
     * The bean types of a managed bean are all its superclasses and all the interfaces it implements,
     * directly or indirectly.
     */
    private static Set<Type> beanTypesOfClass(Class<?> javaClass) {
        Set<Type> beanTypes = new LinkedHashSet<>();
        for (Class<?> current = javaClass; current != null; current = current.getSuperclass()) {
            beanTypes.add(current);
        }
        for (Class<?> current = javaClass; current != null; current = current.getSuperclass()) {
            collectInterfaces(current, beanTypes);
        }
        return beanTypes;
    }

    /**
     * The bean types of a producer are the produced type and every superclass and interface it implements,
     * directly or indirectly.
     */
    private static Set<Type> beanTypesOfProducedType(Type producedType) {
        Set<Type> beanTypes = new LinkedHashSet<>();
        beanTypes.add(producedType);
        Class<?> raw = rawTypeOf(producedType);
        if (raw != null) {
            for (Class<?> current = raw; current != null; current = current.getSuperclass()) {
                beanTypes.add(current);
            }
            collectInterfaces(raw, beanTypes);
        }
        return beanTypes;
    }

    /**
     * The types declared by {@code @Typed} on the bean class, which restrict the bean types of the bean and
     * of its producer methods and fields.
     */
    private static Set<Class<?>> typedRestrictionOf(Set<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Typed typed) {
                return new LinkedHashSet<>(Arrays.asList(typed.value()));
            }
        }
        return Set.of();
    }

    /**
     * Restricts the bean types to the ones declared by {@code @Typed}. {@code java.lang.Object} always
     * remains a bean type.
     */
    private static Set<Type> restrict(Set<Type> beanTypes, Set<Class<?>> typedRestriction) {
        if (typedRestriction.isEmpty()) {
            return beanTypes;
        }
        Set<Type> restricted = new LinkedHashSet<>();
        restricted.add(Object.class);
        for (Class<?> typed : typedRestriction) {
            for (Type beanType : beanTypes) {
                if (beanType.equals(typed)
                        || (beanType instanceof ParameterizedType parameterized && parameterized.getRawType().equals(typed))) {
                    restricted.add(beanType);
                }
            }
        }
        return restricted;
    }

    private static void collectInterfaces(Class<?> type, Set<Type> target) {
        for (Class<?> candidate : type.getInterfaces()) {
            if (target.add(candidate)) {
                collectInterfaces(candidate, target);
            }
        }
    }

    private static Class<?> rawTypeOf(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) {
            return raw;
        }
        return null;
    }

    private static String classKey(Class<?> javaClass) {
        return "class:" + javaClass.getName();
    }

    private static String methodKey(AnnotatedMethod<?> method) {
        Method javaMethod = method.getJavaMember();
        return "method:" + javaMethod.getDeclaringClass().getName() + "#" + javaMethod.getName()
                + Arrays.toString(javaMethod.getParameterTypes());
    }

    private static String fieldKey(AnnotatedField<?> field) {
        Field javaField = field.getJavaMember();
        return "field:" + javaField.getDeclaringClass().getName() + "#" + javaField.getName();
    }

    private static String keyOf(Annotated annotated) {
        if (annotated instanceof AnnotatedType<?> annotatedType) {
            return classKey(annotatedType.getJavaClass());
        }
        if (annotated instanceof AnnotatedMember<?> annotatedMember) {
            Object member = annotatedMember.getJavaMember();
            if (member instanceof Method method) {
                return "method:" + method.getDeclaringClass().getName() + "#" + method.getName()
                        + Arrays.toString(method.getParameterTypes());
            }
            if (member instanceof Field field) {
                return "field:" + field.getDeclaringClass().getName() + "#" + field.getName();
            }
        }
        return null;
    }
}