package ua.com.bravi.bravi.seller.stores.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "store_settings")
public class StoreSettingsEntity {

    /** Shared PK with the owning store (stores.id); assigned, not generated. */
    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "default_weight_unit", length = 16)
    private String defaultWeightUnit;

    @Column(name = "default_dimension_unit", length = 16)
    private String defaultDimensionUnit;

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    @Column(name = "default_language", length = 8)
    private String defaultLanguage;

    @Column(name = "timezone", length = 64)
    private String timezone;

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
