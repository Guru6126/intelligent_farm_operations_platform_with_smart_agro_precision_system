package com.farmverse.farmverse_chatbot.agro;

import java.util.List;

public interface SchemaSource {

    record ForeignKey(String fkColumn, String pkTable, String pkColumn) {
    }

    List<String> tables() throws Exception;

    List<String> columns(String table) throws Exception;

    List<ForeignKey> foreignKeys(String table) throws Exception;
}
