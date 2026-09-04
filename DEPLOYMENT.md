# EduChess FYP Deployment Guide

Target host: **Railway**, deploying the `Dockerfile`. §2 covers moving to any other
Docker host.

---

## 0. Deploy to Railway

Railway builds the multi-stage `Dockerfile` (Stockfish is baked into the image) on its
own build machines. The build runs in the container, so the app's fat-JAR packaging is
never constrained by the runtime instance size.

### 0.1 Create the project

1. https://railway.app → **New Project** → **Deploy from GitHub repo** → select this repo.
2. Railway reads `railway.json` and builds with the `Dockerfile`. No build/start command needed.
3. `PORT` is injected automatically — do **not** set it yourself.

### 0.2 Add a MySQL database

1. In the same project: **New** → **Database** → **Add MySQL**.
2. This creates a `MySQL` service exposing `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`,
   `MYSQLUSER`, `MYSQLPASSWORD` on the private network.

### 0.3 Set variables on the web service

**Minimum to boot** (set these before the first deploy): `DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`. The app starts without any
Stripe or `APP_BASE_URL` values — payments are an optional integration and only the
Checkout button is disabled until they are set. So: deploy → generate the domain
(§0.4) → come back and add `APP_BASE_URL` + the Stripe vars → redeploy.

Use Railway reference syntax so the DB values track the database service:

| Variable | Value |
| --- | --- |
| `DATABASE_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DATABASE_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `DATABASE_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `JWT_SECRET` | a long random string (`openssl rand -base64 48`) |
| `SPRING_PROFILES_ACTIVE` | `prod` (already defaulted in the image; set to override) |
| `APP_BASE_URL` | your public URL once known, e.g. `https://educhess-fyp.up.railway.app` (add after §0.4) |
| `STOCKFISH_PATH` | leave unset (image default `/usr/games/stockfish`) |
| `STRIPE_SECRET_KEY` | `sk_test_...` (sandbox) or `sk_live_...`. Checkout is disabled if unset. |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` from the Stripe webhook you create (see §0.6). Without it, payments only confirm when the browser returns from Checkout. |
| `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` | optional; derived from `APP_BASE_URL` if unset |
| `RECEIPT_ISSUER_NAME`, `RECEIPT_ISSUER_REG_NO`, `RECEIPT_ISSUER_ADDRESS`, `RECEIPT_ISSUER_EMAIL`, `RECEIPT_ISSUER_PHONE` | printed on the PDF receipt. Fill these for a receipt that is usable as a record of payment. |
| `RECEIPT_TAX_NOTE` | optional free-text line on the receipt (e.g. a tax-relief reference). You are responsible for its accuracy — the system does not verify tax deductibility. |
| `PUZZLE_AI_BASE_URL` | optional; Puzzle AI disabled if unset |
| `PUZZLE_VISION_SCRIPT`, `PUZZLE_VISION_MODEL` | **leave unset** — the YOLO/OpenCV pipeline is not in the container |

### 0.4 Networking & health

- **Settings → Networking → Generate Domain** to get a public URL, then set `APP_BASE_URL`
  to it and redeploy.
- Health check is `GET /` (configured in `railway.json`). The app only becomes healthy
  once MySQL is reachable, so add the database and its variables before the first deploy.

### 0.5 File uploads (optional, for persistence)

`PUZZLE_UPLOAD_STORAGE_DIR` / `PUZZLE_RECOGNITION_STORAGE_DIR` default to `uploads/...`
inside the container and are lost on redeploy. For durable storage, add a **Volume**
in Railway (Settings → Volumes), mount it at e.g. `/data`, and set both dirs under `/data`.

### 0.6 Payments (Stripe) and receipts

Students pay through **Stripe Checkout** (`POST /api/payments/{id}/checkout` → redirect to
`checkout.stripe.com`). Stripe **test mode** is the sandbox — no separate setup.

1. **Get keys:** Stripe Dashboard → Developers → API keys → copy the **test** secret key
   into `STRIPE_SECRET_KEY`. Set `APP_BASE_URL` to your Railway domain.
2. **Create the webhook:** Developers → Webhooks → Add endpoint
   - URL: `https://<your-domain>/api/payments/webhook`
   - Events: `checkout.session.completed`, `checkout.session.async_payment_succeeded`
   - Copy the **Signing secret** (`whsec_...`) into `STRIPE_WEBHOOK_SECRET`.
   The webhook is what confirms a payment if the buyer closes the tab before returning.
   The success redirect (`/api/payments/checkout/success`) also confirms, and both paths
   are idempotent.
