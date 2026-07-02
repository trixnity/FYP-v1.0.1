# Render + Aiven Deployment Guide for EduChess FYP

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

## 2. Render deployment

### Render service setup
1. Create a new Web Service on Render.
2. Connect your Git repository.
3. Use `render.yaml` in the repo to define the service.

### Render build and start commands
- Build: `./mvnw clean package -DskipTests`
- Start: `java -jar target/*.jar`
- Health check path: `/`

### Render environment variables
Set these variables in the Render dashboard:
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `APP_BASE_URL` (e.g. `https://your-app.onrender.com`)
- `STRIPE_SECRET_KEY`
- `STRIPE_SUCCESS_URL` (optional if APP_BASE_URL is set)
- `STRIPE_CANCEL_URL` (optional if APP_BASE_URL is set)
- `PUZZLE_AI_BASE_URL` (optional; if unset, Puzzle AI is disabled)
- `STOCKFISH_PATH` (optional; defaults to `/usr/games/stockfish` in the Docker runtime)
- `PUZZLE_VISION_SCRIPT` (optional; if using vision pipeline)
- `PUZZLE_VISION_MODEL` (optional; if using vision pipeline)
- `PUZZLE_UPLOAD_STORAGE_DIR` (optional)
- `PUZZLE_RECOGNITION_STORAGE_DIR` (optional)
- `PORT` (optional; defaults to `8080`)

## 3. Aiven MySQL connection
Use environment variables provided by Aiven.

For Render, set:
- `DATABASE_URL` as the JDBC URL from Aiven, including `sslMode=REQUIRED`
- `DATABASE_USERNAME` as the Aiven database user
- `DATABASE_PASSWORD` as the Aiven database password

Example Aiven JDBC URL:
```text
jdbc:mysql://your-host:3306/your-db?useSSL=true&requireSSL=true&sslMode=REQUIRED&serverTimezone=UTC
```

## 4. Production profile and environment variables
The production configuration is in `src/main/resources/application-prod.properties`.
Render automatically uses the `prod` profile when `SPRING_PROFILES_ACTIVE=prod` is set.

If you want to activate production explicitly, set:
```text
SPRING_PROFILES_ACTIVE=prod
```

## 5. Build for production

On Render or locally with production profile:
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
- Use Aiven for the managed MySQL instance and Render environment variables for production settings.
