package com.farmverse.farmverse_chatbot;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.farmverse.farmverse_chatbot.agro.AgroSchema;
import com.farmverse.farmverse_chatbot.agro.AgroSchemaResolver;
import com.farmverse.farmverse_chatbot.agro.ConnectionSource;
import com.farmverse.farmverse_chatbot.agro.DbCheck;
import com.farmverse.farmverse_chatbot.agro.FarmerChat;
import com.farmverse.farmverse_chatbot.agro.FarmerRepository;
import com.farmverse.farmverse_chatbot.agro.FarmerSelfTest;
import com.farmverse.farmverse_chatbot.agro.JdbcSchemaSource;
import com.farmverse.farmverse_chatbot.agro.SchemaException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PreDestroy;

@Service
public class FarmerRecordsService {

    private final Environment env;
    private final FarmerChat chat;
    private HikariDataSource dataSource;
    private volatile boolean ready;
    private volatile long readyCheckedAt;

    public FarmerRecordsService(Environment env) {
        this.env = env;
        this.chat = new FarmerChat(this::repository, Clock.systemDefaultZone());
    }

    public Optional<FarmerChat.Result> answer(String question, String lang, boolean roman) {
        try {
            return chat.answer(question, lang, roman);
        } catch (RuntimeException e) {
            System.err.println("[farmer-records] unexpected error, using normal answer flow: " + e);
            return Optional.empty();
        }
    }

    public Map<String, Object> dbCheck() {
        return DbCheck.run(source(), env::getProperty);
    }

    public List<Map<String, Object>> selfTest() {
        return FarmerSelfTest.run(chat);
    }

    public boolean isReady() {
        long now = System.currentTimeMillis();
        if (now - readyCheckedAt > 15_000) {
            try {
                repository();
                ready = true;
            } catch (Exception e) {
                ready = false;
            }
            readyCheckedAt = now;
        }
        return ready;
    }

    private ConnectionSource source() {
        return () -> dataSource().getConnection();
    }

    private FarmerRepository repository() throws SQLException {
        try (Connection c = dataSource().getConnection()) {
            JdbcSchemaSource meta = new JdbcSchemaSource(c);
            AgroSchema schema = AgroSchemaResolver.resolve(meta, meta.quoteString(), env::getProperty);
            return new FarmerRepository(source(), schema);
        } catch (SchemaException e) {
            throw new FarmerChat.SchemaProblem(e.getMessage());
        } catch (SQLException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    private synchronized HikariDataSource dataSource() {
        if (dataSource == null) {
            HikariConfig cfg = new HikariConfig();
            cfg.setPoolName("agroprecision");
            cfg.setJdbcUrl(env.getProperty("agro.datasource.url",
                    "jdbc:mysql://localhost:3306/agroprecision?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
            cfg.setUsername(env.getProperty("agro.datasource.username", "root"));
            cfg.setPassword(env.getProperty("agro.datasource.password", ""));
            cfg.setMaximumPoolSize(3);
            cfg.setConnectionTimeout(5000);
            cfg.setInitializationFailTimeout(-1);
            dataSource = new HikariDataSource(cfg);
        }
        return dataSource;
    }

    @PreDestroy
    void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