3. **Test the flow:** in the app, click **Pay Now**, use card `4242 4242 4242 4242`,
   any future expiry, any CVC. You are redirected back and the payment shows **PAID**.
   For FPX (Malaysian online banking) enable it in the Stripe Dashboard and use the test
   bank flow — no code change needed.
4. **Local webhook testing:** `stripe listen --forward-to localhost:8080/api/payments/webhook`
   (Stripe CLI) prints a `whsec_...` to use as `STRIPE_WEBHOOK_SECRET` in dev.

**Receipts.** Once PAID, `GET /api/payments/{id}/receipt` returns JSON and
`GET /api/payments/{id}/receipt/pdf` returns a PDF with an immutable receipt number
(`EDU-<year>-<id>`), the issuer block from the `RECEIPT_ISSUER_*` vars, an itemised
line, totals, and the payment method. **Malaysian e-Invoice (MyInvois/LHDN) is not
integrated** — the PDF is a conventional official receipt, not a validated e-Invoice.
`POST /api/payments/{id}/pay` records an off-gateway payment and is **admin-only**.

---

## 1. Build and run locally

### Run locally with dev profile
```powershell
./mvnw clean package -DskipTests
java -jar target/fyp-0.0.1-SNAPSHOT.jar
```

The application loads `application-dev.properties` by default because `application.properties` sets:
```properties
spring.profiles.active=dev
```

### Local dev configuration
Local configuration lives in `src/main/resources/application-dev.properties` and includes:
- local MySQL at `jdbc:mysql://localhost:3306/fypdb`
- local Stockfish path
- local puzzle AI base URL
- local Stripe callback URLs
- local upload and recognition storage paths

## 2. Deploying to another host

Any platform that builds a `Dockerfile` works the same way as Railway (§0): point it at
the repo, let it build the image, and provide the environment variables from §0.3 and
§0.6. The container reads `PORT`, installs Stockfish itself, and defaults to the `prod`
profile. For a managed MySQL such as Aiven, set `DATABASE_URL` to its JDBC URL including
`sslMode=REQUIRED`:

```text
jdbc:mysql://your-host:3306/your-db?useSSL=true&requireSSL=true&sslMode=REQUIRED&serverTimezone=UTC
```

## 4. Production profile and environment variables
The production configuration is in `src/main/resources/application-prod.properties`.
The container sets `SPRING_PROFILES_ACTIVE=prod` by default (see the `Dockerfile`).

If you want to activate production explicitly, set:
```text
SPRING_PROFILES_ACTIVE=prod
```

## 5. Build for production

Railway builds this for you from the `Dockerfile`. To build the JAR locally:
```powershell
./mvnw clean package -DskipTests
```

## 6. Run locally with production settings
Set environment variables and run:
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:mysql://localhost:3306/fypdb?useSSL=false&serverTimezone=UTC"
$env:DATABASE_USERNAME="root"
$env:DATABASE_PASSWORD="admin"
$env:JWT_SECRET="your-secret"
java -jar target/fyp-0.0.1-SNAPSHOT.jar
```

## 7. Notes
- The app preserves static frontend pages under `src/main/resources/static/`.
- Static routes such as `/`, `/login.html`, `/dashboard.html`, `/analysis.html`, `/puzzle-library.html`, `/know-our-coaches.html`, and `/admin-class-applications.html` are served by Spring Boot static resource handling.
- Do not hardcode secrets or local database credentials in production.
- Provide all production settings as environment variables on the host (Railway's
  MySQL service, or a managed instance such as Aiven).
