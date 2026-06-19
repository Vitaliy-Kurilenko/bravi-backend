package ua.com.bravi.bravi.orders.api;

import ua.com.bravi.bravi.orders.domain.Order;
import ua.com.bravi.bravi.orders.domain.OrderItemEdit;
import ua.com.bravi.bravi.orders.domain.OrderSearchQuery;

import java.util.List;

public interface OrdersApi {

    OrderPage search(Long storeId, OrderSearchQuery query);

    OrderView getById(Long storeId, Long orderId);

    Long create(Long storeId, Order order);

    void update(Long storeId, Long orderId, Order patch);

    void delete(Long storeId, Long orderId);

    void addItem(Long storeId, Long orderId, OrderItemEdit item);

    void updateItem(Long storeId, Long orderId, Long itemId, OrderItemEdit patch);

    void deleteItem(Long storeId, Long orderId, Long itemId);

    List<OrderStatusView> listStatuses();
}
