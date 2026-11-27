# Getting Started

This guide covers adding FluDa Conditional Beans to your project and declaring your first conditional bean.

## Requirements

- JDK 21 or later.
- CDI 4.1 or later (`jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0`).
- No runtime dependency beyond the CDI API.

## Adding the dependency

Choose the runtime that matches your deployment.

### CDI Full

Use the portable extension when your application runs in a full CDI container — GlassFish, WildFly, Weld SE, or any other Jakarta EE runtime:

```xml
<dependency>
    <groupId>io.github.fludakit</groupId>
    <artifactId>fluda-conditional-beans-full</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The `full` artifact depends on `api` transitively, so the core API types are on the classpath as well.

### CDI Lite

Use the build compatible extension when your application is built with a CDI Lite capable build-time extension processor — for example Quarkus Arc Lite:

```xml
<dependency>
    <groupId>io.github.fludakit</groupId>
    <artifactId>fluda-conditional-beans-lite</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The `lite` artifact depends on `api` transitively as well.

## Declaring your first condition

A condition is a plain class with a public no-argument constructor. It is written against the annotation that declares it, so it can read that annotation's values without reflection.

Start with a condition that reads a configuration property:

```java
public class ChinaCondition implements Condition<Conditional> {

    @Override
    public boolean matches(ConditionContext context, Conditional annotation) {
        return "CN".equals(context.getProperty("app.country").orElse(null));
    }
}
```

Then declare it on a bean:

```java
@ApplicationScoped
@Conditional(ChinaCondition.class)
public class ChinaTaxCalculator implements TaxCalculator {
}
```

When `app.country` is not `CN`, the bean is **not part of the deployment**. It is not injectable, it does not show up in `BeanManager.getBeans(...)`, and it produces no `ProcessBean` event.

The extension is registered through `META-INF/services/jakarta.enterprise.inject.spi.Extension`, so it is active in every deployment that has the artifact on the classpath — there is nothing else to enable.

## Composing your own annotation

Because `@Conditional` is a stereotype, it can be used as a meta-annotation. This is how the built-in conditions are written, and it is how an application declares an annotation of its own:

```java
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD })
@Conditional(ChinaCondition.class)
public @interface ConditionalOnCountry {
}
```

A condition written against that annotation is handed the annotation instance:

```java
public class CountryCondition implements Condition<ConditionalOnCountry> {

    @Override
    public boolean matches(ConditionContext context, ConditionalOnCountry annotation) {
        return "CN".equals(context.getProperty("app.country").orElse(null));
    }
}
```

## Using the built-in conditions

The built-in conditions cover the cases that come up most often. They are plain annotations, so they compose with your own:

```java
@ApplicationScoped
@ConditionalOnClass(name = "com.example.OptionalIntegration")
public class OptionalIntegrationBean {
}
```

```java
@ApplicationScoped
@ConditionalOnProperty(name = "app.country", havingValue = "CN")
public class ChinaTaxCalculator implements TaxCalculator {
}
```

```java
@ApplicationScoped
@ConditionalOnMissingBean(LegacyPaymentService.class)
public class PaymentServiceConfiguration {
}
```

See the [module overview](index.md) for the full table of built-in conditions and the API they are built on.

## Combining conditions

All collected conditions have to match. The first one that does not match vetoes the bean. The repeatable form and the array form carry the same meaning and may be mixed:

```java
@ApplicationScoped
@Conditional(ChinaCondition.class)
@Conditional(DatabaseAvailable.class)
public class ChinaTaxCalculator implements TaxCalculator {
}
```

## Building

```bash
./mvnw clean install
```
