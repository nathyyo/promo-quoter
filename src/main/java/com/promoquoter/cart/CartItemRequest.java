package com.promoquoter.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CartItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive @Max(CartItemRequest.MAX_QTY) Integer qty) {

    static final int MAX_QTY = 10_000;
}
