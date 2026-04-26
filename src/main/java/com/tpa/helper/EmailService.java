package com.tpa.helper;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${admin.email}")
    private String adminEmail;

    @Async
    public void sendOtp(String name, String email, Integer otp) {
        try {
            String subject = "TPA Account Creation - OTP Verification";

            String text = "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                    + "<h2 style='color: #0056b3;'>TPA Claim System</h2>"
                    + "<p>Dear <b>" + name + "</b>,</p>"
                    + "<p>Your One Time Password (OTP) for account registration is: <h3 style='color: #d9534f;'>" + otp + "</h3></p>"
                    + "<p>This OTP is valid for exactly <b>5 minutes</b>. Please do not share this code with anyone.</p>"
                    + "<br/><p>Best regards,<br/>TPA Claim System Team</p>"
                    + "</div>"
                    + "</body></html>";

            sendEmail(email, subject, text);

        } catch (Exception e) {
            log.error("failed to send OTP email to {}", email, e);
        }
    }

    @Async
    public void sendConfirmation(String name, String email, String password) {
        try {
            String subject = "TPA Registration Successful";

            String text = "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                    + "<h2 style='color: #0056b3;'>Welcome to TPA Claim System</h2>"
                    + "<p>Dear <b>" + name + "</b>,</p>"
                    + "<p>Your account has been successfully registered. You can now log in to the portal using your registered email address:</p>"
                    + "<ul>"
                    + "<li><b>Email:</b> " + email + "</li>"
                    + "</ul>"
                    + "<p style='color: #555;'>For security, never share your password with anyone. If you did not create this account, please contact support immediately.</p>"
                    + "<br/><p>Best regards,<br/>TPA Claim System Team</p>"
                    + "</div>"
                    + "</body></html>";

            sendEmail(email, subject, text);

        } catch (Exception e) {
            log.error("failed to send confirmation email to {}", email, e);
        }
    }

    @Async
    public void sendPaymentConfirmation(String email, Long orderId, Double amount) {
        try {
            String subject = "TPA Payment Successful";

            String text = "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                    + "<h2 style='color: #28a745;'>Payment Received</h2>"
                    + "<p>Your payment has been successfully processed.</p>"
                    + "<ul>"
                    + "<li><b>Reference ID:</b> " + orderId + "</li>"
                    + "<li><b>Amount Paid:</b> $" + amount + "</li>"
                    + "</ul>"
                    + "<br/><p>Thank you,<br/>TPA Claim System Team</p>"
                    + "</div>"
                    + "</body></html>";

            sendEmail(email, subject, text);

        } catch (Exception e) {
            log.error("failed to send payment email to {}", email, e);
        }
    }

    @Async("taskExecutor")
    public void sendClaimStatusNotification(String email, Long claimId, String status, String messageStr) {
        try {
            String subject = "TPA Claim #" + claimId + " Status Update: " + status;

            String text = "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                    + "<h2 style='color: #0056b3;'>Claim Status Update</h2>"
                    + "<p>Hello,</p>"
                    + "<p>Your claim <b>#" + claimId + "</b> has been updated to: <span style='font-weight: bold; color: " 
                    + (status.equalsIgnoreCase("APPROVED") ? "#28a745" : (status.equalsIgnoreCase("REJECTED") ? "#dc3545" : "#ffc107")) 
                    + ";'>" + status + "</span></p>"
                    + "<p><b>Details:</b></p>"
                    + "<div style='background: #f8f9fa; padding: 10px; border-left: 3px solid #0056b3;'>" + messageStr + "</div>"
                    + "<br/><p>Best regards,<br/>TPA Claim System Team</p>"
                    + "</div>"
                    + "</body></html>";

            sendEmail(email, subject, text);

        } catch (Exception e) {
            log.error("failed to send claim notification email to {}", email, e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

        mimeMessageHelper.setFrom(adminEmail, "TPA Claim System");
        mimeMessageHelper.setTo(to);
        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }
}