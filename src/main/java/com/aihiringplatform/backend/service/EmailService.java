package com.aihiringplatform.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private org.springframework.core.env.Environment env;

    @jakarta.annotation.PostConstruct
    public void debugSmtpConfig() {
        String username = env.getProperty("spring.mail.username");
        String password = env.getProperty("spring.mail.password");
        
        logger.info("=== SMTP DEBUG LOGGING (STARTUP) ===");
        logger.info("MAIL_USERNAME loaded={}", (username != null && !username.isEmpty()));
        
        if (username != null && !username.isEmpty()) {
            String maskedUsername = "***";
            if (username.contains("@")) {
                int atIndex = username.indexOf("@");
                if (atIndex > 3) {
                    maskedUsername = username.substring(0, 3) + "***" + username.substring(atIndex);
                } else {
                    maskedUsername = "***" + username.substring(atIndex);
                }
            }
            logger.info("MAIL_USERNAME={}", maskedUsername);
        }
        
        logger.info("MAIL_PASSWORD loaded={}", (password != null && !password.isEmpty()));
        if (password != null) {
            logger.info("MAIL_PASSWORD length={}", password.length());
        }
        logger.info("====================================");
    }

    public void sendRegistrationOtp(String to, String otp) {
        String subject = "Verify your SmartATS account";
        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;\">"
                + "<h2 style=\"color: #4f46e5; text-align: center;\">SmartATS</h2>"
                + "<h3 style=\"color: #333;\">Verify your email address</h3>"
                + "<p>Thank you for registering with SmartATS. To complete your registration, please use the following OTP:</p>"
                + "<div style=\"background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 24px; letter-spacing: 5px; font-weight: bold; border-radius: 5px; margin: 20px 0;\">"
                + otp
                + "</div>"
                + "<p>This OTP will expire in 5 minutes. Do not share this code with anyone.</p>"
                + "<p>If you did not create an account, please ignore this email.</p>"
                + "<br/>"
                + "<p>Best regards,<br/>The SmartATS Team</p>"
                + "</div>";

        sendHtmlEmail(to, subject, content);
    }

    public void sendForgotPasswordOtp(String to, String otp) {
        String subject = "Reset your SmartATS password";
        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;\">"
                + "<h2 style=\"color: #4f46e5; text-align: center;\">SmartATS</h2>"
                + "<h3 style=\"color: #333;\">Password Reset Request</h3>"
                + "<p>We received a request to reset your password. Use the following OTP to proceed:</p>"
                + "<div style=\"background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 24px; letter-spacing: 5px; font-weight: bold; border-radius: 5px; margin: 20px 0;\">"
                + otp
                + "</div>"
                + "<p>This OTP will expire in 5 minutes. Do not share this code with anyone.</p>"
                + "<p>If you did not request a password reset, please ignore this email.</p>"
                + "<br/>"
                + "<p>Best regards,<br/>The SmartATS Team</p>"
                + "</div>";

        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = env.getProperty("spring.mail.username", "noreply@smartats.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(fromAddress);

            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send OTP email. Please try again later.");
        }
    }
}
