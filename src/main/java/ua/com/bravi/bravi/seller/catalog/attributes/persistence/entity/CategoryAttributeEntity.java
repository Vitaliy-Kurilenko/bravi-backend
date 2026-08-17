package ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Binds an attribute to a category; every descendant category inherits the binding. */
@Getter
@Setter
@Entity
@Table(name = "store_category_attributes", indexes = {
        @Index(name = "idx_store_category_attributes_category_id", columnList = "category_id"),
        @Index(name = "idx_store_category_attributes_attribute_id", columnList = "attribute_id")
})
public class CategoryAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @NotNull
    @Column(name = "attribute_id", nullable = false)
    private Long attributeId;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
