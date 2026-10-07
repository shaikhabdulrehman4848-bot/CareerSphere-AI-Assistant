package com.careersphere.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiAiProvider implements AiProvider {
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public GeminiAiProvider(
        @Value("${ai.gemini.api-key:}") String apiKey,
        @Value("${ai.gemini.model:gemini-2.5-flash}") String model,
        @Value("${ai.gemini.endpoint}") String endpoint,
        ObjectMapper mapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.mapper = mapper;
    }

    @Override
    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public String generateJson(String prompt) {
        if (!isConfigured()) throw new AiProviderException("AI provider is not configured.");
        try {
            String jsonBody = mapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.35)
            ));
            URI uri = URI.create(endpoint + "/" + model + ":generateContent?key=" + apiKey);
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(35))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 || response.statusCode() >= 500) {
                throw new AiProviderException("The AI provider is temporarily busy.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("The AI provider rejected the request.");
            }
            JsonNode root = mapper.readTree(response.body());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            if (text.isBlank()) throw new AiProviderException("The AI provider returned an empty response.");
            return unwrapJson(text);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException("The AI provider could not be reached.");
        }
    }

    private String unwrapJson(String text) {
        String result = text.trim();
        if (result.startsWith("```")) {
            result = result.replaceFirst("^```(?:json)?\\s*", "");
            result = result.replaceFirst("\\s*```$", "");
        }
        int start = result.indexOf('{');
        int end = result.lastIndexOf('}');
        if (start < 0 || end < start) throw new AiProviderException("The AI response was not valid JSON.");
        return result.substring(start, end + 1);
    }
}
