package ua.com.bravi.bravi;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Removes business rows created by end-to-end tests while preserving Flyway-seeded reference data
 * (roles, permissions, role_permissions, *_statuses). Rows are deleted one table at a time in
 * FK-safe order, which leaves the seeded system roles in place.
 */
public final class TestDatabaseCleaner {

    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
            "order_shipments",
            "order_items",
            "orders",
            "store_product_attribute_values",
            "store_category_attributes",
            "store_attribute_options",
            "store_attributes",
            "store_product_discounts",
            "store_product_images",
            "store_products",
            "store_categories",
            "store_manufacturers",
            "store_contacts",
            "store_settings",
            "sales_channels",
            "stores",
            "seller_accounts",
            "membership_roles",
            "memberships",
            "accounts",
            "users"
    );

    private TestDatabaseCleaner() {
    }

    public static void clean(JdbcTemplate jdbcTemplate) {
        TABLES_IN_DELETE_ORDER.forEach(table -> jdbcTemplate.execute("DELETE FROM " + table));
    }
}
