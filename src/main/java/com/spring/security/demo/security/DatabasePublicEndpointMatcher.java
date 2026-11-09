package com.spring.security.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class DatabasePublicEndpointMatcher implements RequestMatcher {
    private final JdbcTemplate jdbc;

    public DatabasePublicEndpointMatcher(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM endpoint_permissions p " +
                        "WHERE p.http_method = ? AND p.path = ? AND p.access_type = 'PUBLIC' " +
                        "AND NOT EXISTS (SELECT 1 FROM endpoint_permissions r " +
                        "WHERE r.http_method = p.http_method AND r.path = p.path AND r.access_type = 'ROLE')",
                Integer.class, request.getMethod(), path);
        return count != null && count > 0;
    }
}
