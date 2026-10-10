package com.medislot.service;

import com.medislot.entity.Notification;
import com.medislot.entity.NotificationType;
import com.medislot.entity.User;
import com.medislot.exception.ApiException;
import com.medislot.repository.NotificationRepository;
import com.medislot.repository.UserRepository;
import com.medislot.service.channel.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationChannel channel = mock(NotificationChannel.class);
    private final User user = new User();
    private NotificationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<NotificationChannel> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(i -> Stream.of(channel));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        service = new NotificationService(repo, users, provider);
    }

    @Test
    void storesTheNotificationAndDeliversItToChannels() {
        service.send(1L, NotificationType.APPOINTMENT_CONFIRMED, "Title", "Body", "/x", 5L, null);
        verify(repo).save(any(Notification.class));
        verify(channel).deliver(eq(user), eq(NotificationType.APPOINTMENT_CONFIRMED), eq("Title"), eq("Body"));
    }

    @Test
    void sameDedupeKeyIsNotSentTwice() {
        when(repo.existsByUserIdAndDedupeKey(1L, "k")).thenReturn(true);
        service.send(1L, NotificationType.APPOINTMENT_REMINDER, "T", "B", null, null, "k");
        verify(repo, never()).save(any(Notification.class));
        verify(channel, never()).deliver(any(), any(), any(), any());
    }

    @Test
    void aFailingChannelDoesNotBreakTheCaller() {
        doThrow(new RuntimeException("smtp down")).when(channel).deliver(any(), any(), any(), any());
        assertDoesNotThrow(() -> service.send(1L, NotificationType.CHECKED_IN, "T", "B", null, null, null));
        verify(repo).save(any(Notification.class));
    }

    @Test
    void unknownUserIsIgnored() {
        when(users.findById(2L)).thenReturn(Optional.empty());
        service.send(2L, NotificationType.CHECKED_IN, "T", "B", null, null, null);
        verify(repo, never()).save(any(Notification.class));
    }

    @Test
    void markingSomeoneElsesNotificationReadIsNotFound() {
        when(repo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());
        assertEquals("NOTIFICATION_NOT_FOUND",
                assertThrows(ApiException.class, () -> service.markRead(1L, 9L)).getCode());
    }
}