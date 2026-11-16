package com.promoquoter.promotion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A persisted promotion definition. Each subclass holds the parameters of one {@link PromotionType};
 * the pricing engine turns these definitions into executable rules.
 */
@Entity
@Table(name = "promotions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class Promotion {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Segments the promotion is limited to. Empty means it applies to every customer. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "promotion_customer_segments", joinColumns = @JoinColumn(name = "promotion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false)
    private Set<CustomerSegment> customerSegments = EnumSet.noneOf(CustomerSegment.class);

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Promotion() {
        // for JPA
    }

    protected Promotion(String name, Set<CustomerSegment> customerSegments) {
        this.id = UUID.randomUUID();
        this.name = Objects.requireNonNull(name, "name").trim();
        if (customerSegments != null && !customerSegments.isEmpty()) {
            this.customerSegments = EnumSet.copyOf(customerSegments);
        }
        this.createdAt = Instant.now();
    }

    public abstract PromotionType getType();

    public boolean appliesTo(CustomerSegment segment) {
        return customerSegments.isEmpty() || customerSegments.contains(segment);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<CustomerSegment> getCustomerSegments() {
        return Set.copyOf(customerSegments);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
