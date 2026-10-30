# FreshCart Backend

FreshCart is an online grocery-store backend built with Spring Boot. The reference ZIP was used only for the architectural flow: `config`, `controllers`, `entities`, `repos`, `services`, JWT security, cart, orders, OTP and Razorpay. The domain is rebuilt around grocery products, customers, carts, payments and deliveries.

## Stack

- Java 17
- Spring Boot 3.5.16
- Spring Web
- Spring Data JPA
- Spring Security + JWT
- MariaDB
- H2 for tests
- Jakarta Validation
- JavaMailSender for OTP email
- Razorpay Java SDK 1.4.10

The Razorpay flow creates the payment order on the server, verifies the checkout signature on the server, and checks payment amount/status before creating the local grocery order.

## Project structure

```text
FreshCart-Backend/
├── pom.xml
├── .env.example
├── README.md
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/java/com/freshcart/
    │   ├── FreshCartApplication.java
    │   ├── config/
    │   │   ├── JwtFilter.java
    │   │   └── SecurityConfig.java
    │   ├── controllers/
    │   │   ├── AdminController.java
    │   │   ├── AuthController.java
    │   │   ├── CartController.java
    │   │   ├── OrderController.java
    │   │   ├── ProductController.java
    │   │   └── UserController.java
    │   ├── dto/
    │   ├── entities/
    │   ├── exceptions/
    │   ├── helpers/
    │   ├── repos/
    │   └── services/
    ├── main/resources/
    │   ├── application.properties
    │   └── application-test.properties
    └── test/java/com/freshcart/
        └── FreshCartApplicationTests.java
```

## Database setup

Create the MariaDB database once:

```sql
CREATE DATABASE freshcart;
```

Then set the environment variables in your shell, IDE run configuration, or deployment platform. Do not put real secrets into source control.

Required production values:

```text
DB_USERNAME
DB_PASSWORD
APP_JWT_SECRET
APP_ADMIN_USERNAME
APP_ADMIN_PASSWORD
APP_RAZORPAY_KEY_ID
APP_RAZORPAY_KEY_SECRET
```

For OTP email:

```text
APP_MAIL_ENABLED=true
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-app-password
```

For local development without email, leave mail disabled. To see an OTP in logs only during local development, set `APP_OTP_DEV_LOG=true`.

## Run

With Maven installed:

```bash
mvn clean spring-boot:run
```

Or with the wrapper:

```bash
./mvnw clean spring-boot:run
```

Windows:

```bat
mvnw.cmd clean spring-boot:run
```

The API starts on `http://localhost:8080` by default. Local development defaults target MariaDB 10.4 on XAMPP and the frontend origin `http://localhost:5173`.

## Authentication flow

1. `POST /auth/reg` creates a normal `USER` account. The client cannot choose the role.
2. OTP is sent to the configured email address.
3. `POST /auth/verify-otp` verifies the OTP.
4. `POST /auth/login` returns a JWT.
5. Send the token as `Authorization: Bearer <token>` for protected endpoints.

The single initial admin account is created by `DataSeeder` from `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` on startup.

## Grocery/product flow

Public catalog:

```text
GET  /product/all
GET  /product/{id}
GET  /product/search?name=milk
GET  /product/category?name=Dairy
GET  /product/brand?name=DailyFresh
GET  /product/price?min=50&max=200
```

Admin product management:

```text
GET    /admin/products
POST   /admin/products
PUT    /admin/products/{id}
DELETE /admin/products/{id}
```

`DELETE` is implemented as a soft delete (`active=false`) so historic order items keep their product reference.

## Cart flow

```text
GET    /cart/getC
GET    /cart/{cartId}/items
POST   /cart/add/{productId}?quantity=1
PUT    /cart/increase/{cartItemId}
PUT    /cart/decrease/{cartItemId}
PUT    /cart/item/{id}
DELETE /cart/item/{id}
POST   /cart/clear/{cartId}
```

All cart mutations validate ownership using the authenticated user and verify product stock.

## Payment/order flow

```text
GET  /order/MyOrders
POST /order/create-order
POST /order/place-order
DELETE /order/deleteItem/{id}
```

`POST /order/create-order` calculates the total from the server-side cart and creates a Razorpay order in INR subunits.

`POST /order/place-order` requires:

```json
{
  "razorpayOrderId": "order_xxx",
  "razorpayPaymentId": "pay_xxx",
  "razorpaySignature": "signature",
  "shipping": {
    "phone": "9999999999",
    "address": "Delivery address",
    "city": "Pune",
    "state": "Maharashtra",
    "pincode": "411001"
  }
}
```

The server verifies that the Razorpay order belongs to the current user, that the cart total has not changed, that the signature is valid, that the Razorpay payment is captured, and that the paid amount equals the server-side cart total. Stock deduction and cart clearing happen transactionally.

## Admin flow

```text
GET /admin/dashboard
GET /admin/users
DELETE /admin/users/{id}
GET /admin/orders
PUT /admin/orders/{id}/status
```

Order status values:

```text
PLACED
CONFIRMED
PACKED
OUT_FOR_DELIVERY
DELIVERED
CANCELLED
```

Cancelling an active order restores its item quantities to product stock. Delivered/cancelled orders cannot be modified through the status endpoint.

## Security fixes included

