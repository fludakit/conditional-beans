package io.github.fludakit.conditional.impl;

import io.github.fludakit.conditional.BeanContext;

import java.util.Arrays;

/**
 * The {@link BeanContext} backed by the potential-bean-set model.
 */
final class DeploymentBeanContext implements BeanContext {

    private final DeploymentModel model;

    DeploymentBeanContext(DeploymentModel model) {
        this.model = model;
    }

    @Override
    public boolean hasBean(Class<?> type, Class<?>... qualifierTypes) {
        return model.hasQualifiedBean(type, Arrays.asList(qualifierTypes));
    }

    @Override
    public boolean hasQualifiedBean(Class<?> type, Class<?>... qualifierTypes) {
        return model.hasQualifiedBean(type, Arrays.asList(qualifierTypes));
    }
}
