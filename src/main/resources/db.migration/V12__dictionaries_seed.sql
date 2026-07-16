-- Seed base dictionaries: CURRENCY, LANGUAGE, WEIGHT_UNIT, DIMENSION_UNIT, TIMEZONE.

INSERT INTO dictionaries (code, name, created_at)
VALUES ('CURRENCY', 'Валюти', now()),
       ('LANGUAGE', 'Мови', now()),
       ('WEIGHT_UNIT', 'Одиниці ваги', now()),
       ('DIMENSION_UNIT', 'Одиниці виміру габаритів', now()),
       ('TIMEZONE', 'Часові пояси', now());

INSERT INTO dictionary_items (dictionary_id, code, name, sort_order, meta, created_at)
SELECT d.id, v.code, v.name, v.sort_order, v.meta::jsonb, now()
FROM dictionaries d,
     (VALUES ('UAH', 'Українська гривня', 10, '{"symbol": "₴", "numeric_code": "980"}'),
             ('USD', 'Долар США', 20, '{"symbol": "$", "numeric_code": "840"}'),
             ('EUR', 'Євро', 30, '{"symbol": "€", "numeric_code": "978"}'),
             ('GBP', 'Фунт стерлінгів', 40, '{"symbol": "£", "numeric_code": "826"}'),
             ('PLN', 'Польський злотий', 50, '{"symbol": "zł", "numeric_code": "985"}'),
             ('CHF', 'Швейцарський франк', 60, '{"symbol": "Fr", "numeric_code": "756"}'),
             ('CZK', 'Чеська крона', 70, '{"symbol": "Kč", "numeric_code": "203"}'),
             ('JPY', 'Японська єна', 80, '{"symbol": "¥", "numeric_code": "392"}'),
             ('CNY', 'Китайський юань', 90, '{"symbol": "¥", "numeric_code": "156"}'),
             ('CAD', 'Канадський долар', 100, '{"symbol": "C$", "numeric_code": "124"}')
     ) AS v(code, name, sort_order, meta)
WHERE d.code = 'CURRENCY';

INSERT INTO dictionary_items (dictionary_id, code, name, sort_order, meta, created_at)
SELECT d.id, v.code, v.name, v.sort_order, v.meta::jsonb, now()
FROM dictionaries d,
     (VALUES ('uk', 'Українська', 10, '{"native_name": "Українська"}'),
             ('en', 'Англійська', 20, '{"native_name": "English"}'),
             ('pl', 'Польська', 30, '{"native_name": "Polski"}'),
             ('de', 'Німецька', 40, '{"native_name": "Deutsch"}'),
             ('fr', 'Французька', 50, '{"native_name": "Français"}'),
             ('es', 'Іспанська', 60, '{"native_name": "Español"}'),
             ('it', 'Італійська', 70, '{"native_name": "Italiano"}'),
             ('cs', 'Чеська', 80, '{"native_name": "Čeština"}'),
             ('sk', 'Словацька', 90, '{"native_name": "Slovenčina"}'),
             ('ro', 'Румунська', 100, '{"native_name": "Română"}'),
             ('hu', 'Угорська', 110, '{"native_name": "Magyar"}'),
             ('bg', 'Болгарська', 120, '{"native_name": "Български"}')
     ) AS v(code, name, sort_order, meta)
WHERE d.code = 'LANGUAGE';

INSERT INTO dictionary_items (dictionary_id, code, name, sort_order, meta, created_at)
SELECT d.id, v.code, v.name, v.sort_order, v.meta::jsonb, now()
FROM dictionaries d,
     (VALUES ('KG', 'Кілограм', 10, '{"symbol": "кг"}'),
             ('G', 'Грам', 20, '{"symbol": "г"}'),
             ('T', 'Тонна', 30, '{"symbol": "т"}'),
             ('LB', 'Фунт', 40, '{"symbol": "lb"}'),
             ('OZ', 'Унція', 50, '{"symbol": "oz"}')
     ) AS v(code, name, sort_order, meta)
WHERE d.code = 'WEIGHT_UNIT';

INSERT INTO dictionary_items (dictionary_id, code, name, sort_order, meta, created_at)
SELECT d.id, v.code, v.name, v.sort_order, v.meta::jsonb, now()
FROM dictionaries d,
     (VALUES ('CM', 'Сантиметр', 10, '{"symbol": "см"}'),
             ('MM', 'Міліметр', 20, '{"symbol": "мм"}'),
             ('M', 'Метр', 30, '{"symbol": "м"}'),
             ('IN', 'Дюйм', 40, '{"symbol": "in"}')
     ) AS v(code, name, sort_order, meta)
WHERE d.code = 'DIMENSION_UNIT';

INSERT INTO dictionary_items (dictionary_id, code, name, sort_order, meta, created_at)
SELECT d.id, v.code, v.name, v.sort_order, '{}'::jsonb, now()
FROM dictionaries d,
     (VALUES ('Europe/Kyiv', 'Київ', 10),
             ('Europe/Warsaw', 'Варшава', 20),
             ('Europe/Prague', 'Прага', 30),
             ('Europe/Bratislava', 'Братислава', 40),
             ('Europe/Budapest', 'Будапешт', 50),
             ('Europe/Bucharest', 'Бухарест', 60),
             ('Europe/Chisinau', 'Кишинів', 70),
             ('Europe/Sofia', 'Софія', 80),
             ('Europe/Berlin', 'Берлін', 90),
             ('Europe/Vienna', 'Відень', 100),
             ('Europe/Paris', 'Париж', 110),
             ('Europe/Madrid', 'Мадрид', 120),
             ('Europe/Rome', 'Рим', 130),
             ('Europe/Amsterdam', 'Амстердам', 140),
             ('Europe/Lisbon', 'Лісабон', 150),
             ('Europe/London', 'Лондон', 160),
             ('America/New_York', 'Нью-Йорк', 170),
             ('America/Chicago', 'Чикаго', 180),
             ('America/Denver', 'Денвер', 190),
             ('America/Los_Angeles', 'Лос-Анджелес', 200),
             ('UTC', 'UTC', 210)
     ) AS v(code, name, sort_order)
WHERE d.code = 'TIMEZONE';
