package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notifications; @Mock UserRepository users;
    NotificationService service;
    @Mock NotificationStreamService streams;
    @BeforeEach void setUp(){service=new NotificationService(notifications,users,streams);}

    @Test void userCanOnlyMarkOwnNotificationRead(){
        when(notifications.findByIdAndUser_Id(9L,1L)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.markRead(1L,9L)).extracting("code").isEqualTo("NOTIFICATION_NOT_FOUND");
        verify(notifications,never()).save(any());
    }

    @Test void marksOwnedNotificationRead(){
        Notification notification=new Notification();
        when(notifications.findByIdAndUser_Id(9L,1L)).thenReturn(Optional.of(notification));
        when(notifications.save(notification)).thenReturn(notification);
        assertThat(service.markRead(1L,9L).read()).isTrue();
    }
}
