package com.plover.backerymanagmentsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionBatchRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class CheckDatesTest {

    @Autowired
    private SupplierPaymentRepository supplierPaymentRepo;

    @Autowired
    private GrnRepository grnRepo;

    @Autowired
    private ProductionBatchRepository productionBatchRepo;

    @Autowired
    private StockAdjustRepository stockAdjustRepo;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    public void printDates() {
        System.out.println("========== DB SALES COLUMNS CHECK START ==========");
        List<java.util.Map<String, Object>> columns = jdbcTemplate.queryForList("SHOW FULL COLUMNS FROM sales");
        columns.forEach(col -> System.out.println("COLUMN: " + col.get("Field") + " | TYPE: " + col.get("Type")));
        System.out.println("========== DB SALES COLUMNS CHECK END ==========");
        
        System.out.println("SupplierPayments dates:");
        supplierPaymentRepo.findAll().forEach(p -> 
            System.out.println("  Payment ID=" + p.getPaymentId() + ", Date=" + p.getPaymentDate())
        );

        System.out.println("GRNs (first 5 and last 5) dates:");
        List<String> grnDates = grnRepo.findAll().stream()
            .map(g -> g.getReceivedDate() != null ? g.getReceivedDate().toLocalDate().toString() : "null")
            .distinct()
            .collect(Collectors.toList());
        System.out.println("  Distinct GRN dates: " + grnDates);

        System.out.println("ProductionBatches distinct dates:");
        List<String> pbDates = productionBatchRepo.findAll().stream()
            .map(pb -> pb.getCreatedAt() != null ? pb.getCreatedAt().toLocalDate().toString() : "null")
            .distinct()
            .collect(Collectors.toList());
        System.out.println("  Distinct Production Batch dates: " + pbDates);

        System.out.println("StockAdjusts dates:");
        stockAdjustRepo.findAll().forEach(sa -> 
            System.out.println("  StockAdjust ID=" + sa.getId() + ", Date=" + (sa.getCreatedAt() != null ? sa.getCreatedAt().toLocalDate() : "null"))
        );

        System.out.println("========== DB DATES CHECKER END ==========");
    }
}
