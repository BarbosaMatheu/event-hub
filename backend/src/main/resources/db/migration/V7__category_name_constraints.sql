UPDATE TB_CATEGORIES SET name = 'Uncategorized' WHERE name IS NULL;
ALTER TABLE TB_CATEGORIES ALTER COLUMN name SET NOT NULL;
ALTER TABLE TB_CATEGORIES ALTER COLUMN name TYPE VARCHAR(100);

-- Category names are unique, ignoring case ("Music" and "music" are the same category).
CREATE UNIQUE INDEX uq_categories_name_lower ON TB_CATEGORIES (lower(name));
