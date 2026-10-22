# Renovation Customer–Interior Designer Matching Platform

CSIT314 Software Development Methodologies — University of Wollongong, SIM S4 2026.

## Project scope and delivery

The official coursework requires multi-role accounts/profiles; designer company information; current/past interior-design project portfolios; customer search/view/favourites; designer interest counts; Platform Manager categories **and daily/weekly/monthly reports**. The periodic reports remain a priority for Sprint 3.

**Available user roles:** Renovation Customer (`CUSTOMER`), Interior Designer (`INTERIOR_DESIGNER`), User Admin (`USER_ADMIN`), Platform Manager (`PLATFORM_MANAGER`), plus anonymous Visitors.

**Sprint 1:** Registration, session login/logout, designer provisioning and forced initial password change; category creation; company information; portfolio add/view/edit/delete and image uploads; public designer directory, profiles and project details. Portfolio work uses split stories #113 (view), #16 (add), #114 (edit) and #115 (delete).

**Sprint 2:** #5 search projects, #7 filter by category, #9 public project photo gallery, #22 save favourites, #52 view saved projects, #17 project view/favourite metrics, #45 compare 2–3 designers, #46 submit reviews, #10 public reviews, #14 designer account settings, #21 customer account settings. The #17 follow-up fix deduplicates concurrent project-detail requests in development React StrictMode. **#44 designer specialisation filtering is NOT implemented**; any deferral remains for team/Taiga decision.

## Technology and architecture

| Layer | Implementation |
| --- | --- |
| Frontend | React 19, React Router 7, JavaScript/JSX, Ant Design 6, Vite 8, npm, Axios, Oxlint, Node `--test` |
| Backend | Java 21 bytecode, Spring Boot 3.5, Maven Wrapper, Spring Web/Security, feature-based Boundary–Control–Entity packages |
| Database | PostgreSQL, Spring Data JPA/Hibernate, Flyway ordered SQL migrations **V1–V9** |
| Authentication | Server-managed Spring Security sessions, HttpOnly same-site cookies, CSRF protection, Argon2id password hashes |
| CI | GitHub Actions: frontend lint/tests/build and Maven/PostgreSQL verification; **not deployment/CD** |

Relevant source: `frontend/src/App.jsx` (routes, AntD theme), `frontend/src/pages/`, `frontend/src/components/`, `frontend/src/api/`; `backend/src/main/java/csit314/renovation/` (feature/BCE code), `backend/src/main/resources/db/migration/`, `backend/src/test/`. The original MongoDB `314-test.zip` is obsolete; do not use it as active implementation.

## Prerequisites and developer setup

- Git, Node.js 24+, npm and a Java JDK 21+; project bytecode target/CI runtime is Java 21. JDK 25 was used successfully by one developer.
- PostgreSQL (developer used PostgreSQL 18) running locally; create a database such as **`csit314_db`** owned by an application role such as **`csit314_app`** with schema-creation privileges. Each teammate uses their own local database/credentials.
- **No global Maven installation is needed.** Use `backend/mvnw.cmd` on Windows or `backend/mvnw` on Unix-like systems.
- Keep all passwords/secret values outside Git. `backend/.env` is ignored. The Spring Boot app does **not** automatically load a `.env` file; load variables into each new shell first.

Example **variable names only** for your local ignored `backend/.env`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_TEST_URL`, `DB_TEST_USERNAME`, `DB_TEST_PASSWORD`, `ALLOW_DEV_DB_TEST_RESET`. Set only values appropriate for your local machine. Never share real values or commit this file.

### Windows PowerShell — load private environment variables

From `backend/`:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

This basic loader handles simple `KEY=value` lines; it is not a general shell parser, and does not interpolate variables or strip quotes. If you prefer, set the environment variables manually in the current terminal. `DB_URL` defaults to `jdbc:postgresql://localhost:5432/csit314_db`, and `DB_USERNAME` defaults to `csit314_app`. `DB_PASSWORD` must be supplied locally.

### Run backend

From `backend/` in the terminal with variables loaded:

```powershell
.\mvnw.cmd spring-boot:run
```

