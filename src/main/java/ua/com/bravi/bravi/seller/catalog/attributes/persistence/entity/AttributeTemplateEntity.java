package ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValueType;

import java.time.Instant;

/** Platform-wide library entry a store copies into its own definitions. Seeded by Flyway, read-only. */
@Getter
@Setter
@Entity
@Table(name = "attribute_templates")
public class AttributeTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 32)
    private AttributeValueType valueType;

    @Column(name = "unit_dictionary_code", length = 64)
    private String unitDictionaryCode;

    @Column(name = "unit_default_code", length = 64)
    private String unitDefaultCode;

    @NotNull
    @Column(name = "variant_defining", nullable = false)
    private Boolean variantDefining = Boolean.FALSE;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

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
