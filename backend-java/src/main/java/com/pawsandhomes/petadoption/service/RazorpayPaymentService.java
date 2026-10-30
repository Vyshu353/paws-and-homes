package com.pawsandhomes.petadoption.service;

import com.pawsandhomes.petadoption.entity.AdoptionApplication;
import com.pawsandhomes.petadoption.entity.PaymentStatus;
import com.pawsandhomes.petadoption.entity.Pet;
import com.pawsandhomes.petadoption.entity.RazorpayPayment;
import com.pawsandhomes.petadoption.entity.User;
import com.pawsandhomes.petadoption.repository.AdoptionApplicationRepository;
import com.pawsandhomes.petadoption.repository.PetRepository;
import com.pawsandhomes.petadoption.repository.RazorpayPaymentRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class RazorpayPaymentService {

    private final AdoptionApplicationRepository adoptionRepository;
    private final RazorpayPaymentRepository paymentRepository;
    private final PetRepository petRepository;
    private final RazorpayApiClient razorpay;
    private final String keySecret;

    public RazorpayPaymentService(
            AdoptionApplicationRepository adoptionRepository,
            RazorpayPaymentRepository paymentRepository,
            PetRepository petRepository,
            RazorpayApiClient razorpay,
            @Value("${razorpay.key-secret:}") String keySecret) {
        this.adoptionRepository = adoptionRepository;
        this.paymentRepository = paymentRepository;
        this.petRepository = petRepository;
        this.razorpay = razorpay;
        this.keySecret = keySecret;
    }

    @Transactional
    public OrderDetails createOrder(Long adoptionId, User user) {
        AdoptionApplication adoption = adoptionRepository.findByIdAndUser_Id(adoptionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adoption application not found."));
        ensureAdoptionPayable(adoption);

        RazorpayPayment existing = paymentRepository
                .findFirstByAdoption_IdAndStatusOrderByCreatedAtDesc(adoptionId, PaymentStatus.CREATED)
                .orElse(null);
        if (existing != null) {
            return orderDetails(existing);
        }

        BigDecimal amount = adoption.getPet().getAdoptionFee();
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This pet does not have a valid adoption contribution.");
        }

        long amountPaise;
        try {
            amountPaise = amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The adoption contribution is not a valid INR amount.");
        }

        String receipt = "a" + adoptionId + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        JsonNode response = razorpay.createOrder(
                amountPaise, receipt, adoption.getId(), user.getId(), adoption.getPet().getId());
        String orderId = text(response.path("id"));
        if (orderId.isBlank()
                || response.path("amount").asLong(-1) != amountPaise
                || !"INR".equals(text(response.path("currency")))
                || !"created".equals(text(response.path("status")))
                || !receipt.equals(text(response.path("receipt")))) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay returned an invalid order.");
        }

        RazorpayPayment payment = new RazorpayPayment();
        payment.setAdoption(adoption);
        payment.setUser(user);
        payment.setPet(adoption.getPet());
        payment.setRazorpayOrderId(orderId);
        payment.setAmount(amount);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        return orderDetails(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResult verify(VerifyPaymentRequest request, User user) {
        if (!RazorpaySignatureVerifier.isValid(
                request.orderId(), request.paymentId(), request.signature(), keySecret)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay payment signature verification failed.");
        }
        RazorpayPayment payment = findPayment(request.orderId(), user);
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (!request.paymentId().equals(payment.getRazorpayPaymentId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This order has already been paid.");
            }
            return result(payment);
        }
        ensureAdoptionPayable(payment.getAdoption());

        long expectedPaise = toPaise(payment.getAmount());
        JsonNode order = razorpay.getOrder(payment.getRazorpayOrderId());
        validateOrder(order, payment, expectedPaise);
        JsonNode razorpayPayment = razorpay.getPayment(request.paymentId());
        validatePayment(razorpayPayment, payment, expectedPaise, request.paymentId());

        String paymentStatus = text(razorpayPayment.path("status"));
        if ("authorized".equals(paymentStatus)) {
            razorpayPayment = razorpay.capturePayment(request.paymentId(), expectedPaise);
            validatePayment(razorpayPayment, payment, expectedPaise, request.paymentId());
        }
        if (!"captured".equals(text(razorpayPayment.path("status")))
                || !razorpayPayment.path("captured").asBoolean(false)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay has not captured this payment.");
        }

        JsonNode settledOrder = razorpay.getOrder(payment.getRazorpayOrderId());
        validateOrder(settledOrder, payment, expectedPaise);
        if (!"paid".equals(text(settledOrder.path("status")))
                || settledOrder.path("amount_paid").asLong(-1) != expectedPaise) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay has not confirmed payment for this order.");
        }

        payment.setRazorpayPaymentId(request.paymentId());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.getAdoption().setStatus("PAID");
        Pet pet = payment.getPet();
        pet.setStatus("ADOPTED");
        paymentRepository.save(payment);
        adoptionRepository.save(payment.getAdoption());
        petRepository.save(pet);
        return result(payment);
    }

    @Transactional
    public PaymentResult cancel(String orderId, User user) {
        RazorpayPayment payment = findPayment(orderId, user);
        if (payment.getStatus() == PaymentStatus.CREATED) {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        }
        return result(payment);
    }

    @Transactional
    public PaymentResult markFailed(String orderId, String paymentId, User user) {
        RazorpayPayment payment = findPayment(orderId, user);
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return result(payment);
        }

        JsonNode remotePayment = razorpay.getPayment(paymentId);
        validatePayment(remotePayment, payment, toPaise(payment.getAmount()), paymentId);
        if (!"failed".equals(text(remotePayment.path("status")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay has not confirmed a failed payment.");
        }

        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus(PaymentStatus.FAILED);
        return result(paymentRepository.save(payment));
    }

    private RazorpayPayment findPayment(String orderId, User user) {
        return paymentRepository.findOwnedByOrderIdForUpdate(orderId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found."));
    }

    private static void ensureAdoptionPayable(AdoptionApplication adoption) {
        if (!"PENDING".equals(adoption.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This adoption application has already been processed.");
        }
        if (!"AVAILABLE".equals(adoption.getPet().getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This pet is no longer available for adoption.");
        }
    }

    private OrderDetails orderDetails(RazorpayPayment payment) {
        return new OrderDetails(
                razorpay.keyId(),
                payment.getRazorpayOrderId(),
                toPaise(payment.getAmount()),
                payment.getCurrency(),
                "Paws & Homes",
                "Adoption contribution - " + payment.getPet().getName(),
                payment.getUser().getEmail());
    }

    private static void validateOrder(JsonNode order, RazorpayPayment payment, long expectedPaise) {
        if (!payment.getRazorpayOrderId().equals(text(order.path("id")))
                || order.path("amount").asLong(-1) != expectedPaise
                || !"INR".equals(text(order.path("currency")))
                || !payment.getAdoption().getId().toString().equals(text(order.path("notes").path("adoptionId")))
                || !payment.getUser().getId().toString().equals(text(order.path("notes").path("userId")))
                || !payment.getPet().getId().toString().equals(text(order.path("notes").path("petId")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order details do not match this adoption.");
        }
    }

    private static void validatePayment(
            JsonNode remotePayment,
            RazorpayPayment payment,
            long expectedPaise,
            String expectedPaymentId) {
        if (!expectedPaymentId.equals(text(remotePayment.path("id")))
                || !payment.getRazorpayOrderId().equals(text(remotePayment.path("order_id")))
                || remotePayment.path("amount").asLong(-1) != expectedPaise
                || !"INR".equals(text(remotePayment.path("currency")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay payment details do not match this adoption.");
        }
    }

    private static long toPaise(BigDecimal amount) {
        try {
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The adoption contribution is not a valid INR amount.");
        }
    }

    private static PaymentResult result(RazorpayPayment payment) {
        return new PaymentResult(
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus());
    }

    private static String text(JsonNode node) {
        return node.isString() ? node.stringValue() : "";
    }

    public record OrderDetails(
            String keyId,
            String orderId,
            long amount,
            String currency,
            String name,
            String description,
            String email) {
    }

    public record VerifyPaymentRequest(String orderId, String paymentId, String signature) {
    }

    public record PaymentResult(
            String orderId,
            String paymentId,
            BigDecimal amount,
            String currency,
            PaymentStatus status) {
    }
}
