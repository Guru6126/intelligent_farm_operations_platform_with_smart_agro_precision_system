package com.smartagro.backend.farm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.smartagro.backend.user.AppUser;
import com.smartagro.backend.user.UserRepository;
import com.smartagro.backend.user.UserRole;

@Service
public class FarmDataService {

	private static final int MAX_LIMIT = 5000;

	private final FarmRepository farmRepository;
	private final PlotRepository plotRepository;
	private final CropRepository cropRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public FarmDataService(
			FarmRepository farmRepository,
			PlotRepository plotRepository,
			CropRepository cropRepository,
			UserRepository userRepository,
			ObjectMapper objectMapper) {
		this.farmRepository = farmRepository;
		this.plotRepository = plotRepository;
		this.cropRepository = cropRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> list(
			Authentication authentication,
			String table,
			String farmId,
			String orderBy,
			boolean ascending,
			Integer requestedLimit) {
		AppUser user = currentUser(authentication);
		List<Map<String, Object>> rows = switch (table) {
			case "farms" -> farmsFor(user, farmId).stream()
					.map(this::farmRow)
					.toList();
			case "plots" -> plotsFor(user, visibleFarmIds(user), farmId).stream()
					.map(this::plotRow)
					.toList();
			case "crops" -> cropsFor(user, visibleFarmIds(user), farmId).stream()
					.map(this::cropRow)
					.toList();
			default -> throw new DataApiException(HttpStatus.NOT_FOUND, "Unsupported data table.");
		};

		int limit = checkedLimit(requestedLimit);
		Comparator<Map<String, Object>> comparator = rowComparator(orderBy);
		if (comparator != null) {
			rows = new ArrayList<>(rows);
			rows.sort(ascending ? comparator : comparator.reversed());
		}
		return rows.stream().limit(limit).toList();
	}

	@Transactional
	public Map<String, Object> create(
			Authentication authentication,
			String table,
			Map<String, Object> body) {
		AppUser user = currentUser(authentication);
		return switch (table) {
			case "farms" -> farmRow(createFarm(user, body));
			case "plots" -> plotRow(createPlot(user, body));
			case "crops" -> cropRow(createCrop(user, body));
			default -> throw new DataApiException(HttpStatus.NOT_FOUND, "Unsupported data table.");
		};
	}

	private Farm createFarm(AppUser user, Map<String, Object> body) {
		if (user.getRole() != UserRole.FARMER && user.getRole() != UserRole.ADMIN) {
			throw forbidden();
		}

		String name = requiredText(body, "name", 120);
		String location = optionalText(body, "location", 255);
		Double latitude = optionalDecimal(body, "latitude", -90, 90);
		Double longitude = optionalDecimal(body, "longitude", -180, 180);
		BigDecimal totalAreaAcres = optionalBigDecimal(body, "total_area_acres", BigDecimal.ZERO, null);
		return farmRepository.save(new Farm(user, name, location, latitude, longitude, totalAreaAcres));
	}

	private Plot createPlot(AppUser user, Map<String, Object> body) {
		Farm farm = manageableFarm(user, requiredId(body, "farm_id"));
		String name = requiredText(body, "name", 100);
		BigDecimal areaAcres = optionalBigDecimal(body, "area_acres", BigDecimal.ZERO, null);
		BigDecimal soilPh = optionalBigDecimal(body, "soil_ph", BigDecimal.ZERO, BigDecimal.valueOf(14));
		String soilType = optionalText(body, "soil_type", 100);
		String boundary = serializeBoundary(body.get("boundary_geojson"));
		return plotRepository.save(new Plot(farm, name, areaAcres, soilPh, soilType, boundary));
	}

	private Crop createCrop(AppUser user, Map<String, Object> body) {
		Farm farm = manageableFarm(user, requiredId(body, "farm_id"));
		String cropName = requiredText(body, "crop_name", 255);
		String variety = optionalText(body, "variety", 255);
		String season = optionalText(body, "season", 100);
		String stage = optionalText(body, "stage", 30);
		if (stage == null) {
			stage = "Planned";
		}

		Long plotId = optionalId(body, "plot_id");
		Plot plot = null;
		if (plotId != null) {
			plot = plotRepository.findById(plotId)
					.orElseThrow(() -> new DataApiException(HttpStatus.NOT_FOUND, "Plot not found."));
			if (!plot.getFarm().getId().equals(farm.getId())) {
				throw new DataApiException(HttpStatus.BAD_REQUEST, "The plot must belong to the selected farm.");
			}
		}

		LocalDate sowingDate = optionalDate(body, "sowing_date");
		LocalDate expectedHarvest = optionalDate(body, "expected_harvest");
		if (sowingDate != null && expectedHarvest != null && expectedHarvest.isBefore(sowingDate)) {
			throw badRequest("Expected harvest cannot be before the sowing date.");
		}
		return cropRepository.save(new Crop(
				farm, plot, cropName, variety, season, stage, sowingDate, expectedHarvest));
	}

	private List<Farm> farmsFor(AppUser user, String farmId) {
		if (farmId != null && !farmId.isBlank()) {
			Farm farm = visibleFarm(user, parseId(farmId, "farmId"));
			return List.of(farm);
		}
		if (user.getRole() == UserRole.ADMIN) {
			return farmRepository.findAll();
		}
		return farmRepository.findAllByOwner_Id(user.getId());
	}

	private List<Plot> plotsFor(AppUser user, List<Long> visibleFarmIds, String farmId) {
		List<Long> farmIds = filterFarmIds(user, visibleFarmIds, farmId);
		return farmIds.isEmpty() ? List.of() : plotRepository.findAllByFarm_IdIn(farmIds);
	}

	private List<Crop> cropsFor(AppUser user, List<Long> visibleFarmIds, String farmId) {
		List<Long> farmIds = filterFarmIds(user, visibleFarmIds, farmId);
		return farmIds.isEmpty() ? List.of() : cropRepository.findAllByFarm_IdIn(farmIds);
	}

	private List<Long> filterFarmIds(AppUser user, List<Long> visibleFarmIds, String farmId) {
		if (farmId != null && !farmId.isBlank()) {
			return List.of(visibleFarm(user, parseId(farmId, "farmId")).getId());
		}
		return visibleFarmIds;
	}

	private List<Long> visibleFarmIds(AppUser user) {
		if (user.getRole() == UserRole.ADMIN) {
			return farmRepository.findAll().stream().map(Farm::getId).toList();
		}
		return farmRepository.findAllByOwner_Id(user.getId()).stream().map(Farm::getId).toList();
	}

	private Farm manageableFarm(AppUser user, Long farmId) {
		Farm farm = visibleFarm(user, farmId);
		if (user.getRole() != UserRole.ADMIN && !farm.getOwner().getId().equals(user.getId())) {
			throw forbidden();
		}
		return farm;
	}

	private Farm visibleFarm(AppUser user, Long farmId) {
		if (user.getRole() == UserRole.ADMIN) {
			return farmRepository.findById(farmId)
					.orElseThrow(() -> new DataApiException(HttpStatus.NOT_FOUND, "Farm not found."));
		}
		return farmRepository.findByIdAndOwner_Id(farmId, user.getId())
				.orElseThrow(() -> forbidden());
	}

	private AppUser currentUser(Authentication authentication) {
		return userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new DataApiException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists."));
	}

	private Map<String, Object> farmRow(Farm farm) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", farm.getId());
		row.put("owner_id", farm.getOwner().getId());
		row.put("name", farm.getName());
		row.put("location", farm.getLocation());
		row.put("latitude", farm.getLatitude());
		row.put("longitude", farm.getLongitude());
		row.put("total_area_acres", farm.getTotalAreaAcres());
		row.put("is_demo", farm.isDemo());
		row.put("created_at", farm.getCreatedAt().toString());
		return row;
	}

