package ua.com.bravi.bravi.seller.account.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.seller.account.persistence.entity.SellerAccountEntity;

public interface ISellerAccountRepository extends JpaRepository<SellerAccountEntity, Long> {
}
