package com.promoquoter.cart;

import com.promoquoter.common.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One or more products in the cart do not have enough stock. Every shortage is reported, so the
 * client can fix the whole cart at once.
 */
public class OutOfStockException extends ApiException {

    private final List<Shortage> shortages;

    public OutOfStockException(List<Shortage> shortages) {
        super(HttpStatus.CONFLICT, "OUT_OF_STOCK", "Insufficient stock",
                shortages.size() == 1
                        ? "Not enough stock for '" + shortages.get(0).name() + "': requested "
                          + shortages.get(0).requested() + ", available " + shortages.get(0).available() + "."
                        : "Not enough stock for " + shortages.size() + " products.");
        this.shortages = List.copyOf(shortages);
    }

    public List<Shortage> getShortages() {
        return shortages;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("shortages", shortages);
    }

    public record Shortage(UUID productId, String name, int requested, int available) {
    }
}
