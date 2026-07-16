package ua.com.bravi.bravi.dictionaries.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.dictionaries.persistence.entity.DictionaryEntity;

import java.util.Optional;

public interface IDictionaryRepository extends JpaRepository<DictionaryEntity, Long> {

    Optional<DictionaryEntity> findByCode(String code);
}
