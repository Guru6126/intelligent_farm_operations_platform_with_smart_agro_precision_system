package com.agroprecision.service;

import com.agroprecision.exception.ApiException;
import com.agroprecision.repository.UserRepository;
import com.agroprecision.security.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FarmService {
    private final JdbcTemplate db;
    private final AccessService access;
    private final UserRepository users;

    public FarmService(JdbcTemplate db, AccessService access, UserRepository users) {
        this.db = db;
        this.access = access;
        this.users = users;
    }

    @Transactional
    public String addMemberByEmail(AuthUser u, String farmId, String email) {
        access.requireManage(u, farmId);
        var member = users.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> ApiException.bad("No user with that email has signed up yet"));
        db.update("INSERT IGNORE INTO farm_members (id, farm_id, user_id) VALUES (?, ?, ?)", UUID.randomUUID().toString(), farmId, member.getId());
        return member.getId();
    }

    /** Creates a fully populated demo farm owned by the current farmer. */
    @Transactional
    public String seedDemo(AuthUser u) {
        if (!u.isFarmer() && !u.isAdmin()) throw ApiException.forbidden();
        String farm = id();
        LocalDate today = LocalDate.now();
        db.update("INSERT INTO farms (id, owner_id, name, location, latitude, longitude, total_area_acres, soil_type, is_demo) VALUES (?,?,?,?,?,?,?,?,TRUE)",
                farm, u.id(), "Green Valley Demo Farm", "Nashik, Maharashtra", 19.997, 73.789, 42.5, "Black cotton");
        String p1 = id(), p2 = id(), p3 = id();
        db.update("INSERT INTO plots (id, farm_id, name, area_acres, soil_ph) VALUES (?,?,?,?,?),(?,?,?,?,?),(?,?,?,?,?)",
                p1, farm, "North Field", 15, 6.8, p2, farm, "River Plot", 12.5, 7.1, p3, farm, "Orchard Block", 15, 6.5);
        String c1 = id(), c2 = id(), c3 = id();
        db.update("INSERT INTO crops (id, farm_id, plot_id, crop_name, variety, season, stage, sowing_date, expected_harvest) VALUES (?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?)",
                c1, farm, p1, "Wheat", "HD-2967", "Rabi 2026", "vegetative", today.minusDays(40), today.plusDays(80),
                c2, farm, p2, "Onion", "Nasik Red", "Rabi 2026", "sown", today.minusDays(15), today.plusDays(100),
                c3, farm, p3, "Grapes", "Thompson Seedless", "Annual", "flowering", today.minusDays(120), today.plusDays(45));
        db.update("INSERT INTO harvests (id, farm_id, crop_id, harvest_date, quantity_kg, revenue) VALUES (?,?,?,?,?,?),(?,?,?,?,?,?)",
                id(), farm, c3, today.minusDays(200), 8200, 410000, id(), farm, c1, today.minusDays(230), 15600, 343200);
        db.update("INSERT INTO inventory_items (id, farm_id, name, category, unit, quantity, reorder_level, unit_cost, expiry_date, supplier) VALUES " +
                        "(?,?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?,?)",
                id(), farm, "Urea", "fertilizer", "kg", 450, 200, 6.5, null, "Kisan Agro",
                id(), farm, "DAP", "fertilizer", "kg", 120, 150, 27, null, "Kisan Agro",
                id(), farm, "Mancozeb", "pesticide", "kg", 18, 10, 420, today.plusDays(25), "CropCare",
                id(), farm, "Wheat seed HD-2967", "seed", "kg", 60, 40, 55, today.plusDays(180), "State Seeds Corp");
        String e1 = id(), e2 = id();
        db.update("INSERT INTO equipment (id, farm_id, name, type, status, next_service) VALUES (?,?,?,?,?,?),(?,?,?,?,?,?)",
                e1, farm, "Mahindra 575 Tractor", "tractor", "available", today.plusDays(20),
                e2, farm, "Power Sprayer", "sprayer", "maintenance", today.plusDays(3));
        db.update("INSERT INTO maintenance_records (id, farm_id, equipment_id, service_date, description, cost) VALUES (?,?,?,?,?,?)",
                id(), farm, e2, today.minusDays(2), "Nozzle and pump seal replacement", 1800);
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        db.update("INSERT INTO irrigation_schedules (id, farm_id, plot_id, water_source, method, scheduled_at, duration_min, water_litres, status) VALUES " +
                        "(?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?,?)",
                id(), farm, p1, "Borewell", "sprinkler", now.plusHours(6), 60, 12000, "upcoming",
                id(), farm, p3, "Canal", "drip", now.plusDays(1), 90, 8000, "upcoming",
                id(), farm, p2, "Borewell", "drip", now.minusDays(1), 45, 5000, "completed");
        String w1 = id();
        db.update("INSERT INTO workers (id, farm_id, name, phone, skill, daily_wage) VALUES (?,?,?,?,?,?),(?,?,?,?,?,?)",
                w1, farm, "Ramesh Patil", "9800000001", "Tractor operator", 650, id(), farm, "Sunita Jadhav", "9800000002", "Pruning", 550);
        db.update("INSERT INTO tasks (id, farm_id, title, description, worker_id, due_date, priority, status) VALUES (?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?),(?,?,?,?,?,?,?,?)",
                id(), farm, "Apply second urea dose", "North Field wheat", w1, today.plusDays(2), "high", "todo",
                id(), farm, "Inspect grape bunches for mildew", null, null, today.plusDays(1), "medium", "in_progress",
                id(), farm, "Clean drip filters", null, null, today.minusDays(1), "low", "done");
        String[][] budget = {{"Seeds", "30000"}, {"Fertilizer", "45000"}, {"Labour", "60000"}, {"Fuel", "20000"}};
        for (String[] b : budget)
            db.update("INSERT INTO budgets (id, farm_id, season, category, planned_amount) VALUES (?,?,?,?,?)", id(), farm, "Rabi 2026", b[0], b[1]);
        Object[][] exp = {{"Seeds", 24500, 35}, {"Fertilizer", 18200, 28}, {"Labour", 22000, 14}, {"Fuel", 6400, 7}, {"Fertilizer", 9800, 3}};
        for (Object[] e : exp)
            db.update("INSERT INTO expenses (id, farm_id, season, category, amount, spent_on) VALUES (?,?,?,?,?,?)",
                    id(), farm, "Rabi 2026", e[0], e[1], today.minusDays((Integer) e[2]));
        String[] cond = {"Sunny", "Partly cloudy", "Cloudy", "Light rain", "Sunny"};
        for (int i = 13; i >= 0; i--) {
            int k = i % 5;
            db.update("INSERT INTO weather_records (id, farm_id, recorded_on, temperature_c, humidity_pct, rainfall_mm, wind_kmh, `condition`) VALUES (?,?,?,?,?,?,?,?)",
                    id(), farm, today.minusDays(i), 24 + (i * 7 % 9), 45 + (i * 11 % 35), k == 3 ? 6 + i % 5 : 0, 6 + i % 10, cond[k]);
        }
        db.update("INSERT INTO notifications (id, user_id, farm_id, title, body, severity) VALUES (?,?,?,?,?,?)",
                id(), u.id(), farm, "Low stock: DAP", "Only 120 kg left (reorder level 150).", "warning");
        return farm;
    }

    private static String id() { return UUID.randomUUID().toString(); }
}
