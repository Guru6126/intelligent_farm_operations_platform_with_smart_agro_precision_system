package com.agroprecision.service;

import com.agroprecision.exception.ApiException;
import com.agroprecision.repository.TableRepository;
import com.agroprecision.security.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** CRUD with authorization and business rules for every farm table. */
@Service
public class DataService {
    public static final Set<String> FARM_TABLES = Set.of("plots", "crops", "crop_activities", "harvests", "inventory_items",
            "stock_transactions", "weather_records", "irrigation_schedules", "equipment", "equipment_bookings",
            "maintenance_records", "workers", "tasks", "budgets", "expenses", "farm_members");
    private static final Set<String> PROTECTED = Set.of("id", "created_at", "owner_id", "created_by", "booked_by");

    private final TableRepository repo;
    private final AccessService access;
    private final JdbcTemplate db;

    public DataService(TableRepository repo, AccessService access, JdbcTemplate db) {
        this.repo = repo;
        this.access = access;
        this.db = db;
    }

    private void checkTable(String t) {
        if (!FARM_TABLES.contains(t) && !t.equals("farms") && !t.equals("notifications")) throw ApiException.notFound();
    }

    private Map<String, Object> clean(String table, Map<String, Object> body) {
        Map<String, String> cols = repo.columns(table);
        Map<String, Object> m = new LinkedHashMap<>();
        body.forEach((k, v) -> { if (cols.containsKey(k) && !PROTECTED.contains(k)) m.put(k, v); });
        return m;
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    public List<Map<String, Object>> list(AuthUser u, String table, String farmId, String orderBy, boolean asc, Integer limit) {
        checkTable(table);
        List<Object> args = new ArrayList<>();
        String where;
        if (table.equals("notifications")) {
            where = "user_id = ?";
            args.add(u.id());
        } else {
            String col = table.equals("farms") ? "id" : "farm_id";
            if (farmId != null && !farmId.isBlank()) {
                access.requireView(u, farmId);
                where = col + " = ?";
                args.add(farmId);
            } else {
                List<String> ids = access.visibleFarmIds(u);
                if (ids == null) where = null;
                else if (ids.isEmpty()) return List.of();
                else {
                    where = col + " IN (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")";
                    args.addAll(ids);
                }
            }
        }
        return repo.select(table, where, args, orderBy, asc, limit);
    }

    @Transactional
    public Map<String, Object> create(AuthUser u, String table, Map<String, Object> body) {
        checkTable(table);
        Map<String, Object> v = clean(table, body);
        String id = UUID.randomUUID().toString();
        v.put("id", id);

        switch (table) {
            case "farms" -> {
                if (!u.isFarmer() && !u.isAdmin()) throw ApiException.forbidden();
                if (str(v.get("name")) == null) throw ApiException.bad("Farm name is required.");
                v.put("owner_id", u.id());
                v.remove("is_demo");
            }
            case "notifications" -> v.put("user_id", u.id());
            default -> {
                String farmId = str(v.get("farm_id"));
                if (farmId == null) throw ApiException.bad("Please choose a farm.");
                boolean manage = access.canManage(u, farmId);
                if (!manage) {
                    access.requireView(u, farmId);
                    // Field-worker permitted writes on assigned farms.
                    switch (table) {
                        case "crop_activities" -> {}
                        case "stock_transactions" -> { if (!"out".equals(v.get("txn_type"))) throw ApiException.forbidden(); }
                        case "equipment_bookings" -> v.put("status", "pending");
                        default -> throw ApiException.forbidden();
                    }
                }
                if (Set.of("crop_activities", "stock_transactions").contains(table)) v.put("created_by", u.id());
                if (table.equals("equipment_bookings")) v.put("booked_by", u.id());
            }
        }

        if (table.equals("stock_transactions")) applyStock(v);
        if (table.equals("equipment_bookings")) checkBooking(v, null);
        repo.insert(table, v);
        if (table.equals("tasks")) notifyAssignee(v, null);
        audit(u, table, "insert", id);
        return repo.findById(table, id).orElseThrow();
    }

    @Transactional
    public Map<String, Object> update(AuthUser u, String table, String id, Map<String, Object> body) {
        checkTable(table);
        Map<String, Object> existing = repo.findById(table, id).orElseThrow(ApiException::notFound);
        Map<String, Object> v = clean(table, body);

        switch (table) {
            case "farms" -> { access.requireManage(u, id); v.remove("is_demo"); }
            case "notifications" -> { if (!u.id().equals(existing.get("user_id"))) throw ApiException.forbidden(); }
            case "stock_transactions" -> throw ApiException.bad("Stock movements cannot be edited; record a correcting movement instead.");
            default -> {
                String farmId = str(existing.get("farm_id"));
                if (!access.canManage(u, farmId)) {
                    boolean onlyStatus = v.keySet().stream().allMatch(k -> k.equals("status"));
                    boolean ok = onlyStatus && ((table.equals("irrigation_schedules") && access.canView(u, farmId))
                            || (table.equals("tasks") && u.id().equals(existing.get("assignee_id"))));
                    if (!ok) throw ApiException.forbidden();
                }
                if (v.containsKey("farm_id") && !Objects.equals(str(v.get("farm_id")), farmId))
                    access.requireManage(u, str(v.get("farm_id")));
            }
        }
        if (table.equals("equipment_bookings")) {
            Map<String, Object> merged = new HashMap<>(existing);
            merged.putAll(v);
            checkBooking(merged, id);
        }
        repo.update(table, id, v);
        if (table.equals("tasks")) notifyAssignee(merge(existing, v), existing);
        audit(u, table, "update", id);
        return repo.findById(table, id).orElseThrow();
    }

