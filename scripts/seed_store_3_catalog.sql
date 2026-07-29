-- Тестові дані каталогу для store_id = 3: 20 виробників, 20 категорій, 30 товарів.
--
-- ЦЕ НЕ FLYWAY-МІГРАЦІЯ. Не переносити у src/main/resources/db.migration —
-- це разові дані для локальної розробки, а не зміна схеми.
--
-- Запуск:  psql "$DB_URL" -f scripts/seed_store_3_catalog.sql
--
-- Скрипт ідемпотентний: повторний запуск нічого не дублює (ON CONFLICT DO NOTHING
-- за тими самими унікальними констрейнтами, що їх перевіряє застосунок).
-- Усе в одній транзакції: або наповнюється повністю, або відкочується.

BEGIN;

-- Магазин має існувати: FK не дасть вставити, але явна помилка зрозуміліша.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM stores WHERE id = 3) THEN
        RAISE EXCEPTION 'Store id=3 не знайдено — створи магазин через онбординг перед сідом';
    END IF;
END $$;

-- public_id у форматі PublicIdGenerator: префікс + '_' + 16 символів base62.
-- pg_temp — тимчасова схема, функція живе лише в цій сесії й прибирати її не треба.
CREATE OR REPLACE FUNCTION pg_temp.public_id(prefix TEXT) RETURNS TEXT AS $$
    SELECT prefix || '_' || string_agg(
        substr('0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz',
               1 + floor(random() * 62)::int, 1), '')
    FROM generate_series(1, 16);
$$ LANGUAGE SQL VOLATILE;


-- ── Виробники (20) ────────────────────────────────────────────────────────────
INSERT INTO store_manufacturers (store_id, public_id, name, description, status, created_at)
SELECT 3, pg_temp.public_id('mfr'), v.name, v.description, 'ACTIVE',
       (now() AT TIME ZONE 'UTC') - (v.ord * INTERVAL '1 hour')
FROM (VALUES
    ( 1, 'Samsung',      'Побутова техніка та електроніка'),
    ( 2, 'LG',           'Побутова техніка, дисплеї'),
    ( 3, 'Sony',         'Аудіо, фото, ігрові консолі'),
    ( 4, 'Apple',        'Смартфони, ноутбуки, аксесуари'),
    ( 5, 'Xiaomi',       'Смартфони та розумний дім'),
    ( 6, 'Bosch',        'Побутова техніка та інструмент'),
    ( 7, 'Philips',      'Освітлення та догляд'),
    ( 8, 'Dell',         'Ноутбуки та монітори'),
    ( 9, 'HP',           'Ноутбуки, принтери'),
    (10, 'Lenovo',       'Ноутбуки та планшети'),
    (11, 'Asus',         'Комплектуючі та ноутбуки'),
    (12, 'Acer',         'Монітори та ноутбуки'),
    (13, 'Canon',        'Фототехніка та друк'),
    (14, 'Nikon',        'Фототехніка'),
    (15, 'JBL',          'Портативна акустика'),
    (16, 'Bose',         'Аудіосистеми та навушники'),
    (17, 'Logitech',     'Периферія для ПК'),
    (18, 'Kingston',     'Пам''ять та накопичувачі'),
    (19, 'Western Digital', 'Жорсткі диски та SSD'),
    (20, 'Anker',        'Зарядні пристрої та кабелі')
) AS v(ord, name, description)
ON CONFLICT ON CONSTRAINT uq_store_manufacturers_store_name DO NOTHING;


-- ── Категорії: 6 кореневих ────────────────────────────────────────────────────
INSERT INTO store_categories (store_id, parent_id, public_id, name, description, status, created_at)
SELECT 3, NULL, pg_temp.public_id('cat'), v.name, v.description, 'ACTIVE',
       (now() AT TIME ZONE 'UTC') - (v.ord * INTERVAL '1 hour')
FROM (VALUES
    (1, 'Електроніка',      'Смартфони, планшети, гаджети'),
    (2, 'Комп''ютери',      'Ноутбуки, ПК, комплектуючі'),
    (3, 'Побутова техніка', 'Техніка для дому та кухні'),
    (4, 'Аудіо',            'Навушники та акустика'),
    (5, 'Фото та відео',    'Камери та оптика'),
    (6, 'Аксесуари',        'Кабелі, чохли, зарядні')
) AS v(ord, name, description)
ON CONFLICT (store_id, name) WHERE parent_id IS NULL DO NOTHING;

