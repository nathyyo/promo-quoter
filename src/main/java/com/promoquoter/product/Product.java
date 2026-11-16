package com.promoquoter.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Version
    private Long version;

    protected Product() {
        // for JPA
    }

    public Product(String name, String category, BigDecimal price, int stock) {
        this.id = UUID.randomUUID();
        this.name = Objects.requireNonNull(name, "name").trim();
        this.category = normalizeCategory(category);
        this.price = Objects.requireNonNull(price, "price");
        this.stock = stock;
    }

    /**
     * Categories are compared case-insensitively, so they are stored in a single canonical form.
     */
    public static String normalizeCategory(String category) {
        return Objects.requireNonNull(category, "category").trim().toUpperCase(Locale.ROOT);
    }

    public boolean hasStock(int quantity) {
        return quantity <= stock;
    }

    /**
     * Takes {@code quantity} units out of stock. Callers must hold a lock on this row.
     *
     * @throws IllegalStateException if there is not enough stock; callers check {@link #hasStock} first
     */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (!hasStock(quantity)) {
            throw new IllegalStateException("Cannot reserve " + quantity + " of product " + id + "; only " + stock + " left");
        }
        stock -= quantity;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public Long getVersion() {
        return version;
    }
}
