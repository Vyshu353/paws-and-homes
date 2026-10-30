package com.pawsandhomes.petadoption.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String senderAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String senderAddress) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    public boolean sendWelcomeTestEmail(String recipient) {
        if (senderAddress.isBlank()) {
            LOGGER.warn("Development test email was not sent: MAIL_FROM is not configured.");
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(recipient);
        message.setSubject("Welcome to Paws & Homes");
        message.setText("""
                Hello!

                This is a test email from Paws & Homes. Our furry friends are excited to meet you.

                Thank you,
                The Paws & Homes Team
                """);
        try {
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            LOGGER.warn(
                    "Development test email delivery failed: category={}, causeTypes={}",
                    classifyFailure(exception),
                    causeTypes(exception));
            return false;
        }
    }

    private static String classifyFailure(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String type = cause.getClass().getSimpleName().toLowerCase();
            String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();

            if (type.contains("authenticationfailed")
                    || type.contains("mailauthentication")
                    || message.contains("535")
                    || message.contains("authentication")
                    || message.contains("invalid login")
                    || message.contains("credentials")) {
                return "SMTP authentication rejected; check the Brevo SMTP login/key and any IP authorization policy";
            }
            if (type.contains("sslhandshake")
                    || message.contains("starttls")
                    || message.contains("tls")
                    || message.contains("ssl")) {
                return "SMTP TLS negotiation failed; check STARTTLS support and network inspection";
            }
            if (type.contains("unknownhost")
                    || type.contains("connect")
                    || type.contains("sockettimeout")
                    || type.contains("noroutetohost")
                    || message.contains("timed out")
                    || message.contains("connection refused")) {
                return "SMTP relay connection failed; check outbound network access to smtp-relay.brevo.com:587";
            }
            if (message.contains("550")
                    || message.contains("553")
                    || message.contains("sender")
                    || message.contains("from address")
                    || message.contains("verified")) {
                return "Brevo rejected the sender or recipient; verify the sender identity and relay permissions";
            }
        }
        return "Unclassified mail-provider failure; inspect the cause types";
    }

    private static String causeTypes(Throwable exception) {
        StringBuilder types = new StringBuilder();
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (!types.isEmpty()) {
                types.append(" -> ");
            }
            types.append(cause.getClass().getSimpleName());
        }
        return types.toString();
    }
}
