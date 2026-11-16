package com.promoquoter.cart;

import com.promoquoter.promotion.CustomerSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class CartRequestTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void defaultsToTheRegularSegment() {
        CartRequest cart = new CartRequest(List.of(new CartItemRequest(a, 1)), null);

        assertThat(cart.customerSegment()).isEqualTo(CustomerSegment.REGULAR);
    }

    @Test
    void addsUpRepeatedProductsKeepingFirstSeenOrder() {
        CartRequest cart = new CartRequest(List.of(
                new CartItemRequest(a, 1),
                new CartItemRequest(b, 2),
                new CartItemRequest(a, 3)), CustomerSegment.VIP);

        assertThat(cart.quantitiesByProduct()).containsExactly(entry(a, 4), entry(b, 2));
    }

    @Test
    void equivalentCartsHaveTheSameFingerprint() {
        CartRequest original = new CartRequest(List.of(new CartItemRequest(a, 2), new CartItemRequest(b, 1)), null);
        CartRequest reordered = new CartRequest(List.of(new CartItemRequest(b, 1), new CartItemRequest(a, 2)), null);
        CartRequest split = new CartRequest(List.of(
                new CartItemRequest(a, 1), new CartItemRequest(b, 1), new CartItemRequest(a, 1)),
                CustomerSegment.REGULAR);

        assertThat(original.fingerprint())
                .hasSize(64)
                .isEqualTo(reordered.fingerprint())
                .isEqualTo(split.fingerprint());
    }

    @Test
    void differentCartsHaveDifferentFingerprints() {
        CartRequest cart = new CartRequest(List.of(new CartItemRequest(a, 2)), CustomerSegment.REGULAR);

        assertThat(cart.fingerprint())
                .isNotEqualTo(new CartRequest(List.of(new CartItemRequest(a, 3)), CustomerSegment.REGULAR).fingerprint())
                .isNotEqualTo(new CartRequest(List.of(new CartItemRequest(b, 2)), CustomerSegment.REGULAR).fingerprint())
                .isNotEqualTo(new CartRequest(List.of(new CartItemRequest(a, 2)), CustomerSegment.VIP).fingerprint());
    }
}
