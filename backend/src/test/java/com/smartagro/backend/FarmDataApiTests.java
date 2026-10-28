package com.smartagro.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.smartagro.backend.farm.CropRepository;
import com.smartagro.backend.farm.FarmRepository;
import com.smartagro.backend.farm.PlotRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FarmDataApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CropRepository cropRepository;

	@Autowired
	private PlotRepository plotRepository;

	@Autowired
	private FarmRepository farmRepository;

	@BeforeEach
	void clearFarmData() {
		cropRepository.deleteAll();
		plotRepository.deleteAll();
		farmRepository.deleteAll();
	}

	@Test
	void createsAndReadsFarmPlotBoundaryAndCropForTheAuthenticatedOwner() throws Exception {
		String token = registerAndGetToken("gis-owner");

		MvcResult farmResponse = mockMvc.perform(post("/api/data/farms")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"North Farm","location":"Nashik","latitude":19.997,"longitude":73.789,"total_area_acres":12.5}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.owner_id").isNumber())
				.andExpect(jsonPath("$.name").value("North Farm"))
				.andReturn();
		long farmId = objectMapper.readTree(farmResponse.getResponse().getContentAsString())
				.get("id").asLong();

		MvcResult plotResponse = mockMvc.perform(post("/api/data/plots")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "farm_id": %d,
								  "name": "North Field",
								  "area_acres": 2.4,
								  "soil_ph": 6.5,
								  "soil_type": "Red Sandy Soil",
								  "boundary_geojson": {
								    "type": "Polygon",
								    "coordinates": [[[79.0, 16.0], [79.01, 16.0], [79.01, 16.01], [79.0, 16.01], [79.0, 16.0]]]
								  }
								}
								""".formatted(farmId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.boundary_geojson.type").value("Polygon"))
				.andExpect(jsonPath("$.soil_type").value("Red Sandy Soil"))
				.andReturn();
		long plotId = objectMapper.readTree(plotResponse.getResponse().getContentAsString())
				.get("id").asLong();

		mockMvc.perform(post("/api/data/crops")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"farm_id":%d,"plot_id":%d,"crop_name":"Rice","stage":"Planned"}
								""".formatted(farmId, plotId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.crop_name").value("Rice"))
				.andExpect(jsonPath("$.plot_id").value(plotId));

		mockMvc.perform(get("/api/data/plots")
						.header("Authorization", "Bearer " + token)
						.param("farmId", Long.toString(farmId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].farm_id").value(farmId))
				.andExpect(jsonPath("$[0].boundary_geojson.coordinates[0][0][0]").value(79.0));

		mockMvc.perform(get("/api/data/crops")
						.header("Authorization", "Bearer " + token)
						.param("farmId", Long.toString(farmId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].crop_name").value("Rice"));
	}

	@Test
	void rejectsMalformedBoundariesAndCrossOwnerFarmReads() throws Exception {
		String ownerToken = registerAndGetToken("gis-owner");
		String otherToken = registerAndGetToken("other-owner");
		MvcResult farmResponse = mockMvc.perform(post("/api/data/farms")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Private Farm"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		long farmId = objectMapper.readTree(farmResponse.getResponse().getContentAsString())
				.get("id").asLong();

		mockMvc.perform(post("/api/data/plots")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"farm_id":%d,"name":"Open Ring","boundary_geojson":{"type":"Polygon","coordinates":[[[79,16],[79.1,16],[79.1,16.1],[79,16.1]]]}}
								""".formatted(farmId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Plot boundary polygon ring must be closed."));

		mockMvc.perform(get("/api/data/plots")
						.header("Authorization", "Bearer " + otherToken)
						.param("farmId", Long.toString(farmId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have access to this farm."));
	}

	@Test
	void farmDataRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/data/farms"))
				.andExpect(status().isUnauthorized());
	}

	private String registerAndGetToken(String prefix) throws Exception {
		String email = prefix + "-" + UUID.randomUUID() + "@example.com";
		MvcResult result = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "GIS Farmer",
								"email", email,
								"password", "FarmPass123!"))))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		return response.get("accessToken").asText();
	}
}
