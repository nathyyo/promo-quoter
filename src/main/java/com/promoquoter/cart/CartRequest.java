package com.promoquoter.cart;

import com.promoquoter.promotion.CustomerSegment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Body of {@code POST /cart/quote} and {@code POST /cart/confirm}.
 *
 * @param customerSegment optional; defaults to {@link CustomerSegment#REGULAR}
 */
public record CartRequest(
        @NotEmpty @Size(max = CartRequest.MAX_ITEMS) List<@NotNull @Valid CartItemRequest> items,
        CustomerSegment customerSegment) {

    static final int MAX_ITEMS = 100;

    public CartRequest {
        if (customerSegment == null) {
            customerSegment = CustomerSegment.REGULAR;
        }
    }

    /**
     * Quantities per product, in first-seen order. Repeated lines for the same product are added
     * together so quantity-based promotions see the full amount.
     */
    public Map<UUID, Integer> quantitiesByProduct() {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CartItemRequest item : items) {
            quantities.merge(item.productId(), item.qty(), Integer::sum);
        }
        return quantities;
    }
}
