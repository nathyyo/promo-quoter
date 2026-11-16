package com.promoquoter.promotion;

import com.promoquoter.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

@Entity
@DiscriminatorValue(PromotionType.Names.PERCENT_OFF_CATEGORY)
public class PercentOffCategoryPromotion extends Promotion {

    @Column(name = "category")
    private String category;

    @Column(name = "percent", precision = 5, scale = 2)
    private BigDecimal percent;

    protected PercentOffCategoryPromotion() {
        // for JPA
    }

    public PercentOffCategoryPromotion(String name, Set<CustomerSegment> segments, String category, BigDecimal percent) {
        super(name, segments);
        this.category = Product.normalizeCategory(category);
        this.percent = Objects.requireNonNull(percent, "percent");
    }

    @Override
    public PromotionType getType() {
        return PromotionType.PERCENT_OFF_CATEGORY;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPercent() {
        return percent;
    }
}
