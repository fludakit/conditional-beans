package io.github.fludakit.conditional.impl.build;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * The CDI Lite build extension for conditional beans.
 * <p>
 * It consumes the resource written by the {@code processor} module,
 * {@code META-INF/fluda-conditional-beans/conditions}, which lists the beans whose built-in conditions did
 * not match at compile time - in particular the bean-existence conditions, which a build extension cannot
 * answer itself because {@code @Enhancement} runs before the complete bean set exists.
 * </p>
 * <p>
 * A disabled class loses its bean-defining annotations (scopes and stereotypes, including
 * {@code @Conditional} itself), which is what turns it back into a non-bean in
 * {@code bean-discovery-mode=annotated}. A disabled producer method or field loses {@code @Produces}.
 * </p>
 */
public class ConditionalBuildExtension implements BuildCompatibleExtension {

    private static final String CONDITIONS_RESOURCE = "META-INF/fluda-conditional-beans/conditions";
    private static final String PRODUCES = "jakarta.enterprise.inject.Produces";

    private volatile DisabledBeans disabledBeans;

    /**
     * Disables every class and producer whose built-in conditions did not match at compile time.
     *
     * @param config the class configuration
     */
    @Enhancement(types = Object.class, withSubtypes = true)
    public void disableConditionalBeans(ClassConfig config) {
        DisabledBeans disabled = disabledBeans();
        ClassInfo info = config.info();

        if (disabled.classes().contains(info.name())) {
            for (AnnotationInfo annotation : info.annotations()) {
                if (isBeanDefining(annotation)) {
                    config.removeAnnotation(candidate -> candidate.name().equals(annotation.name()));
                }
            }
        }

        for (MethodConfig method : config.methods()) {
            MethodInfo methodInfo = method.info();
            if (disabled.methods().contains(key(methodInfo.declaringClass().name(), methodInfo.name()))) {
                method.removeAnnotation(annotation -> annotation.name().equals(PRODUCES));
            }
        }
        for (FieldConfig field : config.fields()) {
            FieldInfo fieldInfo = field.info();
            if (disabled.fields().contains(key(fieldInfo.declaringClass().name(), fieldInfo.name()))) {
                field.removeAnnotation(annotation -> annotation.name().equals(PRODUCES));
            }
        }
    }

    private DisabledBeans disabledBeans() {
        DisabledBeans result = this.disabledBeans;
        if (result == null) {
            result = load();
            this.disabledBeans = result;
        }
        return result;
    }

    private static DisabledBeans load() {
        Set<String> classes = new HashSet<>();
        Set<String> methods = new HashSet<>();
        Set<String> fields = new HashSet<>();
        InputStream in = ConditionalBuildExtension.class.getClassLoader().getResourceAsStream(CONDITIONS_RESOURCE);
        if (in == null) {
            return new DisabledBeans(classes, methods, fields);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("class:")) {
                    classes.add(line.substring("class:".length()));
                } else if (line.startsWith("method:")) {
                    methods.add(line.substring("method:".length()));
                } else if (line.startsWith("field:")) {
                    fields.add(line.substring("field:".length()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new DisabledBeans(classes, methods, fields);
    }

    /**
     * Whether an annotation makes a class a bean: a scope type or a stereotype.
     */
    private static boolean isBeanDefining(AnnotationInfo annotation) {
        for (AnnotationInfo meta : annotation.declaration().annotations()) {
            String name = meta.name();
            if (name.equals("jakarta.inject.Scope")
                    || name.equals("jakarta.enterprise.context.NormalScope")
                    || name.equals("jakarta.enterprise.inject.Stereotype")) {
                return true;
            }
        }
        return false;
    }

    private static String key(String declaringClass, String member) {
        return declaringClass + "#" + member;
    }

    private record DisabledBeans(Set<String> classes, Set<String> methods, Set<String> fields) {
    }
}
