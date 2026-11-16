package com.promoquoter.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    static final int MAX_BATCH_SIZE = 100;

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /**
     * Accepts a JSON array of products, or a single product object (treated as a one-element array).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ProductResponse> create(
            @RequestBody @NotEmpty @Size(max = MAX_BATCH_SIZE) List<@Valid CreateProductRequest> requests) {
        return service.create(requests);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
