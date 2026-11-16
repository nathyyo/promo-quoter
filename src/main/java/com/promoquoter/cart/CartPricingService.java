package com.promoquoter.cart;

import com.promoquoter.common.error.InvalidReferenceException;
import com.promoquoter.pricing.PricingEngine;
import com.promoquoter.pricing.PricingItem;
import com.promoquoter.pricing.PricingResult;
import com.promoquoter.product.Product;
import com.promoquoter.promotion.PromotionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prices a cart against the current catalogue and promotions. It reads data but never changes it.
 */
@Service
public class CartPricingService {

    private final PromotionRepository promotions;
    private final PricingEngine engine;

    public CartPricingService(PromotionRepository promotions, PricingEngine engine) {
        this.promotions = promotions;
        this.engine = engine;
    }

    /**
     * Prices the cart using the given products, which must include every product in the cart.
     * Callers load the products so they control how (for example, with or without row locks).
     */
    public PricingResult price(CartRequest cart, Collection<Product> products) {
        Map<UUID, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<UUID, Integer> quantities = cart.quantitiesByProduct();

        List<UUID> missing = quantities.keySet().stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidReferenceException("Product", missing);
        }

        List<PricingItem> items = new ArrayList<>();
        quantities.forEach((productId, qty) -> {
            Product product = byId.get(productId);
            items.add(new PricingItem(product.getId(), product.getName(), product.getCategory(), product.getPrice(), qty));
        });
        return engine.price(items, cart.customerSegment(), promotions.findAllByOrderByCreatedAtAscIdAsc());
    }
}
