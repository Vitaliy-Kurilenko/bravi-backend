package ua.com.bravi.bravi.dictionaries.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.dictionaries.persistence.entity.DictionaryEntity;
import ua.com.bravi.bravi.dictionaries.persistence.entity.DictionaryItemEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DictionaryItemRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private IDictionaryRepository dictionaryRepository;

    @Autowired
    private IDictionaryItemRepository repository;

    private DictionaryEntity newDictionary() {
        DictionaryEntity dictionary = new DictionaryEntity();
        dictionary.setCode("TEST_" + UUID.randomUUID());
        dictionary.setName("Тестовий довідник");
        return dictionaryRepository.saveAndFlush(dictionary);
    }

    private static DictionaryItemEntity newItem(DictionaryEntity dictionary, String code, int sortOrder) {
        DictionaryItemEntity item = new DictionaryItemEntity();
        item.setDictionary(dictionary);
        item.setCode(code);
        item.setName("Елемент " + code);
        item.setSortOrder(sortOrder);
        return item;
    }

    @Test
    void filtersInactiveAndOrdersBySortOrder() {
        DictionaryEntity dictionary = newDictionary();
        repository.saveAndFlush(newItem(dictionary, "SECOND", 20));
        repository.saveAndFlush(newItem(dictionary, "FIRST", 10));
        DictionaryItemEntity inactive = newItem(dictionary, "HIDDEN", 5);
        inactive.setActive(false);
        repository.saveAndFlush(inactive);

        List<DictionaryItemEntity> items =
                repository.findByDictionaryIdAndActiveTrueOrderBySortOrderAscCodeAsc(dictionary.getId());

        assertThat(items).extracting(DictionaryItemEntity::getCode).containsExactly("FIRST", "SECOND");
    }

    @Test
    void enforcesUniqueCodePerDictionary() {
        DictionaryEntity dictionary = newDictionary();
        repository.saveAndFlush(newItem(dictionary, "DUP", 10));

        assertThatThrownBy(() -> repository.saveAndFlush(newItem(dictionary, "DUP", 20)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roundTripsJsonbMeta() {
        DictionaryEntity dictionary = newDictionary();
        DictionaryItemEntity item = newItem(dictionary, "META", 10);
        item.setMeta(Map.of("symbol", "₴", "numeric_code", "980"));
        Long id = repository.saveAndFlush(item).getId();

        DictionaryItemEntity loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getMeta())
                .containsEntry("symbol", "₴")
                .containsEntry("numeric_code", "980");
    }

    @Test
    void prePersistSetsCreatedAt() {
        DictionaryItemEntity saved = repository.saveAndFlush(newItem(newDictionary(), "TS", 10));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
    }

    @Test
    void existsChecksDictionaryCodeItemCodeAndActive() {
        DictionaryEntity dictionary = newDictionary();
        repository.saveAndFlush(newItem(dictionary, "ACTIVE", 10));
        DictionaryItemEntity inactive = newItem(dictionary, "INACTIVE", 20);
        inactive.setActive(false);
        repository.saveAndFlush(inactive);

        assertThat(repository.existsByDictionary_CodeAndCodeAndActiveTrue(dictionary.getCode(), "ACTIVE")).isTrue();
        assertThat(repository.existsByDictionary_CodeAndCodeAndActiveTrue(dictionary.getCode(), "INACTIVE")).isFalse();
        assertThat(repository.existsByDictionary_CodeAndCodeAndActiveTrue("NOPE", "ACTIVE")).isFalse();
    }
}
