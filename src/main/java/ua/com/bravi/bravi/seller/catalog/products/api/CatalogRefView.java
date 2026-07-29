package ua.com.bravi.bravi.seller.catalog.products.api;

/** Посилання на суміжний агрегат каталогу (категорія/виробник): public id + назва. */
public record CatalogRefView(
        String id,
        String name
) {
}
