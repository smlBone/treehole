package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Result;
import org.mf.treehole.dto.HandleReportRequest;
import org.mf.treehole.dto.ReportRequest;
import org.mf.treehole.service.ReportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Resource
    private ReportService reportService;

    @PostMapping
    public Result<Void> createReport(@RequestBody ReportRequest req) {
        return reportService.createReport(req);
    }

    @GetMapping("/pending")
    public Result<?> getPendingReports(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return reportService.getPendingReports(page, size);
    }

    @PostMapping("/handle")
    public Result<Void> handleReport(@RequestBody HandleReportRequest req) {
        return reportService.handleReport(req);
    }
}
