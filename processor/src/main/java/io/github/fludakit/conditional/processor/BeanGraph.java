package io.github.fludakit.conditional.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The complete potential-bean-set model built from a single source round.
 * <p>
 * A type is a bean by its scope/stereotype annotations, a producer method by {@code @Produces} and a
 * producer field by {@code @Produces}. Bean types and qualifiers are derived the same way the CDI Full
 * {@code DeploymentModel} does, but over the language model of {@code javax.lang.model}. Because the round
 * is complete before emission, {@code hasBean} is answerable here - the capability the build extension
 * lacks.
 * </p>
 */
final class BeanGraph {

    private static final String DEFAULT = "jakarta.enterprise.inject.Default";
    private static final String ANY = "jakarta.enterprise.inject.Any";
    private static final String NAMED = "jakarta.inject.Named";

    private static final Set<String> BEAN_DEFINING_ANNOTATIONS = Set.of(
            "jakarta.enterprise.context.ApplicationScoped",
            "jakarta.enterprise.context.RequestScoped",
            "jakarta.enterprise.context.SessionScoped",
            "jakarta.enterprise.context.ConversationScoped",
            "jakarta.inject.Singleton",
            "jakarta.enterprise.context.Dependent");

    private record BeanDescriptor(Set<String> beanTypes, Set<String> qualifiers) {
    }

    private final Map<String, BeanDescriptor> beans = new LinkedHashMap<>();

    private final Elements elements;
    private final Types types;

    BeanGraph(Elements elements, Types types) {
        this.elements = elements;
        this.types = types;
    }

    /**
     * Captures a discovered type and, when its declaring class is a managed bean, its producers.
     */
    void capture(TypeElement type) {
        boolean managed = isManagedBean(type);
        if (managed) {
            beans.put("class:" + type.getQualifiedName(), new BeanDescriptor(
                    supertypesOf(type.asType()), qualifiersOf(type)));
        }
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                if (managed && isProducer(method) && !isDisposer(method)) {
                    beans.put(methodKey(method), new BeanDescriptor(
                            supertypesOf(method.getReturnType()), qualifiersOf(method)));
                }
            } else if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                if (managed && isProducer(field)) {
                    beans.put(fieldKey(field), new BeanDescriptor(
                            supertypesOf(field.asType()), qualifiersOf(field)));
                }
            }
        }
    }

    /**
     * Whether a bean of the given type carrying a qualifier of each given type exists.
     */
    boolean hasBean(String typeName, List<String> qualifierTypeNames) {
        for (BeanDescriptor bean : beans.values()) {
            if (!bean.beanTypes().contains(typeName)) {
                continue;
            }
            if (qualifierTypeNames.isEmpty()) {
                if (!bean.qualifiers().contains(DEFAULT)) {
                    continue;
                }
            } else if (!bean.qualifiers().containsAll(qualifierTypeNames)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isManagedBean(TypeElement type) {
        for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
            if (BEAN_DEFINING_ANNOTATIONS.contains(annotationType.getQualifiedName().toString())) {
                return true;
            }
            if (hasMetaAnnotation(annotationType, "jakarta.inject.Scope")
                    || hasMetaAnnotation(annotationType, "jakarta.enterprise.context.NormalScope")
                    || hasMetaAnnotation(annotationType, "jakarta.enterprise.inject.Stereotype")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMetaAnnotation(TypeElement annotationType, String metaAnnotationName) {
        for (AnnotationMirror meta : annotationType.getAnnotationMirrors()) {
            if (meta.getAnnotationType().toString().equals(metaAnnotationName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProducer(Element element) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().toString().equals("jakarta.enterprise.inject.Produces")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDisposer(Element element) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().toString().equals("jakarta.enterprise.inject.Disposes")) {
                return true;
            }
        }
        return false;
    }

    private Set<String> qualifiersOf(Element element) {
        Set<String> qualifiers = new LinkedHashSet<>();
        boolean exclusive = false;
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
            if (hasMetaAnnotation(annotationType, "jakarta.inject.Qualifier")) {
                String name = annotationType.getQualifiedName().toString();
                qualifiers.add(name);
                if (!name.equals(ANY) && !name.equals(NAMED) && !name.equals(DEFAULT)) {
                    exclusive = true;
                }
            }
        }
        if (!exclusive) {
            qualifiers.add(DEFAULT);
        }
        qualifiers.add(ANY);
        return qualifiers;
    }

    private Set<String> supertypesOf(TypeMirror type) {
        Set<String> names = new LinkedHashSet<>();
        collectSupertypes(type, names);
        return names;
    }

    private void collectSupertypes(TypeMirror type, Set<String> names) {
        if (type.getKind() != TypeKind.DECLARED) {
            return;
        }
        DeclaredType declared = (DeclaredType) type;
        TypeElement element = (TypeElement) declared.asElement();
        if (!names.add(element.getQualifiedName().toString())) {
            return;
        }
        TypeMirror superclass = element.getSuperclass();
        if (superclass.getKind() == TypeKind.DECLARED) {
            collectSupertypes(superclass, names);
        }
        for (TypeMirror iface : element.getInterfaces()) {
            collectSupertypes(iface, names);
        }
    }

    private static String methodKey(ExecutableElement method) {
        return "method:" + ((TypeElement) method.getEnclosingElement()).getQualifiedName()
                + "#" + method.getSimpleName()
                + Arrays.toString(method.getParameters().stream().map(p -> p.asType().toString()).toArray());
    }

    private static String fieldKey(VariableElement field) {
        return "field:" + ((TypeElement) field.getEnclosingElement()).getQualifiedName()
                + "#" + field.getSimpleName();
    }
}
