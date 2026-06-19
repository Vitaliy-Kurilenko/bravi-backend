package ua.com.bravi.bravi.orders.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.orders.persistence.entity.OrderStatusEntity;

import java.util.Optional;

public interface IOrderStatusRepository extends JpaRepository<OrderStatusEntity, Long> {

    Optional<OrderStatusEntity> findByCode(String code);
}
