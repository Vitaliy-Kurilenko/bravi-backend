package ua.com.bravi.bravi.orders.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ua.com.bravi.bravi.orders.persistence.entity.OrderEntity;

public interface IOrderEntityRepository
        extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {
}
