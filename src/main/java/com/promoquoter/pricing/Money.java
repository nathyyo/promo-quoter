package com.promoquoter.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Monetary arithmetic. Amounts are {@link BigDecimal}s rounded HALF_UP to cents; intermediate
 * results keep full precision until they are rounded once.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {
    }

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING);
    }

    /** {@code percent}% of {@code amount}, rounded to cents. */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return round(amount.multiply(percent).divide(HUNDRED, MathContext.DECIMAL128));
    }

    /** {@code amount × numerator ÷ denominator}, rounded to cents. */
    public static BigDecimal proportion(BigDecimal amount, int numerator, int denominator) {
        if (denominator <= 0) {
            throw new IllegalArgumentException("denominator must be positive");
        }
        return round(amount.multiply(BigDecimal.valueOf(numerator))
                .divide(BigDecimal.valueOf(denominator), MathContext.DECIMAL128));
    }
}
