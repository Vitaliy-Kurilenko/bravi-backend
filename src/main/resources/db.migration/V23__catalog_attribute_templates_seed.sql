-- Starter library of common product attributes. A seller adopts a template into their store with one
-- call, which copies it into store_attributes together with its options; template_code stays as the
-- stable key for future marketplace mapping. Option positions are zero-based and gap-free so the copy
-- satisfies the ordering constraints on store_attribute_options.

INSERT INTO attribute_templates (code, name, value_type, unit_dictionary_code, unit_default_code,
                                 variant_defining, sort_order, created_at)
VALUES ('COLOR', 'Колір', 'SELECT', NULL, NULL, TRUE, 10, now()),
       ('SIZE', 'Розмір', 'SELECT', NULL, NULL, TRUE, 20, now()),
       ('MATERIAL', 'Матеріал', 'MULTI_SELECT', NULL, NULL, FALSE, 30, now()),
       ('GENDER', 'Стать', 'SELECT', NULL, NULL, FALSE, 40, now()),
       ('SEASON', 'Сезон', 'SELECT', NULL, NULL, FALSE, 50, now()),
       ('STYLE', 'Стиль', 'SELECT', NULL, NULL, FALSE, 60, now()),
       ('PATTERN', 'Візерунок', 'SELECT', NULL, NULL, FALSE, 70, now()),
       ('WEIGHT_NET', 'Вага нетто', 'NUMBER', 'WEIGHT_UNIT', 'G', FALSE, 80, now()),
       ('WARRANTY_MONTHS', 'Гарантія, місяців', 'NUMBER', NULL, NULL, FALSE, 90, now()),
       ('PACKAGE_CONTENTS', 'Комплектація', 'TEXT', NULL, NULL, FALSE, 100, now());


INSERT INTO attribute_template_options (template_id, code, name, sort_order)
SELECT t.id, v.code, v.name, v.sort_order
FROM attribute_templates t,
     (VALUES ('BLACK', 'Чорний', 0),
             ('WHITE', 'Білий', 1),
             ('GREY', 'Сірий', 2),
             ('RED', 'Червоний', 3),
             ('BLUE', 'Синій', 4),
             ('GREEN', 'Зелений', 5),
             ('YELLOW', 'Жовтий', 6),
             ('BEIGE', 'Бежевий', 7),
             ('BROWN', 'Коричневий', 8),
             ('MULTICOLOR', 'Різнокольоровий', 9)
     ) AS v(code, name, sort_order)
WHERE t.code = 'COLOR';

INSERT INTO attribute_template_options (template_id, code, name, sort_order)
SELECT t.id, v.code, v.name, v.sort_order
FROM attribute_templates t,
     (VALUES ('XS', 'XS', 0),
             ('S', 'S', 1),
             ('M', 'M', 2),
             ('L', 'L', 3),
             ('XL', 'XL', 4),
             ('XXL', 'XXL', 5),
             ('XXXL', 'XXXL', 6)
     ) AS v(code, name, sort_order)
WHERE t.code = 'SIZE';

INSERT INTO attribute_template_options (template_id, code, name, sort_order)
SELECT t.id, v.code, v.name, v.sort_order
FROM attribute_templates t,
     (VALUES ('COTTON', 'Бавовна', 0),
             ('POLYESTER', 'Поліестер', 1),
             ('LEATHER', 'Шкіра', 2),
             ('ECO_LEATHER', 'Екошкіра', 3),
             ('WOOL', 'Вовна', 4),
             ('LINEN', 'Льон', 5),
             ('METAL', 'Метал', 6),
             ('PLASTIC', 'Пластик', 7),
             ('WOOD', 'Дерево', 8),
             ('GLASS', 'Скло', 9)
     ) AS v(code, name, sort_order)
WHERE t.code = 'MATERIAL';

INSERT INTO attribute_template_options (template_id, code, name, sort_order)
SELECT t.id, v.code, v.name, v.sort_order
FROM attribute_templates t,
     (VALUES ('MALE', 'Чоловіча', 0),
             ('FEMALE', 'Жіноча', 1),
             ('UNISEX', 'Унісекс', 2),
             ('KIDS', 'Дитяча', 3)
     ) AS v(code, name, sort_order)
WHERE t.code = 'GENDER';

INSERT INTO attribute_template_options (template_id, code, name, sort_order)
SELECT t.id, v.code, v.name, v.sort_order
FROM attribute_templates t,
     (VALUES ('SUMMER', 'Літо', 0),
             ('WINTER', 'Зима', 1),
             ('DEMI', 'Демісезон', 2),
             ('ALL_SEASON', 'Всесезон', 3)
     ) AS v(code, name, sort_order)
WHERE t.code = 'SEASON';

INSERT INTO attribute_template_options (template_id, code, name, sort_order)
SELECT t.id, v.code, v.name, v.sort_order
FROM attribute_templates t,
     (VALUES ('CLASSIC', 'Класичний', 0),
             ('SPORT', 'Спортивний', 1),
             ('CASUAL', 'Кежуал', 2),
             ('EVENING', 'Вечірній', 3)
     ) AS v(code, name, sort_order)
WHERE t.code = 'STYLE';

INSERT INTO attribute_template_options (template_id, code, name, sort_order)
SELECT t.id, v.code, v.name, v.sort_order
FROM attribute_templates t,
     (VALUES ('SOLID', 'Однотонний', 0),
             ('STRIPES', 'Смужка', 1),
             ('CHECKS', 'Клітинка', 2),
             ('FLORAL', 'Квітковий', 3),
             ('GEOMETRIC', 'Геометричний', 4)
     ) AS v(code, name, sort_order)
WHERE t.code = 'PATTERN';
