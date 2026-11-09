package com.spring.security.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("demo")
public class DemoJwtConfig {
    // Local testing only. Never use this shared key with a real identity provider.
    private static final String DEMO_SECRET = "demo-only-secret-change-before-any-deployment";

    @Bean
    JwtDecoder jwtDecoder() {
        var key = new SecretKeySpec(DEMO_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("demo"));
        return decoder;
    }
}
