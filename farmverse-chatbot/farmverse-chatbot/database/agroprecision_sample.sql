CREATE DATABASE IF NOT EXISTS agroprecision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agroprecision;

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS farms (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id         BIGINT NOT NULL,
    name             VARCHAR(150) NOT NULL,
    location         VARCHAR(200),
    total_area_acres DECIMAL(10,2),
    soil_type        VARCHAR(80),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crops (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id          BIGINT NOT NULL,
    crop_name        VARCHAR(100) NOT NULL,
    variety          VARCHAR(100),
    season           VARCHAR(50),
    stage            VARCHAR(50),
    sowing_date      DATE,
    expected_harvest DATE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (farm_id) REFERENCES farms(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS harvests (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id      BIGINT NOT NULL,
    crop_id      BIGINT NOT NULL,
    harvest_date DATE NOT NULL,
    quantity_kg  DECIMAL(12,2) NOT NULL,
    revenue      DECIMAL(12,2),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (farm_id) REFERENCES farms(id),
    FOREIGN KEY (crop_id) REFERENCES crops(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @first_this_month = DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE()) - 1 DAY);
SET @first_last_month = DATE_SUB(@first_this_month, INTERVAL 1 MONTH);

INSERT INTO users (full_name, email)
SELECT 'Kumaran', 'kumaran@example.com' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE full_name = 'Kumaran');
INSERT INTO users (full_name, email)
SELECT 'Priya Sharma', 'priya@example.com' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE full_name = 'Priya Sharma');
INSERT INTO users (full_name, email)
SELECT 'Arjun Rao', 'arjun@example.com' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE full_name = 'Arjun Rao');
INSERT INTO users (full_name, email)
SELECT 'Ravi Kumar', 'ravi@example.com' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE full_name = 'Ravi Kumar');
INSERT INTO users (full_name, email)
SELECT 'Geetha Rani', 'geetha@example.com' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE full_name = 'Geetha Rani');

INSERT INTO farms (owner_id, name, location, total_area_acres, soil_type)
SELECT u.id, 'Kumaran Green Acres', 'Guntur, Andhra Pradesh', 5.50, 'Black soil'
FROM users u WHERE u.full_name = 'Kumaran'
  AND NOT EXISTS (SELECT 1 FROM farms f WHERE f.owner_id = u.id AND f.name = 'Kumaran Green Acres');
INSERT INTO farms (owner_id, name, location, total_area_acres, soil_type)
SELECT u.id, 'Priya Orchard', 'Nashik, Maharashtra', 3.00, 'Red soil'
FROM users u WHERE u.full_name = 'Priya Sharma'
  AND NOT EXISTS (SELECT 1 FROM farms f WHERE f.owner_id = u.id AND f.name = 'Priya Orchard');
INSERT INTO farms (owner_id, name, location, total_area_acres, soil_type)
SELECT u.id, 'Rao Fields', 'Warangal, Telangana', 2.25, 'Loamy soil'
FROM users u WHERE u.full_name = 'Arjun Rao'
  AND NOT EXISTS (SELECT 1 FROM farms f WHERE f.owner_id = u.id AND f.name = 'Rao Fields');
INSERT INTO farms (owner_id, name, location, total_area_acres, soil_type)
SELECT u.id, 'Ravi Farm', 'Mandya, Karnataka', 4.00, 'Clay soil'
FROM users u WHERE u.full_name = 'Ravi Kumar'
  AND NOT EXISTS (SELECT 1 FROM farms f WHERE f.owner_id = u.id AND f.name = 'Ravi Farm');
INSERT INTO farms (owner_id, name, location, total_area_acres, soil_type)
SELECT u.id, 'Rani Cotton Farm', 'Adilabad, Telangana', 6.00, 'Black soil'
FROM users u WHERE u.full_name = 'Geetha Rani'
  AND NOT EXISTS (SELECT 1 FROM farms f WHERE f.owner_id = u.id AND f.name = 'Rani Cotton Farm');

INSERT INTO crops (farm_id, crop_name, variety, season, stage, sowing_date, expected_harvest)
SELECT f.id, 'Rice', 'MTU-1010', 'Kharif', 'Harvested', DATE_SUB(@first_last_month, INTERVAL 120 DAY), DATE_ADD(@first_last_month, INTERVAL 9 DAY)
FROM farms f WHERE f.name = 'Kumaran Green Acres'
  AND NOT EXISTS (SELECT 1 FROM crops c WHERE c.farm_id = f.id AND c.crop_name = 'Rice');
INSERT INTO crops (farm_id, crop_name, variety, season, stage, sowing_date, expected_harvest)
SELECT f.id, 'Tomato', 'Arka Rakshak', 'Rabi', 'Fruiting', DATE_SUB(@first_last_month, INTERVAL 60 DAY), DATE_ADD(@first_this_month, INTERVAL 20 DAY)
FROM farms f WHERE f.name = 'Kumaran Green Acres'
  AND NOT EXISTS (SELECT 1 FROM crops c WHERE c.farm_id = f.id AND c.crop_name = 'Tomato');
