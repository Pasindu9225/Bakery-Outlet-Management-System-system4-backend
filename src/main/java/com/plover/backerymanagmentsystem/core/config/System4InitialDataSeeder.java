package com.plover.backerymanagmentsystem.core.config;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Automatically seeds initial Admin, POS user accounts and sample products
 * into System 4 database if empty.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class System4InitialDataSeeder implements CommandLineRunner {

    private final AuthRepository authRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. Seed Auth Users if empty
            if (authRepository.count() == 0) {
                log.info("System 4 database user table (bmsauth) is empty. Seeding default Admin and POS Cashier accounts...");

                // Admin Account
                AuthModel adminUser = AuthModel.builder()
                        .id(IdUtil.uuidToBytes(UUID.randomUUID()))
                        .username("admin_123")
                        .passwordHash(BCrypt.hashpw("admin123", BCrypt.gensalt()))
                        .firstName("System4")
                        .lastName("Admin")
                        .email("admin@system4.com")
                        .roleId("1") // Admin role
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();

                // POS Cashier Account
                AuthModel posUser = AuthModel.builder()
                        .id(IdUtil.uuidToBytes(UUID.randomUUID()))
                        .username("pos_123")
                        .passwordHash(BCrypt.hashpw("pos123", BCrypt.gensalt()))
                        .firstName("POS")
                        .lastName("Cashier")
                        .email("pos@system4.com")
                        .roleId("8") // POS role
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();

                authRepository.save(adminUser);
                authRepository.save(posUser);

                log.info("Successfully seeded default users: admin_123 (pass: admin123), pos_123 (pass: pos123)");
            }

            // 2. Seed Sample Products if empty
            if (productRepository.count() == 0) {
                log.info("System 4 products table is empty. Seeding sample products...");

                Product[] sampleProducts = new Product[] {
                    Product.builder().productName("White Bread").productCode("WB001").description("Fresh white bread").unitPrice(250.00).isActive(true).unitOfMeasure("pieces").build(),
                    Product.builder().productName("Whole Wheat Bread").productCode("WWB001").description("Healthy whole wheat bread").unitPrice(300.00).isActive(true).unitOfMeasure("pieces").build(),
                    Product.builder().productName("Croissant").productCode("CR001").description("Buttery croissant").unitPrice(180.00).isActive(true).unitOfMeasure("pieces").build(),
                    Product.builder().productName("Chocolate Cake").productCode("CC001").description("Delicious chocolate cake").unitPrice(2500.00).isActive(true).unitOfMeasure("pieces").build(),
                    Product.builder().productName("Glazed Donut").productCode("DN001").description("Glazed donut").unitPrice(120.00).isActive(true).unitOfMeasure("pieces").build()
                };

                for (Product p : sampleProducts) {
                    productRepository.save(p);
                }
                log.info("Successfully seeded 5 sample products into System 4 database");
            }
        } catch (Exception e) {
            log.error("Error during System4InitialDataSeeder execution: {}", e.getMessage(), e);
        }
    }
}
