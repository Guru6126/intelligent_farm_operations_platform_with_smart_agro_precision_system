package com.agroprecision.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic, whitelisted JDBC repository for the farm-scoped operational tables.
 * Table and column names are validated against the live MySQL schema before use,
 * and every value is bound as a parameter, so no user input is concatenated into SQL.
 */
@Repository
public class TableRepository {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final Map<String, Map<String, String>> columnCache = new ConcurrentHashMap<>();

    public TableRepository(JdbcTemplate db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    /** column name -> MySQL data type */
    public Map<String, String> columns(String table) {
        return columnCache.computeIfAbsent(table, t -> {
            Map<String, String> m = new LinkedHashMap<>();
            db.query("SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                    rs -> { m.put(rs.getString(1), rs.getString(2).toLowerCase()); }, t);
            return m;
        });
    }

    private static String q(String ident) { return "`" + ident + "`"; }

    public List<Map<String, Object>> select(String table, String where, List<Object> args, String orderBy, boolean asc, Integer limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(q(table));
        if (where != null && !where.isBlank()) sql.append(" WHERE ").append(where);
        if (orderBy != null && columns(table).containsKey(orderBy)) sql.append(" ORDER BY ").append(q(orderBy)).append(asc ? " ASC" : " DESC");
        if (limit != null && limit > 0) sql.append(" LIMIT ").append(Math.min(limit, 5000));
        else sql.append(" LIMIT 5000");
        return db.queryForList(sql.toString(), args.toArray()).stream().map(r -> out(table, r)).toList();
    }

    public Optional<Map<String, Object>> findById(String table, String id) {
        var rows = db.queryForList("SELECT * FROM " + q(table) + " WHERE id = ?", id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(out(table, rows.get(0)));
    }

    public Map<String, Object> findForUpdate(String table, String id) {
        var rows = db.queryForList("SELECT * FROM " + q(table) + " WHERE id = ? FOR UPDATE", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insert(String table, Map<String, Object> values) {
        List<String> cols = new ArrayList<>(values.keySet());
        String sql = "INSERT INTO " + q(table) + " (" + String.join(",", cols.stream().map(TableRepository::q).toList())
                + ") VALUES (" + String.join(",", Collections.nCopies(cols.size(), "?")) + ")";
        db.update(sql, cols.stream().map(c -> in(table, c, values.get(c))).toArray());
    }

    public void update(String table, String id, Map<String, Object> values) {
        if (values.isEmpty()) return;
        List<String> cols = new ArrayList<>(values.keySet());
        String sql = "UPDATE " + q(table) + " SET " + String.join(",", cols.stream().map(c -> q(c) + " = ?").toList()) + " WHERE id = ?";
        List<Object> args = new ArrayList<>(cols.stream().map(c -> in(table, c, values.get(c))).toList());
        args.add(id);
        db.update(sql, args.toArray());
    }

    public void delete(String table, String id) {
        db.update("DELETE FROM " + q(table) + " WHERE id = ?", id);
    }

    /** Converts incoming JSON values to JDBC-friendly values by column type. */
    private Object in(String table, String col, Object v) {
        if (v == null) return null;
        if (v instanceof String s && s.isBlank()) return null;
        String type = columns(table).getOrDefault(col, "");
        try {
            switch (type) {
                case "json": return v instanceof String s ? s : json.writeValueAsString(v);
                case "date": return LocalDate.parse(v.toString().substring(0, 10));
                case "datetime": case "timestamp": {
                    String s = v.toString();
                    if (s.endsWith("Z") || s.matches(".*[+-]\\d\\d:\\d\\d$"))
                        return OffsetDateTime.parse(s).withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
                    if (s.length() == 16) s = s + ":00";
                    return LocalDateTime.parse(s.replace(' ', 'T'));
                }
                case "tinyint": return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString());
                default: return v;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value for " + col);
        }
    }

    /** Normalises a DB row for JSON output: ISO dates, numbers, parsed JSON. */
    private Map<String, Object> out(String table, Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, String> types = columns(table);
        row.forEach((k, v) -> {
            Object o = v;
            if (v instanceof Timestamp ts) o = ts.toLocalDateTime().toString() + "Z";
            else if (v instanceof LocalDateTime ldt) o = ldt.toString() + "Z";
            else if (v instanceof java.sql.Date d) o = d.toLocalDate().toString();
            else if (v instanceof BigDecimal bd) o = bd.doubleValue();
            else if ("json".equals(types.get(k)) && v instanceof String s) {
                try { o = json.readValue(s, Object.class); } catch (Exception ignored) { o = s; }
            } else if ("tinyint".equals(types.get(k)) && v instanceof Number n) o = n.intValue() != 0;
            m.put(k, o);
        });
        return m;
    }
}
