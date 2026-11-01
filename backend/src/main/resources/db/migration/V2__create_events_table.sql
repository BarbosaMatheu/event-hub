CREATE TABLE TB_EVENTS (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    date_time TIMESTAMP,
    local VARCHAR(255),
    maximum_capacity INTEGER,
    price NUMERIC(19, 2),
    category_id UUID NOT NULL,
    CONSTRAINT fk_event_category FOREIGN KEY (category_id) REFERENCES TB_CATEGORIES (id)
);
