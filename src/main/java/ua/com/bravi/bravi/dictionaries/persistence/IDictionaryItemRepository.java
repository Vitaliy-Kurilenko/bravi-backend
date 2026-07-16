package ua.com.bravi.bravi.dictionaries.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.bravi.bravi.dictionaries.persistence.entity.DictionaryItemEntity;

import java.util.List;

public interface IDictionaryItemRepository extends JpaRepository<DictionaryItemEntity, Long> {

    List<DictionaryItemEntity> findByDictionaryIdAndActiveTrueOrderBySortOrderAscCodeAsc(Long dictionaryId);

    boolean existsByDictionary_CodeAndCodeAndActiveTrue(String dictionaryCode, String code);
}
