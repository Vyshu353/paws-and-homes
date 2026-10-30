package com.pawsandhomes.petadoption.controller;

import com.pawsandhomes.petadoption.service.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/api/test-email")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        boolean sent = emailService.sendWelcomeTestEmail(request.recipient());
        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Welcome test email sent successfully."));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "success", false,
                "message", "Email could not be sent. Check the development backend mail configuration."));
    }

    public record TestEmailRequest(@NotBlank @Email @Size(max = 254) String recipient) {
    }
}
