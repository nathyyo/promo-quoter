package com.promoquoter.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@DiscriminatorValue(PromotionType.Names.BUY_X_GET_Y)
public class BuyXGetYPromotion extends Promotion {

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "free_quantity")
    private Integer freeQuantity;

    protected BuyXGetYPromotion() {
        // for JPA
    }

    public BuyXGetYPromotion(String name, Set<CustomerSegment> segments, UUID productId, int buyQuantity, int freeQuantity) {
        super(name, segments);
        this.productId = Objects.requireNonNull(productId, "productId");
        this.buyQuantity = buyQuantity;
        this.freeQuantity = freeQuantity;
    }

    @Override
    public PromotionType getType() {
        return PromotionType.BUY_X_GET_Y;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getBuyQuantity() {
        return buyQuantity;
    }

    public int getFreeQuantity() {
        return freeQuantity;
    }
}
