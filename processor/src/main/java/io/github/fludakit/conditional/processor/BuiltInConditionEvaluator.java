package io.github.fludakit.conditional.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Statically evaluates the six built-in conditional annotations against the {@link BeanGraph} and the
 * compile environment. A custom user {@code Condition} class is never run here - arbitrary application code
 * cannot execute inside an annotation processor.
 */
final class BuiltInConditionEvaluator {

    private static final Set<String> BUILT_INS = Set.of(
            "io.github.fludakit.conditional.annotation.ConditionalOnProperty",
            "io.github.fludakit.conditional.annotation.ConditionalOnMissingProperty",
            "io.github.fludakit.conditional.annotation.ConditionalOnClass",
            "io.github.fludakit.conditional.annotation.ConditionalOnMissingClass",
            "io.github.fludakit.conditional.annotation.ConditionalOnBean",
            "io.github.fludakit.conditional.annotation.ConditionalOnMissingBean");

    private final Elements elements;
    private final BeanGraph graph;

    BuiltInConditionEvaluator(Elements elements, BeanGraph graph) {
        this.elements = elements;
        this.graph = graph;
    }

    /**
     * Evaluates every discovered type and producer, returning the keys of the beans whose conditions do not
     * match, as {@code class:FQCN}, {@code method:FQCN#name} and {@code field:FQCN#name}.
     */
    List<String> evaluate(Iterable<? extends Element> roots) {
        List<String> disabled = new ArrayList<>();
        for (Element root : roots) {
            if (root instanceof TypeElement type) {
                evaluateType(type, disabled);
            }
        }
        return disabled;
    }

    private void evaluateType(TypeElement type, List<String> disabled) {
        if (!matches(type)) {
            disabled.add("class:" + type.getQualifiedName());
        }
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD && isProducer(enclosed)) {
                if (!matches(enclosed)) {
                    disabled.add("method:" + type.getQualifiedName() + "#" + enclosed.getSimpleName());
                }
            } else if (enclosed.getKind() == ElementKind.FIELD && isProducer(enclosed)) {
                if (!matches(enclosed)) {
                    disabled.add("field:" + type.getQualifiedName() + "#" + enclosed.getSimpleName());
                }
            }
        }
    }

    private boolean matches(Element element) {
        for (AnnotationMirror annotation : builtInAnnotations(element)) {
            if (!evaluate(annotation)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluate(AnnotationMirror annotation) {
        return switch (annotation.getAnnotationType().toString()) {
            case "io.github.fludakit.conditional.annotation.ConditionalOnProperty" ->
                    evaluateOnProperty(annotation);
            case "io.github.fludakit.conditional.annotation.ConditionalOnMissingProperty" ->
                    evaluateOnMissingProperty(annotation);
            case "io.github.fludakit.conditional.annotation.ConditionalOnClass" ->
                    evaluateOnClass(annotation);
            case "io.github.fludakit.conditional.annotation.ConditionalOnMissingClass" ->
                    evaluateOnMissingClass(annotation);
            case "io.github.fludakit.conditional.annotation.ConditionalOnBean" ->
                    evaluateOnBean(annotation);
            case "io.github.fludakit.conditional.annotation.ConditionalOnMissingBean" ->
                    evaluateOnMissingBean(annotation);
            default -> true;
        };
    }

    private boolean evaluateOnProperty(AnnotationMirror annotation) {
        List<String> names = strings(annotation, "name");
        List<String> values = strings(annotation, "value");
        boolean matchIfMissing = bool(annotation, "matchIfMissing");
        for (String name : names) {
            Optional<String> value = property(name);
            if (value.isEmpty()) {
                if (!matchIfMissing) {
                    return false;
                }
                continue;
            }
            if (!values.isEmpty() && !values.contains(value.get())) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateOnMissingProperty(AnnotationMirror annotation) {
        for (String name : strings(annotation, "name")) {
            if (property(name).isPresent()) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateOnClass(AnnotationMirror annotation) {
        for (String name : strings(annotation, "name")) {
            if (elements.getTypeElement(name) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateOnMissingClass(AnnotationMirror annotation) {
        for (String name : strings(annotation, "name")) {
            if (elements.getTypeElement(name) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateOnBean(AnnotationMirror annotation) {
        List<String> qualifiers = classes(annotation, "qualifier");
        for (String type : classes(annotation, "value")) {
            if (!graph.hasBean(type, qualifiers)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateOnMissingBean(AnnotationMirror annotation) {
        List<String> qualifiers = classes(annotation, "qualifier");
        for (String type : classes(annotation, "value")) {
            if (graph.hasBean(type, qualifiers)) {
                return false;
            }
        }
        return true;
    }

    private List<AnnotationMirror> builtInAnnotations(Element element) {
        List<AnnotationMirror> result = new ArrayList<>();
        collect(element.getAnnotationMirrors(), result, 0);
        return result;
    }

    private void collect(List<? extends AnnotationMirror> annotations, List<AnnotationMirror> result, int depth) {
        if (depth > 3) {
            return;
        }
        for (AnnotationMirror annotation : annotations) {
            if (BUILT_INS.contains(annotation.getAnnotationType().toString())) {
                result.add(annotation);
            }
            TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
            if (hasMetaAnnotation(annotationType, "jakarta.enterprise.inject.Stereotype")) {
                collect(annotationType.getAnnotationMirrors(), result, depth + 1);
            }
        }
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

    private Optional<String> property(String name) {
        String value = System.getProperty(name);
        if (value != null) {
            return Optional.of(value);
        }
        value = System.getenv(name);
        if (value != null) {
            return Optional.of(value);
        }
        String relaxed = name.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        value = System.getenv(relaxed);
        return Optional.ofNullable(value);
    }

    private List<String> strings(AnnotationMirror mirror, String member) {
        AnnotationValue value = memberValue(mirror, member);
        if (value == null || !(value.getValue() instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            result.add(String.valueOf(((AnnotationValue) item).getValue()));
        }
        return result;
    }

    private boolean bool(AnnotationMirror mirror, String member) {
        AnnotationValue value = memberValue(mirror, member);
        return value != null && Boolean.TRUE.equals(value.getValue());
    }

    private List<String> classes(AnnotationMirror mirror, String member) {
        AnnotationValue value = memberValue(mirror, member);
        if (value == null || !(value.getValue() instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            Object raw = ((AnnotationValue) item).getValue();
            result.add(raw instanceof TypeMirror type ? type.toString() : String.valueOf(raw));
        }
        return result;
    }

    private AnnotationValue memberValue(AnnotationMirror mirror, String member) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(member)) {
                return entry.getValue();
            }
        }
        TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
        for (Element enclosed : annotationType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD && enclosed.getSimpleName().contentEquals(member)) {
                return ((ExecutableElement) enclosed).getDefaultValue();
            }
        }
        return null;
    }
}
