# Paws & Homes — Pet Adoption Platform

A full-stack pet adoption starter with a responsive React frontend, Java Spring Boot and Express APIs, PostgreSQL database, JWT authentication, Razorpay Standard Checkout (test-mode-ready), Stripe Checkout compatibility, and email notifications.

## Features

- Browse, search, and filter adoptable pets; view each pet's profile.
- Create an account and sign in with a JWT-backed session.
- Submit an adoption application and track it in the user dashboard.
- Accept adoption contributions with Razorpay Standard Checkout. The Java API verifies payment signatures and provider-side order/payment details before recording success and marking a pet adopted.
- Send welcome, application-received, and payment-confirmation email notifications through SMTP.
- Run PostgreSQL and pgAdmin locally with Docker Compose.

## Project layout

```text
pet-adoption-platform/
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── context/
│   │   ├── App.jsx
│   │   └── main.jsx
│   └── package.json
├── backend/
│   ├── config/
│   ├── controllers/
│   ├── middleware/
│   ├── models/
│   ├── routes/
│   ├── services/
│   ├── .env.example
│   ├── server.js
│   └── package.json
├── docker-compose.yml
└── README.md
```

The database schema and sample pets are initialized automatically when the API starts.

## Run locally

Requirements: Node.js 20+, npm, and Docker Desktop (or a local PostgreSQL 16 instance).

1. Copy `backend/.env.example` to `backend/.env` and set a unique `JWT_SECRET` (at least 32 characters). For Docker Compose, use the default local database URL in the example; Compose overrides its hostname and password for the backend container.
2. Start the database and pgAdmin:

   ```sh
   docker compose up -d db pgadmin
   ```

3. Install and start the API:

   ```sh
   cd backend
   npm install
   npm run dev
   ```

4. In another terminal, install and start the frontend:

   ```sh
   cd frontend
   npm install
   npm run dev
   ```

5. Open `http://localhost:5173`. The API health check is at `http://localhost:5000/api/health`.

pgAdmin is available at `http://localhost:5050`. Sign in with `admin@example.com` / `change-me` (or your Compose overrides), then register a server with host `localhost`, port `5432`, database `pet_adoption`, username `postgres`, and the configured PostgreSQL password.

## Payments (Stripe test mode)

Set `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` in `backend/.env`. The webhook endpoint is `POST /api/payments/webhook`. For local testing, install Stripe CLI and forward events:

```sh
stripe listen --forward-to localhost:5000/api/payments/webhook
```

Use the webhook signing secret printed by Stripe CLI. Test Checkout with Stripe's test cards; never use a real secret key for local development. Checkout is intentionally unavailable until a secret key is configured.

## Email

Set `SMTP_HOST`, `SMTP_PORT`, `SMTP_SECURE`, `SMTP_USER`, `SMTP_PASS`, and optionally `MAIL_FROM` to a trusted SMTP provider. When SMTP is not configured, notification delivery is skipped with a server log; mail delivery errors are logged without losing a successful application or payment.

## Run everything in containers

Create `backend/.env` as described above, set a strong `POSTGRES_PASSWORD` and non-default pgAdmin credentials in your shell or a root `.env` file, then:

```sh
docker compose up --build
```

The frontend is served at `http://localhost:8080`, the API at `http://localhost:5000`, and pgAdmin at `http://localhost:5050`. `VITE_API_URL` is embedded into the frontend at image build time. For a public deployment, set it to the public API URL, set `FRONTEND_URL` to the public frontend origin, use a managed PostgreSQL database, enable `PGSSL=true` if required by the provider, configure Stripe's production webhook, and store all secrets in the hosting provider's secret manager. Do not expose PostgreSQL or pgAdmin publicly.

## API overview

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/api/health` | Public | Health check |
| GET | `/api/pets` | Public | List available pets (`species`, `search`, `limit` optional) |
| GET | `/api/pets/:id` | Public | Pet details |
| POST | `/api/auth/signup` | Public | Create account |
| POST | `/api/auth/login` | Public | Sign in |
| GET | `/api/auth/me` | JWT | Current user |
| POST | `/api/adoptions` | JWT | Submit application |
| GET | `/api/adoptions/mine` | JWT | List own applications |
| POST | `/api/payments/razorpay/orders` | JWT | Create or reuse an adoption payment order |
| POST | `/api/payments/razorpay/verify` | JWT | Verify and record a captured payment |
| POST | `/api/payments/razorpay/failure` | JWT | Confirm a failed payment with Razorpay |
| POST | `/api/payments/razorpay/cancel` | JWT | Record a dismissed checkout |
| POST | `/api/payments/checkout` | JWT | Create Stripe Checkout session |
| POST | `/api/payments/webhook` | Stripe signature | Process Checkout completion |

`backend/.env` and credentials are intentionally excluded from version control. Replace the demo credentials used by Compose before any shared or deployed use.

## Java Spring Boot backend

The separate `backend-java/` project is a Java 21 / Spring Boot 4 Maven API. It does not replace or modify the existing Node.js backend or React frontend. The Spring API listens on port `5000`, the same local API port used by the frontend.

Set the PostgreSQL password in the VS Code PowerShell terminal (do not put it in `application.properties`):

```powershell
$env:DB_PASSWORD = "<your PostgreSQL password>"
```

The default connection uses `jdbc:postgresql://localhost:5432/pet_adoption_db` and username `postgres`. If needed, set `DB_URL` and `DB_USERNAME` in the same terminal. From the `backend-java` directory, build or run with the included Maven Wrapper:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

