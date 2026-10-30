package com.agroprecision.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Ensures database integrity on startup:
 * 1. Finds and cleans up existing duplicate plots within the same farm.
 * 2. Enforces unique index `uq_farm_plot_name (farm_id, name)` on the plots table.
 */
@Component
@Order(1)
public class DatabaseMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);
    private final JdbcTemplate db;

    public DatabaseMigrationRunner(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public void run(ApplicationArguments args) {
        deduplicatePlots();
        ensurePlotUniqueConstraint();
    }

    private void deduplicatePlots() {
        try {
            // Find duplicate plot names per farm
            List<Map<String, Object>> duplicates = db.queryForList(
                "SELECT farm_id, LOWER(TRIM(name)) AS plot_name, COUNT(*) AS cnt " +
                "FROM plots " +
                "GROUP BY farm_id, LOWER(TRIM(name)) " +
                "HAVING cnt > 1"
            );

            for (Map<String, Object> dup : duplicates) {
                String farmId = (String) dup.get("farm_id");
                String plotName = (String) dup.get("plot_name");
                log.warn("Found duplicate plots in farm {} with name '{}'", farmId, plotName);

                // Fetch all matching plot records ordered by creation date
                List<Map<String, Object>> plots = db.queryForList(
                    "SELECT id, boundary_geojson FROM plots " +
                    "WHERE farm_id = ? AND LOWER(TRIM(name)) = ? " +
                    "ORDER BY created_at ASC",
                    farmId, plotName
                );

                if (plots.size() <= 1) continue;

                // Pick best candidate to keep (prefer one that has boundary_geojson or linked crops)
                String keepId = null;
                for (Map<String, Object> p : plots) {
                    String pid = (String) p.get("id");
                    boolean hasBoundary = p.get("boundary_geojson") != null && !p.get("boundary_geojson").toString().isBlank();
                    Integer cropCount = db.queryForObject("SELECT COUNT(*) FROM crops WHERE plot_id = ?", Integer.class, pid);
                    if (hasBoundary || (cropCount != null && cropCount > 0)) {
                        keepId = pid;
                        break;
                    }
                }
                if (keepId == null) {
                    keepId = (String) plots.get(0).get("id");
                }

                // For all other duplicate records: re-point references and delete
                for (Map<String, Object> p : plots) {
                    String pid = (String) p.get("id");
                    if (pid.equals(keepId)) continue;

                    // Re-link crops and irrigation schedules to keepId
                    db.update("UPDATE crops SET plot_id = ? WHERE plot_id = ?", keepId, pid);
                    db.update("UPDATE irrigation_schedules SET plot_id = ? WHERE plot_id = ?", keepId, pid);

                    // Delete redundant duplicate plot
                    db.update("DELETE FROM plots WHERE id = ?", pid);
                    log.info("Deleted duplicate plot id {} in favor of {}", pid, keepId);
                }
            }
        } catch (Exception e) {
            log.error("Error during plot deduplication migration: {}", e.getMessage(), e);
        }
    }

    private void ensurePlotUniqueConstraint() {
        try {
            Integer exists = db.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'plots' AND INDEX_NAME = 'uq_farm_plot_name'",
                Integer.class
            );
            if (exists == null || exists == 0) {
                log.info("Adding unique constraint uq_farm_plot_name on plots(farm_id, name)...");
                db.execute("ALTER TABLE plots ADD UNIQUE KEY uq_farm_plot_name (farm_id, name)");
                log.info("Successfully added unique constraint uq_farm_plot_name.");
            }
        } catch (Exception e) {
            log.error("Could not add uq_farm_plot_name constraint: {}", e.getMessage());
        }
    }
}

