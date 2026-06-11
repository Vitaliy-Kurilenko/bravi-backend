package ua.com.bravi.bravi.catalog.categories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.catalog.categories.domain.Category;
import ua.com.bravi.bravi.catalog.categories.domain.CategoryStatus;
import ua.com.bravi.bravi.catalog.categories.exception.CategoryAlreadyExistsException;
import ua.com.bravi.bravi.catalog.categories.exception.CategoryHasChildrenException;
import ua.com.bravi.bravi.catalog.categories.persistence.ICategoryEntityRepository;
import ua.com.bravi.bravi.catalog.categories.persistence.entity.CategoryEntity;
import ua.com.bravi.bravi.catalog.categories.persistence.mapper.CategoryEntityMapper;
import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long STORE_ID = 7L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long CATEGORY_ID = 42L;

    private final ICategoryEntityRepository repository = mock(ICategoryEntityRepository.class);
    private final CategoryEntityMapper mapper = mock(CategoryEntityMapper.class);

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(repository, mapper);
    }

    private static CategoryEntity entityOwnedBy(Long storeId) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(CATEGORY_ID);
        entity.setStoreId(storeId);
        entity.setName("Shoes");
        return entity;
    }

    private static Category request(Long parentId, CategoryStatus status) {
        return new Category(null, null, parentId, "Shoes", null, status, null, null);
    }

    @Test
    void createRootDefaultsStatusToActiveWhenAbsent() {
        CategoryEntity entity = new CategoryEntity();
        entity.setStatus(null);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        service.create(STORE_ID, request(null, null));

        assertThat(entity.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(entity.getStoreId()).isEqualTo(STORE_ID);
    }

    @Test
    void createKeepsProvidedStatus() {
        CategoryEntity entity = new CategoryEntity();
        entity.setStatus(CategoryStatus.INACTIVE);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        service.create(STORE_ID, request(null, CategoryStatus.INACTIVE));

        assertThat(entity.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
    }

    @Test
    void createMapsDuplicateNameToConflict() {
        CategoryEntity entity = new CategoryEntity();
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new DataIntegrityViolationException("uq_categories_child_name"));

        assertThatThrownBy(() -> service.create(STORE_ID, request(null, null)))
                .isInstanceOf(CategoryAlreadyExistsException.class);
    }

    @Test
    void createUnderUnknownParentThrowsNotFound() {
        when(repository.findByStoreId(STORE_ID)).thenReturn(List.of());
        when(mapper.toDomain(List.of())).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(STORE_ID, request(123L, null)))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updateWithoutParentDoesNotResolveStoreTree() {
        CategoryEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));

        service.update(STORE_ID, CATEGORY_ID, request(null, CategoryStatus.INACTIVE));

        verify(mapper).updateEntity(entity, request(null, CategoryStatus.INACTIVE));
        verify(repository).flush();
        verify(repository, never()).findByStoreId(any());
    }

    @Test
    void updateRejectsCategoryOfAnotherStore() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(entityOwnedBy(OTHER_STORE_ID)));

        assertThatThrownBy(() -> service.update(STORE_ID, CATEGORY_ID, request(null, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void getByIdRejectsCategoryOfAnotherStore() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(entityOwnedBy(OTHER_STORE_ID)));

        assertThatThrownBy(() -> service.getById(STORE_ID, CATEGORY_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getByIdThrowsWhenAbsent() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(STORE_ID, CATEGORY_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteBlockedWhenCategoryHasChildren() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(entityOwnedBy(STORE_ID)));
        when(repository.existsByParentId(CATEGORY_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(STORE_ID, CATEGORY_ID))
                .isInstanceOf(CategoryHasChildrenException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRemovesLeafCategory() {
        CategoryEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
        when(repository.existsByParentId(CATEGORY_ID)).thenReturn(false);

        service.delete(STORE_ID, CATEGORY_ID);

        verify(repository).delete(entity);
    }
}
