package com.pawsandhomes.petadoption.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class RazorpayApiClient {

    private static final String API_BASE = "https://api.razorpay.com/v1";

    private final String keyId;
    private final String keySecret;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RazorpayApiClient(
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret,
            ObjectMapper objectMapper) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String keyId() {
        ensureConfigured();
        return keyId;
    }

    public JsonNode createOrder(long amountPaise, String receipt, Long adoptionId, Long userId, Long petId) {
        String body = """
                {"amount":%d,"currency":"INR","receipt":"%s","notes":{"adoptionId":"%d","userId":"%d","petId":"%d"}}
                """.formatted(amountPaise, receipt, adoptionId, userId, petId);
        return send("POST", "/orders", body);
    }

    public JsonNode getOrder(String orderId) {
        return send("GET", "/orders/" + safeId(orderId), null);
    }

    public JsonNode getPayment(String paymentId) {
        return send("GET", "/payments/" + safeId(paymentId), null);
    }

    public JsonNode capturePayment(String paymentId, long amountPaise) {
        return send(
                "POST",
                "/payments/" + safeId(paymentId) + "/capture",
                "{\"amount\":%d,\"currency\":\"INR\"}".formatted(amountPaise));
    }

    private JsonNode send(String method, String path, String body) {
        ensureConfigured();
        String credentials = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(API_BASE + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + credentials)
                .header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Razorpay could not complete the payment request.");
            }
            return objectMapper.readTree(response.body());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Could not read Razorpay's payment response.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "The Razorpay request was interrupted.", exception);
        }
    }

    private void ensureConfigured() {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET on the backend.");
        }
        if (!keyId.startsWith("rzp_test_")) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "This integration is restricted to Razorpay Test Mode keys.");
        }
    }

    private static String safeId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_]{1,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay identifier is invalid.");
        }
        return id;
    }
}
