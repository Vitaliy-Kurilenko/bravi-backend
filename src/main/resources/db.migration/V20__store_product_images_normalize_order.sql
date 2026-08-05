-- Image position becomes the single source of truth for a product gallery.
-- Renumber every gallery into a gap-free zero-based sequence, keeping the image
-- currently flagged as primary at position 0.

WITH ordered AS (SELECT id,
                        ROW_NUMBER() OVER (PARTITION BY product_id
                            ORDER BY is_primary DESC, sort_order, id) - 1 AS position
                 FROM store_product_images)
UPDATE store_product_images img
SET sort_order = ordered.position
FROM ordered
WHERE img.id = ordered.id;