-- ── Категорії: 14 дочірніх (parent резолвиться за назвою) ─────────────────────
INSERT INTO store_categories (store_id, parent_id, public_id, name, description, status, created_at)
SELECT 3,
       (SELECT id FROM store_categories WHERE store_id = 3 AND name = v.parent AND parent_id IS NULL),
       pg_temp.public_id('cat'), v.name, v.description, 'ACTIVE',
       (now() AT TIME ZONE 'UTC') - (v.ord * INTERVAL '1 hour')
FROM (VALUES
    ( 7, 'Смартфони',        'Електроніка',      'Мобільні телефони'),
    ( 8, 'Планшети',         'Електроніка',      'Планшетні комп''ютери'),
    ( 9, 'Розумний дім',     'Електроніка',      'Датчики та автоматизація'),
    (10, 'Ноутбуки',         'Комп''ютери',      'Портативні комп''ютери'),
    (11, 'Монітори',         'Комп''ютери',      'Дисплеї для ПК'),
    (12, 'Накопичувачі',     'Комп''ютери',      'SSD, HDD, флешки'),
    (13, 'Периферія',        'Комп''ютери',      'Миші, клавіатури'),
    (14, 'Холодильники',     'Побутова техніка', 'Холодильна техніка'),
    (15, 'Пральні машини',   'Побутова техніка', 'Пральна техніка'),
    (16, 'Кухонна техніка',  'Побутова техніка', 'Дрібна кухонна техніка'),
    (17, 'Навушники',        'Аудіо',            'Провідні та бездротові'),
    (18, 'Портативна акустика', 'Аудіо',         'Колонки та саундбари'),
    (19, 'Фотоапарати',      'Фото та відео',    'Дзеркальні та бездзеркальні'),
    (20, 'Зарядні пристрої', 'Аксесуари',        'Адаптери та павербанки')
) AS v(ord, name, parent, description)
ON CONFLICT (store_id, parent_id, name) WHERE parent_id IS NOT NULL DO NOTHING;


-- ── Товари (30) ───────────────────────────────────────────────────────────────
-- Категорія/виробник/статус наявності резолвляться за назвою чи кодом, тож скрипт
-- не залежить від згенерованих id. price — NUMERIC(19,4), розміри — NUMERIC(12,3).
INSERT INTO store_products (store_id, public_id, category_id, manufacturer_id, stock_status_id,
                            name, sku, code, description, price, quantity,
                            weight, width, height, length, status, created_at)
SELECT 3,
       pg_temp.public_id('prd'),
       (SELECT id FROM store_categories    WHERE store_id = 3 AND name = v.category),
       (SELECT id FROM store_manufacturers WHERE store_id = 3 AND name = v.manufacturer),
       (SELECT id FROM stock_statuses      WHERE code = v.stock_status),
       v.name, v.sku, v.code, v.description, v.price, v.quantity,
       v.weight, v.width, v.height, v.length, v.status,
       (now() AT TIME ZONE 'UTC') - (v.ord * INTERVAL '3 hours')
