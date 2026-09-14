package com.plover.backerymanagmentsystem.manager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import java.util.List;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @Autowired
    private BillOfMaterialRepository bomRepo;

    @GetMapping("/boms")
    public List<BillOfMaterial> getBoms() {
        return bomRepo.findAll();
    }
}
