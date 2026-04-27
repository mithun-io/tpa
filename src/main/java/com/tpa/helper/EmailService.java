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
                    + (status.contains("APPROVED") || status.equalsIgnoreCase("SETTLED") ? "#28a745" : (status.equalsIgnoreCase("REJECTED") ? "#dc3545" : "#ffc107")) 
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

    @Async
    public void sendCarrierApprovalEmail(String email, String companyName) {
        try {
            String subject = "✅ Your Carrier Application Has Been Approved — TPA Claim System";

            String text = "<html><body style='font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;'>"
                    + "<div style='background: linear-gradient(135deg, #1a6b2e, #28a745); padding: 30px 24px; text-align: center;'>"
                    + "<h1 style='color: #fff; margin: 0; font-size: 24px;'>🎉 Application Approved!</h1>"
                    + "</div>"
                    + "<div style='padding: 28px 24px;'>"
                    + "<p>Dear <b>" + companyName + "</b>,</p>"
                    + "<p>We are pleased to inform you that your carrier registration application on the <b>TPA Claim System</b> has been <span style='color: #28a745; font-weight: bold;'>APPROVED</span> by our admin team.</p>"
                    + "<div style='background: #f0fff4; border-left: 4px solid #28a745; border-radius: 4px; padding: 14px 18px; margin: 20px 0;'>"
                    + "<p style='margin: 0; color: #155724; font-weight: bold;'>✅ Your account is now active.</p>"
                    + "<p style='margin: 8px 0 0 0; color: #155724; font-size: 14px;'>You can now log in to the carrier portal using your registered email and password.</p>"
                    + "</div>"
                    + "<p style='font-size: 14px; color: #555;'>If you have any questions, please contact our support team.</p>"
                    + "<br/><p>Best regards,<br/><b>TPA Claim System Team</b></p>"
                    + "</div>"
                    + "<div style='background: #f8f9fa; padding: 14px 24px; text-align: center; border-top: 1px solid #ddd;'>"
                    + "<p style='margin: 0; font-size: 12px; color: #888;'>This is an automated message. Please do not reply directly to this email.</p>"
                    + "</div>"
                    + "</div>"
                    + "</body></html>";

            sendEmail(email, subject, text);
            log.info("Carrier approval email sent to {}", email);

        } catch (Exception e) {
            log.error("Failed to send carrier approval email to {}", email, e);
        }
    }

    @Async
    public void sendCarrierRejectionEmail(String email, String companyName) {
        try {
            String subject = "❌ Your Carrier Application Status — TPA Claim System";

            String text = "<html><body style='font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;'>"
                    + "<div style='background: linear-gradient(135deg, #7b1c1c, #dc3545); padding: 30px 24px; text-align: center;'>"
                    + "<h1 style='color: #fff; margin: 0; font-size: 24px;'>Application Not Approved</h1>"
                    + "</div>"
                    + "<div style='padding: 28px 24px;'>"
                    + "<p>Dear <b>" + companyName + "</b>,</p>"
                    + "<p>Thank you for applying to join the <b>TPA Claim System</b> carrier network.</p>"
                    + "<p>After reviewing your application, our compliance team has determined that we are unable to approve your carrier registration at this time.</p>"
                    + "<div style='background: #fff5f5; border-left: 4px solid #dc3545; border-radius: 4px; padding: 14px 18px; margin: 20px 0;'>"
                    + "<p style='margin: 0; color: #721c24; font-weight: bold;'>❌ Application Status: Not Approved</p>"
                    + "<p style='margin: 8px 0 0 0; color: #721c24; font-size: 14px;'>If you believe this is in error or would like to provide additional documentation, please contact our support team.</p>"
                    + "</div>"
                    + "<p style='font-size: 14px; color: #555;'>We appreciate your interest and encourage you to reach out if you have any questions.</p>"
                    + "<br/><p>Best regards,<br/><b>TPA Claim System Team</b></p>"
                    + "</div>"
                    + "<div style='background: #f8f9fa; padding: 14px 24px; text-align: center; border-top: 1px solid #ddd;'>"
                    + "<p style='margin: 0; font-size: 12px; color: #888;'>This is an automated message. Please do not reply directly to this email.</p>"
                    + "</div>"
                    + "</div>"
                    + "</body></html>";

            sendEmail(email, subject, text);
            log.info("Carrier rejection email sent to {}", email);

        } catch (Exception e) {
            log.error("Failed to send carrier rejection email to {}", email, e);
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