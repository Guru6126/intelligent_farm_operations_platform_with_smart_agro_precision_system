package com.smartagro.backend.farm;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class FarmDataController {

	private final FarmDataService farmDataService;

	public FarmDataController(FarmDataService farmDataService) {
		this.farmDataService = farmDataService;
	}

	@GetMapping("/{table}")
	public List<Map<String, Object>> list(
			Authentication authentication,
			@PathVariable String table,
			@RequestParam(required = false) String farmId,
			@RequestParam(required = false) String orderBy,
			@RequestParam(defaultValue = "false") boolean ascending,
			@RequestParam(required = false) Integer limit) {
		return farmDataService.list(authentication, table, farmId, orderBy, ascending, limit);
	}

	@PostMapping("/{table}")
	public ResponseEntity<Map<String, Object>> create(
			Authentication authentication,
			@PathVariable String table,
			@RequestBody Map<String, Object> body) {
		Map<String, Object> created = farmDataService.create(authentication, table, body);
		return ResponseEntity
				.created(URI.create("/api/data/" + table + "/" + created.get("id")))
				.body(created);
	}
}
