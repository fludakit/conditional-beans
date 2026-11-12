package io.github.fludakit.conditional.lite;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;

public final class AnnotationProxyFactory {

    private AnnotationProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A create(AnnotationInfo info) {
        try {
            Class<?> annotationClass = Class.forName(info.declaration().name());
            Map<String, AnnotationMember> members = info.members();

            return (A) Proxy.newProxyInstance(
                    annotationClass.getClassLoader(),
                    new Class<?>[]{annotationClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("annotationType")) {
                            return annotationClass;
                        }
                        AnnotationMember member = members.get(method.getName());
                        if (member != null) {
                            return resolveMember(member, method);
                        }
                        return method.getDefaultValue();
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct AnnotationInfo bridge proxy", e);
        }
    }

    private static Object resolveMember(AnnotationMember member, Method method) {
        return switch (member.kind()) {
            case STRING -> member.asString();
            case BOOLEAN -> member.asBoolean();
            case INT -> member.asInt();
            case LONG -> member.asLong();
            case DOUBLE -> member.asDouble();
            case FLOAT -> member.asFloat();
            case BYTE -> member.asByte();
            case SHORT -> member.asShort();
            case CHAR -> member.asChar();
            case CLASS -> loadClass(member.asType().asClass().declaration().name());
            case ENUM -> resolveEnum(member, method.getReturnType());
            case NESTED_ANNOTATION -> create(member.asNestedAnnotation());
            case ARRAY -> resolveArray(member, method.getReturnType().getComponentType());
        };
    }

    private static Object resolveArray(AnnotationMember member, Class<?> componentType) {
        List<AnnotationMember> elements = member.asArray();
        Object array = Array.newInstance(componentType, elements.size());

        for (int i = 0; i < elements.size(); i++) {
            AnnotationMember elem = elements.get(i);
            Object val = switch (elem.kind()) {
                case STRING -> elem.asString();
                case BOOLEAN -> elem.asBoolean();
                case INT -> elem.asInt();
                case LONG -> elem.asLong();
                case DOUBLE -> elem.asDouble();
                case FLOAT -> elem.asFloat();
                case BYTE -> elem.asByte();
                case SHORT -> elem.asShort();
                case CHAR -> elem.asChar();
                case CLASS -> loadClass(elem.asType().asClass().declaration().name());
                case ENUM -> resolveEnum(elem, componentType);
                case NESTED_ANNOTATION -> create(elem.asNestedAnnotation());
                default -> elem.toString();
            };
            Array.set(array, i, val);
        }
        return array;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object resolveEnum(AnnotationMember member, Class<?> enumType) {
        return Enum.valueOf((Class<Enum>) enumType, member.asEnumConstant());
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load annotation class member: " + className, e);
        }
    }
}
