package br.com.gestaofrota.infra.notifications;

import br.com.gestaofrota.application.services.NotificationEmailSender;
import br.com.gestaofrota.infra.config.notifications.NotificationEmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class SmtpNotificationEmailSender implements NotificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationEmailSender.class);

    private final NotificationEmailProperties properties;

    public SmtpNotificationEmailSender(NotificationEmailProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean sendEmail(String recipientEmail, String subject, String body) {
        if (!properties.enabled() || properties.host() == null || properties.host().isBlank()) {
            return false;
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return false;
        }

        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(properties.host().trim());
            mailSender.setPort(properties.port());
            mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
            if (properties.username() != null && !properties.username().isBlank()) {
                mailSender.setUsername(properties.username().trim());
            }
            if (properties.password() != null && !properties.password().isBlank()) {
                mailSender.setPassword(properties.password());
            }

            Properties javaMailProperties = mailSender.getJavaMailProperties();
            javaMailProperties.put("mail.smtp.auth", Boolean.toString(properties.smtpAuth()));
            javaMailProperties.put("mail.smtp.starttls.enable", Boolean.toString(properties.startTls()));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(recipientEmail.trim());
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Falha ao enviar e-mail de notificação para {}: {}", recipientEmail, exception.getMessage());
            return false;
        }
    }
}