Once running, `http://localhost:8080/actuator/health` should return status `UP`. Stop with Ctrl+C. Flyway applies missing migrations automatically on startup; Hibernate validates the schema (`ddl-auto=validate`). **Do not manually create app tables or rewrite earlier applied migrations.**

### Run frontend

From `frontend/` in another terminal:

```powershell
npm.cmd ci
npm.cmd run dev
```

Open the Vite URL printed in the terminal (typically `http://localhost:5173`). Its root route currently redirects to `/register`; this is a routing default, **not** evidence that registration is the only implemented feature. Vite proxies `/api` requests to the backend for local development. Production requires HTTPS and correctly configured same-origin `/api` routing.

## Available browser routes (22 October source snapshot)

| Audience | Routes |
| --- | --- |
| Visitor / all users | `/register`, `/login`, `/designers`, `/designers/:designerId`, `/projects`, `/projects/:projectId` |
| Customer | `/customer`, `/customer/account`, `/customer/favourites`, `/customer/compare` |
| Interior Designer | `/designer`, `/designer/account`, `/designer/change-password`, `/designer/company-information`, `/designer/portfolio`, `/designer/portfolio/new`, `/designer/portfolio/:projectId/edit` |
| User Admin | `/admin/designers/new` |
| Platform Manager | `/manager/categories` |

Customer comparison is a **2–3 designer** side-by-side flow. Account-settings screens change **display name and sign-in email**, not passwords or business/portfolio details. Public reading of designer reviews is permitted; only customers can submit them. A designer's project metrics count public project-detail views **once per browser session/project** and display the **current** number of favourites. Internal role/ownership checks happen in the backend, not just the React UI.

**Not present yet:** `/admin/...` user account search/suspend/reactivate and management reports (Sprint 3 candidates), designer specialisation filtering (#44), or any assumed deployed site.

## Tests and local database safety

Run frontend checks from `frontend/`:

```powershell
npm.cmd run check
```

This runs **Oxlint, Node unit tests and a Vite build**. Run backend Maven verification from `backend/` with test DB configuration set:

```powershell
.\mvnw.cmd verify
```

**Important: backend integration tests include destructive fixture cleanup.** `backend/src/test/java/csit314/renovation/TestDatabaseGuard.java` allows database names ending `_test`, or exactly `csit314_db` **only when** `ALLOW_DEV_DB_TEST_RESET=true`. In this project, developers intentionally reuse **disposable** local `csit314_db` for tests when opted in. These tests can delete accounts, companies, projects, favourites and related demo data. **Never** enable the reset flag for UAT/prod/important data. A separate `_test` database is a valid safer alternative, not a requirement for the current opted-in local workflow; CI uses its own isolated PostgreSQL database.

A separate locally downloaded `verify-local.ps1` has been used by the developer to check Git hygiene, guarded backend verification and frontend checks; **it is not part of this ZIP's tracked repository**. If you have that script and your test DB is disposable, run from repo root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$HOME\Downloads\verify-local.ps1"
```

## Team workflow and reporting

1. Use actual local `main` after confirming clean Git status and `git pull --ff-only origin main`. Never assume a source ZIP includes unstaged changes or Git history.
2. Review user story, official requirements, dependency, BCE responsibilities, role permissions and acceptance criteria. For UI, agree a standalone HTML mockup **before app implementation**.
3. Make reviewable story-scoped changes, migrations and tests; verify locally, inspect staged files, and retain real commits/PR/CI evidence. Push/PR/merge only after team approval and applicable verification.
4. Record genuine tester outcomes separately from developer smoke tests; keep Taiga, diagrams, sprint artefacts and the final report consistent with actual events. Do not invent PR approvals, dates, meetings, UAT or deployment.
5. The private project context and detailed planning documents should **not** be committed to GitHub by default; keep environment passwords and demo credentials out of the repository.

**Coursework due:** 12 November 2026, 23:59. The course also expects selected detailed use case/BCE/sequence documentation, a data-driven development plan, ethical considerations, realistic demo data, authentic meetings and contribution declarations.
