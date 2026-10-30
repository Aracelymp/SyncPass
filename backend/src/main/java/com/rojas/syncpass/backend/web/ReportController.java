package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.dto.ApiDtos.ImpactReportDto;
import com.rojas.syncpass.backend.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/impact")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public ImpactReportDto impact() {
        return reportService.impact();
    }
}
