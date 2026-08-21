-- A tag now carries the colour its badge is drawn in, which supersedes the "no colour" note in V25.
-- The value is a presentation one, kept as the seller picked it in a colour picker; the seven-char
-- upper-case #RRGGBB form is the only shape the application ever writes.
--
-- The column is NOT NULL because a tag never lacks a colour: one submitted without a colour — and
-- every tag minted implicitly from a name — is given one from a fixed palette. Existing rows are
-- backfilled the same way, spread across the palette so a store's tags do not come out identical.

ALTER TABLE store_tags ADD COLUMN color VARCHAR(7);

UPDATE store_tags SET color = (ARRAY[
    '#E5484D', '#F76B15', '#FFB224', '#46A758',
    '#12A594', '#0091FF', '#8E4EC6', '#E93D82'
])[(id % 8) + 1] WHERE color IS NULL;

ALTER TABLE store_tags ALTER COLUMN color SET NOT NULL;

-- No DEFAULT: the colour is chosen in the application, where it depends on the tag's name. A
-- default here would quietly paper over a write path that forgot to pick one.

-- The shape is a schema fact, not a business rule, so the database states it too.
ALTER TABLE store_tags
    ADD CONSTRAINT ck_store_tags_color CHECK (color ~ '^#[0-9A-F]{6}$');
