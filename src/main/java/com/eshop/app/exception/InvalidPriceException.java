package com.eshop.app.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exception thrown when product price validation fails.
 * 
 * <p>Validation rules:
 * <ul>
 *   <li>Price must be positive</li>
 *   <li>Discount price must be less than regular price</li>
 *   <li>Discount percentage must be between 0-100%</li>
 *   <li>Price must not exceed maximum allowed value</li>
 * </ul>
 * 
 * <p>HTTP Status: 400 BAD REQUEST
 * 
 * @since 1.0
 */
@Getter
public class InvalidPriceException extends RuntimeException {
    
    private final BigDecimal price;
    private final BigDecimal discountPrice;
    private final String validationRule;
    
    /**
     * Construct exception with message only.
     * 
     * @param message the error message
     */
    public InvalidPriceException(String message) {
        super(message);
        this.price = null;
        this.discountPrice = null;
        this.validationRule = null;
    }
    
    /**
     * Construct exception with price details.
     * 
     * @param message the error message
     * @param price the invalid price
     * @param discountPrice the discount price (may be null)
     */
    public InvalidPriceException(String message, BigDecimal price, BigDecimal discountPrice) {
        super(message);
        this.price = price;
        this.discountPrice = discountPrice;
        this.validationRule = null;
    }
    
    /**
     * Construct exception with full details.
     * 
     * @param message the error message
     * @param price the invalid price
     * @param discountPrice the discount price (may be null)
     * @param validationRule the violated rule
     */
    public InvalidPriceException(String message, BigDecimal price, 
                                BigDecimal discountPrice, String validationRule) {
        super(message);
        this.price = price;
        this.discountPrice = discountPrice;
        this.validationRule = validationRule;
    }
}
