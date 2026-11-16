package com.promoquoter.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotNull @PositiveOrZero Integer stock) {

    Product toEntity() {
        return new Product(name, category, price, stock);
    }
}
