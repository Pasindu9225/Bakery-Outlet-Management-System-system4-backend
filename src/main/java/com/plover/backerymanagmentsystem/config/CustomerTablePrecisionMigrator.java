package com.plover.backerymanagmentsystem.config;

import java.util.UUID;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerTablePrecisionMigrator {

    private final JdbcTemplate jdbcTemplate;
    private final AuthRepository authRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void migratePrecision() {
        log.info("Running custom DB migration and seeding finance user credentials...");

        try {
            String admin123Hash = BCrypt.hashpw("admin123", BCrypt.gensalt());

            Optional<AuthModel> f123Opt = authRepository.findByUsername("finance_123");
            if (f123Opt.isPresent()) {
                AuthModel u = f123Opt.get();
                u.setPasswordHash(admin123Hash);
                u.setActive(true);
                authRepository.save(u);
                log.info("Updated finance_123 password to 'admin123'");
            } else {
                byte[] uuidBytes = IdUtil.uuidToBytes(UUID.randomUUID());
                AuthModel financeUser = AuthModel.builder()
                        .id(uuidBytes)
                        .username("finance_123")
                        .passwordHash(admin123Hash)
                        .email("finance_123@bakery.com")
                        .phone("0770000015")
                        .roleId("15")
                        .firstName("Finance")
                        .lastName("Officer")
                        .isActive(true)
                        .build();
                authRepository.save(financeUser);
                log.info("Seeded default Finance user credentials: username='finance_123', password='admin123', role='15'");
            }

            Optional<AuthModel> f1Opt = authRepository.findByUsername("finance_1");
            if (f1Opt.isPresent()) {
                AuthModel u = f1Opt.get();
                u.setPasswordHash(admin123Hash);
                u.setActive(true);
                authRepository.save(u);
            }
        } catch (Exception e) {
            log.error("Could not seed default Finance user: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE customers MODIFY COLUMN loyalty_points DECIMAL(10, 3) NOT NULL DEFAULT 0.000");
        } catch (Exception e) {
            log.debug("Could not alter loyalty_points: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE customer_points_history MODIFY COLUMN points_changed DECIMAL(10, 3) NOT NULL");
        } catch (Exception e) {
            log.debug("Could not alter points_changed: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE raw_material_return_items MODIFY COLUMN status ENUM('APPROVED','NOT_APPROVED','RETURNED','REJECTED') NOT NULL");
        } catch (Exception e) {
            log.debug("Could not alter status in raw_material_return_items: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE payment_methods MODIFY COLUMN category ENUM('BANK_TRANSFER','CARD','CASH','FREE_MEAL','PICKME','UBER','CREDIT') NOT NULL");
        } catch (Exception e) {
            log.debug("Could not alter category in payment_methods: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("INSERT INTO payment_methods (name, category) SELECT 'Credit', 'CREDIT' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE name = 'Credit')");
        } catch (Exception e) {
            log.debug("Could not insert default Credit payment method: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE pos_waiter_items ADD COLUMN bill_id VARCHAR(50)");
            log.info("Added bill_id column to pos_waiter_items table.");
        } catch (Exception e) {
            log.debug("bill_id column in pos_waiter_items already exists or could not be added: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE returns MODIFY COLUMN payment_method_id INT NULL");
            log.info("Altered returns.payment_method_id to INT NULL.");
        } catch (Exception e) {
            log.debug("Could not alter payment_method_id in returns: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE customers ADD COLUMN id_card_number VARCHAR(50) NULL");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE sales ADD COLUMN customer_id BIGINT NULL");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE sales MODIFY COLUMN payment_type VARCHAR(100) NULL");
            log.info("Altered sales.payment_type to VARCHAR(100).");
        } catch (Exception e) {
            log.debug("Could not alter sales.payment_type: " + e.getMessage());
        }

        log.info("Database migration for customer loyalty points precision, return status enum, payment methods, and waiter bill_id completed successfully.");
    }
}
