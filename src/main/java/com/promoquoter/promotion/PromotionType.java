package com.promoquoter.promotion;

public enum PromotionType {

    /** A percentage off every product in a category. */
    PERCENT_OFF_CATEGORY,

    /** A percentage off a product once its quantity reaches a threshold; the highest reached tier wins. */
    TIERED_BULK_DISCOUNT,

    /** For a product, every group of X paid units earns Y free units. */
    BUY_X_GET_Y;

    /** Discriminator values must be compile-time constants, so they are mirrored here. */
    static final class Names {
        static final String PERCENT_OFF_CATEGORY = "PERCENT_OFF_CATEGORY";
        static final String TIERED_BULK_DISCOUNT = "TIERED_BULK_DISCOUNT";
        static final String BUY_X_GET_Y = "BUY_X_GET_Y";

        private Names() {
        }
    }
}
