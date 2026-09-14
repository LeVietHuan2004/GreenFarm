package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.*;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContactService {
    private final ContactRepository contacts;
    private final UserRepository users;
    private final NotificationService notifications;

    public ContactService(ContactRepository contacts, UserRepository users, NotificationService notifications) {
        this.contacts = contacts; this.users = users; this.notifications = notifications;
    }

    @Transactional
    public ContactResponse create(Long userId, ContactRequest request) {
        Contact contact = new Contact();
        if (userId != null) contact.setUser(users.findById(userId).orElse(null));
        contact.setFullName(request.fullName().trim());
        contact.setPhoneNumber(clean(request.phoneNumber())); contact.setEmail(clean(request.email()));
        contact.setMessage(request.message().trim()); contact.setStatus("open");
        Contact saved = contacts.save(contact);
        String message = "Có liên hệ hỗ trợ mới từ " + saved.getFullName();
        notifications.notifyRole("admin", "contact", message, "/admin/contacts");
        notifications.notifyRole("staff", "contact", message, "/staff/contacts");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> findAll(String rawStatus, Pageable pageable) {
        String status = clean(rawStatus);
        var page = status == null ? contacts.findAllByOrderByCreatedAtDescIdDesc(pageable)
            : contacts.findAllByStatusOrderByCreatedAtDescIdDesc(parseStatus(status), pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findMine(Long userId) {
        return contacts.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ContactResponse reply(Long responderId, Long id, ContactReplyRequest request) {
        Contact contact = find(id); User responder = users.getReferenceById(responderId);
        contact.setResponse(request.response().trim()); contact.setStatus("replied"); contact.setReplied(true);
        contact.setRespondedBy(responder); contact.setRespondedAt(LocalDateTime.now());
        Contact saved = contacts.save(contact);
        notifications.notifyUser(saved.getUser(), "contact", "GreenFarm đã phản hồi yêu cầu hỗ trợ #" + saved.getId(), "/contact");
        return toResponse(saved);
    }

    @Transactional
    public ContactResponse resolve(Long responderId, Long id) {
        Contact contact = find(id); contact.setStatus("resolved"); contact.setReplied(true);
        if (contact.getRespondedBy() == null) contact.setRespondedBy(users.getReferenceById(responderId));
        if (contact.getRespondedAt() == null) contact.setRespondedAt(LocalDateTime.now());
        Contact saved = contacts.save(contact);
        notifications.notifyUser(saved.getUser(), "contact", "Yêu cầu hỗ trợ #" + saved.getId() + " đã được xử lý", "/contact");
        return toResponse(saved);
    }

    private Contact find(Long id) { return contacts.findById(id).orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "CONTACT_NOT_FOUND", "Không tìm thấy liên hệ")); }
    private String parseStatus(String status) { String value=status.toLowerCase(); if (!value.equals("open")&&!value.equals("replied")&&!value.equals("resolved")) throw new ApplicationException(HttpStatus.BAD_REQUEST,"INVALID_CONTACT_STATUS","Trạng thái liên hệ không hợp lệ"); return value; }
    private String clean(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private ContactResponse toResponse(Contact value) { User responder=value.getRespondedBy(); return new ContactResponse(value.getId(), value.getUser()==null?null:value.getUser().getId(), value.getFullName(), value.getPhoneNumber(), value.getEmail(), value.getMessage(), value.getStatus(), value.getResponse(), responder==null?null:responder.getId(), responder==null?null:responder.getName(), value.getRespondedAt(), value.getCreatedAt(), value.getUpdatedAt()); }
}
