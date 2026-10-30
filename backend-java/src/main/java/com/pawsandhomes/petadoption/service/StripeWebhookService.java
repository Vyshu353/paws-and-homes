package com.pawsandhomes.petadoption.service;

import com.pawsandhomes.petadoption.entity.AdoptionApplication;
import com.pawsandhomes.petadoption.repository.AdoptionApplicationRepository;
import com.pawsandhomes.petadoption.repository.PetRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class StripeWebhookService {

    private static final long SIGNATURE_TOLERANCE_SECONDS = 300;

    private final String webhookSecret;
    private final ObjectMapper objectMapper;
    private final AdoptionApplicationRepository adoptionRepository;
    private final PetRepository petRepository;

    public StripeWebhookService(
            @Value("${stripe.webhook-secret:}") String webhookSecret,
            ObjectMapper objectMapper,
            AdoptionApplicationRepository adoptionRepository,
            PetRepository petRepository) {
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
        this.adoptionRepository = adoptionRepository;
        this.petRepository = petRepository;
    }

    @Transactional
    public void handle(byte[] body, String signatureHeader) {
        if (webhookSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook verification is not configured.");
        }
        verifySignature(body, signatureHeader);

        try {
            JsonNode event = objectMapper.readTree(body);
            if (!"checkout.session.completed".equals(textValue(event.path("type")))) {
                return;
            }
            JsonNode session = event.path("data").path("object");
            if (!"paid".equals(textValue(session.path("payment_status")))) {
                return;
            }

            long adoptionId = Long.parseLong(textValue(session.path("metadata").path("adoptionId")));
            AdoptionApplication adoption = adoptionRepository.findById(adoptionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adoption application not found."));
            if (!textValue(session.path("id")).equals(adoption.getStripeSessionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe session does not match the adoption application.");
            }
            long expectedAmount = adoption.getPet().getAdoptionFee().movePointRight(2).longValueExact();
            if (!"inr".equalsIgnoreCase(textValue(session.path("currency")))
                    || session.path("amount_total").asLong(-1) != expectedAmount) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe payment amount does not match the adoption contribution.");
            }
            if (!"PAID".equals(adoption.getStatus())) {
                adoption.setStatus("PAID");
                adoptionRepository.save(adoption);
                adoption.getPet().setStatus("ADOPTED");
                petRepository.save(adoption.getPet());
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook event.", exception);
        }
    }

    private static String textValue(JsonNode node) {
        return node.isString() ? node.stringValue() : "";
    }

    private void verifySignature(byte[] body, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe signature header is required.");
        }
        String timestamp = null;
        String suppliedSignature = null;
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if ("t".equals(pair[0])) {
                timestamp = pair[1];
            } else if ("v1".equals(pair[0]) && suppliedSignature == null) {
                suppliedSignature = pair[1];
            }
        }
        if (timestamp == null || suppliedSignature == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe signature header is invalid.");
        }

        try {
            long timestampSeconds = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - timestampSeconds) > SIGNATURE_TOLERANCE_SECONDS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe signature timestamp has expired.");
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + new String(body, StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(suppliedSignature);
            if (!MessageDigest.isEqual(expected, supplied)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe signature verification failed.");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe signature verification failed.", exception);
        }
    }
}
