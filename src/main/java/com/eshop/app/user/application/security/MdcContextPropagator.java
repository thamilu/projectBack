package com.eshop.app.user.application.security;

import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MdcContextPropagator {

    public <T> Mono<T> propagate(Mono<T> mono) {
        return propagate(mono, Map.of());
    }

    public <T> Mono<T> propagate(Mono<T> mono, String email) {
        return propagate(mono, email != null ? Map.of("email", email) : Map.of());
    }

    public <T> Mono<T> propagate(Mono<T> mono, Map<String, String> context) {
        String correlationId = getOrCreateCorrelationId();

        return Mono.deferContextual(
                ctx -> {
                    try {
                        MDC.put("correlationId", correlationId);
                        context.forEach(MDC::put);

                        return mono.doOnEach(
                                        signal -> {
                                            MDC.put("correlationId", correlationId);
                                            context.forEach(MDC::put);
                                        })
                                .doFinally(
                                        signal -> {
                                            MDC.remove("correlationId");
                                            context.keySet().forEach(MDC::remove);
                                        })
                                .doOnCancel(
                                        () -> {
                                            MDC.remove("correlationId");
                                            context.keySet().forEach(MDC::remove);
                                        });
                    } catch (Exception e) {
                        MDC.remove("correlationId");
                        context.keySet().forEach(MDC::remove);
                        throw e;
                    }
                });
    }

    private String getOrCreateCorrelationId() {
        String existing = MDC.get("correlationId");
        return (existing != null && !existing.isBlank()) ? existing : UUID.randomUUID().toString();
    }
}
