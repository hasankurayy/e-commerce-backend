package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository logRepository;
    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body,
                          String eventType, Long orderId, Long userId) {
        boolean sent = false;
        String errorMessage = null;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            sent = true;
            log.info("Email sent to {} for event {}", to, eventType);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Failed to send email to {} for event {}: {}", to, eventType, e.getMessage());
        }

        logRepository.save(NotificationLog.builder()
                .eventType(eventType)
                .orderId(orderId)
                .userId(userId)
                .recipient(to)
                .subject(subject)
                .sent(sent)
                .errorMessage(errorMessage)
                .build());
    }
}
