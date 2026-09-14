package com.plover.backerymanagmentsystem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import java.util.List;

@Component
public class DbChecker implements CommandLineRunner {

    @Autowired
    private BillOfMaterialRepository bomRepo;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========== DB CHECKER START ==========");
        List<BillOfMaterial> boms = bomRepo.findAll();
        for (BillOfMaterial bom : boms) {
            System.out.println("BOM: id=" + bom.getId() + ", parent=" + bom.getParentProductId() 
                + ", child=" + bom.getChildItemId() + ", type=" + bom.getChildType() 
                + ", qty=" + bom.getQuantity() + ", active=" + bom.getIsActive());
        }
        System.out.println("========== DB CHECKER END ==========");
    }
}
