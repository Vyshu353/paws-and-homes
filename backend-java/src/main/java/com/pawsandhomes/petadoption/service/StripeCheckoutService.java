package com.pawsandhomes.petadoption.service;

import com.pawsandhomes.petadoption.entity.AdoptionApplication;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class StripeCheckoutService {

    private static final URI CHECKOUT_SESSIONS_URI = URI.create("https://api.stripe.com/v1/checkout/sessions");

    private final String secretKey;
    private final String frontendUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public StripeCheckoutService(
            @Value("${stripe.secret-key:}") String secretKey,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CheckoutSession createSession(AdoptionApplication adoption) {
        if (secretKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Payments are not configured. Set STRIPE_SECRET_KEY on the backend.");
        }

        BigDecimal fee = adoption.getPet().getAdoptionFee();
        if (fee == null || fee.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This pet does not have a valid adoption contribution.");
        }

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("mode", "payment");
        fields.put("customer_email", adoption.getUser().getEmail());
        fields.put("success_url", frontendUrl + "/dashboard?payment=success");
        fields.put("cancel_url", frontendUrl + "/dashboard?payment=cancelled");
        fields.put("line_items[0][quantity]", "1");
        fields.put("line_items[0][price_data][currency]", "inr");
        fields.put("line_items[0][price_data][unit_amount]", fee.movePointRight(2).longValueExact() + "");
        fields.put("line_items[0][price_data][product_data][name]", "Adoption contribution - " + adoption.getPet().getName());
        fields.put("metadata[adoptionId]", adoption.getId().toString());
        fields.put("metadata[userId]", adoption.getUser().getId().toString());

        HttpRequest request = HttpRequest.newBuilder(CHECKOUT_SESSIONS_URI)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encode(fields)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode payload = objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String stripeMessage = textValue(payload.path("error").path("message"));
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        stripeMessage.isBlank() ? "Stripe could not create a checkout session." : stripeMessage);
            }
            String id = textValue(payload.path("id"));
            String url = textValue(payload.path("url"));
            if (id.isBlank() || url.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe returned an incomplete checkout session.");
            }
            return new CheckoutSession(id, url);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read Stripe's checkout response.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe checkout request was interrupted.", exception);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The adoption contribution is not a valid INR amount.", exception);
        }
    }

    private static String encode(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String textValue(JsonNode node) {
        return node.isString() ? node.stringValue() : "";
    }

    public record CheckoutSession(String id, String url) {
    }
}
