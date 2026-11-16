package com.promoquoter.pricing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A discount one promotion gave on one cart line.
 */
public record LineDiscount(UUID promotionId, String promotionName, BigDecimal amount) {
}
