package com.agri.ecommerce.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
        "GreenFarmTestSecretThatIsLongEnoughForHmacSigning",
        3_600_000
    );

    @Test
    void generatesAndValidatesTokenForUser() {
        GreenFarmUserDetails userDetails = new GreenFarmUserDetails(
            42L,
            "tester@greenfarm.local",
            "hashed-password",
            List.of(),
            true,
            true
        );

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo(userDetails.getUsername());
        assertThat(jwtService.isValid(token, userDetails)).isTrue();
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600);
    }
}
