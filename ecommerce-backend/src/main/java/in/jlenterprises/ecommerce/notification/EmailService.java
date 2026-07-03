package in.jlenterprises.ecommerce.notification;

import in.jlenterprises.ecommerce.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails. Runs asynchronously so a slow SMTP server never
 * blocks the API thread, and failures are logged rather than propagated (the
 * business operation, e.g. registration, still succeeds).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.from = props.mail().from();
    }

    @Async
    public void sendOtp(String to, String code) {
        send(to, "Your JL Enterprises verification code",
                "Your one-time code is " + code + ". It expires shortly. Do not share it with anyone.");
    }

    @Async
    public void sendVerificationLink(String to, String link) {
        send(to, "Verify your JL Enterprises account",
                "Welcome! Please verify your email by opening this link:\n\n" + link);
    }

    @Async
    public void sendPasswordResetLink(String to, String link) {
        send(to, "Reset your JL Enterprises password",
                "We received a request to reset your password. Open this link to continue:\n\n" + link
                        + "\n\nIf you did not request this, you can ignore this email.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (MailException e) {
            log.warn("Failed to send email to {} (subject: {})", to, subject, e);
        }
    }
}
