package br.com.gestaofrota.infra.config.notifications;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.email")
public record NotificationEmailProperties(
        boolean enabled,
        String from,
        String host,
        Integer port,
        String username,
        String password,
        Boolean smtpAuth,
        Boolean startTls
) {
    public NotificationEmailProperties {
        if (from == null || from.isBlank()) {
            from = "no-reply@fsph.se.gov.br";
        }
        if (port == null || port <= 0) {
            port = 587;
        }
        if (smtpAuth == null) {
            smtpAuth = true;
        }
        if (startTls == null) {
            startTls = true;
        }
    }
}
