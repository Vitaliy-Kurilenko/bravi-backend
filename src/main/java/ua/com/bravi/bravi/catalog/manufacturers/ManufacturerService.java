package ua.com.bravi.bravi.catalog.manufacturers;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.catalog.manufacturers.domain.Manufacturer;
import ua.com.bravi.bravi.catalog.manufacturers.domain.ManufacturerStatus;
import ua.com.bravi.bravi.catalog.manufacturers.exception.ManufacturerAlreadyExistsException;
import ua.com.bravi.bravi.catalog.manufacturers.persistence.IManufacturerEntityRepository;
import ua.com.bravi.bravi.catalog.manufacturers.persistence.entity.ManufacturerEntity;
import ua.com.bravi.bravi.catalog.manufacturers.persistence.mapper.ManufacturerEntityMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManufacturerService implements ManufacturersApi {

    private final IManufacturerEntityRepository manufacturerRepository;
    private final ManufacturerEntityMapper manufacturerEntityMapper;

    @Override
    public List<ManufacturerView> findByStoreId(Long storeId) {
        return manufacturerEntityMapper.toViews(manufacturerRepository.findByStoreId(storeId));
    }

    @Override
    public ManufacturerView getById(Long storeId, Long manufacturerId) {
        return manufacturerEntityMapper.toView(requireOwned(storeId, manufacturerId));
    }

    @Override
    @Transactional
    public Long create(Long storeId, Manufacturer manufacturer) {
        ManufacturerEntity entity = manufacturerEntityMapper.toEntity(manufacturer);
        entity.setStoreId(storeId);
        if (entity.getStatus() == null) {
            entity.setStatus(ManufacturerStatus.ACTIVE);
        }
        try {
            return manufacturerRepository.save(entity).getId();
        } catch (DataIntegrityViolationException duplicateName) {
            throw new ManufacturerAlreadyExistsException("Manufacturer with this name already exists in the store");
        }
    }

    @Override
    @Transactional
    public void update(Long storeId, Long manufacturerId, Manufacturer patch) {
        ManufacturerEntity entity = requireOwned(storeId, manufacturerId);
        manufacturerEntityMapper.updateEntity(entity, patch);
        try {
            manufacturerRepository.flush();
        } catch (DataIntegrityViolationException duplicateName) {
            throw new ManufacturerAlreadyExistsException("Manufacturer with this name already exists in the store");
        }
    }

    @Override
    @Transactional
    public void delete(Long storeId, Long manufacturerId) {
        manufacturerRepository.delete(requireOwned(storeId, manufacturerId));
    }

    private ManufacturerEntity requireOwned(Long storeId, Long manufacturerId) {
        ManufacturerEntity entity = manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> new NotFoundException("Manufacturer not found"));
        entity.requireOwnedBy(storeId);
        return entity;
    }
}
