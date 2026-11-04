package com.farmverse.farmverse_chatbot;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaService {

    private final RestClient restClient;
    private final String chatModel;
    private final String embedModel;
    private final String multilingualModel;

    public OllamaService(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.chat-model:qwen2.5:1.5b}") String chatModel,
            @Value("${ollama.embed-model:nomic-embed-text}") String embedModel,
            @Value("${ollama.multilingual-model:gemma3:4b}") String multilingualModel,
            @Value("${ollama.timeout-seconds:120}") int timeoutSeconds) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        this.chatModel = chatModel;
        this.embedModel = embedModel;
        this.multilingualModel = multilingualModel;
    }

    public String multilingualModelName() {
        return multilingualModel;
    }

    private volatile long lastFallbackWarning = 0;
    private volatile long modelCheckAt = 0;
    private volatile boolean modelInstalled = false;

    public boolean isMultilingualModelInstalled() {
        long now = System.currentTimeMillis();
        if (now - modelCheckAt < 20_000) {
            return modelInstalled;
        }
        boolean found = false;
        try {
            Map<?, ?> tags = restClient.get().uri("/api/tags").retrieve().body(Map.class);
            if (tags != null && tags.get("models") instanceof List<?> models) {
                for (Object m : models) {
                    if (m instanceof Map<?, ?> model
                            && model.get("name") != null
                            && LanguageSupport.modelMatches(model.get("name").toString(), multilingualModel)) {
                        found = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            found = false;
        }
        modelInstalled = found;
        modelCheckAt = now;
        return found;
    }

    public boolean isAvailable() {
        try {
            restClient.get().uri("/api/tags").retrieve().body(String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String ask(String prompt) {
        return generate(chatModel, prompt, null);
    }

    public String askMultilingual(String prompt) {
        try {
            return generate(multilingualModel, prompt, 0.2);
        } catch (RuntimeException e) {
            if (multilingualModel.equals(chatModel)) {
                throw e;
            }
            long now = System.currentTimeMillis();
            if (now - lastFallbackWarning > 60_000) {
                lastFallbackWarning = now;
                System.err.println("Model '" + multilingualModel + "' failed (" + e.getMessage()
                        + "). Run: ollama pull " + multilingualModel
                        + " . Using '" + chatModel + "' instead.");
            }
            return generate(chatModel, prompt, 0.2);
        }
    }

    private String generate(String model, String prompt, Double temperature) {

        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", false);
        if (temperature != null) {
            request.put("options", Map.of("temperature", temperature));
        }

        Map<?, ?> response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("response") == null) {
            throw new IllegalStateException("AI response was not available.");
        }

        return response.get("response").toString();
    }

    public List<Double> createEmbedding(String text) {
        double[] vector = embedBatch(List.of(text)).get(0);
        List<Double> result = new ArrayList<>(vector.length);
        for (double v : vector) {
            result.add(v);
        }
        return result;
    }

    public List<double[]> embedBatch(List<String> texts) {

        Map<String, Object> request = Map.of(
                "model", embedModel,
                "input", texts
        );

        Map<?, ?> response = restClient.post()
                .uri("/api/embed")
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("embeddings") instanceof List<?> embeddings)
                || embeddings.size() != texts.size()) {
            throw new IllegalStateException(
                    "Embedding response was not available.");
        }

        List<double[]> result = new ArrayList<>(embeddings.size());
        for (Object item : embeddings) {
            List<?> raw = (List<?>) item;
            double[] vector = new double[raw.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = ((Number) raw.get(i)).doubleValue();
            }
            result.add(vector);
        }
        return result;
    }
}
