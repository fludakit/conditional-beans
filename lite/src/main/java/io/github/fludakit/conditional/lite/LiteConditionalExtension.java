package io.github.fludakit.conditional.lite;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Conditionals;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;
import jakarta.enterprise.lang.model.AnnotationTarget;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LiteConditionalExtension implements BuildCompatibleExtension {

    private final Set<String> discoveredClasses = ConcurrentHashMap.newKeySet();
    private final Set<String> discoveredProducers = ConcurrentHashMap.newKeySet();
    private final Set<BeanInfo> registeredBeans = ConcurrentHashMap.newKeySet();
    private final Set<AnnotationTarget> deferredConditionalTargets = ConcurrentHashMap.newKeySet();

    @Enhancement(types = Object.class, withSubtypes = true)
    public void enhanceClass(ClassConfig classConfig) {
        ClassInfo classInfo = classConfig.info();
        discoveredClasses.add(classInfo.name());

        List<AnnotationInfo> conditionalAnns = unwrapConditionalAnnotations(classInfo.annotations());
        if (conditionalAnns.isEmpty()) {
            return;
        }

        ConditionContext context = createBuildContext(classInfo);
        if (!evaluateAllAnnotations(conditionalAnns, context)) {
            discoveredClasses.remove(classInfo.name());
            classConfig.removeAllAnnotations();
            classConfig.addAnnotation(Vetoed.class);
            return;
        }

        deferredConditionalTargets.add(classInfo);
    }

    @Enhancement(types = Object.class, withSubtypes = true)
    public void enhanceProducerMethod(MethodConfig methodConfig) {
        MethodInfo methodInfo = methodConfig.info();
        if (!methodInfo.hasAnnotation(Produces.class)) {
            return;
        }

        String producerId = producerId(methodInfo.declaringClass().name(), methodInfo.name());
        discoveredProducers.add(producerId);

        List<AnnotationInfo> conditionalAnns = unwrapConditionalAnnotations(methodInfo.annotations());
        if (conditionalAnns.isEmpty()) {
            return;
        }

        ConditionContext context = createBuildContext(methodInfo);
        if (!evaluateAllAnnotations(conditionalAnns, context)) {
            discoveredProducers.remove(producerId);
            methodConfig.removeAnnotation(a -> a.declaration().name().equals(Produces.class.getName()));
            return;
        }

        deferredConditionalTargets.add(methodInfo);
    }

    @Enhancement(types = Object.class, withSubtypes = true)
    public void enhanceProducerField(FieldConfig fieldConfig) {
        FieldInfo fieldInfo = fieldConfig.info();
        if (!fieldInfo.hasAnnotation(Produces.class)) {
            return;
        }

        String producerId = producerId(fieldInfo.declaringClass().name(), fieldInfo.name());
        discoveredProducers.add(producerId);

        List<AnnotationInfo> conditionalAnns = unwrapConditionalAnnotations(fieldInfo.annotations());
        if (conditionalAnns.isEmpty()) {
            return;
        }

        ConditionContext context = createBuildContext(fieldInfo);
        if (!evaluateAllAnnotations(conditionalAnns, context)) {
            discoveredProducers.remove(producerId);
            fieldConfig.removeAnnotation(a -> a.declaration().name().equals(Produces.class.getName()));
            return;
        }

        deferredConditionalTargets.add(fieldInfo);
    }

    @Registration(types = Object.class)
    public void collectRegisteredBeans(BeanInfo beanInfo) {
        registeredBeans.add(beanInfo);
    }

    @Validation
    public void validateDeferredConditions(Messages messages) {
        for (AnnotationTarget target : deferredConditionalTargets) {
            List<AnnotationInfo> anns = unwrapConditionalAnnotations(target.annotations());
            if (anns.isEmpty()) {
                continue;
            }
            ConditionContext context = createBuildContext(target);
            if (!evaluateAllAnnotations(anns, context)) {
                messages.error(describe(target) + " failed its @Conditional check after full bean registration.");
            }
        }
    }

    private String producerId(String declaringClass, String memberName) {
        return declaringClass + "#" + memberName;
    }

    private String describe(AnnotationTarget target) {
        if (target.isDeclaration()) {
            if (target.asDeclaration().isClass()) {
                return "Bean class " + target.asDeclaration().asClass().name();
            }
            if (target.asDeclaration().isMethod()) {
                MethodInfo m = target.asDeclaration().asMethod();
                return "Producer method " + m.declaringClass().name() + "#" + m.name();
            }
            if (target.asDeclaration().isField()) {
                FieldInfo f = target.asDeclaration().asField();
                return "Producer field " + f.declaringClass().name() + "#" + f.name();
            }
        }
        return "Conditional element";
    }

    private List<AnnotationInfo> unwrapConditionalAnnotations(Collection<AnnotationInfo> annotations) {
        List<AnnotationInfo> result = new ArrayList<>();

        if (annotations == null) {
            return result;
        }

        for (AnnotationInfo annInfo : annotations) {
            try {
                ClassInfo decl = annInfo.declaration();
                if (decl == null) {
                    continue;
                }
                String name = decl.name();

                // Direct @Conditional / @Conditionals on the element
                if (Conditional.class.getName().equals(name) || Conditionals.class.getName().equals(name)) {
                    result.add(annInfo);
                    continue;
                }

                // Meta-annotation/stereotype carrying @Conditional or @Conditionals
                if (decl.hasAnnotation(Conditional.class) || decl.hasAnnotation(Conditionals.class)) {
                    result.add(annInfo);
                    continue;
                }

                // Repeatable container whose elements carry conditional annotations
                try {
                    AnnotationMember valueMember = annInfo.member("value");
                    if (valueMember != null) {
                        collectNestedConditionalAnnotations(valueMember, result);
                    }
                } catch (Throwable ignored) {
                    // Defensive: some BCE/ArC implementations may behave differently
                }
            } catch (Throwable ignored) {
                // Skip problematic annotations defensively
            }
        }
        return result;
    }

    private void collectNestedConditionalAnnotations(AnnotationMember member, List<AnnotationInfo> result) {
        AnnotationMember.Kind kind;
        try {
            kind = member.kind();
        } catch (Exception e) {
            return;
        }

        if (kind == AnnotationMember.Kind.ARRAY) {
            List<AnnotationMember> elements;
            try {
                elements = member.asArray();
            } catch (Exception e) {
                return;
            }
            for (AnnotationMember element : elements) {
                collectNestedConditionalAnnotations(element, result);
            }
            return;
        }

        if (kind == AnnotationMember.Kind.NESTED_ANNOTATION) {
            AnnotationInfo nested;
            try {
                nested = member.asNestedAnnotation();
            } catch (Exception e) {
                return;
            }
            ClassInfo nestedDecl = nested.declaration();
            String nestedName = nestedDecl.name();
            if (Conditional.class.getName().equals(nestedName)
                    || Conditionals.class.getName().equals(nestedName)
                    || nestedDecl.hasAnnotation(Conditional.class)
                    || nestedDecl.hasAnnotation(Conditionals.class)) {
                result.add(nested);
            }
        }
    }

    private boolean evaluateAllAnnotations(List<AnnotationInfo> conditionalAnns, ConditionContext context) {
        for (AnnotationInfo customAnn : conditionalAnns) {
            String name = customAnn.declaration().name();

            if (Conditional.class.getName().equals(name)) {
                if (!evaluateConditionalMeta(customAnn, customAnn, context)) {
                    return false;
                }
                continue;
            }

            if (Conditionals.class.getName().equals(name)) {
                AnnotationMember value = customAnn.member("value");
                if (value != null) {
                    for (AnnotationMember element : safeArray(value)) {
                        if (isNested(element)) {
                            AnnotationInfo meta = element.asNestedAnnotation();
                            if (!evaluateConditionalMeta(meta, customAnn, context)) {
                                return false;
                            }
                        }
                    }
                }
                continue;
            }

            ClassInfo customDecl = customAnn.declaration();

            if (customDecl.hasAnnotation(Conditional.class)) {
                AnnotationInfo meta = customDecl.annotation(Conditional.class);
                if (meta != null && !evaluateConditionalMeta(meta, customAnn, context)) {
                    return false;
                }
            }

            if (customDecl.hasAnnotation(Conditionals.class)) {
                AnnotationInfo metaContainer = customDecl.annotation(Conditionals.class);
                if (metaContainer != null) {
                    AnnotationMember value = metaContainer.member("value");
                    if (value != null) {
                        for (AnnotationMember element : safeArray(value)) {
                            if (isNested(element)) {
                                AnnotationInfo meta = element.asNestedAnnotation();
                                if (!evaluateConditionalMeta(meta, customAnn, context)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isNested(AnnotationMember member) {
        try {
            return member.kind() == AnnotationMember.Kind.NESTED_ANNOTATION;
        } catch (Exception e) {
            return false;
        }
    }

    private List<AnnotationMember> safeArray(AnnotationMember member) {
        try {
            if (member.kind() == AnnotationMember.Kind.ARRAY) {
                return member.asArray();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean evaluateConditionalMeta(
            AnnotationInfo conditionalMeta,
            AnnotationInfo targetAnnotation,
            ConditionContext context) {

        Annotation proxy = AnnotationProxyFactory.create(targetAnnotation);
        AnnotationMember valueMember = conditionalMeta.member("value");
        if (valueMember == null) {
            return true;
        }

        for (AnnotationMember conditionMember : safeArray(valueMember)) {
            try {
                String conditionClassName = conditionMember.asType().asClass().declaration().name();
                Class<?> clazz = Class.forName(conditionClassName, false, Thread.currentThread().getContextClassLoader());
                Condition condition = (Condition) clazz.getDeclaredConstructor().newInstance();
                if (!condition.matches(context, proxy)) {
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException("Build-time condition evaluation failed for conditional annotation", e);
            }
        }
        return true;
    }

    private ConditionContext createBuildContext(Object currentTarget) {
        return new ConditionContext() {
            @Override
            public Optional<String> getEnvironmentValue(String key) {
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
                String targetTypeName = beanType.getName();

                if (!registeredBeans.isEmpty()) {
                    return registeredBeans.stream()
                            .filter(bean -> !isSameTarget(bean, currentTarget))
                            .anyMatch(bean ->
                                    bean.types().stream().anyMatch(t -> matchesType(t, targetTypeName))
                                            && matchesQualifiers(bean.qualifiers(), qualifiers)
                            );
                }

                boolean classMatch = discoveredClasses.stream()
                        .filter(name -> !(currentTarget instanceof ClassInfo ci && ci.name().equals(name)))
                        .anyMatch(name -> targetTypeName.equals(name) || isKnownAssignable(name, targetTypeName));
                if (classMatch) {
                    // qualifier info for discovered class fallback is limited; best-effort
                    if (qualifiers.length == 0) {
                        return true;
                    }
                }

                return discoveredProducers.stream()
                        .anyMatch(id -> id.contains("#") && targetTypeName != null && false);
            }
        };
    }

    private boolean isKnownAssignable(String candidateName, String targetTypeName) {
        try {
            Class<?> candidate = Class.forName(candidateName, false, Thread.currentThread().getContextClassLoader());
            Class<?> target = Class.forName(targetTypeName, false, Thread.currentThread().getContextClassLoader());
            return target.isAssignableFrom(candidate);
        } catch (Throwable ignored) {
            return candidateName.equals(targetTypeName);
        }
    }

    private boolean isSameTarget(BeanInfo bean, Object currentTarget) {
        if (currentTarget instanceof ClassInfo classInfo && bean.declaringClass() != null) {
            return bean.declaringClass().name().equals(classInfo.name()) && !bean.isProducerMethod() && !bean.isProducerField();
        }
        if (currentTarget instanceof MethodInfo methodInfo && bean.isProducerMethod()) {
            return bean.declaringClass() != null
                    && bean.declaringClass().name().equals(methodInfo.declaringClass().name())
                    && bean.name() != null
                    && bean.name().equals(methodInfo.name());
        }
        if (currentTarget instanceof FieldInfo fieldInfo && bean.isProducerField()) {
            return bean.declaringClass() != null
                    && bean.declaringClass().name().equals(fieldInfo.declaringClass().name())
                    && bean.name() != null
                    && bean.name().equals(fieldInfo.name());
        }
        return false;
    }

    private boolean matchesType(Type type, String targetTypeName) {
        if (!type.isClass()) {
            return false;
        }
        return isSubtypeOf(type.asClass().declaration(), targetTypeName);
    }

    private boolean isSubtypeOf(ClassInfo classInfo, String targetTypeName) {
        if (classInfo == null) {
            return false;
        }
        if (classInfo.name().equals(targetTypeName)) {
            return true;
        }
        Type superClass = classInfo.superClass();
        if (superClass != null && superClass.isClass()
                && isSubtypeOf(superClass.asClass().declaration(), targetTypeName)) {
            return true;
        }
        for (Type superInterface : classInfo.superInterfaces()) {
            if (superInterface.isClass()
                    && isSubtypeOf(superInterface.asClass().declaration(), targetTypeName)) {
            return true;
            }
        }
        return false;
    }

    private boolean matchesQualifiers(
            Collection<AnnotationInfo> presentAnnotations,
            Class<? extends Annotation>[] requiredQualifiers) {

        List<AnnotationInfo> typeSafeQualifiers = presentAnnotations.stream()
                .filter(ann -> ann.declaration() != null && ann.declaration().hasAnnotation(Qualifier.class))
                .filter(ann -> !ann.name().equals(jakarta.inject.Named.class.getName()))
                .toList();

        if (requiredQualifiers.length == 0) {
            boolean hasCustomQualifier = typeSafeQualifiers.stream()
                    .anyMatch(q -> !q.name().equals(Any.class.getName()));
            return !hasCustomQualifier || typeSafeQualifiers.stream()
                    .anyMatch(q -> q.name().equals(Default.class.getName()));
        }

        for (Class<? extends Annotation> required : requiredQualifiers) {
            if (required.equals(Any.class)) {
                continue;
            }
            boolean matched = typeSafeQualifiers.stream()
                    .anyMatch(present -> present.name().equals(required.getName()));
            if (!matched) {
                return false;
            }
        }
        return true;
    }
}
