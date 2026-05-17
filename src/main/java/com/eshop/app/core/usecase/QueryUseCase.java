package com.eshop.app.core.usecase;

/**
 * [HARDEN] Generic Query Use Case contract.
 *
 * Defines the standard input/output contract for all query-side use cases.
 * Query use cases are read-only — they MUST NOT mutate state.
 *
 * Enforces CQRS-lite:
 * - Commands: UseCase<C, R> — write, mutate, orchestrate
 * - Queries:  QueryUseCase<Q, R> — read, project, never mutate
 *
 * Usage pattern:
 * <pre>
 * public class GetProductByIdQuery implements QueryUseCase<Long, ProductResponse> {
 *     public ProductResponse execute(Long productId) { ... }
 * }
 * </pre>
 *
 * @param <Q> the query/input type
 * @param <R> the result/output type
 */
public interface QueryUseCase<Q, R> {

    /**
     * Executes the query use case with the given query input.
     * Implementations must be side-effect free.
     *
     * @param query the query input, never null
     * @return the query result
     */
    R execute(Q query);
}
