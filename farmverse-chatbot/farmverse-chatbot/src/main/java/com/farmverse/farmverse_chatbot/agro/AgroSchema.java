package com.farmverse.farmverse_chatbot.agro;

import java.util.LinkedHashMap;
import java.util.Map;

public record AgroSchema(String quote, Table users, Table farms, Table crops, Table harvests,
                         String defaultUnit) {

    public record Table(String name, Map<String, String> cols) {

        public String col(String logical) {
            return cols.get(logical);
        }

        public boolean has(String logical) {
            return cols.containsKey(logical);
        }
    }

    public Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("users", users.name() + " " + users.cols());
        m.put("farms", farms.name() + " " + farms.cols());
        m.put("crops", crops.name() + " " + crops.cols());
        m.put("harvests", harvests.name() + " " + harvests.cols());
        m.put("defaultUnit", defaultUnit == null ? "(none, a unit column or plain numbers are used)" : defaultUnit);
        return m;
    }
}
