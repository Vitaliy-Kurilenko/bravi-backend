package ua.com.bravi.bravi.seller.catalog.categories.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;
import ua.com.bravi.bravi.seller.stores.domain.StoreOwned;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "store_categories", indexes = {
        @Index(name = "idx_store_categories_store_id", columnList = "store_id"),
        @Index(name = "idx_store_categories_parent_id", columnList = "parent_id")
})
public class CategoryEntity implements StoreOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @NotNull
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "parent_id")
    private Long parentId;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }
}
