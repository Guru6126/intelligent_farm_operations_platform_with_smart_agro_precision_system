USE agroprecision;

SELECT 'users'    AS tbl, COUNT(*) AS `rows` FROM users
UNION ALL SELECT 'farms',    COUNT(*) FROM farms
UNION ALL SELECT 'crops',    COUNT(*) FROM crops
UNION ALL SELECT 'harvests', COUNT(*) FROM harvests;

SELECT u.id, u.full_name,
       COUNT(DISTINCT f.id) AS farms,
       COUNT(DISTINCT c.id) AS crops,
       COUNT(h.id)          AS harvests,
       MAX(h.harvest_date)  AS last_harvest
FROM users u
LEFT JOIN farms    f ON f.owner_id = u.id
LEFT JOIN crops    c ON c.farm_id  = f.id
LEFT JOIN harvests h ON h.crop_id  = c.id
GROUP BY u.id, u.full_name
ORDER BY u.full_name;

SELECT id, full_name FROM users WHERE LOWER(full_name) LIKE '%kumaran%';

SELECT c.crop_name, h.harvest_date, h.quantity_kg
FROM users u
JOIN farms    f ON f.owner_id = u.id
JOIN crops    c ON c.farm_id  = f.id
JOIN harvests h ON h.crop_id  = c.id
WHERE LOWER(u.full_name) LIKE '%kumaran%'
  AND h.harvest_date >= DATE_SUB(DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE()) - 1 DAY), INTERVAL 1 MONTH)
  AND h.harvest_date <  DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE()) - 1 DAY)
ORDER BY h.harvest_date DESC;
