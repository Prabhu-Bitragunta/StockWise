-- Reference schema for a fresh MySQL 8 database. Existing installations are
-- upgraded additively by the backend; do not drop tables to apply this file.
CREATE DATABASE IF NOT EXISTS supply_chain_mgmt CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE supply_chain_mgmt;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(254) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  display_name VARCHAR(255) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_users_role (role)
);
CREATE TABLE IF NOT EXISTS auth_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_auth_session_hash (token_hash)
);
CREATE TABLE IF NOT EXISTS suppliers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  contact_person VARCHAR(255), email VARCHAR(255), phone VARCHAR(255), address VARCHAR(500),
  lead_time_days INT NOT NULL DEFAULT 7,
  rating DOUBLE NOT NULL DEFAULT 4,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE IF NOT EXISTS products (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sku VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(160) NOT NULL, description VARCHAR(1000),
  category VARCHAR(100) NOT NULL, unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  cost_price DECIMAL(12,2) NOT NULL DEFAULT 0, current_stock INT NOT NULL DEFAULT 0,
  quantity INT NOT NULL DEFAULT 0, reorder_level INT NOT NULL DEFAULT 0,
  reorder_point INT NOT NULL DEFAULT 0, optimal_stock_level INT NOT NULL DEFAULT 0,
  supplier_id BIGINT, supplier_name VARCHAR(160), sold_last_30_days BIGINT NOT NULL DEFAULT 0,
  sold_last30days BIGINT NOT NULL DEFAULT 0, unit VARCHAR(30) NOT NULL DEFAULT 'pcs',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
  INDEX idx_product_category (category), INDEX idx_product_supplier (supplier_id)
);
CREATE TABLE IF NOT EXISTS purchase_orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  po_number VARCHAR(40) NOT NULL UNIQUE, supplier_id BIGINT, created_by BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING', total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  expected_delivery_date DATE, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  supplier_name VARCHAR(160), line_count INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
  CONSTRAINT fk_po_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_purchase_order_status (status)
);
CREATE TABLE IF NOT EXISTS purchase_order_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  purchase_order_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
  quantity INT NOT NULL, unit_cost DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_po_item_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_po_item_product FOREIGN KEY (product_id) REFERENCES products(id),
  INDEX idx_po_item_product (product_id)
);
CREATE TABLE IF NOT EXISTS stock_transactions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL, user_id BIGINT, type VARCHAR(20) NOT NULL, quantity INT NOT NULL,
  reference_note VARCHAR(500), transaction_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  productName VARCHAR(160), note VARCHAR(500), created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_transaction_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_stock_product_date (product_id, transaction_date), INDEX idx_stock_type_date (type, transaction_date)
);
