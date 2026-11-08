package io.github.fludakit.conditional.serviceloader;

import io.github.fludakit.conditional.Conditional;
import io.github.fludakit.conditional.support.Conditions;
import jakarta.enterprise.context.ApplicationScoped;

/** A bean that the default discovery has to disable. */
@ApplicationScoped
@Conditional(Conditions.AlwaysFalse.class)
public class ServiceLoaderDisabledBean {
}