package com.lost.link.lost.link_backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lost.link.lost.link_backend.model.Item;

@Service
public class GeminiImageMatchingService {

    private static final int MAX_CANDIDATES = 20;
    private static final long MAX_REQUEST_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final String PROMPT = "Compare these two photos of lost-and-found objects. "
            + "Return only JSON with exactly these fields: {\"score\": number, \"reason\": string}. "
            + "Score how likely they are the same physical item from 0 to 1. Consider object type, color, shape, "
            + "brand, markings, and distinctive details. Do not infer identity from the surrounding location. "
            + "Use a conservative score when the photos are unclear.";

    private final GridFsTemplate gridFsTemplate;
    private final ImageFeatureService imageFeatureService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public GeminiImageMatchingService(GridFsTemplate gridFsTemplate, ImageFeatureService imageFeatureService,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:${GEMINI_API_KEY:${GOOGLE_API_KEY:}}}") String apiKey,
            @Value("${gemini.model:${GEMINI_MODEL:gemini-1.5-flash}}") String model) {
        this.gridFsTemplate = gridFsTemplate;
        this.imageFeatureService = imageFeatureService;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-1.5-flash";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<Map<String, Object>> findMatches(MultipartFile queryImage, List<Item> candidates,
            double minSimilarity) {
        validateQueryImage(queryImage);
        if (Double.isNaN(minSimilarity) || minSimilarity < 0 || minSimilarity > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minSimilarity must be between 0 and 1");
        }
        List<Item> imageCandidates = candidates.stream()
            .filter(item -> item.getImageFileId() != null && ObjectId.isValid(item.getImageFileId()))
                .limit(MAX_CANDIDATES)
                .toList();
        if (imageCandidates.isEmpty()) {
            return List.of();
        }
        try {
            byte[] queryBytes = queryImage.getBytes();
            if (!isConfigured()) {
                List<Double> queryFeatures = imageFeatureService.extract(queryImage);
                return imageCandidates.stream()
                        .map(item -> buildLocalResult(queryFeatures, item, minSimilarity))
                        .filter(result -> result != null)
                        .sorted(Comparator.comparing(result -> (Double) result.get("similarity"), Comparator.reverseOrder()))
                        .toList();
            }
            return imageCandidates.stream()
                .map(item -> compareAndBuildResult(queryBytes, queryImage.getContentType(), item, minSimilarity))
                .filter(result -> result != null)
                .sorted(Comparator.comparing(result -> (Double) result.get("similarity"), Comparator.reverseOrder()))
                .toList();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read the uploaded image", exception);
        }
    }

    public boolean isConfigured() {
        return !apiKey.isBlank()
                && !apiKey.equalsIgnoreCase("replace-with-your-gemini-api-key")
                && !apiKey.toLowerCase().contains("your-gemini-api-key")
                && !apiKey.toLowerCase().contains("your-api-key");
    }

    private Map<String, Object> buildLocalResult(List<Double> queryFeatures, Item candidate, double minSimilarity) {
        double score = imageFeatureService.similarity(queryFeatures, candidate.getImageFeatures());
        if (score < minSimilarity) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", candidate.toResponseMap());
        result.put("similarity", score);
        result.put("similarityPercentage", Math.round(score * 10000) / 100.0);
        result.put("result", "MATCH");
        result.put("reason", "Matched using local visual image similarity.");
        return result;
    }

    private Map<String, Object> compareAndBuildResult(byte[] queryBytes, String queryContentType, Item candidate,
            double minSimilarity) {
        try {
            GridFsResource resource = getResource(candidate.getImageFileId());
            if (resource == null) {
                return null;
            }
            byte[] candidateBytes;
            try (var inputStream = resource.getInputStream()) {
                candidateBytes = inputStream.readAllBytes();
            }
            if (candidateBytes.length > MAX_REQUEST_IMAGE_SIZE) {
                return null;
            }
            GeminiScore score = compare(queryBytes, queryContentType, candidateBytes,
                    candidate.getImageContentType());
            if (score.score() < minSimilarity) {
                return null;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("item", candidate.toResponseMap());
            result.put("similarity", score.score());
            result.put("similarityPercentage", Math.round(score.score() * 10000) / 100.0);
            result.put("result", "MATCH");
            result.put("reason", score.reason());
            return result;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to read an image needed for AI matching", exception);
        }
    }

    private GeminiScore compare(byte[] first, String firstContentType, byte[] second, String secondContentType) {
        try {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("text", PROMPT));
                parts.add(Map.of("inlineData", Map.of("mimeType", safeContentType(firstContentType),
                    "data", Base64.getEncoder().encodeToString(first))));
                parts.add(Map.of("inlineData", Map.of("mimeType", safeContentType(secondContentType),
                    "data", Base64.getEncoder().encodeToString(second))));

            String body = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", parts)),
                    "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.0)));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model
                        + ":generateContent"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini image matching request failed (HTTP " + response.statusCode() + "): "
                        + geminiErrorMessage(response.body()));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = findResponseText(root);
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini returned no comparison result");
            }
            JsonNode scoreNode = objectMapper.readTree(normalizeJson(text));
            double score = scoreNode.path("score").asDouble(Double.NaN);
            if (!Double.isFinite(score) || score < 0 || score > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Gemini returned an invalid image match score");
            }
            return new GeminiScore(score, scoreNode.path("reason").asText("Visual comparison completed."));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini image matching was interrupted",
                    exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini image matching is unavailable",
                    exception);
        }
    }

    private GridFsResource getResource(String fileId) {
        var file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        return file == null ? null : gridFsTemplate.getResource(file);
    }

    private void validateQueryImage(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getSize() > MAX_REQUEST_IMAGE_SIZE
                || image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A readable image file of 10 MB or smaller is required for matching");
        }
    }

    private String safeContentType(String contentType) {
        return contentType == null || !contentType.startsWith("image/") ? "image/jpeg" : contentType;
    }

    private String findResponseText(JsonNode root) {
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }
        for (JsonNode part : parts) {
            String text = part.path("text").asText("").trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String geminiErrorMessage(String responseBody) {
        try {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            String message = error.path("message").asText("").trim();
            return message.isBlank() ? "The provider returned an unknown error" : message;
        } catch (IOException exception) {
            return "The provider returned an invalid error response";
        }
    }

    private String normalizeJson(String text) {
        if (text.startsWith("```") && text.endsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            return firstLineEnd >= 0 ? text.substring(firstLineEnd + 1, text.length() - 3).trim() : text;
        }
        return text;
    }

    private record GeminiScore(double score, String reason) {
    }
}