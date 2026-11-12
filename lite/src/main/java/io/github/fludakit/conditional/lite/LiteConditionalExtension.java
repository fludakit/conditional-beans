package io.github.fludakit.conditional.lite;

import io.github.fludakit.conditional.Condition;
import io.github.fludakit.conditional.ConditionContext;
import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.Conditionals;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.inject.Qualifier;

public class LiteConditionalExtension implements BuildCompatibleExtension {

    private final Set<ClassInfo> discoveredClasses = ConcurrentHashMap.newKeySet();
    private final Set<MethodInfo> discoveredProducers = ConcurrentHashMap.newKeySet();
    private final Set<BeanInfo> registeredBeans = ConcurrentHashMap.newKeySet();
    private final Set<ClassInfo> deferredConditionalClasses = ConcurrentHashMap.newKeySet();
    private final Set<MethodInfo> deferredConditionalProducers = ConcurrentHashMap.newKeySet();

    @Enhancement(types = Object.class, withSubtypes = true)
    public void evaluateClassConditions(ClassConfig classConfig) {
        ClassInfo classInfo = classConfig.info();
        discoveredClasses.add(classInfo);

        List<AnnotationInfo> conditionalAnns = unwrapConditionalAnnotations(classInfo.annotations());
        if (conditionalAnns.isEmpty()) {
            return;
        }

        deferredConditionalClasses.add(classInfo);
        ConditionContext context = createBuildContext(classConfig);

        if (!evaluateAllAnnotations(conditionalAnns, context)) {
            discoveredClasses.remove(classInfo);
            deferredConditionalClasses.remove(classInfo);
            classConfig.removeAllAnnotations();
            classConfig.addAnnotation(Vetoed.class);
        }
    }

    @Enhancement(types = Object.class, withSubtypes = true)
    public void evaluateProducerMethodConditions(MethodConfig methodConfig) {
        MethodInfo methodInfo = methodConfig.info();
        if (!methodInfo.hasAnnotation(Produces.class)) {
            return;
        }

        discoveredProducers.add(methodInfo);

        List<AnnotationInfo> conditionalAnns = unwrapConditionalAnnotations(methodInfo.annotations());
        if (conditionalAnns.isEmpty()) {
            return;
        }

        deferredConditionalProducers.add(methodInfo);
        ConditionContext context = createBuildContext(methodConfig);

        if (!evaluateAllAnnotations(conditionalAnns, context)) {
            discoveredProducers.remove(methodInfo);
            deferredConditionalProducers.remove(methodInfo);
            methodConfig.removeAnnotation(a -> a.declaration().name().equals(Produces.class.getName()));
        }
    }

    @Registration(types = Object.class)
    public void collectRegisteredBeans(BeanInfo beanInfo) {
        registeredBeans.add(beanInfo);
    }

    @Validation
    public void validateDeferredConditions(Messages messages) {
        for (ClassInfo classInfo : deferredConditionalClasses) {
            ConditionContext classContext = createBuildContext(classInfo);
            List<AnnotationInfo> anns = unwrapConditionalAnnotations(classInfo.annotations());
            if (!evaluateAllAnnotations(anns, classContext)) {
                messages.error("Bean class " + classInfo.name()
                        + " failed its @Conditional check after full bean registration.");
            }
        }

        for (MethodInfo methodInfo : deferredConditionalProducers) {
            ConditionContext methodContext = createBuildContext(methodInfo);
            List<AnnotationInfo> anns = unwrapConditionalAnnotations(methodInfo.annotations());
            if (!evaluateAllAnnotations(anns, methodContext)) {
                messages.error("Producer method " + methodInfo.declaringClass().name() + "#" + methodInfo.name()
                        + " failed its @Conditional check after full bean registration.");
            }
        }
    }

    private List<AnnotationInfo> unwrapConditionalAnnotations(Collection<AnnotationInfo> annotations) {
        List<AnnotationInfo> result = new ArrayList<>();

        for (AnnotationInfo annInfo : annotations) {
            ClassInfo decl = annInfo.declaration();

            if (decl.hasAnnotation(Conditional.class) || decl.hasAnnotation(Conditionals.class)) {
                result.add(annInfo);
                continue;
            }

            AnnotationMember valueMember = annInfo.member("value");
            if (valueMember != null && valueMember.kind() == AnnotationMember.Kind.ARRAY) {
                for (AnnotationMember element : valueMember.asArray()) {
                    if (element.kind() == AnnotationMember.Kind.NESTED_ANNOTATION) {
                        AnnotationInfo nested = element.asNestedAnnotation();
                        if (nested.declaration().hasAnnotation(Conditional.class)
                                || nested.declaration().hasAnnotation(Conditionals.class)) {
                            result.add(nested);
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean evaluateAllAnnotations(List<AnnotationInfo> conditionalAnns, ConditionContext context) {
        for (AnnotationInfo customAnn : conditionalAnns) {
            ClassInfo customDecl = customAnn.declaration();

            if (customDecl.hasAnnotation(Conditional.class)) {
                AnnotationInfo meta = customDecl.annotation(Conditional.class);
                if (!evaluateConditionalMeta(meta, customAnn, context)) {
                    return false;
                }
            }

            if (customDecl.hasAnnotation(Conditionals.class)) {
                AnnotationInfo metaContainer = customDecl.annotation(Conditionals.class);
                for (AnnotationMember element : metaContainer.member("value").asArray()) {
                    AnnotationInfo meta = element.asNestedAnnotation();
                    if (!evaluateConditionalMeta(meta, customAnn, context)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean evaluateConditionalMeta(
            AnnotationInfo conditionalMeta,
            AnnotationInfo targetAnnotation,
            ConditionContext context) {

        Annotation proxy = AnnotationProxyFactory.create(targetAnnotation);
        for (AnnotationMember conditionMember : conditionalMeta.member("value").asArray()) {
            String conditionClassName = conditionMember.asType().asClass().declaration().name();
            try {
                Class<?> clazz = Class.forName(conditionClassName);
                Condition condition = (Condition) clazz.getDeclaredConstructor().newInstance();
                if (!condition.matches(context, proxy)) {
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException("Build-time condition evaluation failed for " + conditionClassName, e);
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
                        .filter(clazz -> !clazz.equals(currentTarget))
                        .anyMatch(clazz ->
                                isSubtypeOf(clazz, targetTypeName)
                                        && matchesQualifiers(clazz.annotations(), qualifiers)
                        );
                if (classMatch) {
                    return true;
                }

                return discoveredProducers.stream()
                        .filter(method -> !method.equals(currentTarget))
                        .anyMatch(method ->
                                matchesType(method.returnType(), targetTypeName)
                                        && matchesQualifiers(method.annotations(), qualifiers)
                        );
            }
        };
    }

    private boolean isSameTarget(BeanInfo bean, Object currentTarget) {
        if (currentTarget instanceof ClassInfo classInfo && bean.declaringClass() != null) {
            return bean.declaringClass().name().equals(classInfo.name()) && !bean.isProducerMethod() && !bean.isProducerField();
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