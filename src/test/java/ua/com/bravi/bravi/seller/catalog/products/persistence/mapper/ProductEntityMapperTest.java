package ua.com.bravi.bravi.seller.catalog.products.persistence.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;
import ua.com.bravi.bravi.seller.catalog.manufacturers.api.ManufacturerView;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;
import ua.com.bravi.bravi.seller.catalog.products.api.CatalogRefView;
import ua.com.bravi.bravi.seller.catalog.products.api.ProductView;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEntityMapperTest {

    private final ProductEntityMapper mapper = Mappers.getMapper(ProductEntityMapper.class);

    private static ProductEntity entity() {
        ProductEntity entity = new ProductEntity();
        entity.setId(42L);
        entity.setPublicId("prd_x");
        entity.setStoreId(7L);
        entity.setCategoryId(55L);
        entity.setManufacturerId(66L);
        entity.setName("Widget");
        entity.setCode("CODE-1");
        entity.setPrice(BigDecimal.ONE);
        entity.setStatus(ProductStatus.ACTIVE);
        return entity;
    }

    private static CategoryView category() {
        return new CategoryView(55L, "cat_x", 7L, null, null, "Ноутбуки", null,
                CategoryStatus.ACTIVE, null, null, null);
    }

    private static ManufacturerView manufacturer() {
        return new ManufacturerView(66L, "mnf_x", 7L, "Lenovo", null, ManufacturerStatus.ACTIVE, null, null);
    }

    @Test
    void toRefTakesPublicIdAsIdAndName() {
        CatalogRefView categoryRef = mapper.toRef(category());
        CatalogRefView manufacturerRef = mapper.toRef(manufacturer());

        assertThat(categoryRef).isEqualTo(new CatalogRefView("cat_x", "Ноутбуки"));
        assertThat(manufacturerRef).isEqualTo(new CatalogRefView("mnf_x", "Lenovo"));
    }

    @Test
    void toRefReturnsNullForMissingReference() {
        assertThat(mapper.toRef((CategoryView) null)).isNull();
        assertThat(mapper.toRef((ManufacturerView) null)).isNull();
    }

    @Test
    void toViewNestsRefsAndKeepsProductOwnFields() {
        ProductView view = mapper.toView(entity(), mapper.toRef(category()), mapper.toRef(manufacturer()), List.of(), List.of(), null);

        assertThat(view.id()).isEqualTo(42L);
        assertThat(view.publicId()).isEqualTo("prd_x");
        assertThat(view.name()).isEqualTo("Widget");
        assertThat(view.category().id()).isEqualTo("cat_x");
        assertThat(view.category().name()).isEqualTo("Ноутбуки");
        assertThat(view.manufacturer().id()).isEqualTo("mnf_x");
        assertThat(view.manufacturer().name()).isEqualTo("Lenovo");
    }

    @Test
    void toViewLeavesRefsNullForProductWithoutCategoryOrManufacturer() {
        ProductView view = mapper.toView(entity(), null, null, List.of(), List.of(), null);

        assertThat(view.category()).isNull();
        assertThat(view.manufacturer()).isNull();
        assertThat(view.name()).isEqualTo("Widget");
    }
}
