package com.medislot.service;

import com.medislot.dto.NotificationDtos.NotificationView;
import com.medislot.entity.Notification;
import com.medislot.entity.NotificationType;
import com.medislot.entity.User;
import com.medislot.exception.ApiException;
import com.medislot.repository.NotificationRepository;
import com.medislot.repository.UserRepository;
import com.medislot.service.channel.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Stores the in-app notification, then hands it to the other channels. A channel failure is logged and
 * ignored. Messages must never contain clinical details.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;
    private final UserRepository users;
    private final ObjectProvider<NotificationChannel> channels;

    public NotificationService(NotificationRepository repo, UserRepository users,
                               ObjectProvider<NotificationChannel> channels) {
        this.repo = repo;
        this.users = users;
        this.channels = channels;
    }

    /** dedupeKey (optional) makes the call idempotent per user. */
    @Transactional
    public void send(Long userId, NotificationType type, String title, String message, String link,
                     Long appointmentId, String dedupeKey) {
        if (userId == null) return;
        if (dedupeKey != null && repo.existsByUserIdAndDedupeKey(userId, dedupeKey)) return;
        User user = users.findById(userId).orElse(null);
        if (user == null) return;

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        n.setAppointmentId(appointmentId);
        n.setDedupeKey(dedupeKey);
        repo.save(n);

        channels.orderedStream().forEach(c -> {
            try {
                c.deliver(user, type, title, message);
            } catch (RuntimeException e) {
                log.warn("Notification channel {} failed: {}", c.getClass().getSimpleName(), e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationView> list(Long userId, boolean unreadOnly) {
        List<Notification> rows = unreadOnly
                ? repo.findTop50ByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                : repo.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        return rows.stream().map(NotificationService::view).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = repo.findByIdAndUserId(id, userId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found."));
        if (n.getReadAt() == null) n.setReadAt(Instant.now());
    }

    @Transactional
    public void markAllRead(Long userId) {
        repo.markAllRead(userId, Instant.now());
    }

    private static NotificationView view(Notification n) {
        return new NotificationView(n.getId(), n.getType().name(), n.getTitle(), n.getMessage(), n.getLink(),
                n.getAppointmentId(), n.getReadAt() != null, n.getCreatedAt());
    }
}