- Registration cannot accept a client-supplied role.
- Password and OTP values are never serialized in API JSON.
- Password validation actually enforces the minimum length and character classes.
- JWT parsing errors are handled without crashing requests.
- Public product reads and admin product mutations use distinct HTTP-method-aware security rules.
- Cart and order operations assert authenticated ownership.
- Money uses `BigDecimal`, not `double`/`float`.
- Stock updates and order creation are transactional and use row locking during checkout.
- Razorpay secrets are environment-backed.
- Razorpay signature, order ownership, amount and captured-payment status are checked server-side.
- A soft delete preserves products referenced by historical orders.
- Global JSON error handling is provided for validation, access, domain and unexpected errors.

## Existing database migration note

If you previously started an older FreshCart build where `users.otp` was created with a shorter VARCHAR length, run this once in MariaDB before registering users again:

```sql
ALTER TABLE users MODIFY COLUMN otp VARCHAR(100) NULL;
```

The current entity defines `otp` as `VARCHAR(100)` because the application stores a BCrypt hash of the six-digit OTP rather than the plaintext OTP.


### Existing local database / Flyway baseline

If you are upgrading an existing FreshCart database that was created before Flyway was introduced, the default development configuration enables `spring.flyway.baseline-on-migrate` at version `1`. This tells Flyway to record the existing schema as the V1 baseline and then apply only newer migrations. Flyway documents this as the supported mechanism for a non-empty schema without a schema history table.

After replacing an older project copy, **do not run an old `target/classes` build**. On Windows, from the project root run:

```powershell
.\mvnw.cmd clean spring-boot:run
```

or, if using Eclipse, run **Maven → Update Project** and then **Project → Clean** before starting the application. This is important when `target/classes` contains resources from an older build.

For production, `baseline-on-migrate` is intentionally `false`. Baseline an existing production database as an explicit deployment step after reviewing the actual schema; do not automatically baseline an arbitrary non-empty production database.

**Database requirement:** the log from the previous local run reports MariaDB 5.5. MariaDB 5.5 is not a suitable production target. Upgrade the local/production database to a currently supported MariaDB release (prefer an LTS line) before deployment.

## Production hardening notes

- Use a supported MariaDB release with TLS and a dedicated application database user. Do not use the MariaDB root account in production.
- Production schema changes are managed by Flyway; `JPA_DDL_AUTO` defaults to `validate` and should not be switched to `update` in production.
- Razorpay checkout reserves inventory for a short period, reuses an active payment attempt for an unchanged cart, blocks cart mutation while payment is pending, and releases expired/cancelled reservations.
- Payment verification remains server-side. The backend never trusts client-submitted totals or prices.
- The API uses bearer JWTs, not cookies. CSRF is therefore disabled intentionally; do not migrate to cookies without adding a CSRF strategy.
- In-memory auth rate limiting is included as a baseline safeguard. For multiple application instances, replace it with a shared/distributed limiter (for example Redis-backed).
- Public product reads expose a paginated `/product/catalog` endpoint supporting composed search, category, brand, price range, pagination, and sort.
- `/actuator/health` is exposed for deployment probes; other actuator endpoints remain unexposed.
- Set strong secret values for JWT, Razorpay and mail credentials through the environment or your secret manager.

### Production startup example

Set `SPRING_PROFILES_ACTIVE=prod`, `JPA_DDL_AUTO=validate`, `FLYWAY_ENABLED=true`, production `APP_CORS_ALLOWED_ORIGINS`, HTTPS/TLS database connectivity, and non-root database credentials.


### Flyway migration sequence

`V1__baseline.sql` creates a fresh schema matching the pre-hardening application model. `V2__production_hardening.sql` adds user lifecycle fields, checkout inventory reservations, payment recovery fields and indexes. On an existing non-Flyway database, `spring.flyway.baseline-on-migrate=true` baselines the current schema at version 1 and applies V2. Review the generated SQL against your live schema before production migration.

### Existing local database / Flyway bootstrap

FreshCart development databases created before Flyway may already contain tables but no `flyway_schema_history`. Non-production startup automatically baselines such schemas at Flyway version 1 and then applies pending migrations. Use `start-local-clean.cmd` or `mvnw.cmd clean spring-boot:run`. Do not enable automatic baselining in production; perform the one-time production baseline only after verifying the existing schema.


### Local setup without repeated file edits

The default `application.properties` is configured for local FreshCart development:

- MariaDB at `localhost:3306/freshcart`
- database user `root` with a blank local password
- admin username `admin@freshcart.com`
- local OTP logging enabled and SMTP disabled
- CORS for `localhost:5173` and `127.0.0.1:5173`
- the current Razorpay TEST key id prefilled
- legacy localhost product-image URLs repaired automatically in local development

Only secrets remain environment-backed. Set your Razorpay TEST secret once as a Windows user environment variable:

```powershell
setx APP_RAZORPAY_KEY_SECRET "YOUR_RAZORPAY_TEST_SECRET"
```

Then open a new terminal and run:

```powershell
.\start-local-clean.cmd
```

Production continues to require explicit secrets and disables automatic image repair.

### One-time Razorpay local setup

The repository intentionally does not contain the Razorpay secret. Run this once from PowerShell:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\setup-local.ps1
```

The script stores the TEST secret in your Windows user environment and does not modify source files. After that, use `start-local-clean.cmd` for normal local startup.

Local-only payment recovery: `start-local-clean.cmd` enables `APP_PAYMENT_AUTO_REFUND_ORPHANED=true`. When an expired local `PENDING` attempt has a captured Razorpay payment but no FreshCart order, the backend verifies and refunds that orphaned TEST payment, releases its inventory reservation, and creates a fresh Razorpay order. Production explicitly disables this behavior.
