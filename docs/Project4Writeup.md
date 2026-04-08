# Project 4 — Requirements Writeup

**Student:** Yunqi Dong  
**Andrew ID:** yunqid  

This document maps each graded requirement **(1, 2, 4, 5, 6, 7)** to the implementation. Per the assignment instructions, **requirement 3 (Handle error conditions)** is not documented here.

---

## Requirement 1 — Native Android application

**a. At least three different kinds of Views**  
The main screen layout `android-app/app/src/main/res/layout/activity_main.xml` includes multiple `android.view.View` subclasses, including **ImageView**, **TextView**, **EditText**, **Button**, and **ScrollView** (which contains another **TextView** for results).

**b. User input**  
The user enters a job search **keyword** in `EditText` `keyword_input`.

**c. HTTP request to my web service**  
`MainActivity` (`android-app/app/src/main/java/edu/cmu/heinz/project4/MainActivity.java`) issues an **`HttpURLConnection` GET** to  
`{BASE_URL}api/jobs?keyword=...&device=...`, where `BASE_URL` is set at build time in `android-app/app/build.gradle` as `BuildConfig.BASE_URL` (your public GitHub Codespaces URL, ending with `/`).

**d. Parse XML or JSON from the web service**  
The app reads the response body and parses it with **`org.json.JSONObject` / `JSONArray`**, expecting JSON shaped like `{ "jobs": [ ... ] }` or `{ "error": "..." }`.

**e. Display new information**  
Matching jobs are formatted (title, company, location, URL) and shown in the **results** `TextView`.

**f. Repeatable use**  
The user can run multiple searches from the same activity without restarting the app (each tap on **Search** calls `runSearch()` again).

---

## Requirement 2 — Web service

**a. Simple API**  
A single mobile-facing endpoint is implemented as **`GET /api/jobs`** (`JobSearchServlet`, `@WebServlet(urlPatterns = "/api/jobs")` in `src/main/java/ds/web/JobSearchServlet.java`).

**b. Receives HTTP requests from the Android app**  
The servlet reads query parameters **`keyword`** (required) and **`device`** (optional, for logging), plus the **`User-Agent`** header when present.

**c. Business logic and third-party API**  
Server-side logic fetches **JSON** from the public **Arbeitnow Job Board API** (`https://www.arbeitnow.com/api/job-board-api`) via `ArbeitnowClient`, then filters jobs by keyword using `JobSearchService` (parsing and matching against title, company, location, and tags). This uses a published JSON API, not HTML screen scraping.

**d. JSON response to the Android app**  
Successful responses are **`application/json`** with a **`jobs`** array. Each job object is **slimmed** in `JobSearchService.toSlimJob` to only the fields the mobile client needs (e.g. `title`, `company_name`, `location`, `url`, and `remote` when present), rather than forwarding the full third-party payload. Errors return JSON such as `{"error":"..."}` with an appropriate HTTP status.

**Servlets (not JAX-RS)**  
The API is implemented with **`HttpServlet`** and **`@WebServlet`**, as required.

**e. GitHub Classroom / deployment repo**  
The repository includes **`Dockerfile`** and **`.devcontainer.json`** at the project root for building and running the web app in **GitHub Codespaces** (see Requirement 7). The Classroom assignment repository should reference or contain these files together with the built **`ROOT.war`** (produced by `mvn package`) or rely on the Docker build stage that runs Maven inside the image (as in this project’s `Dockerfile`).

---

## Requirement 4 — Log useful information (≥ 6 fields per mobile interaction)

For each **mobile → `/api/jobs`** request, the service persists one document via `AccessLogRepository.newAccessDocument` / `insertAccessLog` (`src/main/java/ds/web/AccessLogRepository.java`). Each log includes **more than six** attributes, covering:

| Logged field | Role |
|--------------|------|
| `receivedAt` | When the request was handled |
| `clientIp` | Client network identity (from `X-Forwarded-For` or remote address) |
| `userAgent` | HTTP User-Agent from the phone |
| `deviceModel` | Device model from the `device` query parameter (sent by the app) |
| `keyword` | Application-specific search parameter |
| `thirdPartyUrl` | Arbeitnow API URL used |
| `thirdPartyStatus` | HTTP status from the third-party call |
| `thirdPartyLatencyMs` | Round-trip time to the third-party API |
| `jobsReturned` | Number of jobs returned to the phone |
| `replyHttpStatus` | HTTP status sent back to the mobile client |

**Dashboard traffic is not logged:** only `JobSearchServlet` calls the logging path; `DashboardServlet` only reads MongoDB for the browser UI.

---

## Requirement 5 — Store logs in MongoDB (cloud)

Log documents are stored in MongoDB database **`project4_task2`**, collection **`access_logs`**, using the Java driver (`MongoClient` from `MongoHolder`). Connection uses **`MONGODB_URI`** when set in the deployment environment (e.g. Codespaces / Docker), with a fallback for local development. Logs persist across Tomcat restarts.

---

## Requirement 6 — Operations dashboard (browser)

**a. Unique URL**  
The dashboard is served at **`GET /dashboard`** (`DashboardServlet` → `dashboard.jsp`).

**b. At least three operations analytics**  
`dashboard.jsp` (with data from `AccessLogRepository`) shows:

1. **Total mobile API requests logged** (`countAll`).
2. **Average Arbeitnow API latency (ms)** (`averageThirdPartyLatencyMs`).
3. **Distinct device model values seen** (`distinctDeviceCountEstimate`).

Additionally, a **“Top search keywords”** table lists the most frequent keywords (aggregated in MongoDB), which supports operational insight beyond the three headline metrics.

**c. Formatted full logs**  
A **Recent access logs** section renders the latest entries in an **HTML `<table>`** with columns for time, client IP, device, keyword, third-party HTTP status, third-party latency, jobs returned, and reply HTTP status. Logs are **not** shown as raw JSON or XML.

---

## Requirement 7 — Deploy via GitHub Codespaces

**a–c. Container definition**  
- **`.devcontainer.json`** builds from the repo **`Dockerfile`** and forwards **port 8080** (Tomcat).  
- **`Dockerfile`**: multi-stage build runs **`mvn -q -DskipTests package`**, then deploys **`ROOT.war`** to Tomcat 9 (`tomcat:9.0-jdk17-temurin-jammy`).

**d–g. Testing and Android access**  
After the Codespace starts, Tomcat listens on **8080**. The **Ports** tab shows the forwarded URL. For the **Android emulator or device** to call the API without GitHub login, port **8080** must be set to **Public** visibility. The same base URL (with trailing slash) is pasted into **`build.gradle`** as `BuildConfig.BASE_URL`.

**h. Custom deployment**  
Pushing this project (or syncing `Dockerfile`, `.devcontainer.json`, and sources) to the Classroom repo and opening a Codespaces environment produces a running instance of this web service; optional **`MONGODB_URI`** can be supplied at runtime (e.g. `docker run -e MONGODB_URI=...`) so logging and the dashboard work against **MongoDB Atlas**.

---

## Quick verification checklist for graders

| Item | How to verify |
|------|----------------|
| Mobile API | `GET /api/jobs?keyword=software&device=Pixel` → JSON `jobs` array |
| Third-party | Server logs show Arbeitnow URL, status, and latency per request |
| Dashboard | Open `/dashboard` in a desktop browser → metrics + tables |
| Android | Install APK, set `BASE_URL` to public Codespace URL, search repeatedly |
| MongoDB | After mobile searches, dashboard totals and recent rows increase |

---

*End of writeup. Export this file to PDF for submission if your course requires a PDF, or use the same content in a narrated screencast as allowed by the README.*