FROM (VALUES
    ( 1, 'Samsung Galaxy S24 128GB',      'SKU-PHN-0001', 'PRD-0001', 'Флагманський смартфон, 6.2"',        'Смартфони',           'Samsung',          'IN_STOCK',     28999.00,  14, 0.167, 0.070, 0.008, 0.147, 'ACTIVE'),
    ( 2, 'Samsung Galaxy A55 256GB',      'SKU-PHN-0002', 'PRD-0002', 'Середній сегмент, AMOLED',           'Смартфони',           'Samsung',          'IN_STOCK',     16499.00,  32, 0.213, 0.077, 0.008, 0.161, 'ACTIVE'),
    ( 3, 'Apple iPhone 15 128GB',         'SKU-PHN-0003', 'PRD-0003', 'USB-C, 6.1", A16 Bionic',            'Смартфони',           'Apple',            'IN_STOCK',     41999.00,   9, 0.171, 0.072, 0.008, 0.148, 'ACTIVE'),
    ( 4, 'Apple iPhone 15 Pro 256GB',     'SKU-PHN-0004', 'PRD-0004', 'Титановий корпус, A17 Pro',          'Смартфони',           'Apple',            'PREORDER',     59999.00,   0, 0.187, 0.077, 0.008, 0.150, 'ACTIVE'),
    ( 5, 'Xiaomi Redmi Note 13 256GB',    'SKU-PHN-0005', 'PRD-0005', 'Бюджетний, 120 Гц',                  'Смартфони',           'Xiaomi',           'IN_STOCK',      8999.00,  57, 0.188, 0.076, 0.008, 0.162, 'ACTIVE'),
    ( 6, 'Xiaomi 14 512GB',               'SKU-PHN-0006', 'PRD-0006', 'Флагман з оптикою Leica',            'Смартфони',           'Xiaomi',           'OUT_OF_STOCK', 34999.00,   0, 0.193, 0.072, 0.008, 0.153, 'ACTIVE'),
    ( 7, 'Apple iPad Air 11" 128GB',      'SKU-TAB-0007', 'PRD-0007', 'Планшет на M2',                      'Планшети',            'Apple',            'IN_STOCK',     27499.00,  11, 0.462, 0.179, 0.006, 0.248, 'ACTIVE'),
    ( 8, 'Samsung Galaxy Tab S9 256GB',   'SKU-TAB-0008', 'PRD-0008', 'AMOLED 11", S Pen у комплекті',       'Планшети',            'Samsung',          'IN_STOCK',     31999.00,   6, 0.498, 0.165, 0.006, 0.254, 'ACTIVE'),
    ( 9, 'Lenovo Tab P12 128GB',          'SKU-TAB-0009', 'PRD-0009', 'Великий екран 12.7"',                'Планшети',            'Lenovo',           'IN_STOCK',     14999.00,  18, 0.615, 0.185, 0.007, 0.293, 'ACTIVE'),
    (10, 'Xiaomi Smart Hub 2',            'SKU-SMH-0010', 'PRD-0010', 'Центр керування розумним домом',     'Розумний дім',        'Xiaomi',           'IN_STOCK',      2499.00,  43, 0.180, 0.095, 0.030, 0.095, 'ACTIVE'),
    (11, 'Philips Hue Starter Kit',       'SKU-SMH-0011', 'PRD-0011', 'Розумне освітлення, 3 лампи',        'Розумний дім',        'Philips',          'IN_STOCK',      5899.00,  22, 0.720, 0.210, 0.110, 0.230, 'ACTIVE'),
    (12, 'Apple MacBook Air 13" M3',      'SKU-NTB-0012', 'PRD-0012', '8/256 ГБ, до 18 год автономності',   'Ноутбуки',            'Apple',            'IN_STOCK',     52999.00,   7, 1.240, 0.304, 0.011, 0.215, 'ACTIVE'),
    (13, 'Dell XPS 14',                   'SKU-NTB-0013', 'PRD-0013', 'Core Ultra 7, 16/512 ГБ',            'Ноутбуки',            'Dell',             'IN_STOCK',     67999.00,   4, 1.680, 0.320, 0.018, 0.216, 'ACTIVE'),
    (14, 'HP Pavilion 15',                'SKU-NTB-0014', 'PRD-0014', 'Ryzen 5, 16/512 ГБ',                 'Ноутбуки',            'HP',               'IN_STOCK',     27999.00,  15, 1.750, 0.360, 0.020, 0.234, 'ACTIVE'),
    (15, 'Lenovo ThinkPad E14',           'SKU-NTB-0015', 'PRD-0015', 'Бізнес-серія, 16/512 ГБ',            'Ноутбуки',            'Lenovo',           'IN_STOCK',     38999.00,   9, 1.590, 0.324, 0.018, 0.220, 'ACTIVE'),
    (16, 'Asus ROG Strix G16',            'SKU-NTB-0016', 'PRD-0016', 'Ігровий, RTX 4060',                  'Ноутбуки',            'Asus',             'OUT_OF_STOCK', 71999.00,   0, 2.500, 0.354, 0.023, 0.264, 'ACTIVE'),
    (17, 'LG UltraGear 27" 165Hz',        'SKU-MON-0017', 'PRD-0017', 'Ігровий монітор QHD',                'Монітори',            'LG',               'IN_STOCK',     12999.00,  13, 6.200, 0.614, 0.450, 0.230, 'ACTIVE'),
    (18, 'Dell UltraSharp U2723QE',       'SKU-MON-0018', 'PRD-0018', '27" 4K IPS Black, USB-C hub',        'Монітори',            'Dell',             'IN_STOCK',     21499.00,   5, 6.800, 0.611, 0.520, 0.185, 'ACTIVE'),
    (19, 'Acer Nitro 24" 180Hz',          'SKU-MON-0019', 'PRD-0019', 'Бюджетний ігровий FHD',              'Монітори',            'Acer',             'IN_STOCK',      6499.00,  27, 3.400, 0.540, 0.400, 0.200, 'ACTIVE'),
    (20, 'Samsung 990 PRO 1TB NVMe',      'SKU-SSD-0020', 'PRD-0020', 'PCIe 4.0, до 7450 МБ/с',             'Накопичувачі',        'Samsung',          'IN_STOCK',      5299.00,  61, 0.009, 0.022, 0.002, 0.080, 'ACTIVE'),
    (21, 'Kingston NV2 2TB NVMe',         'SKU-SSD-0021', 'PRD-0021', 'PCIe 4.0, бюджетний',                'Накопичувачі',        'Kingston',         'IN_STOCK',      4199.00,  48, 0.008, 0.022, 0.002, 0.080, 'ACTIVE'),
    (22, 'WD Blue 4TB HDD',               'SKU-HDD-0022', 'PRD-0022', '3.5", 5400 об/хв',                   'Накопичувачі',        'Western Digital',  'IN_STOCK',      3899.00,  33, 0.450, 0.101, 0.026, 0.147, 'ACTIVE'),
    (23, 'Logitech MX Master 3S',         'SKU-PER-0023', 'PRD-0023', 'Бездротова миша для роботи',         'Периферія',           'Logitech',         'IN_STOCK',      3999.00,  40, 0.141, 0.084, 0.051, 0.125, 'ACTIVE'),
    (24, 'Logitech MX Keys S',            'SKU-PER-0024', 'PRD-0024', 'Клавіатура з підсвіткою',            'Периферія',           'Logitech',         'IN_STOCK',      4499.00,  25, 0.810, 0.430, 0.021, 0.132, 'ACTIVE'),
    (25, 'Bosch Serie 4 No Frost',        'SKU-REF-0025', 'PRD-0025', 'Холодильник 186 см, No Frost',       'Холодильники',        'Bosch',            'IN_STOCK',     32999.00,   3, 67.000, 0.600, 1.860, 0.650, 'ACTIVE'),
    (26, 'LG DoorCooling+ 2м',            'SKU-REF-0026', 'PRD-0026', 'Холодильник з інверторним компресором', 'Холодильники',     'LG',               'PREORDER',     41999.00,   0, 78.000, 0.595, 2.030, 0.682, 'ACTIVE'),
    (27, 'Bosch Serie 6 9кг',             'SKU-WSH-0027', 'PRD-0027', 'Пральна машина, 1400 об/хв',         'Пральні машини',      'Bosch',            'IN_STOCK',     24999.00,   6, 72.000, 0.598, 0.848, 0.590, 'ACTIVE'),
    (28, 'Philips Airfryer XXL',          'SKU-KIT-0028', 'PRD-0028', 'Мультипіч 7.3 л',                    'Кухонна техніка',     'Philips',          'IN_STOCK',      8999.00,  17, 8.100, 0.320, 0.400, 0.430, 'ACTIVE'),
    (29, 'Sony WH-1000XM5',               'SKU-HPH-0029', 'PRD-0029', 'Накладні з шумозаглушенням',         'Навушники',           'Sony',             'IN_STOCK',     13999.00,  21, 0.250, 0.170, 0.080, 0.250, 'ACTIVE'),
    (30, 'JBL Charge 5',                  'SKU-SPK-0030', 'PRD-0030', 'Портативна колонка, IP67',           'Портативна акустика', 'JBL',              'IN_STOCK',      5499.00,  29, 0.960, 0.223, 0.097, 0.094, 'ACTIVE')
) AS v(ord, name, sku, code, description, category, manufacturer, stock_status,
       price, quantity, weight, width, height, length, status)
ON CONFLICT ON CONSTRAINT uq_store_products_store_code DO NOTHING;


-- Підсумок: має бути 20 / 20 / 30.
SELECT 'store_manufacturers' AS table_name, count(*) AS rows FROM store_manufacturers WHERE store_id = 3
UNION ALL
SELECT 'store_categories', count(*) FROM store_categories WHERE store_id = 3
UNION ALL
SELECT 'store_products', count(*) FROM store_products WHERE store_id = 3;

COMMIT;
