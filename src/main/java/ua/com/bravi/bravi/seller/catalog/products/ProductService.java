package ua.com.bravi.bravi.seller.catalog.products;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageUpload;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductPage;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductsApi;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSortBy;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.catalog.products.exception.ProductAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IProductEntityRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IProductImageEntityRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IStockStatusRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.ProductSpecifications;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductImageEntity;
import ua.com.bravi.bravi.seller.catalog.products.persistence.mapper.ProductEntityMapper;
import ua.com.bravi.bravi.seller.catalog.products.persistence.mapper.ProductImageEntityMapper;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.shared.media.MediaCategory;
import ua.com.bravi.bravi.shared.media.MediaStorage;
import ua.com.bravi.bravi.shared.media.MediaUploadRequest;
import ua.com.bravi.bravi.shared.media.PresignedUpload;
import ua.com.bravi.bravi.shared.media.StoredObject;
import ua.com.bravi.bravi.shared.media.exception.InvalidMediaUploadException;
import ua.com.bravi.bravi.shared.media.exception.MediaObjectNotFoundException;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements ProductsApi {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String DUPLICATE = "Product with this code or SKU already exists in the store";

    private final IProductEntityRepository productRepository;
    private final IProductImageEntityRepository imageRepository;
    private final IStockStatusRepository stockStatusRepository;
    private final ProductEntityMapper productEntityMapper;
    private final ProductImageEntityMapper imageEntityMapper;
    private final CategoriesApi categoriesApi;
    private final ManufacturersApi manufacturersApi;
    private final MediaStorage mediaStorage;

    @Override
    public ProductPage search(Long storeId, ProductSearchQuery query) {
        int page = Math.max(query.page(), 1);
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        ProductSortBy sortBy = query.sortBy() != null ? query.sortBy() : ProductSortBy.CREATED_AT;
        SortOrder sortOrder = query.sortOrder() != null ? query.sortOrder() : SortOrder.DESC;

        List<Long> categoryFilterIds = resolveCategoryFilter(storeId, query.categoryIds());
        List<Long> manufacturerFilterIds = resolveManufacturerFilter(storeId, query.manufacturerIds());

        Sort.Direction direction = sortOrder == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy.getProperty()));

        Page<ProductEntity> result = productRepository.findAll(
                ProductSpecifications.forStore(storeId, query, categoryFilterIds, manufacturerFilterIds), pageable);
        List<ProductEntity> products = result.getContent();
        Map<Long, List<ProductImageView>> imagesByProduct = imagesByProduct(products);
        Map<Long, CategoryView> categories = categoriesById(storeId, products);
        Map<Long, ManufacturerView> manufacturers = manufacturersById(storeId, products);

        List<ProductView> data = products.stream()
                .map(entity -> toView(entity,
                        categories.get(entity.getCategoryId()),
                        manufacturers.get(entity.getManufacturerId()),
                        imagesByProduct.getOrDefault(entity.getId(), List.of())))
                .toList();

        int pages = (int) Math.ceil((double) result.getTotalElements() / limit);
        return new ProductPage(data, data.size(), result.getTotalElements(), limit, pages, page, sortBy, sortOrder);
    }

    @Override
    public ProductView getById(Long storeId, Long productId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        entity.requireOwnedBy(storeId);
        return toView(storeId, entity, imageViews(entity.getId()));
    }

    @Override
    public ProductView getByPublicId(Long storeId, String publicId) {
        ProductEntity entity = requireOwned(storeId, publicId);
        return toView(storeId, entity, imageViews(entity.getId()));
    }

    @Override
    @Transactional
    public ProductView create(Long storeId, Product product) {
        validateStockStatus(product.stockStatusId());
        ProductEntity entity = productEntityMapper.toEntity(product);
        entity.setStoreId(storeId);
        entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.PRODUCT_PREFIX));
        entity.setCategoryId(resolveCategoryId(storeId, product.categoryId()));
        entity.setManufacturerId(resolveManufacturerId(storeId, product.manufacturerId()));
        if (entity.getStatus() == null) {
            entity.setStatus(ProductStatus.ACTIVE);
        }
        try {
            ProductEntity saved = productRepository.save(entity);
            log.info("Product created storeId={} productId={} publicId={}",
                    storeId, saved.getId(), saved.getPublicId());
            return toView(storeId, saved, List.of());
        } catch (DataIntegrityViolationException duplicate) {
            throw new ProductAlreadyExistsException(DUPLICATE);
        }
    }

    @Override
    @Transactional
    public void update(Long storeId, String publicId, Product patch) {
        ProductEntity entity = requireOwned(storeId, publicId);
        validateStockStatus(patch.stockStatusId());
        if (patch.categoryId() != null) {
            entity.setCategoryId(resolveCategoryId(storeId, patch.categoryId()));
        }
        if (patch.manufacturerId() != null) {
            entity.setManufacturerId(resolveManufacturerId(storeId, patch.manufacturerId()));
        }
        productEntityMapper.updateEntity(entity, patch);
        try {
            productRepository.flush();
        } catch (DataIntegrityViolationException duplicate) {
            throw new ProductAlreadyExistsException(DUPLICATE);
        }
        log.info("Product updated storeId={} publicId={}", storeId, publicId);
    }

    @Override
    @Transactional
    public void delete(Long storeId, String publicId) {
        ProductEntity entity = requireOwned(storeId, publicId);
        imageRepository.findByProductIdOrderBySortOrderAsc(entity.getId())
                .forEach(image -> mediaStorage.delete(image.getStorageKey()));
        productRepository.delete(entity); // store_product_images знімаються ON DELETE CASCADE
        log.info("Product deleted storeId={} publicId={}", storeId, publicId);
    }

    @Override
    public List<ProductImageView> listImages(Long storeId, String publicId) {
        return imageViews(requireOwned(storeId, publicId).getId());
    }

    @Override
    public PresignedUpload presignImageUpload(Long storeId, String publicId, ImageUpload upload) {
        ProductEntity product = requireOwned(storeId, publicId);
        MediaCategory.PRODUCT_IMAGE.validate(upload.contentType(), upload.size());
        log.debug("Presigning product image upload storeId={} publicId={} contentType={} size={}",
                storeId, publicId, upload.contentType(), upload.size());
        return mediaStorage.presignUpload(new MediaUploadRequest(
                MediaCategory.PRODUCT_IMAGE, imageScope(storeId, product.getId()),
                upload.contentType(), upload.size(), upload.originalFilename()));
    }

    @Override
    @Transactional
    public ProductImageView confirmImage(Long storeId, String publicId, String storageKey, boolean primary) {
        ProductEntity product = requireOwned(storeId, publicId);
        requireOwnedKey(storeId, product.getId(), storageKey);
        StoredObject object = mediaStorage.stat(storageKey)
                .orElseThrow(() -> new MediaObjectNotFoundException("Image upload not found or expired; upload again"));
        MediaCategory.PRODUCT_IMAGE.validate(object.contentType(), object.size());

        int existing = imageRepository.countByProductId(product.getId());
        ProductImageEntity entity = new ProductImageEntity();
        entity.setProductId(product.getId());
        entity.setStorageKey(storageKey);
        entity.setContentType(object.contentType());
        entity.setSizeBytes(object.size());
        entity.setSortOrder(existing);
        entity.setIsPrimary(existing == 0 || primary);

        ProductImageEntity saved = imageRepository.save(entity);
        if (Boolean.TRUE.equals(saved.getIsPrimary())) {
            demoteOtherPrimaries(product.getId(), saved.getId());
        }
        log.info("Product image added storeId={} publicId={} imageId={} primary={}",
                storeId, publicId, saved.getId(), saved.getIsPrimary());
        return toImageView(saved);
    }

    @Override
    @Transactional
    public ProductImageView setPrimaryImage(Long storeId, String publicId, Long imageId) {
        ProductEntity product = requireOwned(storeId, publicId);
        ProductImageEntity image = requireImage(product.getId(), imageId);
        image.setIsPrimary(true);
        ProductImageEntity saved = imageRepository.save(image);
        demoteOtherPrimaries(product.getId(), saved.getId());
        log.info("Product primary image changed storeId={} publicId={} imageId={}", storeId, publicId, imageId);
        return toImageView(saved);
    }

    @Override
    @Transactional
    public void deleteImage(Long storeId, String publicId, Long imageId) {
        ProductEntity product = requireOwned(storeId, publicId);
        ProductImageEntity image = requireImage(product.getId(), imageId);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        imageRepository.delete(image);
        mediaStorage.delete(image.getStorageKey());

        if (wasPrimary) {
            imageRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        imageRepository.save(next);
                    });
        }
        log.info("Product image deleted storeId={} publicId={} imageId={} wasPrimary={}",
                storeId, publicId, imageId, wasPrimary);
    }

    private void validateStockStatus(Long stockStatusId) {
        if (stockStatusId != null && !stockStatusRepository.existsById(stockStatusId)) {
            throw new NotFoundException("Stock status not found");
        }
    }

    /** Резолвить public id категорії в internal bigint (заодно валідує існування й приналежність магазину). */
    private Long resolveCategoryId(Long storeId, String categoryPublicId) {
        return categoryPublicId == null ? null : categoriesApi.getByPublicId(storeId, categoryPublicId).id();
    }

    private Long resolveManufacturerId(Long storeId, String manufacturerPublicId) {
        return manufacturerPublicId == null ? null : manufacturersApi.getByPublicId(storeId, manufacturerPublicId).id();
    }

    private List<Long> resolveCategoryFilter(Long storeId, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return null;
        }
        return publicIds.stream().map(pid -> categoriesApi.getByPublicId(storeId, pid).id()).toList();
    }

    private List<Long> resolveManufacturerFilter(Long storeId, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return null;
        }
        return publicIds.stream().map(pid -> manufacturersApi.getByPublicId(storeId, pid).id()).toList();
    }

    private ProductView toView(Long storeId, ProductEntity entity, List<ProductImageView> images) {
        CategoryView category = entity.getCategoryId() == null ? null
                : categoriesApi.getById(storeId, entity.getCategoryId());
        ManufacturerView manufacturer = entity.getManufacturerId() == null ? null
                : manufacturersApi.getById(storeId, entity.getManufacturerId());
        return toView(entity, category, manufacturer, images);
    }

    private ProductView toView(ProductEntity entity, CategoryView category, ManufacturerView manufacturer,
                               List<ProductImageView> images) {
        return productEntityMapper.toView(entity, productEntityMapper.toRef(category),
                productEntityMapper.toRef(manufacturer), images);
    }

    /** Batch-резолв суміжних агрегатів для сторінки: один lookup на кожен унікальний id, не на кожен товар. */
    private Map<Long, CategoryView> categoriesById(Long storeId, List<ProductEntity> products) {
        return distinctRefs(products, ProductEntity::getCategoryId, id -> categoriesApi.getById(storeId, id));
    }

    private Map<Long, ManufacturerView> manufacturersById(Long storeId, List<ProductEntity> products) {
        return distinctRefs(products, ProductEntity::getManufacturerId, id -> manufacturersApi.getById(storeId, id));
    }

    private static <V> Map<Long, V> distinctRefs(List<ProductEntity> products,
                                                 Function<ProductEntity, Long> idExtractor,
                                                 Function<Long, V> resolver) {
        return products.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), resolver));
    }

    private void demoteOtherPrimaries(Long productId, Long keepImageId) {
        imageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .filter(image -> !image.getId().equals(keepImageId) && Boolean.TRUE.equals(image.getIsPrimary()))
                .forEach(image -> {
                    image.setIsPrimary(false);
                    imageRepository.save(image);
                });
    }

    private Map<Long, List<ProductImageView>> imagesByProduct(List<ProductEntity> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = products.stream().map(ProductEntity::getId).toList();
        return imageRepository.findByProductIdInOrderBySortOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(ProductImageEntity::getProductId,
                        Collectors.mapping(this::toImageView, Collectors.toList())));
    }

    private List<ProductImageView> imageViews(Long productId) {
        return imageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .map(this::toImageView)
                .toList();
    }

    private ProductImageView toImageView(ProductImageEntity entity) {
        return imageEntityMapper.toView(entity, mediaStorage.publicUrl(entity.getStorageKey()));
    }

    private ProductImageEntity requireImage(Long productId, Long imageId) {
        ProductImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Product image not found"));
        if (!image.getProductId().equals(productId)) {
            throw new NotFoundException("Product image not found");
        }
        return image;
    }

    private ProductEntity requireOwned(Long storeId, String publicId) {
        return productRepository.findByStoreIdAndPublicId(storeId, publicId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void requireOwnedKey(Long storeId, Long productId, String storageKey) {
        String expectedPrefix = MediaCategory.PRODUCT_IMAGE.keyPrefix(imageScope(storeId, productId)) + "/";
        if (storageKey == null || !storageKey.startsWith(expectedPrefix)) {
            throw new InvalidMediaUploadException("storage_key", "Storage key does not belong to this product");
        }
    }

    private static String imageScope(Long storeId, Long productId) {
        return storeId + "/" + productId;
    }
}
