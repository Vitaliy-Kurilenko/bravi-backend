package ua.com.bravi.bravi.seller.stores.contacts.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ua.com.bravi.bravi.seller.stores.contacts.domain.ContactType;
import ua.com.bravi.bravi.seller.stores.domain.StoreOwned;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "store_contacts", indexes = {
        @Index(name = "idx_store_contacts_store_id", columnList = "store_id")
})
public class StoreContactEntity implements StoreOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ContactType type;

    @NotNull
    @Column(name = "value", nullable = false, length = 512)
    private String value;

    @Column(name = "comment", length = 512)
    private String comment;

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
