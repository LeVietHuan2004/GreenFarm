package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agri.ecommerce.config.GhnProperties;
import org.junit.jupiter.api.Test;

class GhnPropertiesTest {
    @Test void stagingRequiresWebhookSecretBeforeCustomersCanSelectGhn() {
        var settings=new GhnProperties(true,"https://dev-online-gateway.ghn.vn","token",123,"",1,"CHOXEMHANGKHONGTHU");
        assertThat(settings.available()).isFalse();
        assertThatThrownBy(settings::requireEnabled).hasMessageContaining("Cấu hình");
    }

    @Test void productionGatewayIsRejectedEvenWithValidCredentials() {
        var settings=new GhnProperties(true,"https://online-gateway.ghn.vn","token",123,"secret",1,"CHOXEMHANGKHONGTHU");
        assertThat(settings.available()).isFalse();
        assertThatThrownBy(settings::requireEnabled).hasMessageContaining("staging");
    }
}
