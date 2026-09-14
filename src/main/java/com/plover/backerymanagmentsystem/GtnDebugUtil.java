package com.plover.backerymanagmentsystem;

import com.plover.backerymanagmentsystem.pos.model.Gtn;
import com.plover.backerymanagmentsystem.pos.model.GtnItem;
import com.plover.backerymanagmentsystem.pos.repository.GtnRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

// @Component
@RequiredArgsConstructor
@Slf4j
public class GtnDebugUtil implements CommandLineRunner {

    private final GtnRepository gtnRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.OutletRepository outletRepository;
    private final com.plover.backerymanagmentsystem.admin.repository.BmsAuthRepository bmsAuthRepository;
    private final com.plover.backerymanagmentsystem.pos.repository.DayProductionRepository dayProductionRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) throws Exception {
        log.info("======= BMS SYSTEM AUDIT =======");
        
        // 0. Audit Users
        log.info("--- USER ACCOUNTS ---");
        bmsAuthRepository.findAll().forEach(u -> {
            log.info("User: {} | Role: {} | Outlet ID: {}", 
                u.getUsername(), u.getRoleId(), u.getOutletId());
        });
        
        // 1. Audit Outlets
        log.info("--- OUTLET LIST ---");
        outletRepository.findAll().forEach(o -> {
            log.info("Outlet ID: {} | Name: {} | Address: {}", o.getOutletId(), o.getName(), o.getAddress());
        });

        // 2. Audit GTNs
        log.info("--- RECENT GTNS ---");
        List<Gtn> allGtns = gtnRepository.findAll();
        allGtns.stream()
            .sorted((a, b) -> b.getGtnId().compareTo(a.getGtnId()))
            .limit(10)
            .forEach(gtn -> {
                log.info("GTN ID: {} | Status: {} | Outlet: {} | Source: {}", 
                    gtn.getGtnId(), gtn.getStatus(), gtn.getOutletId(), gtn.getSource());
                
                try {
                    List<GtnItem> items = gtn.getGtnItems();
                    if (items != null) {
                        items.size(); 
                        items.forEach(item -> {
                            log.info("   -> [GTN-ITEM] ID: {} | Product: {} | Qty: {} | Status: {}", 
                                item.getGtnItemId(), 
                                item.getProduct() != null ? item.getProduct().getProductName() : "UNKNOWN",
                                item.getExpectedQty(),
                                item.getStatus());
                        });
                    }
                } catch (Exception e) {
                    log.error("   -> [ERROR] Could not load items for GTN {}: {}", gtn.getGtnId(), e.getMessage());
                }
            });

        // 3. Audit DayProduction
        log.info("--- TODAY'S PRODUCTION RECORDS (ALL OUTLETS) ---");
        dayProductionRepository.findAll().stream()
            .filter(dp -> java.time.LocalDate.now().equals(dp.getOrderedDate()))
            .forEach(dp -> {
                log.info("Production ID: {} | Outlet: {} | Date: {}", dp.getProductionId(), dp.getOutletId(), dp.getOrderedDate());
                if (dp.getDayProductionItems() != null) {
                    dp.getDayProductionItems().forEach(item -> {
                        log.info("   -> [POS-ITEM] ID: {} | Product: {} | Current Qty: {}", 
                            item.getDayProductionItemId(),
                            item.getProduct() != null ? item.getProduct().getProductName() : "UNKNOWN",
                            item.getCurrentQty());
                    });
                }
            });

        log.info("==================================");
    }
}
