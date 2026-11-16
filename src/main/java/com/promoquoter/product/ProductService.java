package com.promoquoter.product;

import com.promoquoter.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates all products in one transaction: either every product is stored or none is.
     */
    @Transactional
    public List<ProductResponse> create(List<CreateProductRequest> requests) {
        List<Product> products = requests.stream().map(CreateProductRequest::toEntity).toList();
        return repository.saveAll(products).stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return repository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
