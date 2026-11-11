package io.github.fludakit.conditional;

/**
 * A reflection-free view of the annotation that declared a {@link Condition}.
 * <p>
 * A condition is handed its declaring annotation through {@link ConditionContext#getAnnotation()} so it can
 * read the members that configure it, the way a built-in condition reads the values of its annotation. The
 * view is deliberately not the reflective {@code java.lang.annotation.Annotation}, so the same condition
 * class runs identically at build time (CDI Lite) and at runtime (CDI Full).
 * </p>
 *
 * @see ConditionContext#getAnnotation()
 */
public interface ConditionDefinition {

    /**
     * The fully qualified name of the annotation type.
     *
     * @return the annotation type name
     */
    String typeName();

    /**
     * The value of a {@code String[]} member.
     *
     * @param member the member name
     * @return the member value, never {@code null}
     */
    String[] strings(String member);

    /**
     * The value of a {@code boolean} member.
     *
     * @param member the member name
     * @return the member value
     */
    boolean bool(String member);

    /**
     * The value of a {@code Class<?>[]} member.
     *
     * @param member the member name
     * @return the member value, never {@code null}
     */
    Class<?>[] classes(String member);
}
