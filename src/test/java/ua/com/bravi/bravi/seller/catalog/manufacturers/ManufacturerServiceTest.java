package ua.com.bravi.bravi.seller.catalog.manufacturers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerPage;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.Manufacturer;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSearchQuery;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSortBy;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;
import ua.com.bravi.bravi.seller.catalog.manufacturers.exception.ManufacturerAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.IManufacturerEntityRepository;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.entity.ManufacturerEntity;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.mapper.ManufacturerEntityMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {

    private static final Long STORE_ID = 7L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long MANUFACTURER_ID = 42L;
    private static final String PUBLIC_ID = "mfr_K9mP2xQa7LwZ8tBn";

    private final IManufacturerEntityRepository repository = mock(IManufacturerEntityRepository.class);
    private final ManufacturerEntityMapper mapper = mock(ManufacturerEntityMapper.class);

    private ManufacturerService service;

    @BeforeEach
    void setUp() {
        service = new ManufacturerService(repository, mapper);
    }

    private static ManufacturerEntity entityOwnedBy(Long storeId) {
        ManufacturerEntity entity = new ManufacturerEntity();
        entity.setId(MANUFACTURER_ID);
        entity.setPublicId(PUBLIC_ID);
        entity.setStoreId(storeId);
        entity.setName("ACME");
        return entity;
    }

    @Test
    void createSetsStoreIdAndPublicIdAndReturnsView() {
        ManufacturerEntity entity = new ManufacturerEntity();
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenAnswer(invocation -> {
            entity.setId(MANUFACTURER_ID);
            return entity;
        });

        service.create(STORE_ID,
                new Manufacturer(null, null, null, "ACME", "desc", ManufacturerStatus.ACTIVE, null, null));

        assertThat(entity.getStoreId()).isEqualTo(STORE_ID);
        assertThat(entity.getPublicId()).startsWith("mfr_");
        verify(repository).save(entity);
        verify(mapper).toView(entity);
    }

    @Test
    void createDefaultsStatusToActiveWhenAbsent() {
        ManufacturerEntity entity = new ManufacturerEntity();
        entity.setStatus(null); // simulate domain without status -> MapStruct produced null
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        service.create(STORE_ID, new Manufacturer(null, null, null, "ACME", null, null, null, null));

        assertThat(entity.getStatus()).isEqualTo(ManufacturerStatus.ACTIVE);
    }

    @Test
    void createKeepsProvidedStatus() {
        ManufacturerEntity entity = new ManufacturerEntity();
        entity.setStatus(ManufacturerStatus.INACTIVE);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        service.create(STORE_ID,
                new Manufacturer(null, null, null, "ACME", null, ManufacturerStatus.INACTIVE, null, null));

        assertThat(entity.getStatus()).isEqualTo(ManufacturerStatus.INACTIVE);
    }

    @Test
    void createMapsDuplicateNameToConflict() {
        ManufacturerEntity entity = new ManufacturerEntity();
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new DataIntegrityViolationException("uq_store_manufacturers_store_name"));

        assertThatThrownBy(() -> service.create(STORE_ID,
                new Manufacturer(null, null, null, "ACME", null, null, null, null)))
                .isInstanceOf(ManufacturerAlreadyExistsException.class);
    }

    @Test
    void searchAppliesDefaultsAndMapsPage() {
        ManufacturerEntity entity = entityOwnedBy(STORE_ID);
        Page<ManufacturerEntity> repoPage = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repoPage);
        when(mapper.toViews(List.of(entity))).thenReturn(List.of(
                new ManufacturerView(MANUFACTURER_ID, PUBLIC_ID, STORE_ID, "ACME", null,
                        ManufacturerStatus.ACTIVE, null, null)));

        ManufacturerPage page = service.search(STORE_ID,
                new ManufacturerSearchQuery(null, null, null, null, 0, 0));

        assertThat(page.data()).hasSize(1);
        assertThat(page.limit()).isEqualTo(20);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.count()).isEqualTo(1);
        assertThat(page.pages()).isEqualTo(1);
        assertThat(page.sortBy()).isEqualTo(ManufacturerSortBy.CREATED_AT);
        assertThat(page.sortOrder()).isEqualTo(SortOrder.DESC);
    }

    @Test
    void searchClampsLimitAndBuildsRequestedSort() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        service.search(STORE_ID,
                new ManufacturerSearchQuery("acme", List.of(ManufacturerStatus.ACTIVE),
                        ManufacturerSortBy.NAME, SortOrder.ASC, 1, 500));

        ArgumentCaptor<Pageable> pageable = forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100); // clamped to MAX_LIMIT
        assertThat(pageable.getValue().getSort().getOrderFor("name")).isNotNull();
        assertThat(pageable.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
    }

    @Test
    void getByIdReturnsViewForOwnedManufacturer() {
        ManufacturerEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findById(MANUFACTURER_ID)).thenReturn(Optional.of(entity));

        service.getById(STORE_ID, MANUFACTURER_ID);

        verify(mapper).toView(entity);
    }

    @Test
    void getByIdRejectsManufacturerOfAnotherStore() {
        when(repository.findById(MANUFACTURER_ID)).thenReturn(Optional.of(entityOwnedBy(OTHER_STORE_ID)));

        assertThatThrownBy(() -> service.getById(STORE_ID, MANUFACTURER_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getByIdThrowsWhenAbsent() {
        when(repository.findById(MANUFACTURER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(STORE_ID, MANUFACTURER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByPublicIdReturnsViewForOwnedManufacturer() {
        ManufacturerEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.of(entity));

        service.getByPublicId(STORE_ID, PUBLIC_ID);

        verify(mapper).toView(entity);
    }

    @Test
    void getByPublicIdThrowsWhenAbsentForStore() {
        when(repository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByPublicId(STORE_ID, PUBLIC_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateAppliesPatchToOwnedManufacturer() {
        ManufacturerEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.of(entity));
        Manufacturer patch = new Manufacturer(null, null, null, "New name", null, ManufacturerStatus.INACTIVE, null, null);

        service.update(STORE_ID, PUBLIC_ID, patch);

        verify(mapper).updateEntity(entity, patch);
        verify(repository).flush();
    }

    @Test
    void updateThrowsWhenManufacturerNotFoundForStore() {
        when(repository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(STORE_ID, PUBLIC_ID,
                new Manufacturer(null, null, null, "x", null, null, null, null)))
                .isInstanceOf(NotFoundException.class);

        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void deleteRemovesOwnedManufacturer() {
        ManufacturerEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.of(entity));

        service.delete(STORE_ID, PUBLIC_ID);

        ArgumentCaptor<ManufacturerEntity> captor = ArgumentCaptor.forClass(ManufacturerEntity.class);
        verify(repository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(entity);
    }

    @Test
    void deleteThrowsWhenManufacturerNotFoundForStore() {
        when(repository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(STORE_ID, PUBLIC_ID))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).delete(any(ManufacturerEntity.class));
    }
}
