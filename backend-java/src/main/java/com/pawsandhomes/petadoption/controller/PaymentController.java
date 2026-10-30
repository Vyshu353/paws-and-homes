package com.pawsandhomes.petadoption.controller;

import com.pawsandhomes.petadoption.entity.AdoptionApplication;
import com.pawsandhomes.petadoption.entity.User;
import com.pawsandhomes.petadoption.repository.AdoptionApplicationRepository;
import com.pawsandhomes.petadoption.repository.UserRepository;
import com.pawsandhomes.petadoption.service.RazorpayPaymentService;
import com.pawsandhomes.petadoption.service.RazorpayPaymentService.OrderDetails;
import com.pawsandhomes.petadoption.service.RazorpayPaymentService.PaymentResult;
import com.pawsandhomes.petadoption.service.RazorpayPaymentService.VerifyPaymentRequest;
import com.pawsandhomes.petadoption.service.StripeCheckoutService;
import com.pawsandhomes.petadoption.service.StripeCheckoutService.CheckoutSession;
import com.pawsandhomes.petadoption.service.StripeWebhookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final AdoptionApplicationRepository adoptionRepository;
    private final UserRepository userRepository;
    private final StripeCheckoutService checkoutService;
    private final StripeWebhookService webhookService;
    private final RazorpayPaymentService razorpayPaymentService;

    public PaymentController(
            AdoptionApplicationRepository adoptionRepository,
            UserRepository userRepository,
            StripeCheckoutService checkoutService,
            StripeWebhookService webhookService,
            RazorpayPaymentService razorpayPaymentService) {
        this.adoptionRepository = adoptionRepository;
        this.userRepository = userRepository;
        this.checkoutService = checkoutService;
        this.webhookService = webhookService;
        this.razorpayPaymentService = razorpayPaymentService;
    }

    @PostMapping("/razorpay/orders")
    public OrderDetails createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderRequest request,
            Authentication authentication) {
        return razorpayPaymentService.createOrder(request.adoptionId(), currentUser(authentication));
    }

    @PostMapping("/razorpay/verify")
    public PaymentResult verifyRazorpayPayment(
            @Valid @RequestBody RazorpayPaymentVerificationRequest request,
            Authentication authentication) {
        return razorpayPaymentService.verify(
                new VerifyPaymentRequest(request.orderId(), request.paymentId(), request.signature()),
                currentUser(authentication));
    }

    @PostMapping("/razorpay/cancel")
    public PaymentResult cancelRazorpayPayment(
            @Valid @RequestBody RazorpayOrderStatusRequest request,
            Authentication authentication) {
        return razorpayPaymentService.cancel(request.orderId(), currentUser(authentication));
    }

    @PostMapping("/razorpay/failure")
    public PaymentResult recordRazorpayFailure(
            @Valid @RequestBody RazorpayFailureRequest request,
            Authentication authentication) {
        return razorpayPaymentService.markFailed(
                request.orderId(), request.paymentId(), currentUser(authentication));
    }

    @PostMapping("/checkout")
    public Map<String, String> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is no longer valid."));
        AdoptionApplication adoption = adoptionRepository.findByIdAndUser_Id(request.adoptionId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adoption application not found."));
        if (!"PENDING".equals(adoption.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This adoption application has already been processed.");
        }
        if (!"AVAILABLE".equals(adoption.getPet().getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This pet is no longer available for adoption.");
        }

        CheckoutSession session = checkoutService.createSession(adoption);
        adoption.setStripeSessionId(session.id());
        adoption.setStripeSessionUrl(session.url());
        adoptionRepository.save(adoption);
        return Map.of("url", session.url());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody byte[] body,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        webhookService.handle(body, signature);
        return ResponseEntity.ok().build();
    }

    public record CheckoutRequest(@NotNull @Positive Long adoptionId) {
    }

    public record RazorpayOrderRequest(@NotNull @Positive Long adoptionId) {
    }

    public record RazorpayPaymentVerificationRequest(
            @NotBlank @Size(max = 100) String orderId,
            @NotBlank @Size(max = 100) String paymentId,
            @NotBlank @Size(max = 128) String signature) {
    }

    public record RazorpayOrderStatusRequest(@NotBlank @Size(max = 100) String orderId) {
    }

    public record RazorpayFailureRequest(
            @NotBlank @Size(max = 100) String orderId,
            @NotBlank @Size(max = 100) String paymentId) {
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User session is no longer valid."));
    }
}
