package com.spring.security.demo.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
public class DatabaseEndpointAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final JdbcTemplate jdbc;

    public DatabaseEndpointAuthorizationManager(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        var request = context.getRequest();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        List<EndpointRule> rules = jdbc.query(
                "SELECT access_type, role_name FROM endpoint_permissions WHERE http_method = ? AND path = ?",
                (rs, row) -> new EndpointRule(rs.getString("access_type"), rs.getString("role_name")),
                request.getMethod(), path);

        if (rules.isEmpty()) return new AuthorizationDecision(false);
        boolean hasRoleRule = rules.stream().anyMatch(rule -> "ROLE".equals(rule.accessType()));
        if (!hasRoleRule && rules.stream().anyMatch(rule -> "PUBLIC".equals(rule.accessType())))
            return new AuthorizationDecision(true);

        Authentication current = authentication.get();
        if (current == null || !current.isAuthenticated()) return new AuthorizationDecision(false);
        boolean allowed = rules.stream()
                .filter(rule -> "ROLE".equals(rule.accessType()) && rule.roleName() != null)
                .anyMatch(rule -> current.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + rule.roleName())));
        return new AuthorizationDecision(allowed);
    }

    private record EndpointRule(String accessType, String roleName) { }
}
