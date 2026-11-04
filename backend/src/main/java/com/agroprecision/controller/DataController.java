package com.agroprecision.controller;

import com.agroprecision.security.AuthUser;
import com.agroprecision.service.DataService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST endpoints for all farm records: /api/data/{table}[/{id}] */
@RestController
@RequestMapping("/api")
public class DataController {
    private final DataService data;

    public DataController(DataService data) { this.data = data; }

    @GetMapping("/data/{table}")
    public List<Map<String, Object>> list(@AuthenticationPrincipal AuthUser u, @PathVariable String table,
                                          @RequestParam(required = false) String farmId,
                                          @RequestParam(required = false) String orderBy,
                                          @RequestParam(defaultValue = "false") boolean ascending,
                                          @RequestParam(required = false) Integer limit) {
        return data.list(u, table, farmId, orderBy, ascending, limit);
    }

    @PostMapping("/data/{table}")
    public Map<String, Object> create(@AuthenticationPrincipal AuthUser u, @PathVariable String table, @RequestBody Map<String, Object> body) {
        return data.create(u, table, body);
    }

    @PutMapping("/data/{table}/{id}")
    public Map<String, Object> update(@AuthenticationPrincipal AuthUser u, @PathVariable String table, @PathVariable String id,
                                      @RequestBody Map<String, Object> body) {
        return data.update(u, table, id, body);
    }

    @DeleteMapping("/data/{table}/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser u, @PathVariable String table, @PathVariable String id) {
        data.delete(u, table, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notifications/read-all")
    public Map<String, Integer> readAll(@AuthenticationPrincipal AuthUser u) {
        return Map.of("updated", data.markAllRead(u));
    }
}
