package com.plover.backerymanagmentsystem.core.config;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            List<String> indexNames = jdbcTemplate.query(
                "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'raw_materials' " +
                "AND COLUMN_NAME = 'material_code' AND INDEX_NAME != 'PRIMARY'",
                (rs, rowNum) -> rs.getString("INDEX_NAME")
            );

            for (String indexName : indexNames) {
                try {
                    log.info("Dropping unique index '{}' on raw_materials.material_code", indexName);
                    jdbcTemplate.execute("ALTER TABLE raw_materials DROP INDEX `" + indexName + "`");
                } catch (Exception e) {
                    log.warn("Failed to drop index '{}': {}", indexName, e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute("ALTER TABLE iou_requests ADD COLUMN issued_amount DOUBLE NULL;");
                log.info("Added issued_amount column to iou_requests");
            } catch (Exception e) {
                log.debug("issued_amount column may already exist: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE iou_request_items ADD COLUMN raw_material_id BIGINT NULL;");
                log.info("Added raw_material_id column to iou_request_items");
            } catch (Exception e) {
                log.debug("raw_material_id column may already exist: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE iou_request_items ADD COLUMN unit_of_measure VARCHAR(50) NULL;");
                log.info("Added unit_of_measure column to iou_request_items");
            } catch (Exception e) {
                log.debug("unit_of_measure column may already exist: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("DatabaseMigrationRunner encountered an error checking constraints: {}", e.getMessage());
        }
    }
}
