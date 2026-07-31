package ua.com.bravi.bravi.seller.orders.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ua.com.bravi.bravi.seller.orders.domain.DeliveryType;
import ua.com.bravi.bravi.seller.stores.domain.StoreOwned;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_store_id", columnList = "store_id"),
        @Index(name = "idx_orders_buyer_id", columnList = "buyer_id"),
        @Index(name = "idx_orders_status_id", columnList = "status_id"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})
public class OrderEntity implements StoreOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @NotNull
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @NotNull
    @Column(name = "status_id", nullable = false)
    private Long statusId;

    /** Read-only association used for joins and view projections; writes go through {@link #statusId}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", insertable = false, updatable = false)
    private OrderStatusEntity status;

    @NotNull
    @Column(name = "recipient_first_name", nullable = false)
    private String recipientFirstName;

    @NotNull
    @Column(name = "recipient_last_name", nullable = false)
    private String recipientLastName;

    @Column(name = "recipient_phone", length = 32)
    private String recipientPhone;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @NotNull
    @Column(name = "delivery_method_code", nullable = false, length = 64)
    private String deliveryMethodCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 32)
    private DeliveryType deliveryType;

    @Column(name = "delivery_country", length = 128)
    private String deliveryCountry;

    @Column(name = "delivery_region", length = 128)
    private String deliveryRegion;

    @Column(name = "delivery_city", length = 128)
    private String deliveryCity;

    @Column(name = "delivery_address", length = 512)
    private String deliveryAddress;

    @Column(name = "delivery_extra", length = 512)
    private String deliveryExtra;

    @Column(name = "delivery_warehouse_no", length = 64)
    private String deliveryWarehouseNo;

    @NotNull
    @Column(name = "payment_method_code", nullable = false, length = 64)
    private String paymentMethodCode;

    @Column(name = "prepayment", precision = 19, scale = 4)
    private BigDecimal prepayment;

    @NotNull
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @NotNull
    @Column(name = "discount_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @NotNull
    @Column(name = "shipping_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingTotal = BigDecimal.ZERO;

    @NotNull
    @Column(name = "total", nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "internal_comment", columnDefinition = "text")
    private String internalComment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderShipmentEntity shipment;

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

    public void addItem(OrderItemEntity item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void setShipment(OrderShipmentEntity shipment) {
        if (shipment != null) {
            shipment.setOrder(this);
        }
        this.shipment = shipment;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }
}
