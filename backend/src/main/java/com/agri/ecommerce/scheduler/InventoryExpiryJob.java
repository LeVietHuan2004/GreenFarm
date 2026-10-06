package com.agri.ecommerce.scheduler;

import com.agri.ecommerce.service.InventoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryExpiryJob {
    private final InventoryService inventory;

    public InventoryExpiryJob(InventoryService inventory) { this.inventory = inventory; }

    @Scheduled(initialDelay = 1000, fixedDelayString = "${app.inventory.expiry-scan-ms:60000}")
    public void refreshExpiredStock() { inventory.syncExpiredProductStock(); }
}
