package ua.com.bravi.bravi.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.persistance.entity.UserEntity;

public interface IUserEntityRepository extends JpaRepository<UserEntity, Long> {
}