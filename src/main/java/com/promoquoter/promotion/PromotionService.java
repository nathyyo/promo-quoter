package com.promoquoter.promotion;

import com.promoquoter.common.error.InvalidReferenceException;
import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PromotionService {

    private final PromotionRepository promotions;
    private final ProductRepository products;

    public PromotionService(PromotionRepository promotions, ProductRepository products) {
        this.promotions = promotions;
        this.products = products;
    }

    /**
     * Creates all promotions in one transaction. Promotions that target a product are rejected
     * unless the product exists, so the pricing engine never sees a dangling reference.
     */
    @Transactional
    public List<PromotionResponse> create(List<CreatePromotionRequest> requests) {
        requireProductsExist(requests);
        List<Promotion> entities = requests.stream().map(CreatePromotionRequest::toEntity).toList();
        return promotions.saveAll(entities).stream().map(PromotionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> list() {
        return promotions.findAllByOrderByCreatedAtAscIdAsc().stream().map(PromotionResponse::from).toList();
    }

    private void requireProductsExist(List<CreatePromotionRequest> requests) {
        Set<UUID> referenced = new HashSet<>();
        requests.stream()
                .map(CreatePromotionRequest::referencedProductId)
                .flatMap(Optional::stream)
                .forEach(referenced::add);
        if (referenced.isEmpty()) {
            return;
        }
        Set<UUID> missing = new HashSet<>(referenced);
        products.findAllById(referenced).stream().map(Product::getId).forEach(missing::remove);
        if (!missing.isEmpty()) {
            throw new InvalidReferenceException("Product", missing);
        }
    }
}
