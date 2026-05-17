package com.eshop.app.core.usecase;

/**
 * [HARDEN] Generic Use Case (Command) contract.
 *
 * Defines the standard input/output contract for all command-side use cases.
 * Command use cases mutate state (create, update, delete).
 *
 * Usage pattern:
 * <pre>
 * public class CreateProductUseCase implements UseCase<ProductCreateRequest, ProductResponse> {
 *     public ProductResponse execute(ProductCreateRequest command) { ... }
 * }
 * </pre>
 *
 * Separate from QueryUseCase to enforce CQRS-lite principle.
 * Business rules MUST NOT be moved into this abstraction — they belong in domain.
 *
 * @param <C> the command/input type
 * @param <R> the result/output type
 */
public interface UseCase<C, R> {

    /**
     * Executes the use case with the given command.
     *
     * @param command the command input, never null
     * @return the use case result
     */
    R execute(C command);
}
