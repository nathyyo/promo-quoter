package com.promoquoter.cart;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.promoquoter.order.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * A confirmed order: its id plus the same itemised breakdown a quote returns.
 */
public record ConfirmResponse(
        UUID orderId,
        Instant createdAt,
        @JsonUnwrapped QuoteResponse price) {

    static ConfirmResponse from(Order order, QuoteResponse price) {
        return new ConfirmResponse(order.getId(), order.getCreatedAt(), price);
    }
}
