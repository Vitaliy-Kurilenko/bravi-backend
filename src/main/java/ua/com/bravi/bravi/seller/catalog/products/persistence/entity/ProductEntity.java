package ua.com.bravi.bravi.seller.catalog.products.persistence.entity;

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
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.stores.domain.StoreOwned;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_store_id", columnList = "store_id"),
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_manufacturer_id", columnList = "manufacturer_id"),
        @Index(name = "idx_products_stock_status_id", columnList = "stock_status_id"),
        @Index(name = "idx_products_created_at", columnList = "created_at")
})
public class ProductEntity implements StoreOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    @NotNull
    @Column(name = "stock_status_id", nullable = false)
    private Long stockStatusId;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sku", length = 128)
    private String sku;

    @NotNull
    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @NotNull
    @Column(name = "partner_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal partnerPrice;

    @NotNull
    @Column(name = "recommended_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal recommendedPrice;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "weight", precision = 12, scale = 3)
    private BigDecimal weight;

    @Column(name = "width", precision = 12, scale = 3)
    private BigDecimal width;

    @Column(name = "height", precision = 12, scale = 3)
    private BigDecimal height;

    @Column(name = "length", precision = 12, scale = 3)
    private BigDecimal length;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProductStatus status = ProductStatus.ACTIVE;

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
