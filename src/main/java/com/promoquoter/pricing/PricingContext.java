package com.promoquoter.pricing;

import com.promoquoter.promotion.CustomerSegment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Mutable state shared by the rules of one calculation: the cart lines and the audit trail.
 * One context is created per calculation and is not shared between threads.
 */
public final class PricingContext {

    private final CustomerSegment customerSegment;
    private final Map<UUID, LineItem> lines = new LinkedHashMap<>();
    private final List<AuditEntry> auditTrail = new ArrayList<>();

    public PricingContext(CustomerSegment customerSegment, List<PricingItem> items) {
        this.customerSegment = Objects.requireNonNull(customerSegment, "customerSegment");
        for (PricingItem item : items) {
            if (lines.putIfAbsent(item.productId(), new LineItem(item)) != null) {
                throw new IllegalArgumentException("Duplicate cart line for product " + item.productId());
            }
        }
    }

    public CustomerSegment customerSegment() {
        return customerSegment;
    }

    /** Cart lines in their original order. */
    public List<LineItem> lines() {
        return List.copyOf(lines.values());
    }

    public Optional<LineItem> line(UUID productId) {
        return Optional.ofNullable(lines.get(productId));
    }

    /** The amount currently payable for the whole cart. */
    public BigDecimal currentTotal() {
        return lines.values().stream().map(LineItem::total).reduce(Money.ZERO, BigDecimal::add);
    }

    public int nextAuditStep() {
        return auditTrail.size() + 1;
    }

    public void audit(AuditEntry entry) {
        auditTrail.add(Objects.requireNonNull(entry, "entry"));
    }

    public List<AuditEntry> auditTrail() {
        return List.copyOf(auditTrail);
    }

    PricingResult toResult() {
        List<PricedLine> pricedLines = lines.values().stream().map(LineItem::toPricedLine).toList();
        BigDecimal subtotal = pricedLines.stream().map(PricedLine::subtotal).reduce(Money.ZERO, BigDecimal::add);
        BigDecimal total = pricedLines.stream().map(PricedLine::total).reduce(Money.ZERO, BigDecimal::add);
        List<AppliedPromotion> applied = auditTrail.stream()
                .filter(AuditEntry::changedPrice)
                .map(AppliedPromotion::from)
                .toList();
        return new PricingResult(customerSegment, pricedLines, applied, subtotal, subtotal.subtract(total), total,
                auditTrail());
    }
}
