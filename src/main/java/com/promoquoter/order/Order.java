package com.promoquoter.order;

import com.promoquoter.pricing.PricingResult;
import com.promoquoter.promotion.CustomerSegment;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A confirmed order. Stock for its lines was reserved in the same transaction that created it.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerSegment customerSegment;

    @ElementCollection
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_number")
    private List<OrderLine> lines = new ArrayList<>();

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountTotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Order() {
        // for JPA
    }

    private Order(PricingResult pricing) {
        this.id = UUID.randomUUID();
        this.customerSegment = pricing.customerSegment();
        this.subtotal = pricing.subtotal();
        this.discountTotal = pricing.discountTotal();
        this.total = pricing.total();
        this.createdAt = Instant.now();
        pricing.lines().forEach(line -> lines.add(new OrderLine(line.productId(), line.name(), line.unitPrice(),
                line.quantity(), line.subtotal(), line.discountTotal(), line.total())));
    }

    public static Order place(PricingResult pricing) {
        return new Order(pricing);
    }

    public UUID getId() {
        return id;
    }

    public CustomerSegment getCustomerSegment() {
        return customerSegment;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
