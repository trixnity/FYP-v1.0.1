# EduChess FYP Deployment Guide

Target host: **Koyeb** (free tier), deploying the `Dockerfile`. §2 covers moving to any
other Docker host.

---

## 0. Deploy to Koyeb

The multi-stage `Dockerfile` builds the app and bakes in Stockfish. Koyeb has **no
managed MySQL**, so the database is external (Aiven free plan below).

### 0.1 Database — Aiven MySQL (free)

1. https://aiven.io → **Create service** → **MySQL** → **Free plan** → pick a region.
2. From the service overview copy **Host**, **Port**, **User**, **Password**, **Database**.
3. Your JDBC URL is:
   `jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED&serverTimezone=UTC`

(Any MySQL 8 works — PlanetScale, TiDB Cloud Serverless, a VPS. Keep `sslMode=REQUIRED`
for a hosted DB.)

### 0.2 Create the Koyeb service

**Option A — Koyeb builds the Dockerfile (simplest):**

1. https://app.koyeb.com → **Create Web Service** → **GitHub** → select this repo, branch `main`.
2. Builder: **Dockerfile** (auto-detected). No build/run command needed.
3. **Instance:** Free (`nano`). **Regions:** one is fine.
4. **Exposed port:** `8080`, protocol HTTP. **Health check:** HTTP path `/`.
5. Add the environment variables in §0.3, then **Deploy**.

**Option B — deploy the prebuilt image (use if Option A's build fails or is slow):**

`.github/workflows/publish-image.yml` builds the image on GitHub's runners and pushes it
to `ghcr.io/trixnity/fyp-v1.0.1:latest` on every push to `main`.

1. Run the workflow once (push to `main`, or Actions → *Publish container image* → Run).
2. GitHub → repo **Packages** → the image → **Package settings** → set visibility
   **Public** (or, to keep it private, add Koyeb registry credentials with a GHCR PAT).
3. Koyeb → **Create Web Service** → **Docker image** → `ghcr.io/trixnity/fyp-v1.0.1:latest`.
4. Same port / health check / env vars as Option A.

### 0.3 Environment variables

**Minimum to boot:** `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`,
`JWT_SECRET`. The app starts without any Stripe or `APP_BASE_URL` values — payments are
optional and only the Checkout button is disabled until they are set. So: deploy → copy
the Koyeb URL → add `APP_BASE_URL` + Stripe vars → redeploy.

| Variable | Value |
| --- | --- |
| `DATABASE_URL` | `jdbc:mysql://<aiven-host>:<port>/<db>?sslMode=REQUIRED&serverTimezone=UTC` |
| `DATABASE_USERNAME` | Aiven user (usually `avnadmin`) |
| `DATABASE_PASSWORD` | Aiven password |
| `JWT_SECRET` | a long random string (`openssl rand -base64 48`) |
| `SPRING_PROFILES_ACTIVE` | `prod` (already the image default; set only to override) |
| `APP_BASE_URL` | your Koyeb URL once known, e.g. `https://educhess-fyp-<org>.koyeb.app` |
| `STOCKFISH_PATH` | leave unset (image default `/usr/games/stockfish`) |
| `STRIPE_SECRET_KEY` | `sk_test_...` (sandbox) or `sk_live_...`. Checkout is disabled if unset. |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` from the Stripe webhook (see §0.5). Without it, payments only confirm when the browser returns from Checkout. |
| `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` | optional; derived from `APP_BASE_URL` if unset |
| `RECEIPT_ISSUER_NAME`, `RECEIPT_ISSUER_REG_NO`, `RECEIPT_ISSUER_ADDRESS`, `RECEIPT_ISSUER_EMAIL`, `RECEIPT_ISSUER_PHONE` | printed on the PDF receipt. Fill these for a usable record of payment. |
| `RECEIPT_TAX_NOTE` | optional free-text line on the receipt. You are responsible for its accuracy — the system does not verify tax deductibility. |
| `PUZZLE_AI_BASE_URL` | optional; Puzzle AI disabled if unset |
| `PUZZLE_VISION_SCRIPT`, `PUZZLE_VISION_MODEL` | **leave unset** — the YOLO/OpenCV pipeline is not in the container |

### 0.4 Notes for the free instance

- The free `nano` instance is ~512 MB RAM. The image already sets
  `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC`; if the app is killed for OOM, move to a
  paid instance or disable heavier features.
- **Uploads are not persistent.** `PUZZLE_UPLOAD_STORAGE_DIR` /
  `PUZZLE_RECOGNITION_STORAGE_DIR` default to `uploads/...` in the container and reset on
  every deploy. For durable files, point them at object storage (out of scope here).
- The app only reports healthy once MySQL is reachable — create the Aiven DB first.

### 0.5 Payments (Stripe) and receipts

Students pay through **Stripe Checkout** (`POST /api/payments/{id}/checkout` → redirect to
`checkout.stripe.com`). Stripe **test mode** is the sandbox — no separate setup.

1. **Get keys:** Stripe Dashboard → Developers → API keys → copy the **test** secret key
   into `STRIPE_SECRET_KEY`. Set `APP_BASE_URL` to your Koyeb URL.
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

Any platform that builds a `Dockerfile` (Fly.io, Render in Docker mode, Cloud Run, a
VPS) works like §0: point it at the repo or the `ghcr.io` image, provide the §0.3
environment variables, expose port `8080`, health-check `/`. The container reads `PORT`,
installs Stockfish itself, and defaults to the `prod` profile. Keep `sslMode=REQUIRED`
in `DATABASE_URL` for any hosted MySQL:

```text
jdbc:mysql://your-host:3306/your-db?useSSL=true&requireSSL=true&sslMode=REQUIRED&serverTimezone=UTC
```

## 3. Production profile and environment variables
The production configuration is in `src/main/resources/application-prod.properties`.
The container sets `SPRING_PROFILES_ACTIVE=prod` by default (see the `Dockerfile`).

If you want to activate production explicitly, set:
```text
SPRING_PROFILES_ACTIVE=prod
```

## 4. Build for production

The host (or the `publish-image.yml` workflow) builds this from the `Dockerfile`. To
build the JAR locally:
```powershell
./mvnw clean package -DskipTests
```

## 5. Run locally with production settings
Set environment variables and run:
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:mysql://localhost:3306/fypdb?useSSL=false&serverTimezone=UTC"
$env:DATABASE_USERNAME="root"
$env:DATABASE_PASSWORD="admin"
$env:JWT_SECRET="your-secret"
java -jar target/fyp-0.0.1-SNAPSHOT.jar
```

## 6. Notes
- The app preserves static frontend pages under `src/main/resources/static/`.
- Static routes such as `/`, `/login.html`, `/dashboard.html`, `/analysis.html`, `/puzzle-library.html`, `/know-our-coaches.html`, and `/admin-class-applications.html` are served by Spring Boot static resource handling.
- Do not hardcode secrets or local database credentials in production.
- Provide all production settings as environment variables on the host; use a managed
  MySQL instance such as Aiven.
