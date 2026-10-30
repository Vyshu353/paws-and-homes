package com.pawsandhomes.petadoption;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pawsandhomes.petadoption.service.RazorpaySignatureVerifier;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class RazorpaySignatureVerifierTests {

    @Test
    void acceptsValidSignatureAndRejectsTamperedValues() throws Exception {
        String orderId = "order_test123";
        String paymentId = "pay_test456";
        String secret = "test_secret_key";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(
                mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));

        assertTrue(RazorpaySignatureVerifier.isValid(orderId, paymentId, signature, secret));
        assertFalse(RazorpaySignatureVerifier.isValid(orderId, "pay_tampered", signature, secret));
        assertFalse(RazorpaySignatureVerifier.isValid(orderId, paymentId, "not-a-signature", secret));
        assertFalse(RazorpaySignatureVerifier.isValid(orderId, paymentId, signature, ""));
    }
}
