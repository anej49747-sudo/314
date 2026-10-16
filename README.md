# Renovation Customer–Interior Designer Matching Platform

CSIT314 Software Development Methodologies — University of Wollongong, SIM S4 2026.

## Project requirements

The official specification requires multi-role accounts/profiles; designer company management; current/past interior-design portfolios; customer search/view/favourites; designer view/shortlist metrics; platform category management; and daily/weekly/monthly reports. Detailed draft user stories and sprint assignments are maintained outside GitHub.

## Target stack

| Layer | Tooling |
| --- | --- |
| Frontend | React 19, JavaScript/JSX, Vite 8, npm, Oxlint |
| Backend | Java 21 target, Spring Boot 3.5.14, Maven Wrapper |
| Persistence | Spring Data JPA/Hibernate, PostgreSQL 18 |
| Schema management | Flyway (schema changes will be introduced with feature work) |
| CI | GitHub Actions, frontend lint/build and backend PostgreSQL integration test |

The local JDK can be 25, but the Java compiler release and CI runtime target are 21.

## Local prerequisites

- Node.js 24 and npm.
- JDK 21+ with `java` and `javac` in `PATH` (JDK 25 works with the chosen Java 21 compiler target).
- PostgreSQL running locally, with database `csit314_db` owned by login role `csit314_app` (configured separately using pgAdmin).
- A normal terminal in the repository root (VS Code PowerShell on Windows works).

## Frontend (PowerShell)

```powershell
cd frontend
npm.cmd ci
npm.cmd run lint
npm.cmd run build
npm.cmd run dev
```

Open the Vite URL printed in the terminal (usually `http://localhost:5173`). It redirects to the **Story #2 customer registration** page (`/register`). Use Ctrl+C to stop the server.

## Backend (PowerShell)

Do **not** put your real PostgreSQL password in `application.properties`, git or the README. In a fresh VS Code terminal, supply it temporarily:

```powershell
$securePassword = Read-Host 'PostgreSQL csit314_app password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
cd backend
.\mvnw.cmd --version
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

By default the backend connects to `jdbc:postgresql://localhost:5432/csit314_db` as `csit314_app`. If your PostgreSQL port differs, set `DB_URL` in that terminal; if using a different user, set `DB_USERNAME`.

Once started, visit `http://localhost:8080/actuator/health` and expect `{"status":"UP"}`. A backend startup or test failure may indicate incorrect database credentials, connectivity, or Flyway permissions. Stop the server with Ctrl+C. An unset `DB_PASSWORD` should cause connection failure; this is intentional.

To remove the temporary password from that terminal after testing:

```powershell
Remove-Item Env:DB_PASSWORD
```

## Tests and workflow

The initial `baseline-checks.yml` workflow validates repository files. The additional `scaffold-ci.yml` workflow runs npm lint/unit tests/build and Maven PostgreSQL integration tests using an ephemeral GitHub-hosted PostgreSQL service account. This CI workflow only runs on GitHub after the branch is pushed; it is not a deployment pipeline. Tests and builds **must be run locally before handing off to CI or manual UAT**.

Use one feature branch/PR per Taiga story, and prefer a squash merge to `main` only after CI passes and manual tester UAT approval. Documentation/infrastructure PRs do not require feature UAT unless user-visible behavior changes. No GitHub repository settings or tester approvals are assumed to be in place.

Story #2 customer registration has been developed in this changeset, but must still pass local/CI verification and manual UAT before it can be marked accepted. Other user stories and authenticated role-based access remain unimplemented. The assigned diagram owner retains responsibility for UML diagrams.

## Sprint 1 · Story #2 — Customer registration

- Open `http://localhost:5173/register` while frontend and backend are running.
- The public form collects optional salutation (`Mr`, `Mrs`, `Ms`, `Mdm`, `Mx`, `Dr`, `Prof`), name or company name, unique email and password (10–128 characters; uppercase, lowercase, number and special character). Password confirmation is browser-only.
- `POST /api/register` creates an active `CUSTOMER` account and returns `201`. Invalid input returns `400`; an already-registered email returns `409`. No login/token is issued yet: `/login` is a clear placeholder for Story #18.
- Passwords are stored as salted Argon2id hashes. The database stores only the hash, never the original password. API clients cannot select an account role.
- Frontend `/api` requests are forwarded to `http://localhost:8080` by Vite's development proxy. When deploying, configure your server/reverse proxy for a same-origin `/api` path.

