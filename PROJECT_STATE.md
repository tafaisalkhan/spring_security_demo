# Project state — 2026-11-09

## Current implementation

- Java 21 / Spring Boot 3.3.13 Maven demo. Java base package: `com.spring.security.demo` in both `src/main/java` and `src/test/java`.
- Three independent timing annotations: `@TrackControllerRequestTime`, `@TrackServiceRequestTime`, and `@TrackMethodTime`. `RequestTimingFilter` measures matching synchronous servlet requests; AOP handles service marking and method timing.
- Local `demo` profile is the default. It validates short-lived HS256 test JWTs with a fixed demo-only key. `scripts/demo-token.ps1` creates tokens for `alice`, `bob`, and `ops`.
- H2 `user_roles` maps JWT `sub` to Spring Security `ROLE_` authorities through `DatabaseRoleConverter`. The token's role claim is ignored. H2 `endpoint_permissions` stores exact HTTP method/path access rules. `DatabaseEndpointAuthorizationManager` reads those rules on each request.
- `WebSecurityCustomizer` fully bypasses Spring Security for database rules marked `PUBLIC`. If both `PUBLIC` and `ROLE` exist for one method/path, `ROLE` wins. The servlet timing filter can still run on public paths.
- Four ordered security chains in `SecurityConfig`: actuator (1), `/admin/**` (2), `/mobile/**` (3), and catch-all (4). The first matching chain runs. Each protected chain validates bearer JWTs and uses the database authorization manager.
- Sample routes: `/demo/*`, `/admin/status`, `/mobile/status`, and Actuator `/actuator/health` and `/actuator/info`. `data.sql` seeds `alice` with USER, `bob` with USER and ADMIN, and `ops` with MONITOR, plus endpoint rules.
- `postman/Performance-Monitor-Demo-RBAC.postman_collection.json` has 19 requests and variables for base URL and the three test tokens.

## Verification

- `mvn clean test -q` passed after the package rename (11 tests).
- `rg` found no `com.example.monitor` references in source, README, POM, scripts, or Postman collection.

## Integration work still needed

- Replace the demo JWT decoder and fixed signing key with a real identity provider issuer/JWK configuration.
- Replace H2 and seed SQL with the application database and migrations. Keep provider subjects aligned with `user_roles.subject`.
- Review endpoint-role mappings for the actual application and decide which public endpoints truly need a full security-chain bypass. Such bypasses omit Spring Security headers and Security Context.
- Timing currently covers synchronous servlet work only; route discovery matches controller method/path, not all MVC conditions.
