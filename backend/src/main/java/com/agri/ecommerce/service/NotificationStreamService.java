package com.agri.ecommerce.service;

import com.agri.ecommerce.dto.response.NotificationResponse;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationStreamService {
    private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); }
        catch (IOException exception) { cleanup.run(); }
        return emitter;
    }

    public void publish(Long userId, NotificationResponse notification) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;
        userEmitters.forEach(emitter -> {
            try { emitter.send(SseEmitter.event().name("notification").id(notification.id().toString()).data(notification)); }
            catch (IOException | IllegalStateException exception) { remove(userId, emitter); }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) emitters.remove(userId, userEmitters);
    }
}
