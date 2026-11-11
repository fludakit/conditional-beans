package io.github.fludakit.conditional.annotation;

import io.github.fludakit.conditional.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a bean only when the named classes are <em>not</em> available to the deployment.
 * <p>
 * Every named class has to be absent from the classpath. The counterpart of this annotation is
 * {@link ConditionalOnClass}, which requires the classes to be present.
 * </p>
 *
 * <pre>{@code
 * @ApplicationScoped
 * @ConditionalOnMissingClass(name = "com.example.LegacyIntegration")
 * public class ModernIntegrationBean {
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Conditional(OnMissingClassCondition.class)
public @interface ConditionalOnMissingClass {

    /**
     * The names of the classes that have to be absent.
     *
     * @return the class names
     */
    String[] name();
}
