package com.farmverse.farmverse_chatbot;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class FarmerRecordsController {

    private final FarmerRecordsService farmerRecords;

    public FarmerRecordsController(FarmerRecordsService farmerRecords) {
        this.farmerRecords = farmerRecords;
    }

    @GetMapping("/db-check")
    public Map<String, Object> dbCheck() {
        return farmerRecords.dbCheck();
    }

    @GetMapping("/farmer-selftest")
    public Map<String, Object> selfTest() {
        List<Map<String, Object>> rows = farmerRecords.selfTest();
        long failed = rows.stream().filter(r -> !Boolean.TRUE.equals(r.get("pass"))).count();
        return Map.of("allPassed", failed == 0, "failed", failed, "cases", rows);
    }
}
