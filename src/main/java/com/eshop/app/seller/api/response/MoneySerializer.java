package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jspecify.annotations.Nullable;

/**
 * Reusable Jackson serializer for formatting monetary BigDecimal values to exactly 2 decimal places.
 *
 * <p><strong>Rounding Strategy:</strong> Values are rounded using {@link RoundingMode#HALF_UP}.
 *
 * <p><strong>JSON Type Contract:</strong> To prevent floating-point precision loss on web/mobile clients,
 * monetary amounts are intentionally serialized as JSON Strings (e.g. {@code "1234.56"}) via {@code writeString()}
 * rather than numeric types.
 *
 * <p><strong>Large/Small Values:</strong> Uses {@code toPlainString()} to avoid scientific notation.
 *
 * @author Platform Engineering
 * @since 1.0
 */
public final class MoneySerializer extends JsonSerializer<BigDecimal> {

    private static final int MONEY_SCALE = 2;

    @Override
    public void serialize(
            @Nullable BigDecimal value,
            JsonGenerator gen,
            SerializerProvider serializers)
            throws IOException {

        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeString(
                value.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                     .toPlainString()
        );
    }
}
