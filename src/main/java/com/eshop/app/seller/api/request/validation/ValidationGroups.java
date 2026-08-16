package com.eshop.app.seller.api.request.validation;

import jakarta.validation.GroupSequence;

/**
 * Centralized namespace for Bean Validation group marker interfaces used in step-wise seller
 * onboarding validation.
 *
 * <h3>Design</h3>
 *
 * <p>Each inner interface is a <em>marker</em> — it carries no methods and exists solely as a type
 * token for Bean Validation's group mechanism. Constraints on request DTO fields reference these
 * groups via the {@code groups} attribute:
 *
 * <pre>{@code
 * @NotBlank(
 *     message = "Shop name is required",
 *     groups = ValidationGroups.Step1Basic.class)
 * private String shopName;
 * }</pre>
 *
 * <p>Controllers activate step-specific validation via Spring's {@code @Validated}:
 *
 * <pre>{@code
 * @PostMapping("/register/step1")
 * public ResponseEntity<Void> registerStep1(
 *         @Validated(ValidationGroups.Step1Basic.class)
 *         @RequestBody SellerRegisterRequest request) { ... }
 * }</pre>
 *
 * <h3>Group Evaluation Semantics</h3>
 *
 * <ul>
 *   <li><strong>Individual step groups</strong> ({@link Step1Basic} through {@link Step5Address}) —
 *       activate only the constraints for that step. Used in step-specific API endpoints.
 *   <li><strong>{@link FullRegistrationAggregate}</strong> — activates all step constraints
 *       simultaneously as an <em>unordered set union</em>. All constraints from all steps are
 *       evaluated regardless of failures in earlier steps. Use when you need all validation errors
 *       at once.
 *   <li><strong>{@link FullRegistrationSequence}</strong> — activates all step constraints in
 *       <em>declared order</em> with <em>short-circuit on failure</em>. If Step 1 fails, Steps 2–5
 *       are not evaluated. Use for full registration validation where step ordering and incremental
 *       feedback matter.
 * </ul>
 *
 * <h3>Default Group Interaction</h3>
 *
 * <p>Constraints declared without an explicit {@code groups} attribute belong to the Bean
 * Validation {@code Default} group. These constraints fire when {@code @Valid} is used (not
 * {@code @Validated}) or when {@code Default.class} is explicitly included. None of the groups
 * defined here extend or include {@code Default} — this is intentional to keep step validation
 * isolated.
 *
 * <h3>Adding New Steps</h3>
 *
 * <p>To add a new validation step:
 *
 * <ol>
 *   <li>Define a new marker interface inside this class (e.g., {@code Step6Shipping})
 *   <li>Add the new group to {@link FullRegistrationAggregate} extends clause
 *   <li>Add the new group to {@link FullRegistrationSequence} annotation in the correct position
 *   <li>Annotate the relevant DTO fields with the new group
 *   <li>Add the corresponding controller endpoint with {@code @Validated(Step6Shipping.class)}
 * </ol>
 *
 * @see jakarta.validation.groups.Default
 * @see GroupSequence
 * @since 1.0
 */
public final class ValidationGroups {

    /**
     * Utility class — prevent instantiation.
     *
     * <p>All members are static inner interfaces; this class is never instantiated.
     */
    private ValidationGroups() {
        throw new UnsupportedOperationException(
                "ValidationGroups is a utility class and cannot be instantiated");
    }

    // ─── Individual Step Groups ───────────────────────────────────────────────

    /**
     * Validation group for <strong>Step 1: Basic Identity</strong>.
     *
     * <p>Activates constraints on core seller identity and contact fields:
     *
     * <ul>
     *   <li>Seller identity type ({@code identityType})
     *   <li>Business types ({@code businessTypes})
     *   <li>Shop name ({@code shopName})
     *   <li>Personal phone ({@code phone})
     *   <li>Business phone ({@code businessPhone})
     *   <li>Terms acceptance ({@code acceptedTerms})
     * </ul>
     *
     * <p>Used with:
     *
     * <pre>{@code @Validated(ValidationGroups.Step1Basic.class)}</pre>
     */
    public interface Step1Basic {}

    /**
     * Validation group for <strong>Step 2: KYC (Know Your Customer)</strong>.
     *
     * <p>Activates constraints on tax identity and registration fields:
     *
     * <ul>
     *   <li>PAN number ({@code panNumber})
     *   <li>GSTIN ({@code gstin}) — conditionally required via {@code @ValidGstinIfRegistered}
     *   <li>GST registration status ({@code gstRegistered})
     *   <li>KYC business type ({@code kycBusinessType})
     *   <li>Aadhaar ({@code aadhar})
     *   <li>Registration proof URL ({@code registrationProof})
     * </ul>
     *
     * <p>Includes cross-field validation via {@link
     * ValidGstinIfRegistered @ValidGstinIfRegistered}.
     *
     * <p>Used with:
     *
     * <pre>{@code @Validated(ValidationGroups.Step2Kyc.class)}</pre>
     */
    public interface Step2Kyc {}

