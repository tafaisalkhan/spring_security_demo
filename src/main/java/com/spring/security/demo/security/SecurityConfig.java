package com.spring.security.demo.security;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    WebSecurityCustomizer publicEndpoints(DatabasePublicEndpointMatcher publicEndpoints) {
        return web -> web.ignoring().requestMatchers(publicEndpoints);
    }

    @Bean
    @Order(1)
    SecurityFilterChain actuatorChain(HttpSecurity http, DatabaseRoleConverter roles,
                                      DatabaseEndpointAuthorizationManager endpoints) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint());
        return jwtChain(http, roles, endpoints);
    }

    @Bean
    @Order(2)
    SecurityFilterChain adminChain(HttpSecurity http, DatabaseRoleConverter roles,
                                   DatabaseEndpointAuthorizationManager endpoints) throws Exception {
        http.securityMatcher("/admin/**");
        return jwtChain(http, roles, endpoints);
    }

    @Bean
    @Order(3)
    SecurityFilterChain mobileChain(HttpSecurity http, DatabaseRoleConverter roles,
                                    DatabaseEndpointAuthorizationManager endpoints) throws Exception {
        http.securityMatcher("/mobile/**");
        return jwtChain(http, roles, endpoints);
    }

    @Bean
    @Order(4)
    SecurityFilterChain remainingChain(HttpSecurity http, DatabaseRoleConverter roles,
                                       DatabaseEndpointAuthorizationManager endpoints) throws Exception {
        return jwtChain(http, roles, endpoints);
    }

    private SecurityFilterChain jwtChain(HttpSecurity http, DatabaseRoleConverter roles,
                                         DatabaseEndpointAuthorizationManager endpoints) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().access(endpoints))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(roles)))
                .build();
    }
}
