package com.eshop.app.seller.shared.exception;

import com.eshop.app.core.exception.base.BusinessException;

/**
 * Thrown when the aggregation of the seller dashboard metrics fails due to underlying service
 * failures.
 */
public class DashboardAggregationException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public DashboardAggregationException(String message, Throwable cause) {
        super(message, "DASHBOARD_AGGREGATION_ERROR", cause);
    }
}
