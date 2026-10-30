# FreshCart Backend — Production Hardening

## Implemented

- Supported Spring Boot 3.5.16 baseline.
- Razorpay Java SDK upgraded to 1.4.10.
- BCrypt strength 12 and JWT secret/expiry fail-fast validation.
- Stateless bearer-token security with explicit CORS allowlist and no wildcard origins.
- Security headers: HSTS, frame deny, content-type protection and strict referrer policy.
- Basic in-memory rate limiting on login, registration and OTP verification. For multiple instances, replace with a shared/distributed limiter.
- Bean validation length and format limits for user, OTP, product, checkout and payment fields.
- Soft user deactivation to preserve historical orders.
- Product `reservedStock` for checkout inventory reservation.
- Payment attempts are idempotent for an unchanged cart and reserve stock before payment.
- Cart mutations are blocked during an active payment reservation.
- Pending reservations expire and are reconciled with Razorpay before stock is released. Captured-but-not-finalized payments keep their reservation so the customer can retry order finalization.
- Payment verification remains server-side and uses Razorpay signature + server-fetched payment/order data.
- Paid-order cancellation uses a dedicated Razorpay refund workflow. Direct status cancellation of paid orders is rejected.
- Flyway migration baseline for existing development schemas; production schema validation with explicit one-time baseline procedure.
- Product catalog endpoint with composed filtering/search/pagination/sort.
- Actuator health endpoint for deployment probes.
- Server-side structured error logging without exposing implementation details to clients.

## Important deployment requirements

- Use MySQL 8+ with TLS and a dedicated non-root application user.
- Set `JPA_DDL_AUTO=validate` in production.
- Keep `FLYWAY_ENABLED=true`.
- Set `APP_CORS_ALLOWED_ORIGINS` to exact HTTPS frontend origin(s).
- Use strong secret-manager/environment values for `APP_JWT_SECRET`, Razorpay, SMTP and admin credentials.
- Use Razorpay test keys in test/staging and live keys only in production.
- Put the API behind HTTPS and a reverse proxy/load balancer.
- For more than one backend instance, move the auth rate limiter and payment cleanup coordination to shared infrastructure.
- Payment reconciliation/webhooks should be monitored operationally; the current customer callback flow is protected against duplicate finalization and stale stock reservations, but a full webhook-driven settlement system is still a future operational enhancement.


### Local startup after replacing an older copy

The default development configuration allows a one-time Flyway baseline of an existing non-empty schema with no `flyway_schema_history`. After extracting this package over/replacing an older project, rebuild resources before starting: `mvnw.cmd clean spring-boot:run`. Do not launch stale `target/classes` output from the older project copy.

Production sets `spring.flyway.baseline-on-migrate=false`. Baseline an existing production schema deliberately and verify it before migrating.

The current local log reports MySQL 5.5; upgrade the database before production deployment.
