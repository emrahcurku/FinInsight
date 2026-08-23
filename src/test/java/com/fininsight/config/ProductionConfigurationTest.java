package com.fininsight.config;

import com.fininsight.auth.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProductionConfigurationTest {

    @Test
    @DisplayName("JwtTokenProvider fails to start if JWT secret key is null or empty")
    void testMissingJwtSecretThrowsException() {
        assertThatThrownBy(() -> new JwtTokenProvider(null, 900000, 604800000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be configured");

        assertThatThrownBy(() -> new JwtTokenProvider("", 900000, 604800000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be configured");

        assertThatThrownBy(() -> new JwtTokenProvider("   ", 900000, 604800000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key must be configured");
    }

    @Test
    @DisplayName("JwtTokenProvider fails to start if JWT secret key is shorter than 256 bits (32 bytes)")
    void testShortJwtSecretThrowsException() {
        assertThatThrownBy(() -> new JwtTokenProvider("short_secret_key_12345", 900000, 604800000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 256 bits");
    }

    @Test
    @DisplayName("JwtTokenProvider starts successfully with valid 256+ bit secret")
    void testValidJwtSecretInitializesSuccessfully() {
        String validSecret = "this_is_a_very_secure_secret_key_that_is_well_over_32_characters_long_for_production";
        JwtTokenProvider provider = new JwtTokenProvider(validSecret, 900000, 604800000);
        assertThat(provider).isNotNull();
    }
}
