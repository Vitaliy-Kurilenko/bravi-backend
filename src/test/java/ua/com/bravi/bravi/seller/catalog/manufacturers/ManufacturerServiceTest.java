package ua.com.bravi.bravi.seller.catalog.manufacturers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.Manufacturer;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;
import ua.com.bravi.bravi.seller.catalog.manufacturers.exception.ManufacturerAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.IManufacturerEntityRepository;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.entity.ManufacturerEntity;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.mapper.ManufacturerEntityMapper;
import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        entity.setStoreId(storeId);
        entity.setName("ACME");
        return entity;
    }

    @Test
    void createSetsStoreIdAndReturnsGeneratedId() {
        ManufacturerEntity entity = new ManufacturerEntity();
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenAnswer(invocation -> {
            entity.setId(MANUFACTURER_ID);
            return entity;
        });

        Long id = service.create(STORE_ID,
                new Manufacturer(null, null, "ACME", "desc", ManufacturerStatus.ACTIVE, null, null));

        assertThat(id).isEqualTo(MANUFACTURER_ID);
        assertThat(entity.getStoreId()).isEqualTo(STORE_ID);
        verify(repository).save(entity);
    }

    @Test
    void createDefaultsStatusToActiveWhenAbsent() {
        ManufacturerEntity entity = new ManufacturerEntity();
        entity.setStatus(null); // simulate domain without status -> MapStruct produced null
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        service.create(STORE_ID, new Manufacturer(null, null, "ACME", null, null, null, null));

        assertThat(entity.getStatus()).isEqualTo(ManufacturerStatus.ACTIVE);
    }

    @Test
    void createKeepsProvidedStatus() {
        ManufacturerEntity entity = new ManufacturerEntity();
        entity.setStatus(ManufacturerStatus.INACTIVE);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        service.create(STORE_ID,
                new Manufacturer(null, null, "ACME", null, ManufacturerStatus.INACTIVE, null, null));

        assertThat(entity.getStatus()).isEqualTo(ManufacturerStatus.INACTIVE);
    }

    @Test
    void createMapsDuplicateNameToConflict() {
        ManufacturerEntity entity = new ManufacturerEntity();
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new DataIntegrityViolationException("uq_manufacturers_store_name"));

        assertThatThrownBy(() -> service.create(STORE_ID,
                new Manufacturer(null, null, "ACME", null, null, null, null)))
                .isInstanceOf(ManufacturerAlreadyExistsException.class);
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
    void updateAppliesPatchToOwnedManufacturer() {
        ManufacturerEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findById(MANUFACTURER_ID)).thenReturn(Optional.of(entity));
        Manufacturer patch = new Manufacturer(null, null, "New name", null, ManufacturerStatus.INACTIVE, null, null);

        service.update(STORE_ID, MANUFACTURER_ID, patch);

        verify(mapper).updateEntity(entity, patch);
        verify(repository).flush();
    }

    @Test
    void updateRejectsManufacturerOfAnotherStore() {
        when(repository.findById(MANUFACTURER_ID)).thenReturn(Optional.of(entityOwnedBy(OTHER_STORE_ID)));

        assertThatThrownBy(() -> service.update(STORE_ID, MANUFACTURER_ID,
                new Manufacturer(null, null, "x", null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void deleteRemovesOwnedManufacturer() {
        ManufacturerEntity entity = entityOwnedBy(STORE_ID);
        when(repository.findById(MANUFACTURER_ID)).thenReturn(Optional.of(entity));

        service.delete(STORE_ID, MANUFACTURER_ID);

        ArgumentCaptor<ManufacturerEntity> captor = ArgumentCaptor.forClass(ManufacturerEntity.class);
        verify(repository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(entity);
    }

    @Test
    void deleteRejectsManufacturerOfAnotherStore() {
        when(repository.findById(MANUFACTURER_ID)).thenReturn(Optional.of(entityOwnedBy(OTHER_STORE_ID)));

        assertThatThrownBy(() -> service.delete(STORE_ID, MANUFACTURER_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).delete(any());
    }
}
