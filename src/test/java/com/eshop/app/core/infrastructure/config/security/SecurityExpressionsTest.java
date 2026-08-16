package com.eshop.app.core.infrastructure.config.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies every {@link SecurityExpressions} constant is syntactically valid SpEL. This does not
 * evaluate the expressions (that would require a live Spring Security context/bean resolver for
 * {@code @userSecurity}/{@code @appProperties}) — it only guards against a typo breaking every
 * {@code @PreAuthorize} annotation that references a constant at application startup.
 */
class SecurityExpressionsTest {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @ParameterizedTest
    @ValueSource(strings = {
            SecurityExpressions.IS_ADMIN,
            SecurityExpressions.IS_SELLER,
            SecurityExpressions.IS_CUSTOMER,
            SecurityExpressions.IS_DELIVERY_AGENT,
            SecurityExpressions.IS_ADMIN_OR_SELF,
            SecurityExpressions.IS_ADMIN_OR_SELF_BY_USER_ID,
            SecurityExpressions.IS_ADMIN_OR_SELF_BY_EMAIL,
            SecurityExpressions.CAN_MANAGE_STORE,
            SecurityExpressions.CAN_VIEW_ORDER,
            SecurityExpressions.IS_AUTHENTICATED
    })
    void expressionParsesAsValidSpel(String expression) {
        assertThatCode(() -> parser.parseExpression(expression)).doesNotThrowAnyException();
    }
}
