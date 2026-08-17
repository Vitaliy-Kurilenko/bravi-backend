package ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One value a product carries for one attribute. Option-based attributes keep a row per chosen
 * option; the remaining types keep a single row filling the column matching their value type.
 */
@Getter
@Setter
@Entity
@Table(name = "store_product_attribute_values", indexes = {
        @Index(name = "idx_store_product_attribute_values_product_id", columnList = "product_id"),
        @Index(name = "idx_store_product_attribute_values_attribute_id", columnList = "attribute_id")
})
public class ProductAttributeValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotNull
    @Column(name = "attribute_id", nullable = false)
    private Long attributeId;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "value_string", columnDefinition = "text")
    private String valueString;

    @Column(name = "value_number", precision = 19, scale = 6)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "unit_code", length = 64)
    private String unitCode;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

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
}
