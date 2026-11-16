package com.promoquoter.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A cart line while promotions are being applied. Discounts accumulate on the line, and the line
 * total can never drop below zero.
 */
public final class LineItem {

    private final PricingItem item;
    private final BigDecimal subtotal;
    private final List<LineDiscount> discounts = new ArrayList<>();
    private BigDecimal total;

    LineItem(PricingItem item) {
        this.item = item;
        this.subtotal = Money.round(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        this.total = subtotal;
    }

    /**
     * Records a discount from a promotion. The amount is rounded to cents and capped at the line's
     * current total, so a line never goes negative.
     *
     * @return the discount actually applied (zero if nothing was left to discount)
     */
    public BigDecimal applyDiscount(PromotionRule rule, BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("discount must not be negative");
        }
        BigDecimal applied = Money.round(amount).min(total);
        if (applied.signum() > 0) {
            discounts.add(new LineDiscount(rule.promotionId(), rule.name(), applied));
            total = total.subtract(applied);
        }
        return applied;
    }

    public UUID productId() {
        return item.productId();
    }

    public String name() {
        return item.name();
    }

    public String category() {
        return item.category();
    }

    public BigDecimal unitPrice() {
        return item.unitPrice();
    }

    public int quantity() {
        return item.quantity();
    }

    /** Unit price × quantity, before any discount. */
    public BigDecimal subtotal() {
        return subtotal;
    }

    /** Amount still payable for this line after the discounts applied so far. */
    public BigDecimal total() {
        return total;
    }

    public List<LineDiscount> discounts() {
        return List.copyOf(discounts);
    }

    PricedLine toPricedLine() {
        return new PricedLine(productId(), name(), category(), unitPrice(), quantity(),
                subtotal, discounts(), subtotal.subtract(total), total);
    }
}
