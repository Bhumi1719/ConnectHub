package com.connecthub.notification.service.impl;

import com.connecthub.notification.dto.BulkNotificationRequest;
import com.connecthub.notification.dto.EmailRequest;
import com.connecthub.notification.dto.SendNotificationRequest;
import com.connecthub.notification.entity.Notification;
import com.connecthub.notification.repository.NotificationRepository;
import com.connecthub.notification.service.NotifService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
@Transactional
public class NotifServiceImpl implements NotifService {

    private static final Logger log = Logger.getLogger(NotifServiceImpl.class.getName());

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@connecthub.com}")
    private String emailFrom;

    public NotifServiceImpl(NotificationRepository notificationRepository,
                            JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    // ─── Send Single Notification ─────────────────────────────────────────────

    @Override
    public Notification send(SendNotificationRequest request) {
        validateType(request.getType());

        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .roomId(request.getRoomId())
                .messageId(request.getMessageId())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent to recipientId=" + request.getRecipientId()
                + " type=" + request.getType());
        return saved;
    }

    // ─── Send Bulk Notifications ──────────────────────────────────────────────
    // e.g. notify all room members when someone joins

    @Override
    public List<Notification> sendBulk(BulkNotificationRequest request) {
        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            throw new RuntimeException("recipientIds cannot be empty");
        }
        validateType(request.getType());

        List<Notification> saved = new ArrayList<>();

        for (Integer recipientId : request.getRecipientIds()) {
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .actorId(request.getActorId())
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .roomId(request.getRoomId())
                    .build();
            saved.add(notificationRepository.save(notification));
        }

        log.info("Bulk notifications sent to " + request.getRecipientIds().size() + " users");
        return saved;
    }

    // ─── Get All Notifications for User ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getByRecipient(Integer recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    // ─── Get Unread Notifications ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadByRecipient(Integer recipientId) {
        return notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(
                recipientId, false);
    }

    // ─── Get Unread Count ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Integer recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    // ─── Mark Single As Read ──────────────────────────────────────────────────

    @Override
    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Notification not found with id: " + notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.info("Notification marked as read: id=" + notificationId);
    }

    // ─── Mark All As Read ─────────────────────────────────────────────────────

    @Override
    public void markAllRead(Integer recipientId) {
        notificationRepository.markAllAsRead(recipientId);
        log.info("All notifications marked as read for recipientId=" + recipientId);
    }

    // ─── Delete Notification ──────────────────────────────────────────────────

    @Override
    public void deleteNotification(Integer notificationId) {
        notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Notification not found with id: " + notificationId));
        notificationRepository.deleteByNotificationId(notificationId);
        log.info("Notification deleted: id=" + notificationId);
    }

    // ─── Send Email ───────────────────────────────────────────────────────────
    // Used for missed DM notifications when user offline > 30 mins

    @Override
    public void sendEmail(EmailRequest request) {
        if (!emailEnabled) {
            log.info("Email disabled. Skipping email to: " + request.getToEmail());
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(emailFrom);
            mail.setTo(request.getToEmail());
            mail.setSubject(request.getSubject());
            mail.setText(request.getBody());
            mailSender.send(mail);
            log.info("Email sent to: " + request.getToEmail());
        } catch (Exception e) {
            log.warning("Failed to send email to " + request.getToEmail()
                    + ": " + e.getMessage());
        }
    }

    // ─── Get All (Admin) ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void validateType(String type) {
        List<String> allowed = List.of("NEW_MESSAGE", "MENTION", "ROOM_INVITE", "SYSTEM");
        if (!allowed.contains(type)) {
            throw new RuntimeException(
                    "Invalid type. Allowed: NEW_MESSAGE, MENTION, ROOM_INVITE, SYSTEM");
        }
    }
}
