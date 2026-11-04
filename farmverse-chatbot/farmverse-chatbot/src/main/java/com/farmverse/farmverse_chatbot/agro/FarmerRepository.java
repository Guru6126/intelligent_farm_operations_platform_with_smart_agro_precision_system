package com.farmverse.farmverse_chatbot.agro;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.farmverse.farmverse_chatbot.agro.FarmerData.FarmCrop;
import com.farmverse.farmverse_chatbot.agro.FarmerData.Farmer;
import com.farmverse.farmverse_chatbot.agro.FarmerData.Harvest;
import com.farmverse.farmverse_chatbot.agro.FarmerData.Summary;

public class FarmerRepository {

    private final ConnectionSource source;
    private final AgroSchema schema;

    public FarmerRepository(ConnectionSource source, AgroSchema schema) {
        this.source = source;
        this.schema = schema;
    }

    public AgroSchema schema() {
        return schema;
    }

    public List<Farmer> findByWords(List<String> words) throws SQLException {
        List<Farmer> out = new ArrayList<>();
        if (words.isEmpty()) {
            return out;
        }
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.candidates(schema, words.size()))) {
            for (int i = 0; i < words.size(); i++) {
                ps.setString(i + 1, "%" + words.get(i).toLowerCase(Locale.ROOT) + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addFarmer(out, rs.getString(1), rs.getString(2));
                }
            }
        }
        return out;
    }

    private static void addFarmer(List<Farmer> out, String id, String name) {
        if (name != null && !name.isBlank()) {
            out.add(new Farmer(id, name.trim()));
        }
    }

    public Summary summary(String farmerId) throws SQLException {
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.summary(schema))) {
            ps.setString(1, farmerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Summary(rs.getInt(1), rs.getInt(2), rs.getInt(3), date(rs.getString(4)));
            }
        }
    }

    public List<FarmCrop> farmsAndCrops(String farmerId) throws SQLException {
        List<FarmCrop> out = new ArrayList<>();
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.farmsAndCrops(schema))) {
            ps.setString(1, farmerId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Integer> labels = columnIndex(rs);
                while (rs.next()) {
                    out.add(new FarmCrop(
                            text(rs, labels, "f_name"), text(rs, labels, "f_location"),
                            area(text(rs, labels, "f_area")), text(rs, labels, "f_soil"),
                            text(rs, labels, "c_name"), text(rs, labels, "c_variety"),
                            text(rs, labels, "c_season"), text(rs, labels, "c_stage"),
                            text(rs, labels, "c_sowing"), text(rs, labels, "c_expected")));
                }
            }
        }
        return out;
    }

    public List<Harvest> harvests(String farmerId, LocalDate from, LocalDate to, String crop) throws SQLException {
        List<Harvest> out = new ArrayList<>();
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.harvests(schema, from != null, to != null, crop != null))) {
            int i = 1;
            ps.setString(i++, farmerId);
            if (from != null) {
                ps.setString(i++, from.toString());
            }
            if (to != null) {
                ps.setString(i++, to.toString());
            }
            if (crop != null) {
                ps.setString(i++, "%" + crop.toLowerCase(Locale.ROOT) + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String unit = rs.getString("harvest_unit");
                    if (unit == null || unit.isBlank()) {
                        unit = schema.defaultUnit();
                    }
                    out.add(new Harvest(rs.getString("farm_name"), rs.getString("crop_name"),
                            date(rs.getString("harvest_date")), number(rs), unit, money(rs.getString("harvest_revenue"))));
                }
            }
        }
        return out;
    }

    public BigDecimal directTotal(LocalDate from, LocalDate to) throws SQLException {
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.directTotal(schema, from != null, to != null))) {
            bindFilter(ps, from, to, null);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal v = rs.getBigDecimal(1);
                return v == null ? BigDecimal.ZERO : v;
            }
        }
    }

    public FarmerData.Counts counts() throws SQLException {
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.counts(schema));
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new FarmerData.Counts(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                    schema.crops().has("stage"));
        }
    }

    public List<Farmer> allFarmers() throws SQLException {
        List<Farmer> out = new ArrayList<>();
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.allFarmers(schema));
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                addFarmer(out, rs.getString(1), rs.getString(2));
            }
        }
        return out;
    }

    public List<String> cropNames() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.cropNames(schema));
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (rs.getString(1) != null) {
                    out.add(rs.getString(1).trim());
                }
            }
        }
        return out;
    }

    public List<FarmerData.FarmerTotal> farmersWithHarvests(LocalDate from, LocalDate to, String crop) throws SQLException {
        List<FarmerData.FarmerTotal> out = new ArrayList<>();
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(
                     AgroSql.farmersWithHarvests(schema, from != null, to != null, crop != null))) {
            bindFilter(ps, from, to, crop);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString(2) != null && !rs.getString(2).isBlank()) {
                        BigDecimal qty = rs.getBigDecimal(4);
                        out.add(new FarmerData.FarmerTotal(rs.getString(2).trim(), rs.getInt(3),
                                qty == null ? BigDecimal.ZERO : qty));
                    }
                }
            }
        }
        return out;
    }

    public List<FarmerData.CropTotal> totals(LocalDate from, LocalDate to, String crop) throws SQLException {
        List<FarmerData.CropTotal> out = new ArrayList<>();
        try (Connection c = source.open();
             PreparedStatement ps = c.prepareStatement(AgroSql.totals(schema, from != null, to != null, crop != null))) {
            bindFilter(ps, from, to, crop);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String unit = rs.getString(2);
                    out.add(new FarmerData.CropTotal(rs.getString(1),
                            unit == null || unit.isBlank() ? schema.defaultUnit() : unit,
                            rs.getBigDecimal(3), rs.getInt(4), rs.getInt(5)));
                }
            }
        }
        return out;
    }

    private static void bindFilter(PreparedStatement ps, LocalDate from, LocalDate to, String crop) throws SQLException {
        int i = 1;
        if (from != null) {
            ps.setString(i++, from.toString());
        }
        if (to != null) {
            ps.setString(i++, to.toString());
        }
        if (crop != null) {
            ps.setString(i, "%" + crop.toLowerCase(Locale.ROOT) + "%");
        }
    }

    public static String formatNumber(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal n = value.stripTrailingZeros();
        if (n.scale() < 0) {
            n = n.setScale(0);
        }
        return n.scale() == 0 ? String.format(Locale.US, "%,d", n.toBigInteger()) : String.format(Locale.US, "%,.2f", n);
    }

    private static String money(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return formatNumber(new BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            return raw.trim();
        }
    }

    public Map<String, Object> overview(int limit) throws SQLException {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Connection c = source.open()) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (AgroSchema.Table t : List.of(schema.users(), schema.farms(), schema.crops(), schema.harvests())) {
                try (PreparedStatement ps = c.prepareStatement(AgroSql.count(schema, t.name()));
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    counts.put(t.name(), rs.getLong(1));
                }
            }
            out.put("rowCounts", counts);
            List<Map<String, Object>> farmers = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(AgroSql.overview(schema, limit));
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", rs.getString(2));
                    row.put("farms", rs.getInt(3));
                    row.put("crops", rs.getInt(4));
                    row.put("harvests", rs.getInt(5));
                    row.put("lastHarvest", String.valueOf(date(rs.getString(6))));
                    farmers.add(row);
                }
            }
            out.put("farmers", farmers);
        }
        return out;
    }

    private static Map<String, Integer> columnIndex(ResultSet rs) throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            m.put(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT), i);
        }
        return m;
    }

    private static String text(ResultSet rs, Map<String, Integer> labels, String label) throws SQLException {
        Integer idx = labels.get(label);
        if (idx == null) {
            return null;
        }
        String v = rs.getString(idx);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private String area(String raw) {
        if (raw == null) {
            return null;
        }
        String col = schema.farms().col("area");
        String unit = col != null && col.toLowerCase(Locale.ROOT).contains("acre") ? " acres" : "";
        try {
            BigDecimal n = new BigDecimal(raw).stripTrailingZeros();
            return (n.scale() < 0 ? n.setScale(0) : n).toPlainString() + unit;
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    static LocalDate date(String raw) {
        if (raw == null || raw.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(raw.substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String number(ResultSet rs) throws SQLException {
        String raw = rs.getString("harvest_qty");
        if (raw == null) {
            return null;
        }
        try {
            BigDecimal n = new BigDecimal(raw.trim());
            n = n.stripTrailingZeros();
            if (n.scale() < 0) {
                n = n.setScale(0);
            }
            return String.format(Locale.US, n.scale() == 0 ? "%,d" : "%,.2f",
                    n.scale() == 0 ? (Object) n.toBigInteger() : (Object) n);
        } catch (NumberFormatException e) {
            return raw.trim();
        }
    }
}
