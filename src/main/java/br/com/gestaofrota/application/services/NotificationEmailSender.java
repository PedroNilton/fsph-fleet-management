package br.com.gestaofrota.application.services;

public interface NotificationEmailSender {

    boolean sendEmail(String recipientEmail, String subject, String body);
}
