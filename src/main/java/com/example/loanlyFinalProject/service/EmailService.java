package com.example.loanlyFinalProject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  @Value("${app.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  public void sendPasswordResetEmail(String toEmail, String token) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromEmail);
      message.setTo(toEmail);
      message.setSubject("Password Reset Request - Loan Banking System");

      String resetLink = frontendUrl + "/reset-password?token=" + token;
      String emailBody =
          String.format(
              "Hello,\n\n"
                  + "You have requested to reset your password.\n\n"
                  + "Click the link below to reset your password:\n%s\n\n"
                  + "This link will expire in 1 hour.\n\n"
                  + "If you did not request this, please ignore this email.\n\n"
                  + "Best regards,\nLoan Banking System Team",
              resetLink);

      message.setText(emailBody);
      mailSender.send(message);

      log.info("Password reset email sent to: {}", toEmail);
    } catch (Exception e) {
      log.error("Failed to send password reset email to: {}", toEmail, e);
      throw new RuntimeException("Failed to send email. Please try again later.");
    }
  }

  public void sendWelcomeEmail(String toEmail, String username) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromEmail);
      message.setTo(toEmail);
      message.setSubject("Welcome to Loan Banking System");

      String emailBody =
          String.format(
              "Hello %s,\n\n"
                  + "Welcome to Loan Banking System!\n\n"
                  + "Your account has been successfully created.\n\n"
                  + "You can now login and start using our services.\n\n"
                  + "Best regards,\nLoan Banking System Team",
              username);

      message.setText(emailBody);
      mailSender.send(message);

      log.info("Welcome email sent to: {}", toEmail);
    } catch (Exception e) {
      log.error("Failed to send welcome email to: {}", toEmail, e);
      // Don't throw exception for welcome email, just log it
    }
  }
}
