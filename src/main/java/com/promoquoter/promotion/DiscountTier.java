package com.promoquoter.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One step of a tiered bulk discount: buying at least {@code minQuantity} units earns {@code percent} off.
 */
@Embeddable
public class DiscountTier {

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;

    @Column(name = "percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    protected DiscountTier() {
        // for JPA
    }

    public DiscountTier(int minQuantity, BigDecimal percent) {
        this.minQuantity = minQuantity;
        this.percent = Objects.requireNonNull(percent, "percent");
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public BigDecimal getPercent() {
        return percent;
    }
}
