package ua.com.bravi.bravi;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Removes business rows created by end-to-end tests while preserving Flyway-seeded reference data
 * (roles, permissions, role_permissions, *_statuses). Deletes in FK-safe order — a {@code TRUNCATE
 * ... CASCADE} would follow the {@code roles.account_id} FK and wipe the seeded system roles.
 */
public final class TestDatabaseCleaner {

    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
            "order_shipments",
            "order_items",
            "orders",
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
