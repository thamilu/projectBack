package com.eshop.app.core.observability.tracing;

import org.slf4j.MDC;
import java.util.UUID;

public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final String MDC_KEY = "correlationId";

    private TraceContext() {}

    public static void start() {
        String traceId = UUID.randomUUID().toString();
        TRACE_ID.set(traceId);
        MDC.put(MDC_KEY, traceId);
    }

    public static void start(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            start();
        } else {
            TRACE_ID.set(traceId);
            MDC.put(MDC_KEY, traceId);
        }
    }

    public static String getTraceId() {
        String traceId = TRACE_ID.get();
        if (traceId == null) {
            traceId = MDC.get(MDC_KEY);
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
                TRACE_ID.set(traceId);
                MDC.put(MDC_KEY, traceId);
            }
        }
        return traceId;
    }

    public static void clear() {
        TRACE_ID.remove();
        MDC.remove(MDC_KEY);
    }
}
