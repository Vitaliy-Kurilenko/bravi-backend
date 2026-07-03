package ua.com.bravi.bravi.seller.account.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ua.com.bravi.bravi.seller.account.domain.SellerOnboardingStatus;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "seller_accounts")
public class SellerAccountEntity {

    /** Shared PK with the owning ACCOUNT (accounts.id); assigned, not generated. */
    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "legal_name")
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false)
    private SellerOnboardingStatus onboardingStatus;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "phone", length = 64)
    private String phone;

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
