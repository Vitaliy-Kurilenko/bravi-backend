-- Seed COUNTRY dictionary (ISO 3166-1 alpha-2 codes; meta: alpha3, numeric_code, phone_code).

INSERT INTO dictionaries (code, name, created_at)
VALUES ('COUNTRY', 'Країни', now());

INSERT INTO dictionary_items (dictionary_id, code, name, sort_order, meta, created_at)
SELECT d.id, v.code, v.name, v.sort_order, v.meta::jsonb, now()
FROM dictionaries d,
     (VALUES ('UA', 'Україна', 10, '{"alpha3": "UKR", "numeric_code": "804", "phone_code": "+380"}'),
             ('PL', 'Польща', 20, '{"alpha3": "POL", "numeric_code": "616", "phone_code": "+48"}'),
             ('CZ', 'Чехія', 30, '{"alpha3": "CZE", "numeric_code": "203", "phone_code": "+420"}'),
             ('SK', 'Словаччина', 40, '{"alpha3": "SVK", "numeric_code": "703", "phone_code": "+421"}'),
             ('HU', 'Угорщина', 50, '{"alpha3": "HUN", "numeric_code": "348", "phone_code": "+36"}'),
             ('RO', 'Румунія', 60, '{"alpha3": "ROU", "numeric_code": "642", "phone_code": "+40"}'),
             ('MD', 'Молдова', 70, '{"alpha3": "MDA", "numeric_code": "498", "phone_code": "+373"}'),
             ('BG', 'Болгарія', 80, '{"alpha3": "BGR", "numeric_code": "100", "phone_code": "+359"}'),
             ('DE', 'Німеччина', 90, '{"alpha3": "DEU", "numeric_code": "276", "phone_code": "+49"}'),
             ('AT', 'Австрія', 100, '{"alpha3": "AUT", "numeric_code": "040", "phone_code": "+43"}'),
             ('FR', 'Франція', 110, '{"alpha3": "FRA", "numeric_code": "250", "phone_code": "+33"}'),
             ('ES', 'Іспанія', 120, '{"alpha3": "ESP", "numeric_code": "724", "phone_code": "+34"}'),
             ('IT', 'Італія', 130, '{"alpha3": "ITA", "numeric_code": "380", "phone_code": "+39"}'),
             ('NL', 'Нідерланди', 140, '{"alpha3": "NLD", "numeric_code": "528", "phone_code": "+31"}'),
             ('BE', 'Бельгія', 150, '{"alpha3": "BEL", "numeric_code": "056", "phone_code": "+32"}'),
             ('PT', 'Португалія', 160, '{"alpha3": "PRT", "numeric_code": "620", "phone_code": "+351"}'),
             ('GB', 'Велика Британія', 170, '{"alpha3": "GBR", "numeric_code": "826", "phone_code": "+44"}'),
             ('IE', 'Ірландія', 180, '{"alpha3": "IRL", "numeric_code": "372", "phone_code": "+353"}'),
             ('CH', 'Швейцарія', 190, '{"alpha3": "CHE", "numeric_code": "756", "phone_code": "+41"}'),
             ('DK', 'Данія', 200, '{"alpha3": "DNK", "numeric_code": "208", "phone_code": "+45"}'),
             ('SE', 'Швеція', 210, '{"alpha3": "SWE", "numeric_code": "752", "phone_code": "+46"}'),
             ('NO', 'Норвегія', 220, '{"alpha3": "NOR", "numeric_code": "578", "phone_code": "+47"}'),
             ('FI', 'Фінляндія', 230, '{"alpha3": "FIN", "numeric_code": "246", "phone_code": "+358"}'),
             ('EE', 'Естонія', 240, '{"alpha3": "EST", "numeric_code": "233", "phone_code": "+372"}'),
             ('LV', 'Латвія', 250, '{"alpha3": "LVA", "numeric_code": "428", "phone_code": "+371"}'),
             ('LT', 'Литва', 260, '{"alpha3": "LTU", "numeric_code": "440", "phone_code": "+370"}'),
             ('GR', 'Греція', 270, '{"alpha3": "GRC", "numeric_code": "300", "phone_code": "+30"}'),
             ('HR', 'Хорватія', 280, '{"alpha3": "HRV", "numeric_code": "191", "phone_code": "+385"}'),
             ('SI', 'Словенія', 290, '{"alpha3": "SVN", "numeric_code": "705", "phone_code": "+386"}'),
             ('US', 'США', 300, '{"alpha3": "USA", "numeric_code": "840", "phone_code": "+1"}'),
             ('CA', 'Канада', 310, '{"alpha3": "CAN", "numeric_code": "124", "phone_code": "+1"}'),
             ('JP', 'Японія', 320, '{"alpha3": "JPN", "numeric_code": "392", "phone_code": "+81"}'),
             ('CN', 'Китай', 330, '{"alpha3": "CHN", "numeric_code": "156", "phone_code": "+86"}')
     ) AS v(code, name, sort_order, meta)
WHERE d.code = 'COUNTRY';