    @Transactional
    public void delete(AuthUser u, String table, String id) {
        checkTable(table);
        Map<String, Object> existing = repo.findById(table, id).orElseThrow(ApiException::notFound);
        switch (table) {
            case "farms" -> access.requireManage(u, id);
            case "notifications" -> { if (!u.id().equals(existing.get("user_id"))) throw ApiException.forbidden(); }
            default -> access.requireManage(u, str(existing.get("farm_id")));
        }
        repo.delete(table, id);
        audit(u, table, "delete", id);
    }

    @Transactional
    public int markAllRead(AuthUser u) {
        return db.update("UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE", u.id());
    }

    private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> m = new HashMap<>(a);
        m.putAll(b);
        return m;
    }

    /** Row-locks the item, rejects negative stock, and raises a low-stock alert. */
    private void applyStock(Map<String, Object> v) {
        String type = str(v.get("txn_type"));
        if (!"in".equals(type) && !"out".equals(type)) throw ApiException.bad("Movement type must be 'in' or 'out'.");
        BigDecimal qty;
        try { qty = new BigDecimal(str(v.get("quantity"))); } catch (Exception e) { throw ApiException.bad("Quantity is required."); }
        if (qty.signum() <= 0) throw ApiException.bad("Quantity must be greater than zero.");
        Map<String, Object> item = repo.findForUpdate("inventory_items", str(v.get("item_id")));
        if (item == null) throw ApiException.bad("Inventory item not found.");
        if (!Objects.equals(str(item.get("farm_id")), str(v.get("farm_id")))) throw ApiException.bad("Item does not belong to this farm.");
        BigDecimal current = (BigDecimal) item.get("quantity");
        BigDecimal next = type.equals("in") ? current.add(qty) : current.subtract(qty);
        if (next.signum() < 0)
            throw ApiException.bad("Insufficient stock: " + current.stripTrailingZeros().toPlainString() + " " + item.get("unit") + " available");
        db.update("UPDATE inventory_items SET quantity = ? WHERE id = ?", next, item.get("id"));
        BigDecimal reorder = (BigDecimal) item.get("reorder_level");
        if (next.compareTo(reorder) <= 0) {
            db.update("INSERT INTO notifications (id, user_id, farm_id, title, body, severity) SELECT ?, owner_id, id, ?, ?, 'warning' FROM farms WHERE id = ?",
                    UUID.randomUUID().toString(), "Low stock: " + item.get("name"),
                    "Only " + next.stripTrailingZeros().toPlainString() + " " + item.get("unit") + " left (reorder level "
                            + reorder.stripTrailingZeros().toPlainString() + ").", item.get("farm_id"));
        }
    }

    /** Business rule: an item of equipment cannot be double-booked for overlapping periods. */
    private void checkBooking(Map<String, Object> v, String selfId) {
        if ("cancelled".equals(v.get("status"))) return;
        Object s = v.get("starts_at"), e = v.get("ends_at");
        if (s == null || e == null) throw ApiException.bad("Start and end time are required.");
        String start = s.toString(), end = e.toString();
        if (end.compareTo(start) <= 0) throw ApiException.bad("End time must be after start time.");
        Integer n = db.queryForObject("SELECT COUNT(*) FROM equipment_bookings WHERE equipment_id = ? AND status <> 'cancelled' AND id <> ? AND starts_at < ? AND ends_at > ?",
                Integer.class, v.get("equipment_id"), selfId == null ? "" : selfId, toDb(end), toDb(start));
        if (n != null && n > 0) throw ApiException.bad("This equipment is already booked for an overlapping time.");
    }

    private static String toDb(String iso) {
        return iso.replace("T", " ").replace("Z", "").replaceAll("\\.\\d+$", "");
    }

    private void notifyAssignee(Map<String, Object> now, Map<String, Object> before) {
        Object a = now.get("assignee_id");
        if (a == null || a.toString().isBlank()) return;
        if (before != null && Objects.equals(str(before.get("assignee_id")), str(a))) return;
        Object due = now.get("due_date");
        db.update("INSERT INTO notifications (id, user_id, farm_id, title, body) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), a, now.get("farm_id"), "New task: " + now.get("title"),
                due == null ? "No due date" : "Due " + due);
    }

    private void audit(AuthUser u, String table, String action, String id) {
        db.update("INSERT INTO audit_logs (user_id, table_name, action, record_id) VALUES (?, ?, ?, ?)", u.id(), table, action, id);
    }
}
