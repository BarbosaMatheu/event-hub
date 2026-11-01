-- Orders now belong to a user and every order item points to the event it buys tickets for.
-- The unit price is a snapshot taken at purchase time (later price changes do not affect it).
--
-- user_id / event_id stay nullable at the database level ONLY because orders created before
-- this migration had neither. The application always sets both (JPA: optional = false).

ALTER TABLE TB_ORDERS ADD COLUMN user_id VARCHAR(255);
ALTER TABLE TB_ORDERS ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES TB_USERS (id);

ALTER TABLE TB_ORDER_ITEMS RENAME COLUMN price TO unit_price;
ALTER TABLE TB_ORDER_ITEMS ADD COLUMN event_id UUID;
ALTER TABLE TB_ORDER_ITEMS ADD CONSTRAINT fk_order_items_event FOREIGN KEY (event_id) REFERENCES TB_EVENTS (id);
ALTER TABLE TB_ORDER_ITEMS ADD CONSTRAINT ck_order_items_quantity CHECK (quantity > 0);
