package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.orders.api.OrdersApi;
import ua.com.bravi.bravi.orders.domain.OrderSearchQuery;
import ua.com.bravi.bravi.orders.domain.OrderSortBy;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderItemEditRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderItemRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.OrderUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.OrderPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.OrderResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.OrderStatusResponse;
import ua.com.bravi.bravi.seller.controller.mapper.OrderDtoMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.stores.api.CurrentStoreHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/orders")
@PreAuthorize("hasAuthority('role_seller')")
@Tag(name = "SellerOrderController")
@RequireStore
public class SellerOrderController {

    private final OrdersApi ordersApi;
    private final OrderDtoMapper orderDtoMapper;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "Search orders",
            description = "Returns a paginated, filtered and sorted list of the current store's orders")
    @GetMapping
    public OrderPageResponse searchOrders(
            @RequestParam(required = false) String search,
            @RequestParam(name = "buyer_ids", required = false) List<Long> buyerIds,
            @RequestParam(name = "payment_method_codes", required = false) List<String> paymentMethodCodes,
            @RequestParam(name = "delivery_method_codes", required = false) List<String> deliveryMethodCodes,
            @RequestParam(name = "recipient_name", required = false) String recipientName,
            @RequestParam(name = "recipient_phone", required = false) String recipientPhone,
            @RequestParam(name = "recipient_email", required = false) String recipientEmail,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "min_total", required = false) BigDecimal minTotal,
            @RequestParam(name = "max_total", required = false) BigDecimal maxTotal,
            @RequestParam(name = "created_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(name = "created_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        OrderSearchQuery query = new OrderSearchQuery(
                search, buyerIds, paymentMethodCodes, deliveryMethodCodes,
                recipientName, recipientPhone, recipientEmail, statuses,
                minTotal, maxTotal, createdFrom, createdTo,
                sortBy == null ? null : OrderSortBy.fromParam(sortBy), sortOrder, page, limit
        );
        return orderDtoMapper.toPageResponse(ordersApi.search(currentStoreHolder.get(), query));
    }

    @Operation(summary = "List order statuses", description = "Returns the extensible list of order statuses")
    @GetMapping("/statuses")
    public List<OrderStatusResponse> getStatuses() {
        return orderDtoMapper.toStatusResponses(ordersApi.listStatuses());
    }

    @Operation(summary = "Get order", description = "Returns a single order of the current store")
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderDtoMapper.toResponse(ordersApi.getById(currentStoreHolder.get(), orderId));
    }

    @Operation(summary = "Create order", description = "Creates an order in the current store")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        Long storeId = currentStoreHolder.get();
        Long orderId = ordersApi.create(storeId, orderDtoMapper.toDomain(request));
        OrderResponse body = orderDtoMapper.toResponse(ordersApi.getById(storeId, orderId));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Update order",
            description = "Partially updates an order: status, recipient, delivery, shipment or comments")
    @PatchMapping("/{orderId}")
    public OrderResponse updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderUpdateRequest request
    ) {
        Long storeId = currentStoreHolder.get();
        ordersApi.update(storeId, orderId, orderDtoMapper.toDomain(request));
        return orderDtoMapper.toResponse(ordersApi.getById(storeId, orderId));
    }

    @Operation(summary = "Delete order", description = "Deletes an order of the current store with its items")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        ordersApi.delete(currentStoreHolder.get(), orderId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add order item", description = "Adds a product line to the order; totals are recalculated")
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemRequest request
    ) {
        Long storeId = currentStoreHolder.get();
        ordersApi.addItem(storeId, orderId, orderDtoMapper.toItemEdit(request));
        OrderResponse body = orderDtoMapper.toResponse(ordersApi.getById(storeId, orderId));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Update order item",
            description = "Changes quantity / sale price, or replaces the product of a line; totals are recalculated")
    @PatchMapping("/{orderId}/items/{itemId}")
    public OrderResponse updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemEditRequest request
    ) {
        Long storeId = currentStoreHolder.get();
        ordersApi.updateItem(storeId, orderId, itemId, orderDtoMapper.toItemEdit(request));
        return orderDtoMapper.toResponse(ordersApi.getById(storeId, orderId));
    }

    @Operation(summary = "Delete order item",
            description = "Removes a line from the order (at least one line must remain); totals are recalculated")
    @DeleteMapping("/{orderId}/items/{itemId}")
    public OrderResponse deleteItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        Long storeId = currentStoreHolder.get();
        ordersApi.deleteItem(storeId, orderId, itemId);
        return orderDtoMapper.toResponse(ordersApi.getById(storeId, orderId));
    }
}