The Pet CRUD API is available at `http://localhost:5000/api/pets` (`GET`, `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`). Pet status values are `AVAILABLE`, `PENDING`, and `ADOPTED`; request validation errors return HTTP 400, missing pets return HTTP 404, successful creation returns HTTP 201, and successful deletion returns HTTP 204. Hibernate uses `ddl-auto=update` to create/update the `pets` table without dropping existing tables or rows. The PostgreSQL connection is established at application startup, so the password environment variable and PostgreSQL service must be available first.

On startup, the Java backend adds sample dogs and cats when matching name-and-breed records are not already present. It assigns Indian city/state locations to new samples and fills only blank locations on existing pets; populated locations and all other pet data are left unchanged. This initializer is safe to run repeatedly and does not delete existing pets.

Adoption contributions are set by the Java API in INR: cats ₹2,000, small dogs ₹3,000, and medium or large dogs ₹5,000. The API derives the amount from the pet's breed on create/update and does not trust a client-supplied fee. On startup, existing pet fee values are safely updated to match these rates; pet records and locations are preserved.

Adoption applications are stored by the Java API and require a signed-in user. The React adoption flow uses Razorpay Standard Checkout; the existing Stripe API remains available for compatibility. Razorpay payment details are persisted in the new `razorpay_payments` table. Hibernate's `ddl-auto=update` adds the table without deleting or rewriting existing users, pets, adoption applications, or payment records.

### Razorpay Standard Checkout (Test Mode)

In the Razorpay Dashboard, enable **Test Mode** and generate a test Key ID and Key Secret. Set them in the PowerShell terminal that will start Spring Boot; the Key Secret is used only by the backend and is never returned to the frontend:

```powershell
$env:RAZORPAY_KEY_ID = "rzp_test_REPLACE_WITH_TEST_KEY_ID"
$env:RAZORPAY_KEY_SECRET = "REPLACE_WITH_TEST_KEY_SECRET"
Set-Location "D:\fswe_project 2\backend-java"
.\mvnw.cmd spring-boot:run
```

In another terminal, run the frontend with `npm run dev` from `frontend`, sign in, choose a pet, and submit an adoption note. The page creates an adoption application, asks the Java API to create the INR order using the server-side pet fee, then opens Razorpay Checkout. The backend verifies Razorpay's HMAC-SHA256 checkout signature, fetches the order and payment directly from Razorpay, checks the order/payment IDs, amount, currency, and captured state, and only then marks the payment `SUCCESS`, the adoption `PAID`, and the pet `ADOPTED`. Authorized payments are captured server-side before success is recorded. Failed and dismissed checkouts are recorded as `FAILED` and `CANCELLED`; neither state marks the adoption paid.

Payment endpoints below require `Authorization: Bearer <JWT>` from `/api/auth/login`. For Postman, choose **Authorization → Bearer Token** and provide a signed-in user's token:

1. Create (or reuse) the server-priced order:

   `POST http://localhost:5000/api/payments/razorpay/orders`

   ```json
   { "adoptionId": 16 }
   ```

   The response contains `keyId`, `orderId`, `amount` (paise), `currency`, and Checkout display fields. The `adoptionId` must belong to the authenticated user.

2. Verify a successful Checkout payment:

   `POST http://localhost:5000/api/payments/razorpay/verify`

   ```json
   {
     "orderId": "order_id_returned_by_create_order",
     "paymentId": "payment_id_returned_by_razorpay_checkout",
     "signature": "razorpay_signature_returned_by_razorpay_checkout"
   }
   ```

   Use the three exact values returned by Razorpay Checkout; a fabricated signature is rejected. Do not send an amount or status—the API obtains and validates those from Razorpay.

