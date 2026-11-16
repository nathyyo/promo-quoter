package com.promoquoter.pricing;

/**
 * Pipeline stages. Percentage discounts run before free-item promotions, so free units are valued
 * at the already-discounted price. Gaps between values leave room for new stages.
 */
public final class RulePriority {

    /** Percentage off a whole category. */
    public static final int CATEGORY_DISCOUNT = 100;

    /** Quantity-based percentage off a single product. */
    public static final int VOLUME_DISCOUNT = 200;

    /** Free units of a product. */
    public static final int FREE_ITEMS = 300;

    private RulePriority() {
    }
}
