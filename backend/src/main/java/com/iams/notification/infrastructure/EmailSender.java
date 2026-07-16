package com.iams.notification.infrastructure;

import com.iams.notification.application.NotificationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * US-NTF-01's SMTP leg. Deliberately thin: failures propagate as exceptions
 * so NotificationDeliveryProcessor owns retry/backoff state (US-NTF-08) in
 * one place. Plain-text mail - notification wording is template-controlled
 * (US-NTF-09), not markup-controlled.
 */
@Component
public class EmailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMailFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