3. Record an unsuccessful attempt after Razorpay's `payment.failed` callback:

   `POST http://localhost:5000/api/payments/razorpay/failure`

   ```json
   {
     "orderId": "order_id_returned_by_create_order",
     "paymentId": "failed_payment_id_from_checkout_error_metadata"
   }
   ```

   The API independently fetches the payment and records `FAILED` only if Razorpay confirms it failed.

4. Record a user-dismissed Checkout modal:

   `POST http://localhost:5000/api/payments/razorpay/cancel`

   ```json
   { "orderId": "order_id_returned_by_create_order" }
   ```

To test a successful card payment, use Razorpay's test card `4100 2800 0000 1007`, any random CVV, a future expiry date, and a 4–10 digit OTP on the mock bank page. To test a decline, use test card `4100 2800 0006 0003` and choose the failure action when prompted. Dismiss the Checkout modal to test `CANCELLED`. See [Razorpay's test card guide](https://razorpay.com/docs/payments/payments/test-card-details/) for current test scenarios. Test credentials and cards do not move real money; never put the Key Secret into Vite variables or frontend code.

### Development-only Brevo email test

The temporary `POST /api/test-email` endpoint is registered only when the Spring `dev` profile is active and requires a JWT for a user with the `ADMIN` role. Configure Brevo SMTP credentials in the backend process environment:

```powershell
$env:MAIL_USERNAME = "your-brevo-smtp-login"
$env:MAIL_PASSWORD = "your-brevo-smtp-key"
$env:MAIL_FROM = "your-verified-sender@example.com"
$env:SPRING_PROFILES_ACTIVE = "dev"
Set-Location "D:\fswe_project 2\backend-java"
.\mvnw.cmd spring-boot:run
```

In Postman, choose **Authorization → Bearer Token** and use an administrator JWT, then send:

`POST http://localhost:5000/api/test-email`

```json
{ "recipient": "recipient@example.com" }
```

The endpoint returns a success response or a generic delivery-failure response; SMTP credentials and provider exception details are never returned or logged. Remove `SPRING_PROFILES_ACTIVE=dev` or use another profile to disable this test endpoint. Do not enable the development profile in production.

`MAIL_USERNAME` must be the SMTP login shown in Brevo SMTP settings, `MAIL_PASSWORD` must be the SMTP key (not the Brevo API key), and `MAIL_FROM` must be an email sender verified in Brevo. `MAIL_FROM` defaults to `MAIL_USERNAME` only when it is not set. Restart Spring Boot after changing environment variables.

### User registration, login, and JWT

The Java API requires a `JWT_SECRET` environment variable. Generate a fresh 48-byte secret in PowerShell (the generated value is only assigned to the current terminal environment):

```powershell
$bytes = [byte[]]::new(48)
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = "base64:$([Convert]::ToBase64String($bytes))"
$env:JWT_EXPIRATION_MS = "900000"
```

Keep the same terminal open while running the backend so its environment variables are inherited. The secret must decode to at least 256 bits. JWTs are signed with HMAC-SHA and expire after 15 minutes by default.

Register with `POST /api/auth/register` (the existing React client can also use `POST /api/auth/signup`):

```json
{
  "fullName": "Jamie Parker",
  "email": "jamie@example.com",
  "password": "choose-a-long-password",
  "phoneNumber": "555-0100"
}
```

Registration creates a `USER` account and returns a bearer token. The registration API accepts either `fullName` or the existing React client's `name` field. The sign-in page lets users choose Adopter or Administrator; `POST /api/auth/login` accepts `email`, `password`, and the selected `role`, then verifies the selection against the account's stored role. A mismatch is rejected, and older clients may omit `role`. `GET /api/auth/me` returns the authenticated user profile. Responses include both `fullName` and `name` for compatibility with the current React UI. Passwords are stored as BCrypt hashes; password hashes are never returned by the API. Duplicate email returns HTTP 409 and invalid input returns HTTP 400.

Public clients can use `GET /api/pets` and `GET /api/pets/{id}` to view available pets. Creating, updating, and deleting pets require an `ADMIN` bearer token. New accounts cannot request or self-assign the admin role. To grant administrator access to a trusted account, first register it, then use pgAdmin's Query Tool to run the following statement with that account's email:

```sql
UPDATE app_users
SET role = 'ADMIN'
WHERE email = lower(trim('trusted-admin@example.com'));
```

The `app_users` table is added by Hibernate's non-destructive `ddl-auto=update`; existing pet records, including Bruno, are not deleted or rewritten by this change. Run the authentication integration tests with `.\mvnw.cmd test` from `backend-java`; tests use an isolated in-memory H2 database and do not alter PostgreSQL data.
