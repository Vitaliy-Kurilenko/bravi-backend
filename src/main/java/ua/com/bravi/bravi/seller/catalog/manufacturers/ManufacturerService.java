package ua.com.bravi.bravi.seller.catalog.manufacturers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerPage;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.Manufacturer;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSearchQuery;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSortBy;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;
import ua.com.bravi.bravi.seller.catalog.manufacturers.exception.ManufacturerAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.IManufacturerEntityRepository;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.ManufacturerSpecifications;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.entity.ManufacturerEntity;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.mapper.ManufacturerEntityMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManufacturerService implements ManufacturersApi {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final IManufacturerEntityRepository manufacturerRepository;
    private final ManufacturerEntityMapper manufacturerEntityMapper;

    @Override
    public ManufacturerPage search(Long storeId, ManufacturerSearchQuery query) {
        int page = Math.max(query.page(), 1);
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        ManufacturerSortBy sortBy = query.sortBy() != null ? query.sortBy() : ManufacturerSortBy.CREATED_AT;
        SortOrder sortOrder = query.sortOrder() != null ? query.sortOrder() : SortOrder.DESC;

        Sort.Direction direction = sortOrder == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy.getProperty()));

        Page<ManufacturerEntity> result =
                manufacturerRepository.findAll(ManufacturerSpecifications.forStore(storeId, query), pageable);
        List<ManufacturerView> data = manufacturerEntityMapper.toViews(result.getContent());

        int pages = (int) Math.ceil((double) result.getTotalElements() / limit);
        return new ManufacturerPage(data, data.size(), result.getTotalElements(), limit, pages, page, sortBy, sortOrder);
    }

    @Override
    public ManufacturerView getById(Long storeId, Long manufacturerId) {
        ManufacturerEntity entity = manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> new NotFoundException("Manufacturer not found"));
        entity.requireOwnedBy(storeId);
        return manufacturerEntityMapper.toView(entity);
    }

    @Override
    public ManufacturerView getByPublicId(Long storeId, String publicId) {
        return manufacturerEntityMapper.toView(requireOwned(storeId, publicId));
    }

    @Override
    @Transactional
    public ManufacturerView create(Long storeId, Manufacturer manufacturer) {
        ManufacturerEntity entity = manufacturerEntityMapper.toEntity(manufacturer);
        entity.setStoreId(storeId);
        entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.MANUFACTURER_PREFIX));
        if (entity.getStatus() == null) {
            entity.setStatus(ManufacturerStatus.ACTIVE);
        }
        try {
            ManufacturerEntity saved = manufacturerRepository.save(entity);
            log.info("Manufacturer created storeId={} manufacturerId={} publicId={}",
                    storeId, saved.getId(), saved.getPublicId());
            return manufacturerEntityMapper.toView(saved);
        } catch (DataIntegrityViolationException duplicateName) {
            throw new ManufacturerAlreadyExistsException("Manufacturer with this name already exists in the store");
        }
    }

    @Override
    @Transactional
    public void update(Long storeId, String publicId, Manufacturer patch) {
        ManufacturerEntity entity = requireOwned(storeId, publicId);
        manufacturerEntityMapper.updateEntity(entity, patch);
        try {
            manufacturerRepository.flush();
        } catch (DataIntegrityViolationException duplicateName) {
            throw new ManufacturerAlreadyExistsException("Manufacturer with this name already exists in the store");
        }
        log.info("Manufacturer updated storeId={} publicId={}", storeId, publicId);
    }

    @Override
    @Transactional
    public void delete(Long storeId, String publicId) {
        manufacturerRepository.delete(requireOwned(storeId, publicId));
        log.info("Manufacturer deleted storeId={} publicId={}", storeId, publicId);
    }

    private ManufacturerEntity requireOwned(Long storeId, String publicId) {
        return manufacturerRepository.findByStoreIdAndPublicId(storeId, publicId)
                .orElseThrow(() -> new NotFoundException("Manufacturer not found"));
    }
}
