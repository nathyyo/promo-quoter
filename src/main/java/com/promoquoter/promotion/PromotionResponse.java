package com.promoquoter.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * A stored promotion. Only the parameters relevant to its {@code type} are present.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromotionResponse(
        UUID id,
        PromotionType type,
        String name,
        Set<CustomerSegment> customerSegments,
        String category,
        BigDecimal percent,
        UUID productId,
        Integer buyQuantity,
        Integer freeQuantity,
        List<Tier> tiers,
        Instant createdAt) {

    public record Tier(int minQuantity, BigDecimal percent) {
    }

    static PromotionResponse from(Promotion promotion) {
        Set<CustomerSegment> segments = new TreeSet<>(promotion.getCustomerSegments());
        return switch (promotion) {
            case PercentOffCategoryPromotion p -> new PromotionResponse(
                    p.getId(), p.getType(), p.getName(), segments,
                    p.getCategory(), p.getPercent(), null, null, null, null, p.getCreatedAt());
            case TieredBulkDiscountPromotion p -> new PromotionResponse(
                    p.getId(), p.getType(), p.getName(), segments,
                    null, null, p.getProductId(), null, null,
                    p.getTiers().stream().map(t -> new Tier(t.getMinQuantity(), t.getPercent())).toList(),
                    p.getCreatedAt());
            case BuyXGetYPromotion p -> new PromotionResponse(
                    p.getId(), p.getType(), p.getName(), segments,
                    null, null, p.getProductId(), p.getBuyQuantity(), p.getFreeQuantity(), null, p.getCreatedAt());
            default -> throw new IllegalArgumentException("Unsupported promotion: " + promotion.getClass().getName());
        };
    }
}
