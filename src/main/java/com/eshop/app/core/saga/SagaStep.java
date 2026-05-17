package com.eshop.app.core.saga;

public interface SagaStep<T> {
    void execute(T context);
    void compensate(T context);
}
