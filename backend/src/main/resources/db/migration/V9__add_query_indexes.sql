-- Indexes for the access patterns used by the API.
CREATE INDEX idx_events_starts_at ON TB_EVENTS (starts_at);
CREATE INDEX idx_events_category_id ON TB_EVENTS (category_id);
CREATE INDEX idx_orders_user_id ON TB_ORDERS (user_id);
CREATE INDEX idx_orders_status_created_at ON TB_ORDERS (status, created_at);
CREATE INDEX idx_order_items_order_id ON TB_ORDER_ITEMS (order_id);
CREATE INDEX idx_order_items_event_id ON TB_ORDER_ITEMS (event_id);
