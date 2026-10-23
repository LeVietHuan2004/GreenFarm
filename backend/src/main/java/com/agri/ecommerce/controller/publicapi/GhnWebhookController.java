package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.service.GhnWebhookService;
import com.agri.ecommerce.common.exception.ApplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/ghn/orders")
public class GhnWebhookController {
    private final GhnWebhookService service;
    private final ObjectMapper json = new ObjectMapper();
    public GhnWebhookController(GhnWebhookService service){this.service=service;}
    @PostMapping public ResponseEntity<Void> receive(@RequestHeader(value="X-GreenFarm-GHN-Secret",required=false) String secret,
                                                       @RequestBody String body) {
        try {
            JsonNode event = json.readTree(body);
            service.receive(secret,event);
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "GHN_WEBHOOK_INVALID", "Nội dung webhook GHN không hợp lệ");
        }
        return ResponseEntity.noContent().build();
    }
}
