package com.eshop.app.order.application.port.in;

import com.eshop.app.order.api.request.CheckoutRequest;
import com.eshop.app.order.api.response.OrderResponse;

/**
 * Use case for processing cart checkout to order.
 */
public interface CheckoutUseCase {
    OrderResponse checkoutAnonymousCart(String cartCode, CheckoutRequest request);
    OrderResponse checkoutAuthenticatedCart(String cartCode, CheckoutRequest request);
}
