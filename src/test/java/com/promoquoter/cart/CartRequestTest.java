package com.promoquoter.cart;

import com.promoquoter.promotion.CustomerSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class CartRequestTest {

    @Test
    void defaultsToTheRegularSegment() {
        CartRequest cart = new CartRequest(List.of(new CartItemRequest(UUID.randomUUID(), 1)), null);

        assertThat(cart.customerSegment()).isEqualTo(CustomerSegment.REGULAR);
    }

    @Test
    void addsUpRepeatedProductsKeepingFirstSeenOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        CartRequest cart = new CartRequest(List.of(
                new CartItemRequest(a, 1),
                new CartItemRequest(b, 2),
                new CartItemRequest(a, 3)), CustomerSegment.VIP);

        assertThat(cart.quantitiesByProduct()).containsExactly(entry(a, 4), entry(b, 2));
    }
}
