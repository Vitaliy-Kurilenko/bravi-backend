package ua.com.bravi.bravi.seller.catalog.products;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributesApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturersApi;
import ua.com.bravi.bravi.seller.catalog.products.api.CatalogRefView;
import ua.com.bravi.bravi.seller.catalog.products.api.ImageUpload;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductImageView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
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
import ua.com.bravi.bravi.shared.exception.ForbiddenException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountPredicates;
import ua.com.bravi.bravi.seller.tags.api.TagPredicates;
import ua.com.bravi.bravi.seller.tags.api.TagsApi;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountsApi;
import ua.com.bravi.bravi.shared.media.MediaStorage;
import ua.com.bravi.bravi.shared.media.StoredObject;
import ua.com.bravi.bravi.shared.media.exception.InvalidMediaUploadException;
import ua.com.bravi.bravi.shared.media.exception.MediaObjectNotFoundException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long STORE_ID = 7L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long PRODUCT_ID = 42L;
    private static final String PUBLIC_ID = "prd_test";
    private static final Long STOCK_STATUS_ID = 1L;
    private static final String KEY = "product-images/7/42/img.png";
    private static final CatalogRefView CATEGORY_REF = new CatalogRefView("cat_x", "Ноутбуки");
    private static final CatalogRefView MANUFACTURER_REF = new CatalogRefView("mnf_x", "Lenovo");

    private final IProductEntityRepository productRepository = mock(IProductEntityRepository.class);
    private final IProductImageEntityRepository imageRepository = mock(IProductImageEntityRepository.class);
    private final IStockStatusRepository stockStatusRepository = mock(IStockStatusRepository.class);
    private final ProductEntityMapper productEntityMapper = mock(ProductEntityMapper.class);
    private final ProductImageEntityMapper imageEntityMapper = mock(ProductImageEntityMapper.class);
    private final CategoriesApi categoriesApi = mock(CategoriesApi.class);
    private final ManufacturersApi manufacturersApi = mock(ManufacturersApi.class);
    private final AttributesApi attributesApi = mock(AttributesApi.class);
    private final MediaStorage mediaStorage = mock(MediaStorage.class);
    private final DiscountsApi discountsApi = mock(DiscountsApi.class);
    private final DiscountPredicates discountPredicates = mock(DiscountPredicates.class);
    private final TagsApi tagsApi = mock(TagsApi.class);
    private final TagPredicates tagPredicates = mock(TagPredicates.class);

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, imageRepository, stockStatusRepository,
                productEntityMapper, imageEntityMapper, categoriesApi, manufacturersApi, attributesApi,
                discountsApi, discountPredicates, tagsApi, tagPredicates, mediaStorage);
    }

    private static Product product(String categoryId, String manufacturerId, Long stockStatusId, ProductStatus status) {
        return new Product(null, null, null, categoryId, manufacturerId, stockStatusId, "Widget", null, "CODE-1",
                null, BigDecimal.ONE, 1, null, null, null, null, status, null, null, null, null);
    }

    private static ProductEntity productEntityOwnedBy(Long storeId) {
        ProductEntity entity = new ProductEntity();
        entity.setId(PRODUCT_ID);
        entity.setStoreId(storeId);
        return entity;
    }

    private static ImageUpload pngUpload() {
        return new ImageUpload("image/png", 3, "p.png");
    }

    private static CategoryView category() {
        return new CategoryView(55L, "cat_x", STORE_ID, null, null, "Ноутбуки", null, null, null, null, null);
    }

    private static ManufacturerView manufacturer() {
        return new ManufacturerView(66L, "mnf_x", STORE_ID, "Lenovo", null, null, null, null);
    }

    private static ProductSearchQuery searchQuery() {
        return new ProductSearchQuery(null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 1, 20);
    }

    @Test
    void createDefaultsStatusToActiveAndAssignsPublicId() {
        ProductEntity entity = new ProductEntity();
        entity.setStatus(null);
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productEntityMapper.toView(any(), any(), any(), any(), any(), any(), any())).thenReturn(mock(ProductView.class));

        service.create(STORE_ID, product(null, null, STOCK_STATUS_ID, null));

        assertThat(entity.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(entity.getStoreId()).isEqualTo(STORE_ID);
        assertThat(entity.getPublicId()).startsWith("prd_");
    }

    @Test
    void createResolvesCategoryPublicIdToInternalId() {
        ProductEntity entity = new ProductEntity();
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(entity);
        CategoryView category = new ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView(
                55L, "cat_x", STORE_ID, null, null, "C", null, null, null, null, null);
        when(categoriesApi.getByPublicId(STORE_ID, "cat_x")).thenReturn(category);
        when(categoriesApi.getById(STORE_ID, 55L)).thenReturn(category);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productEntityMapper.toView(any(), any(), any(), any(), any(), any(), any())).thenReturn(mock(ProductView.class));

        service.create(STORE_ID, product("cat_x", null, STOCK_STATUS_ID, null));

        assertThat(entity.getCategoryId()).isEqualTo(55L);
    }

    @Test
    void createValidatesCategoryAgainstStore() {
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(new ProductEntity());
        when(categoriesApi.getByPublicId(STORE_ID, "cat_x")).thenThrow(new ForbiddenException("nope"));

        assertThatThrownBy(() -> service.create(STORE_ID, product("cat_x", null, STOCK_STATUS_ID, null)))
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
    void createMapsDuplicateCodeToConflictOnCodeField() {
        ProductEntity entity = new ProductEntity();
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(entity)).thenThrow(violationOf("uq_store_products_store_code"));

        assertThatThrownBy(() -> service.create(STORE_ID, product(null, null, STOCK_STATUS_ID, null)))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("field", "code");
    }

    @Test
    void createMapsDuplicateSkuToConflictOnSkuField() {
        ProductEntity entity = new ProductEntity();
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(entity)).thenThrow(violationOf("uq_store_products_store_sku"));

        assertThatThrownBy(() -> service.create(STORE_ID, product(null, null, STOCK_STATUS_ID, null)))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("field", "sku");
    }

    @Test
    void createRethrowsUnmappedIntegrityViolation() {
        ProductEntity entity = new ProductEntity();
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        when(productEntityMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(entity)).thenThrow(violationOf("fk_store_products_stock_status"));

        assertThatThrownBy(() -> service.create(STORE_ID, product(null, null, STOCK_STATUS_ID, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateMapsDuplicateSkuToConflictOnSkuField() {
        ProductEntity entity = productEntityOwnedBy(STORE_ID);
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.of(entity));
        when(stockStatusRepository.existsById(STOCK_STATUS_ID)).thenReturn(true);
        doThrow(violationOf("uq_store_products_store_sku")).when(productRepository).flush();

        assertThatThrownBy(() -> service.update(STORE_ID, PUBLIC_ID, product(null, null, STOCK_STATUS_ID, null)))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("field", "sku");
    }

    /** Builds the exception in the shape Spring produces: a data access exception wrapping a named Hibernate cause. */
    private static DataIntegrityViolationException violationOf(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("duplicate key value", new SQLException(), constraint));
    }

    @Test
    void getByPublicIdReturnsNotFoundForOtherStore() {
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByPublicId(STORE_ID, PUBLIC_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByPublicIdPassesResolvedCategoryAndManufacturerRefsToMapper() {
        ProductEntity entity = productEntityOwnedBy(STORE_ID);
        entity.setCategoryId(55L);
        entity.setManufacturerId(66L);
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.of(entity));
        when(categoriesApi.getById(STORE_ID, 55L)).thenReturn(category());
        when(manufacturersApi.getById(STORE_ID, 66L)).thenReturn(manufacturer());
        when(productEntityMapper.toRef(category())).thenReturn(CATEGORY_REF);
        when(productEntityMapper.toRef(manufacturer())).thenReturn(MANUFACTURER_REF);

        service.getByPublicId(STORE_ID, PUBLIC_ID);

        verify(productEntityMapper).toView(eq(entity), eq(CATEGORY_REF), eq(MANUFACTURER_REF), any(), any(), any(), any());
    }

    @Test
    void getByPublicIdLeavesRefsNullWhenProductHasNoCategoryOrManufacturer() {
        ProductEntity entity = productEntityOwnedBy(STORE_ID);
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.of(entity));

        service.getByPublicId(STORE_ID, PUBLIC_ID);

        verify(categoriesApi, never()).getById(any(), any());
        verify(manufacturersApi, never()).getById(any(), any());
        verify(productEntityMapper).toView(eq(entity), isNull(), isNull(), any(), any(), any(), any());
    }

    @Test
    void searchResolvesEachDistinctReferenceOnlyOnce() {
        ProductEntity first = productEntityOwnedBy(STORE_ID);
        ProductEntity second = productEntityOwnedBy(STORE_ID);
        second.setId(43L);
        List.of(first, second).forEach(entity -> {
            entity.setCategoryId(55L);
            entity.setManufacturerId(66L);
        });
        when(productRepository.findAll(ArgumentMatchers.<Specification<ProductEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));
        when(categoriesApi.getById(STORE_ID, 55L)).thenReturn(category());
        when(manufacturersApi.getById(STORE_ID, 66L)).thenReturn(manufacturer());
        when(productEntityMapper.toRef(category())).thenReturn(CATEGORY_REF);
        when(productEntityMapper.toRef(manufacturer())).thenReturn(MANUFACTURER_REF);
        when(productEntityMapper.toView(any(), any(), any(), any(), any(), any(), any())).thenReturn(mock(ProductView.class));

        service.search(STORE_ID, searchQuery());

        verify(categoriesApi, times(1)).getById(STORE_ID, 55L);
        verify(manufacturersApi, times(1)).getById(STORE_ID, 66L);
        verify(productEntityMapper, times(2)).toView(any(), eq(CATEGORY_REF), eq(MANUFACTURER_REF), any(), any(), any(), any());
    }

    @Test
    void getByIdRejectsProductOfAnotherStore() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntityOwnedBy(OTHER_STORE_ID)));

        assertThatThrownBy(() -> service.getById(STORE_ID, PRODUCT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteRemovesImageObjectsThenProduct() {
        ProductEntity entity = productEntityOwnedBy(STORE_ID);
        ProductImageEntity image = new ProductImageEntity();
        image.setStorageKey("key-1");
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID)).thenReturn(Optional.of(entity));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(List.of(image));

        service.delete(STORE_ID, PUBLIC_ID);

        verify(mediaStorage).delete("key-1");
        verify(productRepository).delete(entity);
    }

    @Test
    void confirmImageAppendsAtTheEndOfTheGallery() {
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID))
                .thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));
        when(mediaStorage.stat(KEY)).thenReturn(Optional.of(new StoredObject(KEY, "image/png", 3)));
        when(imageRepository.countByProductId(PRODUCT_ID)).thenReturn(2);
        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageEntityMapper.toView(any(), any())).thenReturn(mock(ProductImageView.class));

        service.confirmImage(STORE_ID, PUBLIC_ID, KEY);

        ArgumentCaptor<ProductImageEntity> captor = ArgumentCaptor.forClass(ProductImageEntity.class);
        verify(imageRepository).save(captor.capture());
        ProductImageEntity saved = captor.getValue();
        assertThat(saved.getStorageKey()).isEqualTo(KEY);
        assertThat(saved.getSortOrder()).isEqualTo(2);
    }

    @Test
    void confirmImageRejectsForeignStorageKey() {
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID))
                .thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));

        assertThatThrownBy(() -> service.confirmImage(STORE_ID, PUBLIC_ID, "product-images/1/2/x.png"))
                .isInstanceOf(InvalidMediaUploadException.class);

        verify(imageRepository, never()).save(any());
    }

    @Test
    void confirmImageRejectsMissingObject() {
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID))
                .thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));
        when(mediaStorage.stat(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmImage(STORE_ID, PUBLIC_ID, KEY))
                .isInstanceOf(MediaObjectNotFoundException.class);
    }

    @Test
    void presignImageValidatesAndDelegates() {
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID))
                .thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));

        service.presignImageUpload(STORE_ID, PUBLIC_ID, pngUpload());

        verify(mediaStorage).presignUpload(any());
    }

    @Test
    void moveImageShiftsTheOtherImages() {
        List<ProductImageEntity> gallery = gallery(10L, 11L, 12L);
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID))
                .thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));
        when(imageRepository.findById(12L)).thenReturn(Optional.of(gallery.get(2)));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(gallery);
        when(imageEntityMapper.toView(any(), any())).thenReturn(mock(ProductImageView.class));

        List<ProductImageView> moved = service.moveImage(STORE_ID, PUBLIC_ID, 12L, 0);

        assertThat(moved).hasSize(3);
        assertThat(gallery).extracting(ProductImageEntity::getId, ProductImageEntity::getSortOrder)
                .containsExactly(tuple(10L, 1), tuple(11L, 2), tuple(12L, 0));
        verify(imageRepository).saveAll(List.of(gallery.get(2), gallery.get(0), gallery.get(1)));
    }

    @Test
    void moveImageRejectsPositionOutsideTheGallery() {
        List<ProductImageEntity> gallery = gallery(10L, 11L);
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID))
                .thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));
        when(imageRepository.findById(10L)).thenReturn(Optional.of(gallery.getFirst()));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(gallery);

        assertThatThrownBy(() -> service.moveImage(STORE_ID, PUBLIC_ID, 10L, 5))
                .isInstanceOf(InvalidProductRequestException.class)
                .extracting("field").isEqualTo("sort_order");

        verify(imageRepository, never()).saveAll(ArgumentMatchers.<List<ProductImageEntity>>any());
    }

    @Test
    void deleteImageResequencesTheRemainingImages() {
        List<ProductImageEntity> gallery = gallery(10L, 11L, 12L);
        when(productRepository.findByStoreIdAndPublicId(STORE_ID, PUBLIC_ID))
                .thenReturn(Optional.of(productEntityOwnedBy(STORE_ID)));
        when(imageRepository.findById(10L)).thenReturn(Optional.of(gallery.getFirst()));
        when(imageRepository.findByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(gallery);

        service.deleteImage(STORE_ID, PUBLIC_ID, 10L);

        verify(mediaStorage).delete("k10");
        assertThat(gallery.get(1).getSortOrder()).isZero();
        assertThat(gallery.get(2).getSortOrder()).isEqualTo(1);
        verify(imageRepository).saveAll(List.of(gallery.get(1), gallery.get(2)));
    }

    /** Images of {@link #PRODUCT_ID} numbered 0..n-1 in the order the ids are given. */
    private static List<ProductImageEntity> gallery(Long... imageIds) {
        List<ProductImageEntity> images = new ArrayList<>();
        for (int position = 0; position < imageIds.length; position++) {
            ProductImageEntity image = new ProductImageEntity();
            image.setId(imageIds[position]);
            image.setProductId(PRODUCT_ID);
            image.setStorageKey("k" + imageIds[position]);
            image.setSortOrder(position);
            images.add(image);
        }
        return images;
    }
}
