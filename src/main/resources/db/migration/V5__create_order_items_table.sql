CREATE TABLE TB_ORDER_ITEMS (
    id UUID PRIMARY KEY,
    price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    order_id UUID,
    CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id) REFERENCES TB_ORDERS (id)
);
