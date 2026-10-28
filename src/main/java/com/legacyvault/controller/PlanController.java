package com.legacyvault.controller;

import com.legacyvault.dto.PlanResponse;
import com.legacyvault.service.PlanCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanCatalogService planCatalog;

    public PlanController(PlanCatalogService planCatalog) {
        this.planCatalog = planCatalog;
    }

    @GetMapping
    public List<PlanResponse> getPlans() {
        return planCatalog.getPlans();
    }
}
