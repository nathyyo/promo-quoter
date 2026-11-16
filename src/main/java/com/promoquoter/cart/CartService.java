package com.promoquoter.cart;

import com.promoquoter.order.Order;
import com.promoquoter.order.OrderRepository;
import com.promoquoter.pricing.PricingResult;
import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CartService {

    private final ProductRepository products;
    private final OrderRepository orders;
    private final CartPricingService pricing;

    public CartService(ProductRepository products, OrderRepository orders, CartPricingService pricing) {
        this.products = products;
        this.orders = orders;
        this.pricing = pricing;
    }

    /**
     * Prices the cart without reserving stock. Stock is only checked when the order is confirmed.
     */
    @Transactional(readOnly = true)
    public QuoteResponse quote(CartRequest cart) {
        List<Product> productsInCart = products.findAllById(cart.quantitiesByProduct().keySet());
        return QuoteResponse.from(pricing.price(cart, productsInCart));
    }

    /**
     * Prices the cart, reserves its stock and records the order, all in one transaction.
     *
     * <p>The products are read with row-level write locks, so concurrent confirmations of the same
     * products run one after another and each sees the stock left by the previous one. If any line
     * lacks stock, nothing is reserved and the whole request fails with 409.
     */
    @Transactional
    public ConfirmResponse confirm(CartRequest cart) {
        Map<UUID, Integer> quantities = cart.quantitiesByProduct();
        List<Product> lockedProducts = products.findAllByIdForUpdate(quantities.keySet());

        PricingResult price = pricing.price(cart, lockedProducts);
        reserveStock(lockedProducts, quantities);

        Order order = orders.save(Order.place(price));
        return ConfirmResponse.from(order, QuoteResponse.from(price));
    }

    private static void reserveStock(List<Product> lockedProducts, Map<UUID, Integer> quantities) {
        List<OutOfStockException.Shortage> shortages = lockedProducts.stream()
                .filter(product -> !product.hasStock(quantities.get(product.getId())))
                .map(product -> new OutOfStockException.Shortage(
                        product.getId(), product.getName(), quantities.get(product.getId()), product.getStock()))
                .toList();
        if (!shortages.isEmpty()) {
            throw new OutOfStockException(shortages);
        }
        lockedProducts.forEach(product -> product.reserve(quantities.get(product.getId())));
    }
}
