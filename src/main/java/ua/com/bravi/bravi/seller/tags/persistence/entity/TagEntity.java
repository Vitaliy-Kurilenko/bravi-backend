package ua.com.bravi.bravi.seller.tags.persistence.entity;

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
import ua.com.bravi.bravi.seller.stores.domain.StoreOwned;
import ua.com.bravi.bravi.seller.tags.domain.TagColor;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "store_tags", indexes = {
        @Index(name = "idx_store_tags_store_target", columnList = "store_id, target")
})
public class TagEntity implements StoreOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @NotNull
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false, length = 32)
    private TagTarget target;

    @NotNull
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @NotNull
    @Column(name = "color", nullable = false, length = TagColor.LENGTH)
    private String color;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TagStatus status = TagStatus.ACTIVE;

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
