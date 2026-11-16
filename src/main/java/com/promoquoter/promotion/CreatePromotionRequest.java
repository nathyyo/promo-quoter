package com.promoquoter.promotion;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Request body for one promotion. The {@code type} property selects the concrete shape, so each
 * promotion type declares and validates only the parameters it needs.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreatePromotionRequest.PercentOffCategory.class, name = PromotionType.Names.PERCENT_OFF_CATEGORY),
        @JsonSubTypes.Type(value = CreatePromotionRequest.TieredBulkDiscount.class, name = PromotionType.Names.TIERED_BULK_DISCOUNT),
        @JsonSubTypes.Type(value = CreatePromotionRequest.BuyXGetY.class, name = PromotionType.Names.BUY_X_GET_Y)
})
public sealed interface CreatePromotionRequest {

    String name();

    Set<CustomerSegment> customerSegments();

    /** The product this promotion targets, if it targets a single product. */
    default Optional<UUID> referencedProductId() {
        return Optional.empty();
    }

    Promotion toEntity();

    record PercentOffCategory(
            @NotBlank @Size(max = 255) String name,
            Set<@NotNull CustomerSegment> customerSegments,
            @NotBlank @Size(max = 100) String category,
            @NotNull @Percentage BigDecimal percent) implements CreatePromotionRequest {

        @Override
        public Promotion toEntity() {
            return new PercentOffCategoryPromotion(name, customerSegments, category, percent);
        }
    }

    record TieredBulkDiscount(
            @NotBlank @Size(max = 255) String name,
            Set<@NotNull CustomerSegment> customerSegments,
            @NotNull UUID productId,
            @NotEmpty @Size(max = 10) List<@NotNull @Valid Tier> tiers) implements CreatePromotionRequest {

        public record Tier(
                @NotNull @Positive Integer minQuantity,
                @NotNull @Percentage BigDecimal percent) {
        }

        @AssertTrue(message = "each tier must have a different minQuantity")
        boolean isTierThresholdsUnique() {
            if (tiers == null) {
                return true;
            }
            List<Integer> thresholds = tiers.stream()
                    .filter(tier -> tier != null && tier.minQuantity() != null)
                    .map(Tier::minQuantity)
                    .toList();
            return thresholds.stream().distinct().count() == thresholds.size();
        }

        @Override
        public Optional<UUID> referencedProductId() {
            return Optional.of(productId);
        }

        @Override
        public Promotion toEntity() {
            List<DiscountTier> discountTiers = tiers.stream()
                    .map(tier -> new DiscountTier(tier.minQuantity(), tier.percent()))
                    .toList();
            return new TieredBulkDiscountPromotion(name, customerSegments, productId, discountTiers);
        }
    }

    record BuyXGetY(
            @NotBlank @Size(max = 255) String name,
            Set<@NotNull CustomerSegment> customerSegments,
            @NotNull UUID productId,
            @NotNull @Positive Integer buyQuantity,
            @NotNull @Positive Integer freeQuantity) implements CreatePromotionRequest {

        @Override
        public Optional<UUID> referencedProductId() {
            return Optional.of(productId);
        }

        @Override
        public Promotion toEntity() {
            return new BuyXGetYPromotion(name, customerSegments, productId, buyQuantity, freeQuantity);
        }
    }
}
