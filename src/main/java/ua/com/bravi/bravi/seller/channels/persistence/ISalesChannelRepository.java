package ua.com.bravi.bravi.seller.channels.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.channels.domain.SalesChannelType;
import ua.com.bravi.bravi.seller.channels.persistence.entity.SalesChannelEntity;

public interface ISalesChannelRepository extends JpaRepository<SalesChannelEntity, Long> {

    boolean existsByStoreIdAndType(Long storeId, SalesChannelType type);
}
