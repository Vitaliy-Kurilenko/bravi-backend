package ua.com.bravi.bravi.seller.orders;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductsApi;
import ua.com.bravi.bravi.seller.orders.api.OrderPage;
import ua.com.bravi.bravi.seller.orders.api.OrderStatusView;
import ua.com.bravi.bravi.seller.orders.api.OrderView;
import ua.com.bravi.bravi.seller.orders.api.OrdersApi;
import ua.com.bravi.bravi.seller.orders.domain.Order;
import ua.com.bravi.bravi.seller.orders.domain.OrderItem;
import ua.com.bravi.bravi.seller.orders.domain.OrderItemEdit;
import ua.com.bravi.bravi.seller.orders.domain.OrderSearchQuery;
import ua.com.bravi.bravi.seller.orders.domain.OrderSortBy;
import ua.com.bravi.bravi.seller.orders.domain.OrderTotals;
import ua.com.bravi.bravi.seller.orders.exception.InvalidOrderRequestException;
import ua.com.bravi.bravi.seller.orders.exception.OrderNotFoundException;
import ua.com.bravi.bravi.seller.orders.persistence.IOrderEntityRepository;
import ua.com.bravi.bravi.seller.orders.persistence.IOrderStatusRepository;
import ua.com.bravi.bravi.seller.orders.persistence.OrderSpecifications;
import ua.com.bravi.bravi.seller.orders.persistence.entity.OrderEntity;
import ua.com.bravi.bravi.seller.orders.persistence.entity.OrderItemEntity;
import ua.com.bravi.bravi.seller.orders.persistence.entity.OrderStatusEntity;
import ua.com.bravi.bravi.seller.orders.persistence.mapper.OrderEntityMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.delivery.api.DeliveryApi;
import ua.com.bravi.bravi.seller.stores.payments.api.PaymentsApi;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.api.IdentityApi;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService implements OrdersApi {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String DEFAULT_STATUS_CODE = "NEW";
    private static final String BUYER_TYPE = "BUYER";

    private final IOrderEntityRepository orderRepository;
    private final IOrderStatusRepository orderStatusRepository;
    private final OrderEntityMapper orderEntityMapper;
    private final IdentityApi usersApi;
    private final ProductsApi productsApi;
    private final PaymentsApi paymentsApi;
    private final DeliveryApi deliveryApi;

    @Override
    public OrderPage search(Long storeId, OrderSearchQuery query) {
        int page = Math.max(query.page(), 1);
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        OrderSortBy sortBy = query.sortBy() != null ? query.sortBy() : OrderSortBy.CREATED_AT;
        SortOrder sortOrder = query.sortOrder() != null ? query.sortOrder() : SortOrder.DESC;

        Sort.Direction direction = sortOrder == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy.getProperty()));

        Page<OrderEntity> result = orderRepository.findAll(OrderSpecifications.forStore(storeId, query), pageable);
        List<OrderView> data = result.getContent().stream()
                .map(orderEntityMapper::toView)
                .toList();

        int pages = (int) Math.ceil((double) result.getTotalElements() / limit);
        return new OrderPage(data, data.size(), result.getTotalElements(), limit, pages, page, sortBy, sortOrder);
    }

    @Override
    public OrderView getById(Long storeId, Long orderId) {
        return orderEntityMapper.toView(requireOwned(storeId, orderId));
    }

    @Override
    @Transactional
    public Long create(Long storeId, Order order) {
        Long buyerId = requireBuyer(order.buyerId());
        requireEnabledPayment(storeId, order.paymentMethodCode());
        requireEnabledDelivery(storeId, order.deliveryMethodCode());
        OrderStatusEntity status = orderStatusRepository.findByCode(DEFAULT_STATUS_CODE)
                .orElseThrow(() -> new NotFoundException("Default order status is not configured"));

        List<OrderItem> items = snapshotItems(storeId, order.items());
        BigDecimal subtotal = OrderTotals.subtotal(items);
        BigDecimal discountTotal = nvl(order.discountTotal());
        BigDecimal shippingTotal = nvl(order.shippingTotal());
        BigDecimal total = OrderTotals.total(subtotal, discountTotal, shippingTotal);

        OrderEntity entity = orderEntityMapper.toEntity(order);
        entity.setStoreId(storeId);
        entity.setBuyerId(buyerId);
        entity.setStatusId(status.getId());
        entity.setSubtotal(subtotal);
        entity.setDiscountTotal(discountTotal);
        entity.setShippingTotal(shippingTotal);
        entity.setTotal(total);
        items.forEach(item -> entity.addItem(orderEntityMapper.toItemEntity(item)));

        return orderRepository.save(entity).getId();
    }

    @Override
    @Transactional
    public void update(Long storeId, Long orderId, Order patch) {
        OrderEntity entity = requireOwned(storeId, orderId);
        if (patch.statusId() != null) {
            OrderStatusEntity status = orderStatusRepository.findById(patch.statusId())
                    .orElseThrow(() -> new InvalidOrderRequestException("status_id", "Unknown order status"));
            entity.setStatusId(patch.statusId());
            entity.setStatus(status);
        }
        orderEntityMapper.updateEntity(entity, patch);
        orderRepository.flush();
    }

    @Override
    @Transactional
    public void delete(Long storeId, Long orderId) {
        orderRepository.delete(requireOwned(storeId, orderId)); // items + shipment знімаються каскадом
    }

    @Override
    @Transactional
    public void addItem(Long storeId, Long orderId, OrderItemEdit item) {
        OrderEntity entity = requireOwned(storeId, orderId);
        OrderItem snapshot = snapshotItem(storeId, item.productId(), item.quantity(), item.salePrice());
        entity.addItem(orderEntityMapper.toItemEntity(snapshot));
        recomputeTotals(entity);
        orderRepository.flush();
    }

    @Override
    @Transactional
    public void updateItem(Long storeId, Long orderId, Long itemId, OrderItemEdit patch) {
        OrderEntity entity = requireOwned(storeId, orderId);
        OrderItemEntity item = requireItem(entity, itemId);

        if (patch.productId() != null && !patch.productId().equals(item.getProductId())) {
            ProductView product = requireProduct(storeId, patch.productId());
            item.setProductId(product.id());
            item.setSku(product.sku());
            item.setCode(product.code());
            item.setName(product.name());
            item.setPartnerPrice(product.partnerPrice());
            item.setSalePrice(patch.salePrice() != null ? patch.salePrice() : product.recommendedPrice());
        } else if (patch.salePrice() != null) {
            item.setSalePrice(patch.salePrice());
        }
        if (patch.quantity() != null) {
            item.setQuantity(patch.quantity());
        }
        recomputeTotals(entity);
        orderRepository.flush();
    }

    @Override
    @Transactional
    public void deleteItem(Long storeId, Long orderId, Long itemId) {
        OrderEntity entity = requireOwned(storeId, orderId);
        OrderItemEntity item = requireItem(entity, itemId);
        if (entity.getItems().size() <= 1) {
            throw new InvalidOrderRequestException("items", "Order must contain at least one item");
        }
        entity.getItems().remove(item); // orphanRemoval видаляє рядок
        recomputeTotals(entity);
        orderRepository.flush();
    }

    @Override
    public List<OrderStatusView> listStatuses() {
        return orderEntityMapper.toStatusViews(orderStatusRepository.findAll());
    }

    private List<OrderItem> snapshotItems(Long storeId, List<OrderItem> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new InvalidOrderRequestException("items", "Order must contain at least one item");
        }
        return requested.stream()
                .map(item -> snapshotItem(storeId, item.productId(), item.quantity(), item.salePrice()))
                .toList();
    }

    /** Бере актуальні дані товару й фіксує снапшот позиції; salePrice за замовч. — recommended_price товару. */
    private OrderItem snapshotItem(Long storeId, Long productId, Integer quantity, BigDecimal salePrice) {
        ProductView product = requireProduct(storeId, productId);
        BigDecimal price = salePrice != null ? salePrice : product.recommendedPrice();
        return new OrderItem(null, product.id(), product.sku(), product.code(), product.name(),
                quantity, product.partnerPrice(), price, null, null);
    }

    private void recomputeTotals(OrderEntity entity) {
        BigDecimal subtotal = OrderTotals.subtotal(orderEntityMapper.toItemDomains(entity.getItems()));
        entity.setSubtotal(subtotal);
        entity.setTotal(OrderTotals.total(subtotal, entity.getDiscountTotal(), entity.getShippingTotal()));
    }

    private OrderItemEntity requireItem(OrderEntity entity, Long itemId) {
        return entity.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new OrderNotFoundException("Order item not found"));
    }

    private ProductView requireProduct(Long storeId, Long productId) {
        if (productId == null) {
            throw new InvalidOrderRequestException("items", "Each order item requires a product_id");
        }
        try {
            return productsApi.getById(storeId, productId);
        } catch (NotFoundException notFound) {
            throw new InvalidOrderRequestException("items", "Product not found: " + productId);
        }
    }

    private Long requireBuyer(Long buyerId) {
        if (buyerId == null) {
            throw new InvalidOrderRequestException("buyer_id", "Buyer is required");
        }
        CurrentUserView buyer;
        try {
            buyer = usersApi.getById(buyerId);
        } catch (NotFoundException notFound) {
            throw new InvalidOrderRequestException("buyer_id", "Buyer not found");
        }
        if (!BUYER_TYPE.equalsIgnoreCase(buyer.type())) {
            throw new InvalidOrderRequestException("buyer_id", "User is not a buyer");
        }
        return buyerId;
    }

    private void requireEnabledPayment(Long storeId, String methodCode) {
        if (methodCode == null) {
            throw new InvalidOrderRequestException("payment_method_code", "Payment method is required");
        }
        boolean enabled = paymentsApi.findEnabledByStoreId(storeId).stream()
                .anyMatch(method -> method.methodCode().equals(methodCode));
        if (!enabled) {
            throw new InvalidOrderRequestException("payment_method_code",
                    "Payment method is not enabled for the store");
        }
    }

    private void requireEnabledDelivery(Long storeId, String methodCode) {
        if (methodCode == null) {
            throw new InvalidOrderRequestException("delivery_method_code", "Delivery method is required");
        }
        boolean enabled = deliveryApi.findEnabledByStoreId(storeId).stream()
                .anyMatch(method -> method.methodCode().equals(methodCode));
        if (!enabled) {
            throw new InvalidOrderRequestException("delivery_method_code",
                    "Delivery method is not enabled for the store");
        }
    }

    private OrderEntity requireOwned(Long storeId, Long orderId) {
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        entity.requireOwnedBy(storeId);
        return entity;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
