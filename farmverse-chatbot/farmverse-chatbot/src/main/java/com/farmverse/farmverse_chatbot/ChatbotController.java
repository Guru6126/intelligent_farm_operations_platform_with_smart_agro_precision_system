package com.farmverse.farmverse_chatbot;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatbotController {

    private final AgricultureDataService dataService;
    private final OllamaService ollamaService;
    private final FarmerRecordsService farmerRecords;

    public ChatbotController(AgricultureDataService dataService,
                             OllamaService ollamaService,
                             FarmerRecordsService farmerRecords) {
        this.dataService = dataService;
        this.ollamaService = ollamaService;
        this.farmerRecords = farmerRecords;
    }

    @GetMapping("/api/chatbot")
    public String chatbot() {
        return "Welcome to FarmVerse Nexus Farmer Chatbot!";
    }

    @GetMapping("/api/chatbot/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "records", dataService.recordCount(),
                "farmerDb", farmerRecords.isReady(),
                "semanticSearch", dataService.isSemanticReady(),
                "ollama", ollamaService.isAvailable(),
                "multilingualModel", ollamaService.multilingualModelName(),
                "multilingualReady", ollamaService.isMultilingualModelInstalled()
        );
    }
}
