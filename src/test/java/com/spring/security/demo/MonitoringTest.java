package com.spring.security.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

@SpringBootTest
@AutoConfigureMockMvc
class MonitoringTest {
    private static final byte[] SECRET = "demo-only-secret-change-before-any-deployment".getBytes(StandardCharsets.UTF_8);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/demo/controller")).andExpect(status().isUnauthorized());
        mvc.perform(get("/demo/plain")).andExpect(status().isUnauthorized());
    }

    @Test void publicEndpointNeedsNoToken() throws Exception {
        mvc.perform(get("/demo/public"))
                .andExpect(status().isOk()).andExpect(content().string("public request measured"));
        mvc.perform(get("/demo/public").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk()).andExpect(content().string("public request measured"));
    }

    @Test void databaseUserRoleAllowsIndividualEndpoints() throws Exception {
        String token = token("alice", "ADMIN"); // Token role cannot grant ADMIN; database role is USER.
        mvc.perform(get("/demo/controller").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string("controller request measured"))
                .andExpect(authenticated().withAuthorities(List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        mvc.perform(get("/demo/service").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string("service request measured"));
        mvc.perform(get("/demo/method").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string("method measured"));
        mvc.perform(get("/demo/plain").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string("no monitoring"));
        mvc.perform(get("/demo/all").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test void databaseAdminRoleAllowsAdminEndpoint() throws Exception {
        mvc.perform(get("/demo/all").header("Authorization", "Bearer " + token("bob", "USER")))
                .andExpect(status().isOk()).andExpect(content().string("service request and method measured"));
    }

    @Test void adminChainRequiresDatabaseAdminRole() throws Exception {
        mvc.perform(get("/admin/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/status").header("Authorization", "Bearer " + token("alice", "ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/status").header("Authorization", "Bearer " + token("bob", "USER")))
                .andExpect(status().isOk()).andExpect(content().string("admin ready"));
    }

    @Test void mobileChainRequiresDatabaseUserRole() throws Exception {
        mvc.perform(get("/mobile/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/mobile/status").header("Authorization", "Bearer " + token("ops", "USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/mobile/status").header("Authorization", "Bearer " + token("alice", "ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string("mobile ready"));
    }

    @Test void actuatorChainRequiresDatabaseMonitorRole() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health").header("Authorization", "Bearer " + token("bob", "MONITOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health").header("Authorization", "Bearer " + token("ops", "USER")))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/info").header("Authorization", "Bearer " + token("ops", "USER")))
                .andExpect(status().isOk());
    }

    @Test void tokenRoleAloneGrantsNothing() throws Exception {
        mvc.perform(get("/demo/all").header("Authorization", "Bearer " + token("unknown", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test void endpointRoleChangeInDatabaseTakesEffectImmediately() throws Exception {
        jdbc.update("UPDATE endpoint_permissions SET role_name = 'USER' WHERE http_method = 'GET' AND path = '/demo/all'");
        try {
            mvc.perform(get("/demo/all").header("Authorization", "Bearer " + token("alice", "ADMIN")))
                    .andExpect(status().isOk());
        } finally {
            jdbc.update("UPDATE endpoint_permissions SET role_name = 'ADMIN' WHERE http_method = 'GET' AND path = '/demo/all'");
        }
    }

    @Test void protectedRuleWinsIfPathAlsoHasPublicRule() throws Exception {
        jdbc.update("INSERT INTO endpoint_permissions (http_method, path, access_type, role_name) VALUES ('GET', '/demo/public', 'ROLE', 'USER')");
        try {
            mvc.perform(get("/demo/public")).andExpect(status().isUnauthorized());
            mvc.perform(get("/demo/public").header("Authorization", "Bearer " + token("alice", "USER")))
                    .andExpect(status().isOk());
        } finally {
            jdbc.update("DELETE FROM endpoint_permissions WHERE http_method = 'GET' AND path = '/demo/public' AND access_type = 'ROLE'");
        }
    }

    @Test void invalidSignatureIsRejected() throws Exception {
        mvc.perform(get("/demo/plain").header("Authorization", "Bearer " + token("alice", "USER") + "x"))
                .andExpect(status().isUnauthorized());
    }

    private static String token(String subject, String tokenRole) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"iss\":\"demo\",\"sub\":\"" + subject
                + "\",\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond()
                + ",\"realm_access\":{\"roles\":[\"" + tokenRole + "\"]}}")
                .getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
        return signingInput + "." + encoder.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }
}
