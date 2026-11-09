# Three independent timing annotations — Java 21 / Spring Boot 3.3.13

This standalone demo combines request timing with local JWT and database-backed RBAC. It is not a copy of your actual Keycloak/RBAC common library.

## Run
Requires JDK 21 and Maven 3.9+. The local token helper uses PowerShell.

```bash
mvn clean test
mvn spring-boot:run
curl http://localhost:8080/demo/public
# In another shell, create a one-hour local token and send it as a bearer token:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/demo-token.ps1 -Subject alice
curl -H "Authorization: Bearer <printed-token>" http://localhost:8080/demo/controller
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/demo-token.ps1 -Subject bob
curl -H "Authorization: Bearer <printed-token>" http://localhost:8080/demo/all
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/demo-token.ps1 -Subject ops
curl -H "Authorization: Bearer <printed-token>" http://localhost:8080/actuator/health
```

## RBAC example

`SecurityConfig` delegates every request to `DatabaseEndpointAuthorizationManager`. Its exact HTTP method and path rules come from `endpoint_permissions`:

| Endpoint | Rule |
| --- | --- |
| `GET /demo/public` | `PUBLIC` |
| `GET /demo/all` | `ADMIN` |
| Other four `GET /demo/*` endpoints | `USER` |
| `GET /admin/status` | `ADMIN` |
| `GET /mobile/status` | `USER` |
| `GET /actuator/health`, `GET /actuator/info` | `MONITOR` |
| Every other request | Denied (no matching database rule) |

To make another endpoint public, insert a row with `access_type = 'PUBLIC'` and an empty `role_name`. For a protected endpoint, use `access_type = 'ROLE'` and the required role name without the `ROLE_` prefix. Rules take effect on the next request. `DatabasePublicEndpointMatcher` makes PUBLIC routes bypass the entire Spring Security filter chain, including JWT validation and SecurityContext setup. A missing or invalid bearer token does not block them. If the same method and path have both PUBLIC and ROLE rows, ROLE wins and the route stays protected. The separate servlet timing filter still runs. Spring Security headers are also skipped on fully public routes; use this full bypass only where that behavior is intended. The demo uses exact method and path matching, not wildcard patterns. Unknown routes are denied.

The demo profile is the default. It validates local HS256 test tokens with the shared key in `DemoJwtConfig`. `DatabaseRoleConverter` reads roles from `user_roles` using the token `sub` value and places `ROLE_` authorities in the Spring Security authentication/context. The token's `realm_access.roles` claim is deliberately ignored. Seed rows are `alice` = `USER`, `bob` = `USER`, `ADMIN`, and `ops` = `MONITOR`. User roles and endpoint rules are read on each request, so database changes affect the next request.

## Security filter chains

`SecurityConfig` defines four ordered chains. The first matching chain handles the request:

| Order | Matcher | Purpose |
| --- | --- | --- |
| 1 | Actuator endpoints | JWT plus database rule, e.g. `MONITOR` for health/info |
| 2 | `/admin/**` | JWT plus database admin rules |
| 3 | `/mobile/**` | JWT plus database mobile rules |
| 4 | All remaining requests | JWT plus remaining database rules |

The `WebSecurityCustomizer` public matcher runs before these chains. It bypasses them for database-marked PUBLIC routes. The servlet timing filter is separate from Spring Security and still runs for annotated routes. The actuator dependency adds actual `/actuator/health` and `/actuator/info` endpoints; both are exposed and require a `MONITOR` database role here.

For a real identity provider, activate a non-demo profile, remove the local decoder and shared key, configure Spring Security resource-server JWT validation using your provider's issuer/JWK URL, and replace the H2 datasource and seed SQL with your application database. The stable provider subject must match your user-role records. Never use the demo signing key outside local testing.

## Postman

Import `postman/Performance-Monitor-Demo-RBAC.postman_collection.json`. Run the app, generate Alice, Bob, and Ops tokens with the PowerShell commands above, and paste them into the collection variables `aliceToken`, `bobToken`, and `opsToken`. The collection covers the demo, admin, mobile, and actuator endpoints plus 401/403 checks. Its requests assert their expected HTTP status.

## Three independent annotations
- `@TrackControllerRequestTime`: apply to controller endpoint. Filter matches its route before security and logs complete synchronous servlet-chain time, even for a security rejection on the matched route.
- `@TrackServiceRequestTime`: apply to service method. Filter starts a timer for every HTTP request while enabled; AOP marks requests that actually execute an annotated service. The filter logs total synchronous request time, including preceding security. A 401/403 before the service executes cannot be associated with that service annotation.
- `@TrackMethodTime`: apply to any proxied Spring method. Aspect logs method execution duration, including synchronous downstream calls. Does not require an HTTP request.

Config in `application.yml`: `monitoring.controller-request-enabled`, `monitoring.service-request-enabled`, `monitoring.method-enabled`.

Expected logs: `/demo/controller` and `/demo/public` REQUEST controllerMatched=true; `/demo/service` REQUEST serviceMatched=true; `/demo/method` METHOD only; `/demo/all` REQUEST controllerMatched=true serviceMatched=true plus METHOD; `/demo/plain` no timing log. Rejected requests to annotated controller routes are also measured.

## Security integration
`RequestTimingFilter` is a `@Component` with `@Order(HIGHEST_PRECEDENCE)`; it runs before the Spring Security filter chain and must not be registered a second time. `SecurityConfig` bypasses PUBLIC routes and invokes the database authorization manager for other routes; the timing filter does not make authorization decisions. Tests verify 401, 403, public access even with an invalid token, SecurityContext authorities, dynamic endpoint rules, and that database roles override the dummy token's role claim. The annotation registry matches HTTP method and path pattern, not every MVC condition (e.g., headers or params).

## Limitations
Synchronous servlet requests only; async/SSE need async-completion handling. AOP requires proxy traversal (same-class self-invocation is not intercepted). Service-triggered request monitoring adds a small per-request timer/attribute cost while enabled. If an annotated service is called outside an HTTP request, `@TrackServiceRequestTime` does not log a request. Timings can overlap and must not be summed. Use sampling/asynchronous logging in high-throughput production systems.
