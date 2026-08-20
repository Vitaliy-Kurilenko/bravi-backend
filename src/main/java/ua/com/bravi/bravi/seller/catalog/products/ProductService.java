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
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributesApi;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributeValueView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributesView;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValue;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageUpload;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountBulkResultView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountPredicates;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountTarget;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountsApi;
import ua.com.bravi.bravi.seller.catalog.discounts.api.ProductDiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.Discount;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SubmittedDiscount;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductPage;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductsApi;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductGallery;
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
import ua.com.bravi.bravi.shared.util.ConstraintViolations;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.time.Instant;
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
    private static final String UQ_CODE = "uq_store_products_store_code";
    private static final String UQ_SKU = "uq_store_products_store_sku";
    private static final String DUPLICATE_CODE = "Product with this code already exists in the store";
    private static final String DUPLICATE_SKU = "Product with this SKU already exists in the store";

    private final IProductEntityRepository productRepository;
    private final IProductImageEntityRepository imageRepository;
    private final IStockStatusRepository stockStatusRepository;
    private final ProductEntityMapper productEntityMapper;
    private final ProductImageEntityMapper imageEntityMapper;
    private final CategoriesApi categoriesApi;
    private final ManufacturersApi manufacturersApi;
    private final AttributesApi attributesApi;
    private final DiscountsApi discountsApi;
    private final DiscountPredicates discountPredicates;
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

        // One instant filters and prices the page, so no product lands on the wrong side of a boundary.
        Instant now = Instant.now();
        Page<ProductEntity> result = productRepository.findAll(
                ProductSpecifications.forStore(storeId, query, categoryFilterIds, manufacturerFilterIds,
                        discountPredicates, now), pageable);
        List<ProductEntity> products = result.getContent();
        Map<Long, List<ProductImageView>> imagesByProduct = imagesByProduct(products);
        Map<Long, ProductDiscountView> discountsByProduct = discountsByProduct(storeId, products, now);
        Map<Long, CategoryView> categories = categoriesById(storeId, products);
        Map<Long, ManufacturerView> manufacturers = manufacturersById(storeId, products);

        List<ProductView> data = products.stream()
                .map(entity -> toView(entity,
                        categories.get(entity.getCategoryId()),
                        manufacturers.get(entity.getManufacturerId()),
                        imagesByProduct.getOrDefault(entity.getId(), List.of()),
                        List.of(),
                        discountsByProduct.get(entity.getId())))
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
        ProductEntity saved;
        try {
            saved = productRepository.save(entity);
        } catch (DataIntegrityViolationException violation) {
            throw duplicateOf(violation, storeId);
        }
        if (product.attributes() != null && !product.attributes().isEmpty()) {
            attributesApi.replaceProductValues(storeId, saved.getId(), saved.getCategoryId(), product.attributes());
        }
        log.info("Product created storeId={} productId={} publicId={}",
                storeId, saved.getId(), saved.getPublicId());
        return toView(storeId, saved, List.of());
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
        // Only a genuine price move can invalidate a fixed-amount discount, so an unchanged price skips the check.
        if (patch.price() != null
                && (entity.getPrice() == null || patch.price().compareTo(entity.getPrice()) != 0)) {
            discountsApi.requireCompatibleWithPrice(storeId, entity.getId(), patch.price(), Instant.now());
        }
        productEntityMapper.updateEntity(entity, patch);
        try {
            productRepository.flush();
        } catch (DataIntegrityViolationException violation) {
            throw duplicateOf(violation, storeId);
        }
        if (patch.attributes() != null) {
            attributesApi.replaceProductValues(storeId, entity.getId(), entity.getCategoryId(), patch.attributes());
        }
        log.info("Product updated storeId={} publicId={}", storeId, publicId);
    }

    @Override
    @Transactional
    public void delete(Long storeId, String publicId) {
        ProductEntity entity = requireOwned(storeId, publicId);
        imageRepository.findByProductIdOrderBySortOrderAsc(entity.getId())
                .forEach(image -> mediaStorage.delete(image.getStorageKey()));
        productRepository.delete(entity); // store_product_images are removed by ON DELETE CASCADE
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
    public ProductImageView confirmImage(Long storeId, String publicId, String storageKey) {
        ProductEntity product = requireOwned(storeId, publicId);
        requireOwnedKey(storeId, product.getId(), storageKey);
        StoredObject object = mediaStorage.stat(storageKey)
                .orElseThrow(() -> new MediaObjectNotFoundException("Image upload not found or expired; upload again"));
        MediaCategory.PRODUCT_IMAGE.validate(object.contentType(), object.size());

        ProductImageEntity entity = new ProductImageEntity();
        entity.setProductId(product.getId());
        entity.setStorageKey(storageKey);
        entity.setContentType(object.contentType());
        entity.setSizeBytes(object.size());
        entity.setSortOrder(imageRepository.countByProductId(product.getId()));

        ProductImageEntity saved = imageRepository.save(entity);
        log.info("Product image added storeId={} publicId={} imageId={} sortOrder={}",
                storeId, publicId, saved.getId(), saved.getSortOrder());
        return toImageView(saved);
    }

    @Override
    @Transactional
    public List<ProductImageView> moveImage(Long storeId, String publicId, Long imageId, int sortOrder) {
        ProductEntity product = requireOwned(storeId, publicId);
        requireImage(product.getId(), imageId);

        List<ProductImageEntity> images = imageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        List<Long> order = gallery(images).move(imageId, sortOrder);
        List<ProductImageView> resequenced = applyOrder(images, order).stream()
                .map(this::toImageView)
                .toList();

        log.info("Product image moved storeId={} publicId={} imageId={} sortOrder={}",
                storeId, publicId, imageId, sortOrder);
        return resequenced;
    }

    @Override
    @Transactional
    public void deleteImage(Long storeId, String publicId, Long imageId) {
        ProductEntity product = requireOwned(storeId, publicId);
        ProductImageEntity image = requireImage(product.getId(), imageId);
        Integer sortOrder = image.getSortOrder();

        List<ProductImageEntity> images = imageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        imageRepository.delete(image);
        mediaStorage.delete(image.getStorageKey());
        applyOrder(images, gallery(images).without(imageId));

        log.info("Product image deleted storeId={} publicId={} imageId={} sortOrder={}",
                storeId, publicId, imageId, sortOrder);
    }

    /** Translates a unique constraint violation into a field-aware conflict; other violations are returned as is. */
    private RuntimeException duplicateOf(DataIntegrityViolationException violation, Long storeId) {
        String constraint = ConstraintViolations.nameOf(violation);
        if (UQ_CODE.equalsIgnoreCase(constraint)) {
            return new ProductAlreadyExistsException("code", DUPLICATE_CODE);
        }
        if (UQ_SKU.equalsIgnoreCase(constraint)) {
            return new ProductAlreadyExistsException("sku", DUPLICATE_SKU);
        }
        log.warn("Unmapped data integrity violation storeId={} constraint={}", storeId, constraint);
        return violation;
    }

    @Override
    public ProductAttributesView describeAttributes(Long storeId, String publicId) {
        ProductEntity product = requireOwned(storeId, publicId);
        return attributesApi.describeProductAttributes(storeId, product.getId(), product.getCategoryId());
    }

    @Override
    @Transactional
    public List<ProductAttributeValueView> replaceAttributes(Long storeId, String publicId,
                                                             List<AttributeValue> values) {
        ProductEntity product = requireOwned(storeId, publicId);
        return attributesApi.replaceProductValues(storeId, product.getId(), product.getCategoryId(), values);
    }

    @Override
    @Transactional
    public List<ProductAttributeValueView> copyAttributesFrom(Long storeId, String publicId,
                                                              String sourcePublicId) {
        ProductEntity target = requireOwned(storeId, publicId);
        ProductEntity source = requireOwned(storeId, sourcePublicId);
        List<AttributeValue> values = attributesApi.exportProductValues(storeId, source.getId());
        log.info("Product attributes copied storeId={} fromProductId={} toProductId={} attributes={}",
                storeId, source.getId(), target.getId(), values.size());
        return attributesApi.replaceProductValues(storeId, target.getId(), target.getCategoryId(), values);
    }

    @Override
    @Transactional
    public int applyAttributesBulk(Long storeId, List<String> publicIds, List<AttributeValue> values) {
        List<ProductEntity> products = publicIds.stream().distinct()
                .map(publicId -> requireOwned(storeId, publicId))
                .toList();
        products.forEach(product ->
                attributesApi.mergeProductValues(storeId, product.getId(), product.getCategoryId(), values));
        log.info("Product attributes applied in bulk storeId={} products={} attributes={}",
                storeId, products.size(), values.size());
        return products.size();
    }

    private void validateStockStatus(Long stockStatusId) {
        if (stockStatusId != null && !stockStatusRepository.existsById(stockStatusId)) {
            throw new NotFoundException("Stock status not found");
        }
    }

    /** Resolves a category public id into an internal bigint, validating that it exists and belongs to the store. */
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

    @Override
    public List<DiscountView> listDiscounts(Long storeId, String publicId) {
        ProductEntity product = requireOwned(storeId, publicId);
        return discountsApi.listForProduct(storeId, product.getId(), Instant.now());
    }

    @Override
    @Transactional
    public List<DiscountView> replaceDiscounts(Long storeId, String publicId, List<SubmittedDiscount> discounts) {
        ProductEntity product = requireOwned(storeId, publicId);
        return discountsApi.replaceForProduct(storeId, product.getId(), product.getPrice(),
                discounts, Instant.now());
    }

    @Override
    @Transactional
    public DiscountBulkResultView applyDiscountsBulk(Long storeId, List<String> publicIds, Discount discount) {
        List<DiscountTarget> targets = publicIds.stream().distinct()
                .map(publicId -> requireOwned(storeId, publicId))
                .map(product -> new DiscountTarget(product.getId(), product.getPublicId(), product.getPrice()))
                .toList();
        return discountsApi.applyBulk(storeId, targets, discount, Instant.now());
    }

    private ProductView toView(Long storeId, ProductEntity entity, List<ProductImageView> images) {
        CategoryView category = entity.getCategoryId() == null ? null
                : categoriesApi.getById(storeId, entity.getCategoryId());
        ManufacturerView manufacturer = entity.getManufacturerId() == null ? null
                : manufacturersApi.getById(storeId, entity.getManufacturerId());
        ProductDiscountView discount = discountsApi
                .activeForProduct(storeId, entity.getId(), entity.getPrice(), Instant.now())
                .orElse(null);
        return toView(entity, category, manufacturer, images,
                attributesApi.listProductValues(storeId, entity.getId(), entity.getCategoryId()), discount);
    }

    private ProductView toView(ProductEntity entity, CategoryView category, ManufacturerView manufacturer,
                               List<ProductImageView> images, List<ProductAttributeValueView> attributes,
                               ProductDiscountView discount) {
        return productEntityMapper.toView(entity, productEntityMapper.toRef(category),
                productEntityMapper.toRef(manufacturer), images, attributes, discount);
    }

    /** One query prices the whole page, and one instant keeps every row on the same side of a boundary. */
    private Map<Long, ProductDiscountView> discountsByProduct(Long storeId, List<ProductEntity> products,
                                                              Instant at) {
        return discountsApi.activeByProduct(storeId, products.stream()
                .map(product -> new DiscountTarget(product.getId(), product.getPublicId(), product.getPrice()))
                .toList(), at);
    }

    /** Resolves neighbouring aggregates for a page with one lookup per distinct id rather than per product. */
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

    private static ProductGallery gallery(List<ProductImageEntity> images) {
        return ProductGallery.of(images.stream().map(ProductImageEntity::getId).toList());
    }

    /** Writes positions back so they match the given order, and returns the images in that order. */
    private List<ProductImageEntity> applyOrder(List<ProductImageEntity> images, List<Long> order) {
        Map<Long, ProductImageEntity> byId = images.stream()
                .collect(Collectors.toMap(ProductImageEntity::getId, Function.identity()));
        List<ProductImageEntity> ordered = order.stream().map(byId::get).toList();
        for (int position = 0; position < ordered.size(); position++) {
            ordered.get(position).setSortOrder(position);
        }
        imageRepository.saveAll(ordered);
        return ordered;
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
