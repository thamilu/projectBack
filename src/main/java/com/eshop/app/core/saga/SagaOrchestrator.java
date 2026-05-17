package com.eshop.app.core.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class SagaOrchestrator {

    public <T> void execute(Saga<T> saga) {
        T context = saga.getContext();
        List<SagaStep<T>> steps = saga.getSteps();
        List<SagaStep<T>> executedSteps = new ArrayList<>();

        log.info("Starting saga execution with {} steps", steps.size());

        for (SagaStep<T> step : steps) {
            try {
                log.debug("Executing saga step: {}", step.getClass().getSimpleName());
                step.execute(context);
                executedSteps.add(step);
            } catch (Exception e) {
                log.error("Saga step failed: {}. Commencing compensating transactions.", step.getClass().getSimpleName(), e);
                compensate(executedSteps, context);
                throw new SagaException("Saga execution aborted due to step failure: " + step.getClass().getSimpleName(), e);
            }
        }

        log.info("Saga execution completed successfully");
    }

    private <T> void compensate(List<SagaStep<T>> executedSteps, T context) {
        log.warn("Compensating {} executed saga steps in reverse order", executedSteps.size());
        
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = executedSteps.get(i);
            try {
                log.debug("Compensating saga step: {}", step.getClass().getSimpleName());
                step.compensate(context);
            } catch (Exception e) {
                log.error("CRITICAL: Saga compensation failed for step: {}", step.getClass().getSimpleName(), e);
                // In production systems, we'd queue failed compensations for manual recovery or dead-letter queueing.
            }
        }
    }
}
