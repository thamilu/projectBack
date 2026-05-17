package com.eshop.app.core.saga;

import java.util.List;

public interface Saga<T> {
    List<SagaStep<T>> getSteps();
    T getContext();
}
