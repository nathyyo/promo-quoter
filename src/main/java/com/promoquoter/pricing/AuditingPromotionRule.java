package com.promoquoter.pricing;

import com.promoquoter.promotion.PromotionType;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Decorator that records an {@link AuditEntry} in the context every time the wrapped rule runs,
 * capturing the cart total before and after. Rules stay free of bookkeeping code.
 */
public final class AuditingPromotionRule implements PromotionRule {

    private final PromotionRule delegate;

    public AuditingPromotionRule(PromotionRule delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void apply(PricingContext context) {
        BigDecimal before = context.currentTotal();
        delegate.apply(context);
        BigDecimal after = context.currentTotal();
        context.audit(new AuditEntry(context.nextAuditStep(), promotionId(), name(), type(), before, after));
    }

    @Override
    public UUID promotionId() {
        return delegate.promotionId();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public PromotionType type() {
        return delegate.type();
    }

    @Override
    public int priority() {
        return delegate.priority();
    }

    @Override
    public String toString() {
        return "Audited[" + delegate + "]";
    }
}
