package io.github.fludakit.conditional.internal;

import jakarta.enterprise.inject.Default;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;
import java.util.Set;

/**
 * A bean that could be part of the deployment, described by its bean types and qualifiers.
 * <p>
 * The descriptor answers the {@code hasBean} question. It is deliberately independent of whether the bean
 * carries a {@code @Conditional} annotation, so that conditions never hide one another.
 * </p>
 */
final class CandidateBean {

    private final Set<Type> beanTypes;
    private final Set<Annotation> qualifiers;

    CandidateBean(Set<Type> beanTypes, Set<Annotation> qualifiers) {
        this.beanTypes = beanTypes;
        this.qualifiers = qualifiers;
    }

    boolean matches(Type requiredType, Annotation[] requiredQualifiers) {
        return matchesType(requiredType) && matchesQualifiers(requiredQualifiers);
    }

    boolean matchesType(Type requiredType) {
        for (Type beanType : beanTypes) {
            if (isAssignable(requiredType, beanType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A bean matches the required qualifiers when it carries all of them. A lookup without qualifiers
     * implies the {@code @Default} qualifier, exactly like {@code BeanManager.getBeans(Type)} does.
     */
    private boolean matchesQualifiers(Annotation[] requiredQualifiers) {
        if (requiredQualifiers == null || requiredQualifiers.length == 0) {
            return hasQualifier(Default.Literal.INSTANCE);
        }
        for (Annotation required : requiredQualifiers) {
            if (!hasQualifier(required)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A bean matches the required qualifier types when it carries a qualifier of each of them. The member
     * values of the qualifiers are not compared. Without a required qualifier type, the
     * {@code @Default} qualifier is required, exactly like a lookup without qualifiers.
     */
    boolean matchesQualifierTypes(Collection<Class<? extends Annotation>> requiredQualifierTypes) {
        if (requiredQualifierTypes.isEmpty()) {
            return hasQualifier(Default.Literal.INSTANCE);
        }
        for (Class<? extends Annotation> requiredType : requiredQualifierTypes) {
            boolean found = false;
            for (Annotation candidate : qualifiers) {
                if (candidate.annotationType().equals(requiredType)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean hasQualifier(Annotation required) {
        for (Annotation candidate : qualifiers) {
            if (qualifierEquals(candidate, required)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Two qualifiers are equal when they have the same annotation type and the same member values.
     */
    private static boolean qualifierEquals(Annotation left, Annotation right) {
        if (!left.annotationType().equals(right.annotationType())) {
            return false;
        }
        for (Method member : right.annotationType().getDeclaredMethods()) {
            try {
                if (!Objects.equals(member.invoke(left), member.invoke(right))) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the required type can be satisfied by the given bean type, following the assignability rules
     * of the CDI specification in a simplified form.
     */
    private static boolean isAssignable(Type required, Type beanType) {
        if (required.equals(beanType)) {
            return true;
        }
        if (required instanceof Class<?> requiredClass) {
            if (beanType instanceof Class<?> beanClass) {
                return requiredClass.isAssignableFrom(beanClass);
            }
            if (beanType instanceof ParameterizedType parameterized) {
                Class<?> raw = rawType(parameterized);
                return raw != null && requiredClass.isAssignableFrom(raw);
            }
            return false;
        }
        if (required instanceof ParameterizedType requiredParameterized) {
            if (beanType instanceof ParameterizedType beanParameterized) {
                if (!Objects.equals(requiredParameterized.getRawType(), beanParameterized.getRawType())) {
                    return false;
                }
                return typeArgumentsAssignable(requiredParameterized.getActualTypeArguments(),
                        beanParameterized.getActualTypeArguments());
            }
            if (beanType instanceof Class<?> beanClass) {
                return Objects.equals(requiredParameterized.getRawType(), beanClass);
            }
        }
        return false;
    }

    private static boolean typeArgumentsAssignable(Type[] required, Type[] actual) {
        if (required.length != actual.length) {
            return false;
        }
        for (int i = 0; i < required.length; i++) {
            if (required[i] instanceof TypeVariable<?> || actual[i] instanceof TypeVariable<?>
                    || required[i] instanceof WildcardType || actual[i] instanceof WildcardType) {
                continue;
            }
            if (!required[i].equals(actual[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> rawType(ParameterizedType type) {
        return type.getRawType() instanceof Class<?> raw ? raw : null;
    }

}