    /**
     * Validation group for <strong>Step 3: Bank Account Details</strong>.
     *
     * <p>Activates constraints on bank account fields:
     *
     * <ul>
     *   <li>Account holder name ({@code accountHolderName})
     *   <li>Account number ({@code accountNumber})
     *   <li>IFSC code ({@code ifscCode})
     *   <li>Bank name ({@code bankName})
     * </ul>
     *
     * <p>Used with:
     *
     * <pre>{@code @Validated(ValidationGroups.Step3Bank.class)}</pre>
     */
    public interface Step3Bank {}

    /**
     * Validation group for <strong>Step 4: Optional Business Modules</strong>.
     *
     * <p>Activates constraints on optional seller-specific module fields:
     *
     * <ul>
     *   <li>Farm details ({@code farmLocationVillage}, {@code landArea}, {@code cropTypes}, {@code
     *       isOwnProduce})
     *   <li>Wholesale details ({@code legalBusinessName}, {@code authorizedSignatory}, {@code
     *       warehouseLocation}, {@code bulkPricingEnabled}, {@code minOrderQuantity})
     * </ul>
     *
     * <p>All fields in this step are optional at the module level; constraints within this group
     * are conditionally activated based on seller business type.
     *
     * <p>Used with:
     *
     * <pre>{@code @Validated(ValidationGroups.Step4Optional.class)}</pre>
     */
    public interface Step4Optional {}

    /**
     * Validation group for <strong>Step 5: Address Details</strong>.
     *
     * <p>Activates constraints on primary and store address fields:
     *
     * <ul>
     *   <li>Primary address ({@code primaryAddress}) — required
     *   <li>Store address ({@code storeAddress}) — optional
     * </ul>
     *
     * <p>Address fields within {@code AddressRequest} are also scoped to this group and are
     * activated via {@code @Valid} cascade on the address field.
     *
     * <p>Used with:
     *
     * <pre>{@code @Validated(ValidationGroups.Step5Address.class)}</pre>
     */
    public interface Step5Address {}

    // ─── Composite Groups ─────────────────────────────────────────────────────

    /**
     * Composite validation group that activates <strong>all step constraints
     * simultaneously</strong> as an <em>unordered set union</em>.
     *
     * <h3>Semantics — IMPORTANT</h3>
     *
     * <p>This group uses interface inheritance ({@code extends}), which per JSR-380 §6.3 collects
     * all constraints from all extended groups into a <strong>flat, unordered set</strong>.
     * Specifically:
     *
     * <ul>
     *   <li>All constraints from Steps 1–5 are evaluated <em>regardless of failures in earlier
     *       steps</em>
     *   <li>The evaluation order between steps is <em>not guaranteed</em>
     *   <li>All constraint violations from all steps are returned in a single response — useful for
     *       bulk validation result collection
     * </ul>
     *
     * <h3>When to Use</h3>
     *
     * <p>Use this group when you need <em>all violations across all steps at once</em>, such as:
     *
     * <ul>
     *   <li>Administrative bulk validation of existing data
     *   <li>Single-page registration forms where all fields are submitted at once and all errors
     *       should be shown simultaneously
     *   <li>Data migration validation
     * </ul>
     *
     * <h3>When NOT to Use</h3>
     *
     * <p>Do NOT use this group when step ordering matters or when you want to prevent Step 2
     * validation from running if Step 1 fails. For that use case, see {@link
     * FullRegistrationSequence}.
     *
     * @see FullRegistrationSequence
     */
    public interface FullRegistrationAggregate
            extends Step1Basic, Step2Kyc, Step3Bank, Step4Optional, Step5Address {}

    /**
     * Ordered sequential validation group that activates all step constraints <strong>in declared
     * step order</strong> with <strong>short-circuit on failure</strong>.
     *
     * <h3>Semantics — IMPORTANT</h3>
     *
     * <p>This group uses {@link GroupSequence @GroupSequence}, which per JSR-380 §6.4 evaluates
     * each group in the declared sequence and stops processing subsequent groups as soon as any
     * constraint violation is found. Specifically:
     *
     * <ul>
     *   <li>Step 1 constraints are evaluated first
     *   <li>If Step 1 has any violations → processing stops; Steps 2–5 are <em>not evaluated</em>
     *   <li>If Step 1 passes → Step 2 is evaluated, and so on
     *   <li>Only the violations from the <em>first failing step</em> are returned
     * </ul>
     *
     * <h3>When to Use</h3>
     *
     * <p>Use this group when:
     *
     * <ul>
     *   <li>Validating a fully submitted registration in a single API call
     *   <li>Step ordering semantically matters (e.g., KYC should not be validated if basic identity
     *       is incomplete)
     *   <li>Returning incremental, step-focused error feedback
     * </ul>
     *
     * <h3>When NOT to Use</h3>
     *
     * <p>Do NOT use this group if you need all violations from all steps at once. For that use
     * case, see {@link FullRegistrationAggregate}.
     *
     * <h3>Usage</h3>
     *
     * <pre>{@code
     * @PostMapping("/register/full")
     * public ResponseEntity<Void> registerFull(
     *         @Validated(ValidationGroups.FullRegistrationSequence.class)
     *         @RequestBody SellerRegisterRequest request) { ... }
     * }</pre>
     *
     * @see FullRegistrationAggregate
     * @see GroupSequence
     */
    @GroupSequence({
        Step1Basic.class,
        Step2Kyc.class,
        Step3Bank.class,
        Step4Optional.class,
        Step5Address.class
    })
    public interface FullRegistrationSequence {}
}
