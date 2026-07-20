package ua.com.bravi.bravi.seller.stores.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "store_settings")
public class StoreSettingsEntity {

    /** Shared PK with the owning store (stores.id); assigned, not generated. */
    @Id
    @Column(name = "store_id")
    private Long storeId;

    /** Dictionary codes (WEIGHT_UNIT / DIMENSION_UNIT); validated via DictionariesApi, not an enum. */
    @Column(name = "default_weight_unit", nullable = false, length = 16)
    private String defaultWeightUnit;

    @Column(name = "default_dimension_unit", nullable = false, length = 16)
    private String defaultDimensionUnit;

    @Column(name = "default_currency", nullable = false, length = 3)
    private Currency defaultCurrency;

    @Column(name = "default_language", nullable = false, length = 8)
    private Locale defaultLanguage;

    @Column(name = "timezone", nullable = false, length = 64)
    private ZoneId timezone;

    @Column(name = "allow_return", nullable = false)
    private Boolean allowReturn = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_hours", columnDefinition = "jsonb")
    private WorkingHours workingHours;

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
