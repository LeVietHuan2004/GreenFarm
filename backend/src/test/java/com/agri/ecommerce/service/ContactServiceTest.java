package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.ContactRequest;
import com.agri.ecommerce.entity.Contact;
import com.agri.ecommerce.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {
    @Mock ContactRepository contacts; @Mock UserRepository users; @Mock NotificationService notifications;
    ContactService service;
    @BeforeEach void setUp(){service=new ContactService(contacts,users,notifications);when(contacts.save(any(Contact.class))).thenAnswer(call->call.getArgument(0));}

    @Test void createsContactAndNotifiesOperationsTeams(){
        var result=service.create(null,new ContactRequest("  Nguyễn An  ","0901","an@example.com","  Tôi cần hỗ trợ đơn hàng  "));
        assertThat(result.fullName()).isEqualTo("Nguyễn An"); assertThat(result.status()).isEqualTo("open");
        verify(notifications).notifyRole(eq("admin"),eq("contact"),contains("Nguyễn An"),eq("/admin/contacts"));
        verify(notifications).notifyRole(eq("staff"),eq("contact"),contains("Nguyễn An"),eq("/staff/contacts"));
    }
}
