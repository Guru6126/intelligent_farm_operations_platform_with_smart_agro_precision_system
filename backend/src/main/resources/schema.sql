-- AgroPrecision MySQL schema. Runs automatically at backend startup (idempotent).
CREATE TABLE IF NOT EXISTS users (
  id CHAR(36) PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(255),
  phone VARCHAR(50),
  reset_token VARCHAR(100),
  reset_expires DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  role VARCHAR(20) NOT NULL,
  UNIQUE KEY uq_user_role (user_id, role),
  CONSTRAINT fk_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS farms (
  id CHAR(36) PRIMARY KEY,
  owner_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  location VARCHAR(255),
  latitude DECIMAL(9,6),
  longitude DECIMAL(9,6),
  total_area_acres DECIMAL(12,2),
  soil_type VARCHAR(100),
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_farm_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS farm_members (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_member (farm_id, user_id),
  CONSTRAINT fk_member_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS plots (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  area_acres DECIMAL(12,2),
  soil_ph DECIMAL(4,2),
  boundary_geojson JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_farm_plot_name (farm_id, name),
  CONSTRAINT fk_plot_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS crops (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  plot_id CHAR(36),
  crop_name VARCHAR(255) NOT NULL,
  variety VARCHAR(255),
  season VARCHAR(100),
  stage VARCHAR(30) NOT NULL DEFAULT 'planned',
  sowing_date DATE,
  expected_harvest DATE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_crop_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_crop_plot FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS crop_activities (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  crop_id CHAR(36),
  activity VARCHAR(255) NOT NULL,
  notes TEXT,
  activity_date DATE NOT NULL DEFAULT (CURRENT_DATE),
  created_by CHAR(36),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_act_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_act_crop FOREIGN KEY (crop_id) REFERENCES crops(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS harvests (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  crop_id CHAR(36),
  harvest_date DATE NOT NULL DEFAULT (CURRENT_DATE),
  quantity_kg DECIMAL(14,2) NOT NULL,
  revenue DECIMAL(14,2) DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_harv_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_harv_crop FOREIGN KEY (crop_id) REFERENCES crops(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS inventory_items (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(50) NOT NULL DEFAULT 'fertilizer',
  unit VARCHAR(20) NOT NULL DEFAULT 'kg',
  quantity DECIMAL(14,3) NOT NULL DEFAULT 0,
  reorder_level DECIMAL(14,3) NOT NULL DEFAULT 0,
  unit_cost DECIMAL(12,2) DEFAULT 0,
  expiry_date DATE,
  supplier VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inv_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS stock_transactions (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  item_id CHAR(36) NOT NULL,
  txn_type VARCHAR(3) NOT NULL,
  quantity DECIMAL(14,3) NOT NULL,
  note TEXT,
  created_by CHAR(36),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_txn_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_txn_item FOREIGN KEY (item_id) REFERENCES inventory_items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS weather_records (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  recorded_on DATE NOT NULL DEFAULT (CURRENT_DATE),
  temperature_c DECIMAL(5,2),
  humidity_pct DECIMAL(5,2),
  rainfall_mm DECIMAL(8,2),
  wind_kmh DECIMAL(6,2),
  `condition` VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wx_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS irrigation_schedules (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  plot_id CHAR(36),
  water_source VARCHAR(100),
  method VARCHAR(30) DEFAULT 'drip',
  scheduled_at DATETIME NOT NULL,
  duration_min INT NOT NULL DEFAULT 30,
  water_litres DECIMAL(14,2) DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'upcoming',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_irr_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_irr_plot FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS equipment (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(100),
  status VARCHAR(20) NOT NULL DEFAULT 'available',
  next_service DATE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_eq_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS equipment_bookings (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  equipment_id CHAR(36) NOT NULL,
  starts_at DATETIME NOT NULL,
  ends_at DATETIME NOT NULL,
  purpose VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  booked_by CHAR(36),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_book_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_book_eq FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS maintenance_records (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  equipment_id CHAR(36),
  service_date DATE NOT NULL DEFAULT (CURRENT_DATE),
  description TEXT,
  cost DECIMAL(12,2) DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mnt_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE,
  CONSTRAINT fk_mnt_eq FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS workers (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  user_id CHAR(36),
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(50),
  skill VARCHAR(100),
  daily_wage DECIMAL(10,2) DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wrk_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  assignee_id CHAR(36),
  worker_id CHAR(36),
  due_date DATE,
  priority VARCHAR(10) NOT NULL DEFAULT 'medium',
  status VARCHAR(20) NOT NULL DEFAULT 'todo',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_task_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS budgets (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  season VARCHAR(100) NOT NULL,
  category VARCHAR(100) NOT NULL,
  planned_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_budget (farm_id, season, category),
  CONSTRAINT fk_bud_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS expenses (
  id CHAR(36) PRIMARY KEY,
  farm_id CHAR(36) NOT NULL,
  season VARCHAR(100),
  category VARCHAR(100) NOT NULL,
  amount DECIMAL(14,2) NOT NULL,
  spent_on DATE NOT NULL DEFAULT (CURRENT_DATE),
  description TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_exp_farm FOREIGN KEY (farm_id) REFERENCES farms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  farm_id CHAR(36),
  title VARCHAR(255) NOT NULL,
  body TEXT,
  severity VARCHAR(20) NOT NULL DEFAULT 'info',
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id CHAR(36),
  table_name VARCHAR(100) NOT NULL,
  action VARCHAR(20) NOT NULL,
  record_id CHAR(36),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
