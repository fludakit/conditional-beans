package io.github.fludakit.conditional.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An annotation processor that evaluates the six built-in conditional annotations at compile time.
 * <p>
 * Because it sees the complete source round before any code is emitted, it can answer bean existence - the
 * one question the CDI Lite build extension cannot answer itself. The result is written to
 * {@code META-INF/fluda-conditional-beans/conditions} as {@code class:FQCN}, {@code method:FQCN#name} and
 * {@code field:FQCN#name} lines, one disabled bean per line, for the build extension to consume.
 * </p>
 */
@SupportedAnnotationTypes("*")
public class ConditionalProcessor extends AbstractProcessor {

    private Elements elements;
    private Types types;
    private Filer filer;
    private Messager messager;
    private BeanGraph graph;
    private final List<TypeElement> discovered = new ArrayList<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.graph = new BeanGraph(elements, types);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element root : roundEnv.getRootElements()) {
            if (root instanceof TypeElement type) {
                discovered.add(type);
                graph.capture(type);
            }
        }
        if (roundEnv.processingOver()) {
            List<String> disabled = new BuiltInConditionEvaluator(elements, graph).evaluate(discovered);
            emit(disabled);
        }
        return false;
    }

    private void emit(List<String> disabled) {
        try (Writer writer = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                "META-INF/fluda-conditional-beans/conditions").openWriter()) {
            for (String key : disabled) {
                writer.write(key);
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Could not write the conditional-beans conditions resource: " + e.getMessage());
        }
    }
}
