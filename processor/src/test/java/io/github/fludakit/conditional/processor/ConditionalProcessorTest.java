package io.github.fludakit.conditional.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the processor resolves bean existence against the complete source round and emits the
 * disabled-beans resource consumed by the CDI Lite build extension.
 */
class ConditionalProcessorTest {

    @TempDir
    Path output;

    @Test
    void disablesBeansWhoseBuiltInConditionsFail() throws IOException {
        Map<String, String> sources = Map.of(
                "com.example.ActualBean",
                """
                package com.example;
                import jakarta.enterprise.context.ApplicationScoped;
                @ApplicationScoped
                public class ActualBean {}
                """,

                "com.example.RequiresBean",
                """
                package com.example;
                import jakarta.enterprise.context.ApplicationScoped;
                import io.github.fludakit.conditional.annotation.ConditionalOnBean;
                @ApplicationScoped
                @ConditionalOnBean(com.example.ActualBean.class)
                public class RequiresBean {}
                """,

                "com.example.RequiresMissingBean",
                """
                package com.example;
                import jakarta.enterprise.context.ApplicationScoped;
                import io.github.fludakit.conditional.annotation.ConditionalOnMissingBean;
                @ApplicationScoped
                @ConditionalOnMissingBean(com.example.ActualBean.class)
                public class RequiresMissingBean {}
                """,

                "com.example.RequiresProperty",
                """
                package com.example;
                import jakarta.enterprise.context.ApplicationScoped;
                import io.github.fludakit.conditional.annotation.ConditionalOnProperty;
                @ApplicationScoped
                @ConditionalOnProperty(name = "com.example.absent", value = "yes")
                public class RequiresProperty {}
                """);

        compile(sources);

        String conditions = Files.readString(output.resolve("META-INF/fluda-conditional-beans/conditions"));
        assertThat(conditions).contains("class:com.example.RequiresMissingBean");
        assertThat(conditions).contains("class:com.example.RequiresProperty");
        assertThat(conditions).doesNotContain("class:com.example.RequiresBean");
    }

    private void compile(Map<String, String> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(output.toFile()));

        List<JavaFileObject> units = sources.entrySet().stream()
                .map(entry -> new SourceObject(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                List.of("-classpath", System.getProperty("java.class.path")), null, units);
        task.setProcessors(List.of(new ConditionalProcessor()));

        boolean success = Boolean.TRUE.equals(task.call());
        fileManager.close();

        assertThat(success)
                .as("compilation failed: %s", diagnostics.getDiagnostics())
                .isTrue();
    }

    private static final class SourceObject extends SimpleJavaFileObject {

        private final String code;

        SourceObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
