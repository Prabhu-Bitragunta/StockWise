-- Reference schema for PostgreSQL. The backend also creates/updates tables on startup.
-- Connect to the supply_chain_mgmt database before running this file.
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(254) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  display_name VARCHAR(255) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
  email_verified BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

CREATE TABLE IF NOT EXISTS auth_sessions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_auth_session_hash ON auth_sessions(token_hash);

CREATE TABLE IF NOT EXISTS account_action_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash CHAR(64) NOT NULL UNIQUE,
  purpose VARCHAR(24) NOT NULL,
  expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  attempt_count INTEGER NOT NULL DEFAULT 0
);
ALTER TABLE account_action_tokens ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_account_action_expires ON account_action_tokens(expires_at);
CREATE TABLE IF NOT EXISTS pending_registration_otps (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  code_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  attempt_count INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pending_registration_otp_expiry ON pending_registration_otps(expires_at);

CREATE TABLE IF NOT EXISTS suppliers (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  contact_person VARCHAR(255), email VARCHAR(255), phone VARCHAR(255), address VARCHAR(500),
  lead_time_days INTEGER NOT NULL DEFAULT 7,
  rating DOUBLE PRECISION NOT NULL DEFAULT 4,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
  id BIGSERIAL PRIMARY KEY,
  sku VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(160) NOT NULL, description VARCHAR(1000),
  category VARCHAR(100) NOT NULL, unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  cost_price DECIMAL(12,2) NOT NULL DEFAULT 0, current_stock INTEGER NOT NULL DEFAULT 0,
  quantity INTEGER NOT NULL DEFAULT 0, reorder_level INTEGER NOT NULL DEFAULT 0,
  reorder_point INTEGER NOT NULL DEFAULT 0, optimal_stock_level INTEGER NOT NULL DEFAULT 0,
  supplier_id BIGINT REFERENCES suppliers(id) ON DELETE SET NULL, supplier_name VARCHAR(160),
  sold_last_30_days BIGINT NOT NULL DEFAULT 0, sold_last30days BIGINT NOT NULL DEFAULT 0,
  unit VARCHAR(30) NOT NULL DEFAULT 'pcs',
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_product_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_product_supplier ON products(supplier_id);

CREATE TABLE IF NOT EXISTS purchase_orders (
  id BIGSERIAL PRIMARY KEY,
  po_number VARCHAR(40) NOT NULL UNIQUE, supplier_id BIGINT REFERENCES suppliers(id) ON DELETE SET NULL,
  created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING', total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  expected_delivery_date DATE, created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  supplier_name VARCHAR(160), line_count INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_purchase_order_status ON purchase_orders(status);

CREATE TABLE IF NOT EXISTS purchase_order_items (
  id BIGSERIAL PRIMARY KEY,
  purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES products(id),
  quantity INTEGER NOT NULL, unit_cost DECIMAL(12,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_po_item_product ON purchase_order_items(product_id);

CREATE TABLE IF NOT EXISTS stock_transactions (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES products(id), user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  type VARCHAR(20) NOT NULL, quantity INTEGER NOT NULL, reference_note VARCHAR(500),
  transaction_date TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  product_name VARCHAR(160), note VARCHAR(500), created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_stock_product_date ON stock_transactions(product_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_stock_type_date ON stock_transactions(type, transaction_date);

CREATE TABLE IF NOT EXISTS manager_role_requests (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id), reason VARCHAR(1000) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_by BIGINT REFERENCES users(id), reviewed_at TIMESTAMP(6) WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_manager_request_status_created ON manager_role_requests(status, created_at);
CREATE INDEX IF NOT EXISTS idx_manager_request_user ON manager_role_requests(user_id);
