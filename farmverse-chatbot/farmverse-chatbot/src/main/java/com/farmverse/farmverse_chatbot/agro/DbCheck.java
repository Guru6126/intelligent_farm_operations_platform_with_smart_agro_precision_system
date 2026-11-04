package com.farmverse.farmverse_chatbot.agro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class DbCheck {

    private DbCheck() {
    }

    public static Map<String, Object> run(ConnectionSource source, Function<String, String> override) {
        Map<String, Object> out = new LinkedHashMap<>();
        try (var c = source.open()) {
            out.put("connected", true);
            out.put("database", c.getCatalog());
            JdbcSchemaSource meta = new JdbcSchemaSource(c);
            AgroSchema schema;
            try {
                schema = AgroSchemaResolver.resolve(meta, meta.quoteString(), override);
            } catch (SchemaException e) {
                out.put("tablesOk", false);
                out.put("problems", e.problems());
                out.put("tablesInDatabase", meta.tables());
                out.put("ready", false);
                return out;
            }
            out.put("tablesOk", true);
            out.put("mapping", schema.describe());
            Map<String, Object> data = new FarmerRepository(source, schema).overview(50);
            out.putAll(data);
            @SuppressWarnings("unchecked")
            Map<String, Long> counts = (Map<String, Long>) data.get("rowCounts");
            boolean hasFarmers = counts.get(schema.users().name()) > 0;
            out.put("hasFarmerRecords", hasFarmers);
            out.put("ready", hasFarmers);
            if (!hasFarmers) {
                out.put("hint", "The users table is empty. Run database/agroprecision_sample.sql or load your farmers.");
            }
        } catch (Exception e) {
            out.put("connected", false);
            out.put("ready", false);
            out.put("problems", List.of(String.valueOf(e.getMessage())));
        }
        return out;
    }
}