	private Map<String, Object> plotRow(Plot plot) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", plot.getId());
		row.put("farm_id", plot.getFarm().getId());
		row.put("name", plot.getName());
		row.put("area_acres", plot.getAreaAcres());
		row.put("soil_ph", plot.getSoilPh());
		row.put("soil_type", plot.getSoilType());
		row.put("boundary_geojson", parseBoundary(plot.getBoundaryGeojson()));
		row.put("created_at", plot.getCreatedAt().toString());
		return row;
	}

	private Map<String, Object> cropRow(Crop crop) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", crop.getId());
		row.put("farm_id", crop.getFarm().getId());
		row.put("plot_id", crop.getPlot() == null ? null : crop.getPlot().getId());
		row.put("crop_name", crop.getCropName());
		row.put("variety", crop.getVariety());
		row.put("season", crop.getSeason());
		row.put("stage", crop.getStage());
		row.put("sowing_date", crop.getSowingDate());
		row.put("expected_harvest", crop.getExpectedHarvest());
		row.put("created_at", crop.getCreatedAt().toString());
		return row;
	}

	private String serializeBoundary(Object value) {
		if (value == null) {
			return null;
		}
		JsonNode geometry = objectMapper.valueToTree(value);
		if (!geometry.isObject() || !"Polygon".equals(geometry.path("type").asText())) {
			throw badRequest("Plot boundary must be a GeoJSON Polygon.");
		}
		JsonNode coordinates = geometry.path("coordinates");
		if (!coordinates.isArray() || coordinates.isEmpty()
				|| !coordinates.get(0).isArray() || coordinates.get(0).size() < 4) {
			throw badRequest("Plot boundary must contain a closed polygon ring.");
		}
		JsonNode ring = coordinates.get(0);
		for (JsonNode point : ring) {
			if (!point.isArray() || point.size() < 2
					|| !point.get(0).isNumber() || !point.get(1).isNumber()
					|| point.get(0).doubleValue() < -180 || point.get(0).doubleValue() > 180
					|| point.get(1).doubleValue() < -90 || point.get(1).doubleValue() > 90) {
				throw badRequest("Plot boundary coordinates must be valid [longitude, latitude] pairs.");
			}
		}
		JsonNode first = ring.get(0);
		JsonNode last = ring.get(ring.size() - 1);
		if (Double.compare(first.get(0).doubleValue(), last.get(0).doubleValue()) != 0
				|| Double.compare(first.get(1).doubleValue(), last.get(1).doubleValue()) != 0) {
			throw badRequest("Plot boundary polygon ring must be closed.");
		}
		return geometry.toString();
	}

	private JsonNode parseBoundary(String value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.readTree(value);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Stored plot boundary contains invalid JSON.", exception);
		}
	}

	private Comparator<Map<String, Object>> rowComparator(String orderBy) {
		if ("name".equals(orderBy)) {
			return Comparator.comparing(row -> Objects.toString(row.get("name"), ""));
		}
		if (orderBy == null || orderBy.isBlank() || "created_at".equals(orderBy)) {
			return Comparator.comparing(row -> Objects.toString(row.get("created_at"), ""));
		}
		throw badRequest("Unsupported orderBy field.");
	}

	private int checkedLimit(Integer limit) {
		if (limit == null) {
			return MAX_LIMIT;
		}
		if (limit < 1) {
			throw badRequest("limit must be a positive integer.");
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private String requiredText(Map<String, Object> body, String key, int maxLength) {
		String value = optionalText(body, key, maxLength);
		if (value == null) {
			throw badRequest(key + " is required.");
		}
		return value;
	}

	private String optionalText(Map<String, Object> body, String key, int maxLength) {
		Object raw = body.get(key);
		if (raw == null) {
			return null;
		}
		if (!(raw instanceof String value)) {
			throw badRequest(key + " must be text.");
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() > maxLength) {
			throw badRequest(key + " must not exceed " + maxLength + " characters.");
		}
		return trimmed;
	}

	private Double optionalDecimal(Map<String, Object> body, String key, double min, double max) {
		BigDecimal value = optionalBigDecimal(body, key, BigDecimal.valueOf(min), BigDecimal.valueOf(max));
		return value == null ? null : value.doubleValue();
	}

	private BigDecimal optionalBigDecimal(
			Map<String, Object> body,
			String key,
			BigDecimal min,
			BigDecimal max) {
		Object raw = body.get(key);
		if (raw == null || raw instanceof String text && text.isBlank()) {
			return null;
		}
		try {
			BigDecimal value = raw instanceof BigDecimal decimal ? decimal : new BigDecimal(raw.toString());
			if (value.compareTo(min) < 0 || max != null && value.compareTo(max) > 0) {
				throw badRequest(key + " is outside the allowed range.");
			}
			return value;
		} catch (NumberFormatException exception) {
			throw badRequest(key + " must be a valid number.");
		}
	}

	private Long requiredId(Map<String, Object> body, String key) {
		Long value = optionalId(body, key);
		if (value == null) {
			throw badRequest(key + " is required.");
		}
		return value;
	}

	private Long optionalId(Map<String, Object> body, String key) {
		Object raw = body.get(key);
		if (raw == null || raw instanceof String text && text.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(raw.toString());
		} catch (NumberFormatException exception) {
			throw badRequest(key + " must be a valid ID.");
		}
	}

	private Long parseId(String value, String key) {
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException exception) {
			throw badRequest(key + " must be a valid ID.");
		}
	}

	private LocalDate optionalDate(Map<String, Object> body, String key) {
		Object raw = body.get(key);
		if (raw == null || raw instanceof String text && text.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(raw.toString());
		} catch (DateTimeParseException exception) {
			throw badRequest(key + " must be an ISO date (YYYY-MM-DD).");
		}
	}

	private DataApiException badRequest(String message) {
		return new DataApiException(HttpStatus.BAD_REQUEST, message);
	}

	private DataApiException forbidden() {
		return new DataApiException(HttpStatus.FORBIDDEN, "You do not have access to this farm.");
	}
}
