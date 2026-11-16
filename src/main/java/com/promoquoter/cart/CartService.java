package com.promoquoter.cart;

import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final ProductRepository products;
    private final CartPricingService pricing;

    public CartService(ProductRepository products, CartPricingService pricing) {
        this.products = products;
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
}
