package com.farmverse.farmverse_chatbot.agro;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionSource {

    Connection open() throws SQLException;
}
