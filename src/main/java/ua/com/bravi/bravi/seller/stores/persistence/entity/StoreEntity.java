package ua.com.bravi.bravi.seller.stores.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;

@Getter
@Setter
@Entity
@Table(name = "stores", indexes = {
        @Index(name = "idx_stores_seller_account_id", columnList = "seller_account_id")
})
public class StoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @NotNull
    @Column(name = "seller_account_id", nullable = false)
    private Long sellerAccountId;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "country")
    private String country;

    @Column(name = "region")
    private String region;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code", length = 25)
    private String postalCode;

    @Column(name = "address")
    private String address;

    @Column(name = "address_additional")
    private String addressAdditional;

    @NotNull
    @Column(name = "timezone", nullable = false, length = 64)
    private ZoneId timezone = ZoneId.of("UTC");

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "logo_key", length = 512)
    private String logoKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_hours", columnDefinition = "jsonb")
    private WorkingHours workingHours;

    @NotNull
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @NotNull
    @Column(name = "allow_return", nullable = false)
    private Boolean allowReturn = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoreStatus status = StoreStatus.ACTIVE;

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
