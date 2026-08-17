package ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity;

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
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeScope;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeStatus;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;
import ua.com.bravi.bravi.seller.stores.domain.StoreOwned;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "store_attributes", indexes = {
        @Index(name = "idx_store_attributes_store_id", columnList = "store_id"),
        @Index(name = "idx_store_attributes_store_scope", columnList = "store_id, scope")
})
public class AttributeEntity implements StoreOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @NotNull
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /** Code of the library template this definition was adopted from, or null when seller-created. */
    @Column(name = "template_code", length = 64)
    private String templateCode;

    @NotNull
    @Column(name = "code", nullable = false, length = 64, updatable = false)
    private String code;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 32, updatable = false)
    private AttributeValueType valueType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private AttributeScope scope = AttributeScope.CATEGORY;

    @Column(name = "unit_dictionary_code", length = 64)
    private String unitDictionaryCode;

    @Column(name = "unit_default_code", length = 64)
    private String unitDefaultCode;

    @NotNull
    @Column(name = "variant_defining", nullable = false)
    private Boolean variantDefining = Boolean.FALSE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AttributeStatus status = AttributeStatus.ACTIVE;

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