### Repeatable PostgreSQL schema creation (Flyway)

**Do not manually create the `user_accounts` table.** In a new empty PostgreSQL database owned by your developer account, Spring Boot runs `backend/src/main/resources/db/migration/V1__create_user_accounts.sql` automatically at startup. Flyway creates `flyway_schema_history`, records the applied version, and does not rerun a completed migration. Hibernate is configured with `ddl-auto=validate`, not `create` or `update`.

For local automated tests, use a **separate empty database** to prevent test cleanup from affecting developer accounts. In pgAdmin, create database `csit314_test` owned by your local `csit314_app` PostgreSQL user. Or, if you have access to `psql` as a database administrator:

```powershell
psql -h localhost -U postgres -d postgres -c "CREATE DATABASE csit314_test OWNER csit314_app;"
```

Only create it once. Existing databases should not be dropped or recreated. Tests deliberately refuse to clean a database whose name does not end in `_test`. In CI, the existing `csit314_test` PostgreSQL service is used automatically.

In Windows VS Code PowerShell (from the repository root):

```powershell
# Only this terminal sees your database password; do not store it in source files.
$securePassword = Read-Host 'Local PostgreSQL password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
cd backend
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

The normal application uses `csit314_db`; all Spring integration tests use `csit314_test` via `application-test.properties`. Override the test connection with `DB_TEST_URL`, `DB_TEST_USERNAME`, and `DB_TEST_PASSWORD` if your teammate's PostgreSQL naming differs; always end test database names in `_test`.

In a second PowerShell terminal:

```powershell
cd frontend
npm.cmd ci
npm.cmd run check
npm.cmd run dev
```

Review `flyway_schema_history` in pgAdmin to see which schema migration ran. Future schema changes require additional ordered migration files (`V2__...sql`, `V3__...sql`) rather than editing `V1` after teammates have applied it.

**Security note (Story #2 baseline):** Registration alone did not issue sessions. Story #18 adds authenticated sessions and a protected customer profile endpoint. Never trust URL/user IDs supplied by a browser as proof of ownership.

## Sprint 1 · Story #18 — Customer login

- `/login` now accepts customer email/password; existing Argon2id password hashes from Story #2 are verified by Spring Security.
- `GET /api/auth/csrf` returns a session-bound token; `POST /api/login` requires it and establishes an HttpOnly cookie session. The browser makes same-origin `/api` requests through Vite's development proxy.
- `GET /api/auth/me` is accessible only to authenticated customers and derives the account from the server-side principal, **not** a user-supplied ID. The temporary `/customer` page checks this endpoint and remains protected on refresh.
- Incorrect credentials and inactive accounts return `401`; forbidden customer-area access returns `403`. Login session fixation protection rotates the session identifier and the CSRF token. The browser does not store session identifiers in local storage.
- No database migration is needed for #18. The existing Flyway migration creates the table automatically on fresh databases. No manual table creation is required.
- Sessions currently expire after 30 minutes of inactivity. Local HTTP uses `SESSION_COOKIE_SECURE=false` by default; use `SESSION_COOKIE_SECURE=true` behind HTTPS for deployment. Do not expose the development backend cross-origin: configure a same-origin reverse proxy for `/api`.
- **Story #19** will add functional logout. Until then, `/customer` clearly notes that sign-out is not yet available. Login tests needing PostgreSQL run in GitHub Actions against the separate ephemeral `csit314_test` database; do not run them against the local `csit314_db`.
- Quick checks: in `frontend`, run `npm.cmd run check`; in `backend`, run `./mvnw.cmd -Dtest=AuthenticationServiceTest,AccountUserDetailsServiceTest test` for unit-only tests on Windows PowerShell. GitHub Actions runs the complete PostgreSQL integration suite after push.

**Java package structure:** Backend code and tests now use `csit314.renovation` with feature-oriented BCE folders (for example, `registration/boundary`, `registration/control`, and `account/entity`). Maven `groupId` is `csit314`. This is a package-only refactor bundled with Story #18; Flyway migrations and REST routes are unchanged.
