
package com.farmverse.farmverse_chatbot;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class EmbeddingTestController {

    private final OllamaService ollamaService;

    public EmbeddingTestController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @GetMapping("/embedding-test")
    public Map<String, Object> testEmbedding() {

        List<Double> vector = ollamaService.createEmbedding(
                "Soil fertility improves healthy plant growth"
        );

        return Map.of(
                "message", "Embedding generated successfully",
                "vector_size", vector.size(),
                "sample_values", vector.subList(0, Math.min(5, vector.size()))
        );
    }
}
