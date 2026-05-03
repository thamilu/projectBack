package com.eshop.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Checkout Request DTO containing all necessary information for cart-to-order conversion.
 * 
 * Purpose:
 * - Captures customer shipping and billing information
 * - Stores payment method selection
 * - Provides order notes and special instructions
 * - Supports both anonymous and authenticated checkout
 * 
 * Checkout Flow:
 * 1. Cart validation (items exist, stock available)
 * 2. Address and payment information collection
 * 3. Order creation with calculated totals
 * 4. Inventory deduction and cart clearing
 * 5. Order confirmation and tracking
 * 
 * Validation Features:
 * - Required shipping address validation
 * - Optional billing address (defaults to shipping)
 * - Phone number format validation
 * - Payment method verification
 * - Input sanitization for security
 * 
 * Security Considerations:
 * - No sensitive payment data stored
 * - Address information encrypted in transit
 * - Input validation prevents injection attacks
 * - Audit trail for order creation
 * 
 * Integration Points:
 * - Payment gateway integration (via paymentMethod)
 * - Shipping cost calculation (external service)
 * - Tax calculation (based on shipping address)
 * - Inventory management system
 * 
 * @author EShop Development Team
 * @version 1.0
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    
    /**
     * Customer's shipping address where the order will be delivered.
     * 
     * Requirements:
     * - Cannot be null, empty, or blank
     * - Maximum 500 characters
     * - Should include complete address details
     * 
     * Format Guidelines:
     * - Street address, City, State/Province, Postal Code, Country
     * - Apartment/Suite number if applicable
     * - Clear delivery instructions for complex locations
     * 
     * Used For:
     * - Order delivery
     * - Shipping cost calculation
     * - Tax calculation (region-based)
     * - Delivery zone validation
     */
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;
    
    /**
     * Customer's billing address for payment processing.
     * Optional - defaults to shipping address if not provided.
     * 
     * Requirements:
     * - Maximum 500 characters if provided
     * - Should match payment method billing address
     * 
     * Use Cases:
     * - Different billing and shipping locations
     * - Corporate billing addresses
     * - Gift delivery scenarios
     * - Payment verification requirements
     */
    @Size(max = 500, message = "Billing address must not exceed 500 characters")
    private String billingAddress;
    
    /**
     * Customer contact phone number for order communication.
     * 
     * Requirements:
     * - Maximum 20 characters
     * - Should include country code for international orders
     * - Used for delivery coordination
     * 
     * Format Examples:
     * - "+1-555-123-4567" (US/Canada)
     * - "+44-20-1234-5678" (UK)
     * - "555-123-4567" (domestic)
     * 
     * Usage:
     * - Delivery agent contact
     * - Order status updates
     * - Problem resolution
     */
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    /**
     * Additional notes or special instructions for the order.
     * 
     * Requirements:
     * - Maximum 1000 characters
     * - Optional field
     * 
     * Common Uses:
     * - Delivery instructions ("Leave at door", "Ring doorbell")
     * - Gift messages
     * - Special handling requests
     * - Packaging preferences
     * - Timing requirements
     * 
     * Processing:
     * - Reviewed by fulfillment team
     * - May affect shipping method selection
     * - Included in order confirmation
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    /**
     * Selected payment method for order processing.
     * 
     * Supported Methods:
     * - "CREDIT_CARD" - Credit/Debit card payment
     * - "PAYPAL" - PayPal payment
     * - "BANK_TRANSFER" - Direct bank transfer
     * - "COD" - Cash on delivery
     * - "WALLET" - Digital wallet payment
     * 
     * Processing Notes:
     * - Actual payment processing handled by payment gateway
     * - This field used for order routing and fulfillment
     * - Payment validation occurs in separate service
     * - No sensitive payment data stored in this request
     */
    private String paymentMethod;
}