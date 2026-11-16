package com.promoquoter.promotion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@DiscriminatorValue(PromotionType.Names.TIERED_BULK_DISCOUNT)
public class TieredBulkDiscountPromotion extends Promotion {

    @Column(name = "product_id")
    private UUID productId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "promotion_discount_tiers", joinColumns = @JoinColumn(name = "promotion_id"))
    @OrderBy("minQuantity ASC")
    private List<DiscountTier> tiers = new ArrayList<>();

    protected TieredBulkDiscountPromotion() {
        // for JPA
    }

    public TieredBulkDiscountPromotion(String name, Set<CustomerSegment> segments, UUID productId, List<DiscountTier> tiers) {
        super(name, segments);
        this.productId = Objects.requireNonNull(productId, "productId");
        this.tiers = tiers.stream()
                .sorted(Comparator.comparingInt(DiscountTier::getMinQuantity))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public PromotionType getType() {
        return PromotionType.TIERED_BULK_DISCOUNT;
    }

    public UUID getProductId() {
        return productId;
    }

    /** Tiers in ascending order of {@code minQuantity}. */
    public List<DiscountTier> getTiers() {
        return List.copyOf(tiers);
    }
}
