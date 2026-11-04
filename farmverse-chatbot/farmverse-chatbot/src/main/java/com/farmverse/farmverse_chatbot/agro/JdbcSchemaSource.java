package com.farmverse.farmverse_chatbot.agro;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcSchemaSource implements SchemaSource {

    private final DatabaseMetaData meta;
    private final String catalog;

    public JdbcSchemaSource(Connection connection) throws SQLException {
        this.meta = connection.getMetaData();
        this.catalog = connection.getCatalog();
    }

    public String quoteString() throws SQLException {
        String q = meta.getIdentifierQuoteString();
        return q == null || q.isBlank() ? "" : q.trim();
    }

    public String databaseName() {
        return catalog;
    }

    @Override
    public List<String> tables() throws SQLException {
        List<String> out = new ArrayList<>();
        try (ResultSet rs = meta.getTables(catalog, null, "%", new String[] {"TABLE", "VIEW"})) {
            while (rs.next()) {
                out.add(rs.getString("TABLE_NAME"));
            }
        }
        return out;
    }

    @Override
    public List<String> columns(String table) throws SQLException {
        List<String> out = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(catalog, null, escape(table), "%")) {
            while (rs.next()) {
                out.add(rs.getString("COLUMN_NAME"));
            }
        }
        return out;
    }

    @Override
    public List<ForeignKey> foreignKeys(String table) throws SQLException {
        List<ForeignKey> out = new ArrayList<>();
        try (ResultSet rs = meta.getImportedKeys(catalog, null, table)) {
            while (rs.next()) {
                out.add(new ForeignKey(rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME")));
            }
        }
        return out;
    }

    private String escape(String name) throws SQLException {
        String esc = meta.getSearchStringEscape();
        if (esc == null || esc.isEmpty()) {
            return name;
        }
        return name.replace("_", esc + "_").replace("%", esc + "%");
    }
}
