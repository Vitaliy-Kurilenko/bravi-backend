-- Coarse RBAC seed: resource×action permissions + OWNER/MEMBER system roles per account type.

INSERT INTO permissions (code, resource, action, account_type, description, status, created_at)
VALUES ('STORE_READ', 'STORE', 'READ', 'SELLER', 'Read stores', 'ACTIVE', now()),
       ('STORE_WRITE', 'STORE', 'WRITE', 'SELLER', 'Manage stores', 'ACTIVE', now()),
       ('PRODUCT_READ', 'PRODUCT', 'READ', 'SELLER', 'Read store products', 'ACTIVE', now()),
       ('PRODUCT_WRITE', 'PRODUCT', 'WRITE', 'SELLER', 'Manage store products', 'ACTIVE', now()),
       ('ORDER_READ', 'ORDER', 'READ', 'SELLER', 'Read seller orders', 'ACTIVE', now()),
       ('ORDER_WRITE', 'ORDER', 'WRITE', 'SELLER', 'Manage seller orders', 'ACTIVE', now()),
       ('CHANNEL_READ', 'CHANNEL', 'READ', 'SELLER', 'Read sales channels', 'ACTIVE', now()),
       ('CHANNEL_WRITE', 'CHANNEL', 'WRITE', 'SELLER', 'Manage sales channels', 'ACTIVE', now()),
       ('SUPPLIER_PRODUCT_READ', 'SUPPLIER_PRODUCT', 'READ', 'SUPPLIER', 'Read supplier products', 'ACTIVE', now()),
       ('SUPPLIER_PRODUCT_WRITE', 'SUPPLIER_PRODUCT', 'WRITE', 'SUPPLIER', 'Manage supplier products', 'ACTIVE', now()),
       ('WAREHOUSE_READ', 'WAREHOUSE', 'READ', 'SUPPLIER', 'Read warehouses & stock', 'ACTIVE', now()),
       ('WAREHOUSE_WRITE', 'WAREHOUSE', 'WRITE', 'SUPPLIER', 'Manage warehouses & stock', 'ACTIVE', now()),
       ('SUPPLY_ORDER_READ', 'SUPPLY_ORDER', 'READ', 'SUPPLIER', 'Read supply orders', 'ACTIVE', now()),
       ('SUPPLY_ORDER_WRITE', 'SUPPLY_ORDER', 'WRITE', 'SUPPLIER', 'Manage supply orders', 'ACTIVE', now());

INSERT INTO roles (public_id, account_id, code, name, description, account_type, is_system, status, created_at)
VALUES ('sys-seller-owner', NULL, 'SELLER_OWNER', 'Seller Owner', 'Full access to the seller account', 'SELLER',
        TRUE, 'ACTIVE', now()),
       ('sys-seller-member', NULL, 'SELLER_MEMBER', 'Seller Member', 'Read access plus order handling', 'SELLER',
        TRUE, 'ACTIVE', now()),
       ('sys-supplier-owner', NULL, 'SUPPLIER_OWNER', 'Supplier Owner', 'Full access to the supplier account',
        'SUPPLIER', TRUE, 'ACTIVE', now()),
       ('sys-supplier-member', NULL, 'SUPPLIER_MEMBER', 'Supplier Member', 'Read-only supplier access', 'SUPPLIER',
        TRUE, 'ACTIVE', now());

-- OWNER roles get every permission of their account type
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM roles r
         JOIN permissions p ON p.account_type = r.account_type
WHERE r.code IN ('SELLER_OWNER', 'SUPPLIER_OWNER');

-- SELLER_MEMBER: all seller reads + order write
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM roles r
         JOIN permissions p ON p.account_type = 'SELLER' AND (p.action = 'READ' OR p.code = 'ORDER_WRITE')
WHERE r.code = 'SELLER_MEMBER';

-- SUPPLIER_MEMBER: all supplier reads
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM roles r
         JOIN permissions p ON p.account_type = 'SUPPLIER' AND p.action = 'READ'
WHERE r.code = 'SUPPLIER_MEMBER';
