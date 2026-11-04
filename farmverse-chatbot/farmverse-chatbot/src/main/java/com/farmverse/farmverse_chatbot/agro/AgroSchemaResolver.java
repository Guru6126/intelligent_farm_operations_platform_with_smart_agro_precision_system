package com.farmverse.farmverse_chatbot.agro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class AgroSchemaResolver {

    private AgroSchemaResolver() {
    }

    private static String[] a(String... names) {
        return names;
    }

    private static final String[] USERS_T = a("users", "user", "farmers", "farmer");
    private static final String[] FARMS_T = a("farms", "farm");
    private static final String[] CROPS_T = a("crops", "crop");
    private static final String[] HARVESTS_T = a("harvests", "harvest", "harvest_records", "harvest_record");

    private static final String[] U_ID = a("id", "user_id", "userid", "farmer_id");
    private static final String[] U_NAME = a("full_name", "name", "fullname", "farmer_name", "display_name",
            "user_name", "username");
    private static final String[] U_FIRST = a("first_name", "firstname", "fname");
    private static final String[] U_LAST = a("last_name", "lastname", "lname", "surname");

    private static final String[] F_ID = a("id", "farm_id");
    private static final String[] F_OWNER = a("owner_id", "user_id", "farmer_id", "userid", "owner");
    private static final String[] F_NAME = a("name", "farm_name", "title");
    private static final String[] F_LOCATION = a("location", "address", "village", "district", "place");
    private static final String[] F_AREA = a("total_area_acres", "area_acres", "area", "total_area", "acres", "size");
    private static final String[] F_SOIL = a("soil_type", "soil");

    private static final String[] C_ID = a("id", "crop_id");
    private static final String[] C_FARM = a("farm_id", "farmid");
    private static final String[] C_NAME = a("crop_name", "name", "crop_type", "crop", "crop_variety", "variety", "title");
    private static final String[] C_VARIETY = a("variety", "crop_variety");
    private static final String[] C_SEASON = a("season", "crop_season");
    private static final String[] C_STAGE = a("stage", "growth_stage", "crop_stage", "status");
    private static final String[] C_SOWING = a("sowing_date", "sown_on", "planting_date", "sowing", "planted_on");
    private static final String[] C_EXPECTED = a("expected_harvest", "expected_harvest_date", "expected_harvest_on");

    private static final String[] H_ID = a("id", "harvest_id");
    private static final String[] H_CROP = a("crop_id", "cropid");
    private static final String[] H_DATE = a("harvest_date", "harvested_on", "harvested_at", "harvest_on", "date",
            "created_at");
    private static final String[] H_QTY = a("quantity_kg", "quantity", "harvest_quantity", "harvested_quantity",
            "yield_kg", "yield_quantity", "yield", "qty", "total_quantity", "amount", "weight");
    private static final String[] H_REVENUE = a("revenue", "total_revenue", "income", "earnings", "sale_amount", "sale_value");
    private static final String[] H_UNIT = a("unit", "quantity_unit", "uom");

    public static AgroSchema resolve(SchemaSource src, String quote, Function<String, String> override)
            throws Exception {

        List<String> problems = new ArrayList<>();
        List<String> all = src.tables();

        String users = table(all, "users", USERS_T, override, problems);
        String farms = table(all, "farms", FARMS_T, override, problems);
        String crops = table(all, "crops", CROPS_T, override, problems);
        String harvests = table(all, "harvests", HARVESTS_T, override, problems);
        if (users == null || farms == null || crops == null || harvests == null) {
            throw new SchemaException(problems);
        }

        List<String> uCols = src.columns(users);
        List<String> fCols = src.columns(farms);
        List<String> cCols = src.columns(crops);
        List<String> hCols = src.columns(harvests);

        SchemaSource.ForeignKey farmsToUsers = fkTo(src, farms, users);
        SchemaSource.ForeignKey cropsToFarms = fkTo(src, crops, farms);
        SchemaSource.ForeignKey harvestsToCrops = fkTo(src, harvests, crops);

        Map<String, String> u = new LinkedHashMap<>();
        put(u, "id", pick(uCols, "users", "id", U_ID, override, problems, true));
        if (u.get("id") == null && farmsToUsers != null) {
            put(u, "id", find(uCols, farmsToUsers.pkColumn()));
        }
        String single = pick(uCols, "users", "name", U_NAME, override, problems, false);
        if (single != null) {
            put(u, "name", single);
        } else {
            String first = pick(uCols, "users", "first", U_FIRST, override, problems, false);
            String last = pick(uCols, "users", "last", U_LAST, override, problems, false);
            if (first != null && last != null) {
                put(u, "first", first);
                put(u, "last", last);
            } else if (first != null) {
                put(u, "name", first);
            } else {
                problems.add("users: no name column (tried " + String.join(", ", U_NAME) + "; columns: " + uCols
                        + "). Set agro.map.users.name=<column>");
            }
        }

        Map<String, String> f = new LinkedHashMap<>();
        put(f, "id", pick(fCols, "farms", "id", F_ID, override, problems, true));
        if (f.get("id") == null && cropsToFarms != null) {
            put(f, "id", find(fCols, cropsToFarms.pkColumn()));
        }
        String owner = pick(fCols, "farms", "owner", F_OWNER, override, problems, false);
        if (owner == null && farmsToUsers != null) {
            owner = find(fCols, farmsToUsers.fkColumn());
        }
        if (owner == null) {
            problems.add("farms: no column links a farm to its farmer (tried " + String.join(", ", F_OWNER)
                    + "; columns: " + fCols + "). Set agro.map.farms.owner=<column>");
        }
        put(f, "owner", owner);
        put(f, "name", pick(fCols, "farms", "name", F_NAME, override, problems, false));
        put(f, "location", pick(fCols, "farms", "location", F_LOCATION, override, problems, false));
        put(f, "area", pick(fCols, "farms", "area", F_AREA, override, problems, false));
        put(f, "soil", pick(fCols, "farms", "soil", F_SOIL, override, problems, false));

        Map<String, String> c = new LinkedHashMap<>();
        put(c, "id", pick(cCols, "crops", "id", C_ID, override, problems, true));
        if (c.get("id") == null && harvestsToCrops != null) {
            put(c, "id", find(cCols, harvestsToCrops.pkColumn()));
        }
        String farmCol = pick(cCols, "crops", "farm", C_FARM, override, problems, false);
        if (farmCol == null && cropsToFarms != null) {
            farmCol = find(cCols, cropsToFarms.fkColumn());
        }
        if (farmCol == null) {
            problems.add("crops: no column links a crop to its farm (tried " + String.join(", ", C_FARM)
                    + "; columns: " + cCols + "). Set agro.map.crops.farm=<column>");
        }
        put(c, "farm", farmCol);
        put(c, "name", pick(cCols, "crops", "name", C_NAME, override, problems, true));
        put(c, "variety", pick(cCols, "crops", "variety", C_VARIETY, override, problems, false));
        put(c, "season", pick(cCols, "crops", "season", C_SEASON, override, problems, false));
        put(c, "stage", pick(cCols, "crops", "stage", C_STAGE, override, problems, false));
        put(c, "sowing", pick(cCols, "crops", "sowing", C_SOWING, override, problems, false));
        put(c, "expected", pick(cCols, "crops", "expected", C_EXPECTED, override, problems, false));
        if (c.get("variety") != null && c.get("variety").equalsIgnoreCase(c.get("name"))) {
            c.remove("variety");
        }

        Map<String, String> h = new LinkedHashMap<>();
        put(h, "id", pick(hCols, "harvests", "id", H_ID, override, problems, false));
        String cropCol = pick(hCols, "harvests", "crop", H_CROP, override, problems, false);
        if (cropCol == null && harvestsToCrops != null) {
            cropCol = find(hCols, harvestsToCrops.fkColumn());
        }
        if (cropCol == null) {
            problems.add("harvests: no column links a harvest to its crop (tried " + String.join(", ", H_CROP)
                    + "; columns: " + hCols + "). Set agro.map.harvests.crop=<column>");
        }
        put(h, "crop", cropCol);
        put(h, "date", pick(hCols, "harvests", "date", H_DATE, override, problems, true));
        put(h, "qty", pick(hCols, "harvests", "qty", H_QTY, override, problems, true));
        put(h, "unit", pick(hCols, "harvests", "unit", H_UNIT, override, problems, false));
        put(h, "revenue", pick(hCols, "harvests", "revenue", H_REVENUE, override, problems, false));

        if (!problems.isEmpty()) {
            throw new SchemaException(problems);
        }

        String unit = clean(override.apply("agro.default-unit"));
        if (unit == null && !h.containsKey("unit")) {
            unit = unitFromColumnName(h.get("qty"));
        }
        return new AgroSchema(quote,
                new AgroSchema.Table(users, u), new AgroSchema.Table(farms, f),
                new AgroSchema.Table(crops, c), new AgroSchema.Table(harvests, h), unit);
    }

    static String unitFromColumnName(String column) {
        if (column == null) {
            return null;
        }
        String n = column.toLowerCase(Locale.ROOT);
        if (n.contains("kg")) {
            return "kg";
        }
        if (n.contains("quintal")) {
            return "quintals";
        }
        if (n.contains("ton")) {
            return "tons";
        }
        return null;
    }

    private static void put(Map<String, String> map, String logical, String column) {
        if (column != null) {
            map.put(logical, column);
        }
    }

    private static String table(List<String> all, String logical, String[] candidates,
                                Function<String, String> override, List<String> problems) {
        String wanted = clean(override.apply("agro.map." + logical + ".table"));
        if (wanted != null) {
            String found = find(all, wanted);
            if (found == null) {
                problems.add("table '" + wanted + "' (agro.map." + logical + ".table) not found. Tables: " + all);
            }
            return found;
        }
        for (String candidate : candidates) {
            String found = find(all, candidate);
            if (found != null) {
                return found;
            }
        }
        problems.add("table for '" + logical + "' not found (tried " + String.join(", ", candidates)
                + "). Tables in the database: " + all + ". Set agro.map." + logical + ".table=<table name>");
        return null;
    }

    private static String pick(List<String> cols, String table, String logical, String[] candidates,
                               Function<String, String> override, List<String> problems, boolean required) {
        String wanted = clean(override.apply("agro.map." + table + "." + logical));
        if (wanted != null) {
            String found = find(cols, wanted);
            if (found == null) {
                problems.add(table + "." + wanted + " (agro.map." + table + "." + logical
                        + ") not found. Columns: " + cols);
            }
            return found;
        }
        for (String candidate : candidates) {
            String found = find(cols, candidate);
            if (found != null) {
                return found;
            }
        }
        if (required) {
            problems.add(table + ": no '" + logical + "' column (tried " + String.join(", ", candidates)
                    + "; columns: " + cols + "). Set agro.map." + table + "." + logical + "=<column>");
        }
        return null;
    }

    private static String find(List<String> names, String wanted) {
        if (wanted == null) {
            return null;
        }
        for (String n : names) {
            if (n.equalsIgnoreCase(wanted)) {
                return n;
            }
        }
        return null;
    }

    private static SchemaSource.ForeignKey fkTo(SchemaSource src, String table, String parent) {
        try {
            for (SchemaSource.ForeignKey fk : src.foreignKeys(table)) {
                if (fk.pkTable() != null && fk.pkTable().equalsIgnoreCase(parent)) {
                    return fk;
                }
            }
        } catch (Exception e) {

        }
        return null;
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
