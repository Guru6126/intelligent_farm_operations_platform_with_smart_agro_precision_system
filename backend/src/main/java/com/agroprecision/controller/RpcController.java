package com.agroprecision.controller;

import com.agroprecision.security.AuthUser;
import com.agroprecision.service.FarmService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Named farm actions that go beyond plain CRUD. */
@RestController
@RequestMapping("/api/rpc")
public class RpcController {
    private final FarmService farms;

    public RpcController(FarmService farms) { this.farms = farms; }

    @PostMapping("/seed_demo_data")
    public Map<String, String> seed(@AuthenticationPrincipal AuthUser u) {
        return Map.of("farmId", farms.seedDemo(u));
    }

    @PostMapping("/add_farm_member_by_email")
    public Map<String, String> addMember(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, String> body) {
        return Map.of("userId", farms.addMemberByEmail(u, body.get("_farm"), body.get("_email")));
    }
}
