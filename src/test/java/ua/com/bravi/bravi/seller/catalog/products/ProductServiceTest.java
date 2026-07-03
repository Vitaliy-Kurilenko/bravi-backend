package ua.com.bravi.bravi.seller.catalog.products;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageUpload;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.config.props.ProductImageStorageProperties;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.catalog.products.exception.InvalidProductRequestException;
import ua.com.bravi.bravi.seller.catalog.products.exception.ProductAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IProductEntityRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IProductImageEntityRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.IStockStatusRepository;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductImageEntity;
import ua.com.bravi.bravi.seller.catalog.products.persistence.mapper.ProductEntityMapper;
import ua.com.bravi.bravi.seller.catalog.products.persistence.mapper.ProductImageEntityMapper;
import ua.com.bravi.bravi.seller.catalog.products.storage.ProductImageStorage;
import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long STORE_ID = 7L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long PRODUCT_ID = 42L;
    private static final Long STOCK_STATUS_ID = 1L;

    private final IProductEntityRepository productRepository = mock(IProductEntityRepository.class);
    private final IProductImageEntityRepository imageRepository = mock(IProductImageEntityRepository.class);
    private final IStockStatusRepository stockStatusRepository = mock(IStockStatusRepository.class);
    private final ProductEntityMapper productEntityMapper = mock(ProductEntityMapper.class);
    private final ProductImageEntityMapper imageEntityMapper = mock(ProductImageEntityMapper.class);
    private final CategoriesApi categoriesApi = mock(CategoriesApi.class);
    private final ManufacturersApi manufacturersApi = mock(ManufacturersApi.class);
    private final ProductImageStorage imageStorage = mock(ProductImageStorage.class);
    private final ProductImageStorageProperties storageProperties = new ProductImageStorageProperties();

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, imageRepository, stockStatusRepository,
                productEntityMapper, imageEntityMapper, categoriesApi, manufacturersApi,
                imageStorage, storageProperties);
    }

    private static Product product(Long categoryId, Long manufacturerId, Long stockStatusId, ProductStatus status) {
        return new Product(null, null, categoryId, manufacturerId, stockStatusId, "Widget", null, "CODE-1",
                null, BigDecimal.ONE, BigDecimal.TEN, 1, null, null, null, null, status, null, null);
    }

    private static ProductEntity productEntityOwnedBy(Long storeId) {
        ProductEntity entity = new ProductEntity();
        entity.setId(PRODUCT_ID);
        entity.setStoreId(storeId);
        return entity;
    }

    private static ImageUpload pngUpload() {
        return new ImageUpload(new byte[]{1, 2, 3}, "image/png", "p.png", 3, false);
    }

    @Test
    void createValidatesStockStatusAndDefaultsStatusToActive() {
        ProductEntity entity = new ProductEntity();
        entity.setStatus(null);
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);

        service.create(STORE_ID, product(null, null, STOCK_STATUS_ID, null));

        assertThat(entity.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(entity.getStoreId()).isEqualTo(STORE_ID);
    }

    @Test
    void createValidatesCategoryAgainstStore() {
        when(categoriesApi.getById(STORE_ID, 5L)).thenThrow(new ForbiddenException("nope"));

        assertThatThrownBy(() -> service.create(STORE_ID, product(5L, null, STOCK_STATUS_ID, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownStockStatus() {
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(STORE_ID, product(null, null, STOCK_STATUS_ID, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createMapsDuplicateToConflict() {
        ProductEntity entity = new ProductEntity();
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(entity)).thenThrow(new DataIntegrityViolationException("uq_products_store_code"));

        assertThatThrownBy(() -> service.create(STORE_ID, product(null, null, STOCK_STATUS_ID, null)))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void getByIdRejectsProductOfAnotherStore() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntityOwnedBy(OTHER_STORE_ID)));

        assertThatThrownBy(() -> service.getById(STORE_ID, PRODUCT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteRemovesImageFilesThenProduct() {
        ProductEntity entity = productEntityOwnedBy(STORE_ID);
        ProductImageEntity image = new ProductImageEntity();
        image.setStorageKey("key-1");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(entity));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(List.of(image));

        service.delete(STORE_ID, PRODUCT_ID);

        verify(imageStorage).delete("key-1");
        verify(productRepository).delete(entity);
    }

    @Test
    void addFirstImageBecomesPrimary() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));
        when(imageStorage.store(any(), eq("image/png"), eq("p.png"))).thenReturn("stored-key");
        when(imageRepository.countByProductId(PRODUCT_ID)).thenReturn(0);
        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(List.of());
        when(imageEntityMapper.toView(any(), any())).thenReturn(mock(ProductImageView.class));

        service.addImage(STORE_ID, PRODUCT_ID, pngUpload());

        ArgumentCaptor<ProductImageEntity> captor = ArgumentCaptor.forClass(ProductImageEntity.class);
        verify(imageRepository).save(captor.capture());
        ProductImageEntity saved = captor.getValue();
        assertThat(saved.getIsPrimary()).isTrue();
        assertThat(saved.getStorageKey()).isEqualTo("stored-key");
        assertThat(saved.getSortOrder()).isZero();
    }

    @Test
    void addImageRejectsNonImageUpload() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));

        ImageUpload pdf = new ImageUpload(new byte[]{1}, "application/pdf", "f.pdf", 1, false);
        assertThatThrownBy(() -> service.addImage(STORE_ID, PRODUCT_ID, pdf))
                .isInstanceOf(InvalidProductRequestException.class);

        verify(imageStorage, never()).store(any(), any(), any());
    }

    @Test
    void deletePrimaryImagePromotesNext() {
        ProductImageEntity primary = new ProductImageEntity();
        primary.setId(10L);
        primary.setProductId(PRODUCT_ID);
        primary.setStorageKey("k10");
        primary.setIsPrimary(true);
        ProductImageEntity next = new ProductImageEntity();
        next.setId(11L);
        next.setProductId(PRODUCT_ID);
        next.setIsPrimary(false);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));
        when(imageRepository.findById(10L)).thenReturn(Optional.of(primary));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(List.of(next));

        service.deleteImage(STORE_ID, PRODUCT_ID, 10L);

        verify(imageStorage).delete("k10");
        assertThat(next.getIsPrimary()).isTrue();
        verify(imageRepository).save(next);
    }
}