INSERT INTO crops (farm_id, crop_name, variety, season, stage, sowing_date, expected_harvest)
SELECT f.id, 'Cotton', 'Bt Hybrid', 'Kharif', 'Flowering', DATE_SUB(@first_this_month, INTERVAL 90 DAY), DATE_ADD(@first_this_month, INTERVAL 60 DAY)
FROM farms f WHERE f.name = 'Priya Orchard'
  AND NOT EXISTS (SELECT 1 FROM crops c WHERE c.farm_id = f.id AND c.crop_name = 'Cotton');
INSERT INTO crops (farm_id, crop_name, variety, season, stage, sowing_date, expected_harvest)
SELECT f.id, 'Maize', 'DKC-9081', 'Kharif', 'Harvested', DATE_SUB(@first_last_month, INTERVAL 200 DAY), DATE_SUB(@first_last_month, INTERVAL 45 DAY)
FROM farms f WHERE f.name = 'Ravi Farm'
  AND NOT EXISTS (SELECT 1 FROM crops c WHERE c.farm_id = f.id AND c.crop_name = 'Maize');
INSERT INTO crops (farm_id, crop_name, variety, season, stage, sowing_date, expected_harvest)
SELECT f.id, 'Cotton', 'Bunny Bt', 'Kharif', 'Harvested', DATE_SUB(@first_last_month, INTERVAL 150 DAY), DATE_ADD(@first_last_month, INTERVAL 14 DAY)
FROM farms f WHERE f.name = 'Rani Cotton Farm'
  AND NOT EXISTS (SELECT 1 FROM crops c WHERE c.farm_id = f.id AND c.crop_name = 'Cotton');
INSERT INTO crops (farm_id, crop_name, variety, season, stage, sowing_date, expected_harvest)
SELECT f.id, 'Chilli', 'Teja', 'Rabi', 'Flowering', DATE_SUB(@first_this_month, INTERVAL 40 DAY), DATE_ADD(@first_this_month, INTERVAL 70 DAY)
FROM farms f WHERE f.name = 'Rani Cotton Farm'
  AND NOT EXISTS (SELECT 1 FROM crops c WHERE c.farm_id = f.id AND c.crop_name = 'Chilli');

INSERT INTO harvests (farm_id, crop_id, harvest_date, quantity_kg, revenue)
SELECT c.farm_id, c.id, DATE_ADD(@first_last_month, INTERVAL 14 DAY), 450.00, 31500.00
FROM crops c JOIN farms f ON f.id = c.farm_id
WHERE f.name = 'Rani Cotton Farm' AND c.crop_name = 'Cotton'
  AND NOT EXISTS (SELECT 1 FROM harvests h WHERE h.crop_id = c.id AND h.harvest_date = DATE_ADD(@first_last_month, INTERVAL 14 DAY));
INSERT INTO harvests (farm_id, crop_id, harvest_date, quantity_kg, revenue)
SELECT c.farm_id, c.id, DATE_ADD(@first_last_month, INTERVAL 9 DAY), 1200.00, 26400.00
FROM crops c JOIN farms f ON f.id = c.farm_id
WHERE f.name = 'Kumaran Green Acres' AND c.crop_name = 'Rice'
  AND NOT EXISTS (SELECT 1 FROM harvests h WHERE h.crop_id = c.id AND h.harvest_date = DATE_ADD(@first_last_month, INTERVAL 9 DAY));
INSERT INTO harvests (farm_id, crop_id, harvest_date, quantity_kg, revenue)
SELECT c.farm_id, c.id, DATE_ADD(@first_last_month, INTERVAL 19 DAY), 350.50, 8762.50
FROM crops c JOIN farms f ON f.id = c.farm_id
WHERE f.name = 'Kumaran Green Acres' AND c.crop_name = 'Tomato'
  AND NOT EXISTS (SELECT 1 FROM harvests h WHERE h.crop_id = c.id AND h.harvest_date = DATE_ADD(@first_last_month, INTERVAL 19 DAY));
INSERT INTO harvests (farm_id, crop_id, harvest_date, quantity_kg, revenue)
SELECT c.farm_id, c.id, DATE_SUB(@first_last_month, INTERVAL 62 DAY), 900.00, 19800.00
FROM crops c JOIN farms f ON f.id = c.farm_id
WHERE f.name = 'Kumaran Green Acres' AND c.crop_name = 'Rice'
  AND NOT EXISTS (SELECT 1 FROM harvests h WHERE h.crop_id = c.id AND h.harvest_date = DATE_SUB(@first_last_month, INTERVAL 62 DAY));

INSERT INTO harvests (farm_id, crop_id, harvest_date, quantity_kg, revenue)
SELECT c.farm_id, c.id, DATE_SUB(@first_last_month, INTERVAL 20 DAY), 750.00, 15000.00
FROM crops c JOIN farms f ON f.id = c.farm_id
WHERE f.name = 'Ravi Farm' AND c.crop_name = 'Maize'
  AND NOT EXISTS (SELECT 1 FROM harvests h WHERE h.crop_id = c.id AND h.harvest_date = DATE_SUB(@first_last_month, INTERVAL 20 DAY));

SELECT 'users' AS tbl, COUNT(*) AS `rows` FROM users
UNION ALL SELECT 'farms', COUNT(*) FROM farms
UNION ALL SELECT 'crops', COUNT(*) FROM crops
UNION ALL SELECT 'harvests', COUNT(*) FROM harvests;
