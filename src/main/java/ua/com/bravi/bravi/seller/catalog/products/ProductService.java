package ua.com.bravi.bravi.seller.catalog.products;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageContent;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageUpload;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductPage;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductsApi;
import ua.com.bravi.bravi.seller.catalog.products.config.props.ProductImageStorageProperties;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSortBy;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.catalog.products.exception.InvalidProductRequestException;
import ua.com.bravi.bravi.seller.catalog.products.exception.ProductAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IProductEntityRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IProductImageEntityRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IStockStatusRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.ProductSpecifications;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductImageEntity;
import ua.com.bravi.bravi.seller.catalog.products.persistence.mapper.ProductEntityMapper;
import ua.com.bravi.bravi.seller.catalog.products.persistence.mapper.ProductImageEntityMapper;
import ua.com.bravi.bravi.seller.catalog.products.storage.ProductImageStorage;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService implements ProductsApi {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String IMAGE_FIELD = "file";
    private static final String DUPLICATE = "Product with this code or SKU already exists in the store";

    private final IProductEntityRepository productRepository;
    private final IProductImageEntityRepository imageRepository;
    private final IStockStatusRepository stockStatusRepository;
    private final ProductEntityMapper productEntityMapper;
    private final ProductImageEntityMapper imageEntityMapper;
    private final CategoriesApi categoriesApi;
    private final ManufacturersApi manufacturersApi;
    private final ProductImageStorage imageStorage;
    private final ProductImageStorageProperties storageProperties;

    @Override
    public ProductPage search(Long storeId, ProductSearchQuery query) {
        int page = Math.max(query.page(), 1);
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        ProductSortBy sortBy = query.sortBy() != null ? query.sortBy() : ProductSortBy.CREATED_AT;
        SortOrder sortOrder = query.sortOrder() != null ? query.sortOrder() : SortOrder.DESC;

        Sort.Direction direction = sortOrder == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy.getProperty()));

        Page<ProductEntity> result = productRepository.findAll(ProductSpecifications.forStore(storeId, query), pageable);
        List<ProductEntity> products = result.getContent();
        Map<Long, List<ProductImageView>> imagesByProduct = imagesByProduct(products);

        List<ProductView> data = products.stream()
                .map(entity -> productEntityMapper.toView(entity,
                        imagesByProduct.getOrDefault(entity.getId(), List.of())))
                .toList();

        int pages = (int) Math.ceil((double) result.getTotalElements() / limit);
        return new ProductPage(data, data.size(), result.getTotalElements(), limit, pages, page, sortBy, sortOrder);
    }

    @Override
    public ProductView getById(Long storeId, Long productId) {
        ProductEntity entity = requireOwned(storeId, productId);
        return productEntityMapper.toView(entity, imageViews(productId));
    }

    @Override
    @Transactional
    public Long create(Long storeId, Product product) {
        validateReferences(storeId, product.categoryId(), product.manufacturerId(), product.stockStatusId());
        ProductEntity entity = productEntityMapper.toEntity(product);
        entity.setStoreId(storeId);
        if (entity.getStatus() == null) {
            entity.setStatus(ProductStatus.ACTIVE);
        }
        try {
            return productRepository.save(entity).getId();
        } catch (DataIntegrityViolationException duplicate) {
            throw new ProductAlreadyExistsException(DUPLICATE);
        }
    }

    @Override
    @Transactional
    public void update(Long storeId, Long productId, Product patch) {
        ProductEntity entity = requireOwned(storeId, productId);
        validateReferences(storeId, patch.categoryId(), patch.manufacturerId(), patch.stockStatusId());
        productEntityMapper.updateEntity(entity, patch);
        try {
            productRepository.flush();
        } catch (DataIntegrityViolationException duplicate) {
            throw new ProductAlreadyExistsException(DUPLICATE);
        }
    }

    @Override
    @Transactional
    public void delete(Long storeId, Long productId) {
        ProductEntity entity = requireOwned(storeId, productId);
        imageRepository.findByProductIdOrderBySortOrderAsc(productId)
                .forEach(image -> imageStorage.delete(image.getStorageKey()));
        productRepository.delete(entity); // product_images знімаються ON DELETE CASCADE
    }

    @Override
    public List<ProductImageView> listImages(Long storeId, Long productId) {
        requireOwned(storeId, productId);
        return imageViews(productId);
    }

    @Override
    @Transactional
    public ProductImageView addImage(Long storeId, Long productId, ImageUpload upload) {
        requireOwned(storeId, productId);
        validateUpload(upload);

        String key = imageStorage.store(upload.content(), upload.contentType(), upload.originalFilename());
        int existing = imageRepository.countByProductId(productId);

    ProductImageEntity entity = new ProductImageEntity();
        entity.setProductId(productId);
        entity.setStorageKey(key);
        entity.setContentType(upload.contentType());
        entity.setSizeBytes(upload.size());
        entity.setOriginalFilename(upload.originalFilename());
        entity.setSortOrder(existing);
        entity.setIsPrimary(existing == 0 || upload.primary());

        ProductImageEntity saved = imageRepository.save(entity);
        if (Boolean.TRUE.equals(saved.getIsPrimary())) {
            demoteOtherPrimaries(productId, saved.getId());
        }
        return toImageView(saved);
    }

    @Override
    @Transactional
    public ProductImageView replaceImage(Long storeId, Long productId, Long imageId, ImageUpload upload) {
        requireOwned(storeId, productId);
        validateUpload(upload);

        ProductImageEntity image = requireImage(productId, imageId);
        String oldKey = image.getStorageKey();
        image.setStorageKey(imageStorage.store(upload.content(), upload.contentType(), upload.originalFilename()));
        image.setContentType(upload.contentType());
        image.setSizeBytes(upload.size());
        image.setOriginalFilename(upload.originalFilename());

        ProductImageEntity saved = imageRepository.save(image);
        imageStorage.delete(oldKey);
        return toImageView(saved);
    }

    @Override
    @Transactional
    public void deleteImage(Long storeId, Long productId, Long imageId) {
        requireOwned(storeId, productId);
        ProductImageEntity image = requireImage(productId, imageId);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        imageRepository.delete(image);
        imageStorage.delete(image.getStorageKey());

        if (wasPrimary) {
            imageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        imageRepository.save(next);
                    });
        }
    }

    @Override
    public ImageContent loadImageContent(Long storeId, Long productId, Long imageId) {
        requireOwned(storeId, productId);
        ProductImageEntity image = requireImage(productId, imageId);
        byte[] content = imageStorage.load(image.getStorageKey());
        return new ImageContent(content, image.getContentType(), image.getOriginalFilename());
    }

    private void validateReferences(Long storeId, Long categoryId, Long manufacturerId, Long stockStatusId) {
        if (categoryId != null) {
            categoriesApi.getById(storeId, categoryId);
        }
        if (manufacturerId != null) {
            manufacturersApi.getById(storeId, manufacturerId);
        }
        if (stockStatusId != null && !stockStatusRepository.existsById(stockStatusId)) {
            throw new NotFoundException("Stock status not found");
        }
    }

    private void validateUpload(ImageUpload upload) {
        if (upload == null || upload.content() == null || upload.content().length == 0) {
            throw new InvalidProductRequestException(IMAGE_FIELD, "Image file is required");
        }
        if (upload.contentType() == null || !upload.contentType().startsWith("image/")) {
            throw new InvalidProductRequestException(IMAGE_FIELD, "Only image uploads are allowed");
        }
        if (upload.size() > storageProperties.getMaxFileSize().toBytes()) {
            throw new InvalidProductRequestException(IMAGE_FIELD, "Image exceeds the maximum allowed size");
        }
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
        return imageEntityMapper.toView(entity, imageUrl(entity.getProductId(), entity.getId()));
    }

    private static String imageUrl(Long productId, Long imageId) {
        return "/seller/products/" + productId + "/images/" + imageId;
    }

    private ProductImageEntity requireImage(Long productId, Long imageId) {
        ProductImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Product image not found"));
        if (!image.getProductId().equals(productId)) {
            throw new NotFoundException("Product image not found");
        }
        return image;
    }

    private ProductEntity requireOwned(Long storeId, Long productId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        entity.requireOwnedBy(storeId);
        return entity;
    }
}
