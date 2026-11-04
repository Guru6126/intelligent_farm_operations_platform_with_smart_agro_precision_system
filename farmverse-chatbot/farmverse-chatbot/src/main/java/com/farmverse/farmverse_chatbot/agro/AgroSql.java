package com.farmverse.farmverse_chatbot.agro;

import java.util.ArrayList;
import java.util.List;

public final class AgroSql {

    private AgroSql() {
    }

    public static String candidates(AgroSchema s, int wordCount) {
        StringBuilder sb = new StringBuilder("SELECT ")
                .append(col(s, "u", s.users().col("id"))).append(", ")
                .append(nameExpr(s)).append(" AS farmer_name")
                .append(" FROM ").append(q(s, s.users().name())).append(" u WHERE ");
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append("LOWER(").append(nameExpr(s)).append(") LIKE ?");
        }
        return sb.append(" LIMIT 200").toString();
    }

    public static String farmsAndCrops(AgroSchema s) {
        List<String> select = new ArrayList<>();
        select.add(col(s, "f", s.farms().col("id")) + " AS f_id");
        addOptional(select, s, "f", s.farms(), "name", "f_name");
        addOptional(select, s, "f", s.farms(), "location", "f_location");
        addOptional(select, s, "f", s.farms(), "area", "f_area");
        addOptional(select, s, "f", s.farms(), "soil", "f_soil");
        select.add(col(s, "c", s.crops().col("id")) + " AS c_id");
        select.add(col(s, "c", s.crops().col("name")) + " AS c_name");
        addOptional(select, s, "c", s.crops(), "variety", "c_variety");
        addOptional(select, s, "c", s.crops(), "season", "c_season");
        addOptional(select, s, "c", s.crops(), "stage", "c_stage");
        addOptional(select, s, "c", s.crops(), "sowing", "c_sowing");
        addOptional(select, s, "c", s.crops(), "expected", "c_expected");
        return "SELECT " + String.join(", ", select)
                + " FROM " + q(s, s.farms().name()) + " f"
                + " LEFT JOIN " + q(s, s.crops().name()) + " c ON "
                + col(s, "c", s.crops().col("farm")) + " = " + col(s, "f", s.farms().col("id"))
                + " WHERE " + col(s, "f", s.farms().col("owner")) + " = ?"
                + " ORDER BY " + col(s, "f", s.farms().col("id")) + ", " + col(s, "c", s.crops().col("id"));
    }

    public static String harvests(AgroSchema s, boolean hasFrom, boolean hasTo, boolean hasCrop) {
        StringBuilder sb = new StringBuilder("SELECT ")
                .append(s.farms().has("name") ? col(s, "f", s.farms().col("name")) : "NULL").append(" AS farm_name, ")
                .append(col(s, "c", s.crops().col("name"))).append(" AS crop_name, ")
                .append(col(s, "h", s.harvests().col("date"))).append(" AS harvest_date, ")
                .append(col(s, "h", s.harvests().col("qty"))).append(" AS harvest_qty, ")
                .append(s.harvests().has("unit") ? col(s, "h", s.harvests().col("unit")) : "NULL")
                .append(" AS harvest_unit, ")
                .append(s.harvests().has("revenue") ? col(s, "h", s.harvests().col("revenue")) : "NULL")
                .append(" AS harvest_revenue")
                .append(" FROM ").append(q(s, s.farms().name())).append(" f")
                .append(" JOIN ").append(q(s, s.crops().name())).append(" c ON ")
                .append(col(s, "c", s.crops().col("farm"))).append(" = ").append(col(s, "f", s.farms().col("id")))
                .append(" JOIN ").append(q(s, s.harvests().name())).append(" h ON ")
                .append(col(s, "h", s.harvests().col("crop"))).append(" = ").append(col(s, "c", s.crops().col("id")))
                .append(" WHERE ").append(col(s, "f", s.farms().col("owner"))).append(" = ?");
        if (hasFrom) {
            sb.append(" AND ").append(col(s, "h", s.harvests().col("date"))).append(" >= ?");
        }
        if (hasTo) {
            sb.append(" AND ").append(col(s, "h", s.harvests().col("date"))).append(" < ?");
        }
        if (hasCrop) {
            sb.append(" AND LOWER(").append(col(s, "c", s.crops().col("name"))).append(") LIKE ?");
        }
        return sb.append(" ORDER BY ").append(col(s, "h", s.harvests().col("date"))).append(" DESC, ")
                .append(col(s, "c", s.crops().col("name"))).toString();
    }

    private static String harvestJoin(AgroSchema s) {
        return " FROM " + q(s, s.farms().name()) + " f"
                + " JOIN " + q(s, s.crops().name()) + " c ON "
                + col(s, "c", s.crops().col("farm")) + " = " + col(s, "f", s.farms().col("id"))
                + " JOIN " + q(s, s.harvests().name()) + " h ON "
                + col(s, "h", s.harvests().col("crop")) + " = " + col(s, "c", s.crops().col("id"));
    }

    private static String harvestFilter(AgroSchema s, boolean hasFrom, boolean hasTo, boolean hasCrop) {
        StringBuilder sb = new StringBuilder(" WHERE 1 = 1");
        if (hasFrom) {
            sb.append(" AND ").append(col(s, "h", s.harvests().col("date"))).append(" >= ?");
        }
        if (hasTo) {
            sb.append(" AND ").append(col(s, "h", s.harvests().col("date"))).append(" < ?");
        }
        if (hasCrop) {
            sb.append(" AND LOWER(").append(col(s, "c", s.crops().col("name"))).append(") LIKE ?");
        }
        return sb.toString();
    }

    public static String farmersWithHarvests(AgroSchema s, boolean hasFrom, boolean hasTo, boolean hasCrop) {
        List<String> groupBy = new ArrayList<>();
        groupBy.add(col(s, "u", s.users().col("id")));
        for (String c : nameColumns(s)) {
            groupBy.add(col(s, "u", c));
        }
        return "SELECT " + col(s, "u", s.users().col("id")) + ", " + nameExpr(s) + " AS farmer_name, "
                + "COUNT(" + col(s, "h", s.harvests().col("crop")) + "), "
                + "SUM(" + col(s, "h", s.harvests().col("qty")) + ")"
                + harvestJoin(s)
                + " JOIN " + q(s, s.users().name()) + " u ON " + col(s, "f", s.farms().col("owner")) + " = "
                + col(s, "u", s.users().col("id"))
                + harvestFilter(s, hasFrom, hasTo, hasCrop)
                + " GROUP BY " + String.join(", ", groupBy)
                + " ORDER BY " + nameExpr(s);
    }

    public static String totals(AgroSchema s, boolean hasFrom, boolean hasTo, boolean hasCrop) {
        String unit = s.harvests().has("unit") ? col(s, "h", s.harvests().col("unit")) : "NULL";
        String groupUnit = s.harvests().has("unit") ? ", " + unit : "";
        return "SELECT " + col(s, "c", s.crops().col("name")) + ", " + unit + ", "
                + "SUM(" + col(s, "h", s.harvests().col("qty")) + "), "
                + "COUNT(" + col(s, "h", s.harvests().col("crop")) + "), "
                + "COUNT(DISTINCT " + col(s, "f", s.farms().col("owner")) + ")"
                + harvestJoin(s)
                + harvestFilter(s, hasFrom, hasTo, hasCrop)
                + " GROUP BY " + col(s, "c", s.crops().col("name")) + groupUnit
                + " ORDER BY " + col(s, "c", s.crops().col("name"));
    }

    public static String counts(AgroSchema s) {
        String growing = s.crops().has("stage")
                ? "(SELECT COUNT(*) FROM " + q(s, s.crops().name()) + " WHERE LOWER(" + q(s, s.crops().col("stage"))
                + ") NOT LIKE '%harvest%')"
                : "(SELECT COUNT(*) FROM " + q(s, s.crops().name()) + ")";
        return "SELECT (SELECT COUNT(*) FROM " + q(s, s.farms().name()) + "), (SELECT COUNT(*) FROM "
                + q(s, s.crops().name()) + "), " + growing + ", (SELECT COUNT(*) FROM " + q(s, s.harvests().name()) + ")";
    }

    public static String directTotal(AgroSchema s, boolean hasFrom, boolean hasTo) {
        StringBuilder sb = new StringBuilder("SELECT SUM(" + q(s, s.harvests().col("qty")) + ") FROM "
                + q(s, s.harvests().name()) + " WHERE 1 = 1");
        if (hasFrom) {
            sb.append(" AND ").append(q(s, s.harvests().col("date"))).append(" >= ?");
        }
        if (hasTo) {
            sb.append(" AND ").append(q(s, s.harvests().col("date"))).append(" < ?");
        }
        return sb.toString();
    }

    public static String allFarmers(AgroSchema s) {
        return "SELECT " + col(s, "u", s.users().col("id")) + ", " + nameExpr(s) + " AS farmer_name FROM "
                + q(s, s.users().name()) + " u ORDER BY " + nameExpr(s) + " LIMIT 1000";
    }

    public static String cropNames(AgroSchema s) {
        return "SELECT DISTINCT " + col(s, "c", s.crops().col("name")) + " FROM " + q(s, s.crops().name()) + " c";
    }

    public static String summary(AgroSchema s) {
        return "SELECT COUNT(DISTINCT " + col(s, "f", s.farms().col("id")) + "), "
                + "COUNT(DISTINCT " + col(s, "c", s.crops().col("id")) + "), "
                + "COUNT(" + col(s, "h", s.harvests().col("crop")) + "), "
                + "MAX(" + col(s, "h", s.harvests().col("date")) + ")"
                + " FROM " + q(s, s.farms().name()) + " f"
                + " LEFT JOIN " + q(s, s.crops().name()) + " c ON "
                + col(s, "c", s.crops().col("farm")) + " = " + col(s, "f", s.farms().col("id"))
                + " LEFT JOIN " + q(s, s.harvests().name()) + " h ON "
                + col(s, "h", s.harvests().col("crop")) + " = " + col(s, "c", s.crops().col("id"))
                + " WHERE " + col(s, "f", s.farms().col("owner")) + " = ?";
    }

    public static String overview(AgroSchema s, int limit) {
        List<String> nameCols = nameColumns(s);
        List<String> groupBy = new ArrayList<>();
        groupBy.add(col(s, "u", s.users().col("id")));
        for (String c : nameCols) {
            groupBy.add(col(s, "u", c));
        }
        return "SELECT " + col(s, "u", s.users().col("id")) + ", " + nameExpr(s) + " AS farmer_name, "
                + "COUNT(DISTINCT " + col(s, "f", s.farms().col("id")) + "), "
                + "COUNT(DISTINCT " + col(s, "c", s.crops().col("id")) + "), "
                + "COUNT(" + col(s, "h", s.harvests().col("crop")) + "), "
                + "MAX(" + col(s, "h", s.harvests().col("date")) + ")"
                + " FROM " + q(s, s.users().name()) + " u"
                + " LEFT JOIN " + q(s, s.farms().name()) + " f ON "
                + col(s, "f", s.farms().col("owner")) + " = " + col(s, "u", s.users().col("id"))
                + " LEFT JOIN " + q(s, s.crops().name()) + " c ON "
                + col(s, "c", s.crops().col("farm")) + " = " + col(s, "f", s.farms().col("id"))
                + " LEFT JOIN " + q(s, s.harvests().name()) + " h ON "
                + col(s, "h", s.harvests().col("crop")) + " = " + col(s, "c", s.crops().col("id"))
                + " GROUP BY " + String.join(", ", groupBy)
                + " ORDER BY " + nameExpr(s) + " LIMIT " + Math.max(1, limit);
    }

    public static String count(AgroSchema s, String table) {
        return "SELECT COUNT(*) FROM " + q(s, table);
    }

    static String nameExpr(AgroSchema s) {
        List<String> cols = nameColumns(s);
        if (cols.size() == 1) {
            return col(s, "u", cols.get(0));
        }
        return "CONCAT_WS(' ', " + col(s, "u", cols.get(0)) + ", " + col(s, "u", cols.get(1)) + ")";
    }

    private static List<String> nameColumns(AgroSchema s) {
        if (s.users().has("name")) {
            return List.of(s.users().col("name"));
        }
        return List.of(s.users().col("first"), s.users().col("last"));
    }

    private static void addOptional(List<String> select, AgroSchema s, String alias, AgroSchema.Table t,
                                    String logical, String as) {
        if (t.has(logical)) {
            select.add(col(s, alias, t.col(logical)) + " AS " + as);
        }
    }

    private static String q(AgroSchema s, String ident) {
        return quote(s.quote(), ident);
    }

    private static String col(AgroSchema s, String alias, String ident) {
        return alias + "." + quote(s.quote(), ident);
    }

    public static String quote(String quote, String ident) {
        if (ident == null || !ident.matches("[A-Za-z0-9_$]+")) {
            throw new IllegalArgumentException("Unsafe table/column name: " + ident);
        }
        String qc = quote == null || quote.isBlank() ? "" : quote.trim();
        return qc + ident + qc;
    }
}
