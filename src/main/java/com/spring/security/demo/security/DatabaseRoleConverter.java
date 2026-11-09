package com.spring.security.demo.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JdbcTemplate jdbc;

    public DatabaseRoleConverter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return new JwtAuthenticationToken(jwt, List.of());
        }
        List<SimpleGrantedAuthority> authorities = jdbc.queryForList(
                "SELECT role_name FROM user_roles WHERE subject = ?", String.class, subject)
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, subject);
    }
}
