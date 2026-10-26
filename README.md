# Lucky Dog

**Lucky Dog** is a renovation customer–interior designer matching platform developed for **CSIT314 Software Development Methodologies** (University of Wollongong / SIM, 2026). Customers can discover projects and designers, while interior designers and platform staff manage portfolios, accounts and content.

This repository contains the **React frontend** and **Spring Boot backend**. The instructions below explain how to run the application source locally on **Windows**.

## Features

- **Visitors:** browse designers and portfolios, search/filter projects, view project photos, reviews, renovation articles and highlighted work.
- **Renovation customers:** register and sign in, save projects to favourites, compare designers, submit reviews/reports, and manage account/security settings.
- **Interior designers:** manage company information and portfolio projects/photos, view project-interest metrics, and manage account/security settings.
- **User admins:** create designer accounts, search/suspend/reactivate or delete eligible accounts, inspect login history, and handle designer reports.
- **Platform managers:** maintain project categories and published project information, publish articles, manage project recognition, and view activity reports (including daily, weekly, monthly and custom date ranges).

Authentication uses Spring Security sessions and CSRF protection. Features are restricted by role on the backend, not only in the UI.

## Technology stack

| Layer | Technologies |
| --- | --- |
| Frontend | React 19, React Router 7, Vite 8, Ant Design 6, Axios, JavaScript/JSX |
| Backend | Java 21, Spring Boot 3.5, Spring Security, Maven Wrapper |
| Persistence | PostgreSQL, Spring Data JPA/Hibernate, Flyway (migrations V1–V16 in this source) |
| Verification | Oxlint, Node.js test runner, Maven/Spring Boot tests, GitHub Actions |

## Repository structure

```text
314/
├── frontend/
│   ├── src/                 # React pages, components, API clients and styles
│   ├── tests/               # Frontend tests
│   ├── package.json
│   └── vite.config.js
├── backend/
│   ├── src/main/java/       # Spring Boot application and BCE feature packages
│   ├── src/main/resources/   # Application configuration and Flyway migrations
│   ├── src/test/            # Backend tests
│   ├── pom.xml
│   └── mvnw.cmd             # Maven Wrapper for Windows
├── .github/workflows/       # CI checks
└── README.md
```

## Prerequisites

Install the following on Windows:

- **Git** (to clone the repository)
- **Node.js 24** and npm (the version used by the GitHub Actions frontend job)
- **JDK 21 or newer** (the backend targets Java 21; CI uses JDK 21)
- **PostgreSQL** (PostgreSQL 18 is used by the current project)
- **PowerShell**

You do **not** need a separate Maven installation; the repository includes the Maven Wrapper. The first dependency download may require internet access.

Confirm your tools in PowerShell:

```powershell
node --version
npm.cmd --version
java -version
git --version
```

## Local setup (Windows)

### 1. Clone the application source

```powershell
git clone https://github.com/anej49747-sudo/314.git
cd 314
```

Repository access is required if the GitHub repository is private. Run the commands below from this repository root unless a different folder is specified.

### 2. Configure PostgreSQL

Create a **local, disposable** PostgreSQL database named `csit314_test`, owned by a login role named `csit314_app`. If you already have that database and role, **do not recreate or drop them**. You can create them through pgAdmin or with a PostgreSQL administrator account.

For a *new* local PostgreSQL setup, connect to the default `postgres` database as an administrator in `psql` and run the following commands **only if the role and database do not already exist**:

```sql
CREATE ROLE csit314_app LOGIN;
```

In `psql`, set a private password interactively (without putting it in your shell history):

```text
\password csit314_app
```

Then create the database:

```sql
CREATE DATABASE csit314_test OWNER csit314_app;
```

The normal application uses `csit314_app`; it does not require superuser access. Do not run the application or tests against a production or important database.

> **Database name matters:** `backend/src/main/resources/application.properties` has a historical fallback URL for `csit314_db`. In the setup below, explicitly set `DB_URL` to `csit314_test` so the application cannot accidentally use that fallback. The two database names are **not interchangeable**.

### 3. Create the backend's local `.env`

Create a file named **`backend/.env`** on your own computer:

```properties
DB_URL=jdbc:postgresql://localhost:5432/csit314_test
DB_USERNAME=csit314_app
DB_PASSWORD=REPLACE_WITH_YOUR_LOCAL_PASSWORD
DB_TEST_URL=jdbc:postgresql://localhost:5432/csit314_test
DB_TEST_USERNAME=csit314_app
DB_TEST_PASSWORD=REPLACE_WITH_YOUR_LOCAL_PASSWORD
ADMIN_BOOTSTRAP_ENABLED=false
MANAGER_BOOTSTRAP_ENABLED=false
```

Replace both password placeholders with the local PostgreSQL role password. **Do not commit or share `.env`**, database passwords or secret values. The file is ignored by Git.

Spring Boot includes `spring.config.import=optional:file:./.env[.properties]`, so it can load `backend/.env` when started **from the `backend` directory**. If your IDE starts Spring Boot from another working directory, configure its working directory or supply equivalent environment variables explicitly.

### 4. Start the backend (Terminal 1)

Open PowerShell at the repository root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Wait for Spring Boot to start. Verify the health endpoint:

<http://localhost:8080/actuator/health>

A healthy running backend reports `"status":"UP"`. Flyway applies pending migrations automatically, and Hibernate validates the schema. You do not need to create application tables manually.

### 5. Start the frontend (Terminal 2)

Open a **second** PowerShell terminal at the repository root:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open **<http://localhost:5173>** (or the URL shown by Vite). The homepage is `/`; use the navigation to browse projects/designers or register and sign in. Vite forwards local `/api` requests to the backend at `http://localhost:8080`.

Keep both terminals running while using the application. Press **Ctrl+C** in each terminal to stop the servers.

### Optional: local administrator and platform-manager accounts

A fresh database does not automatically contain privileged users. The backend provides **opt-in bootstrap creation** for local development using the Spring `local` profile and the `ADMIN_BOOTSTRAP_*` / `MANAGER_BOOTSTRAP_*` environment variables. Supply valid private email/password values and enable only the required bootstrap flag while creating that account; then disable it. See `backend/src/main/java/csit314/renovation/security/` for the existing bootstrap configuration.

There are no published default account credentials in this source repository. Customers can use `/register`; approved designer accounts are provisioned by a user admin.

## Main application routes

| Audience | Pages |
| --- | --- |
| Public | `/`, `/designers`, `/designers/:designerId`, `/projects`, `/projects/:projectId`, `/articles`, `/articles/:articleId`, `/register`, `/login` |
| Customer | `/customer`, `/customer/account`, `/customer/security`, `/customer/change-email`, `/customer/change-password`, `/customer/favourites`, `/customer/compare` |
| Interior designer | `/designer`, `/designer/account`, `/designer/security`, `/designer/change-email`, `/designer/change-password`, `/designer/company-information`, `/designer/portfolio`, `/designer/portfolio/new`, `/designer/portfolio/:projectId/edit` |
| User admin | `/admin/accounts`, `/admin/designers/new`, `/admin/reports` |
| Platform manager | `/manager`, `/manager/categories`, `/manager/projects`, `/manager/articles`, `/manager/recognitions`, `/manager/reports` |

Role-specific screens require an appropriate signed-in account. Opening a protected page or API endpoint does not bypass server-side permissions.

## Running checks and tests

**Frontend** (from `frontend/`):

```powershell
npm.cmd ci
npm.cmd run check
```

`check` runs Oxlint, Node tests and a Vite production build. For individual checks, use `npm.cmd run lint`, `npm.cmd run test` or `npm.cmd run build`.

**Backend** (from `backend/`):

```powershell
.\mvnw.cmd verify
```

**Warning — database cleanup:** Backend integration tests contain destructive fixture setup/cleanup. They can delete data in `csit314_test` (or another database whose name ends in `_test`). **Only run backend tests when the configured database is intentionally disposable.** Test configuration is in `backend/src/test/resources/application-test.properties`, which may fall back to `DB_URL` when `DB_TEST_URL` is not supplied. Check both URLs before testing. Never set `ALLOW_DEV_DB_TEST_RESET=true` on an important database; that flag permits the historical `csit314_db` test-reset exception.

GitHub Actions workflows live in `.github/workflows/` and check frontend lint/tests/build and backend Maven verification using an isolated PostgreSQL test service. A successful CI run is not evidence of production deployment.

## Common issues

| Problem | Check |
| --- | --- |
| Backend cannot connect to PostgreSQL | PostgreSQL is running; `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` match the intended local database; the account has access. |
| Spring Boot uses the wrong database | Start from `backend/` so `.env` imports correctly; confirm the effective `DB_URL` rather than relying on the `csit314_db` fallback. |
| Flyway / Hibernate startup error | Review the SQL migration failure, database ownership and existing schema. Do **not** manually edit applied migrations or delete an unknown database. |
| Frontend cannot reach the API | Ensure Spring Boot is running on port `8080`, then restart Vite if needed. |
| Missing project/article images | Image files are stored on the filesystem (not in PostgreSQL). By default the backend uses `${user.home}/.csit314/project-images`, configurable via `app.project-images.directory`. Records alone do not restore image bytes. |
| `mvnw.cmd` or `npm.cmd` not found | Check you are in `backend/` or `frontend/` respectively; verify Java, Node.js and npm are installed. |
| Tests unexpectedly affect local records | Inspect `DB_TEST_URL`, `DB_URL` and `ALLOW_DEV_DB_TEST_RESET` before running Maven tests. Use only disposable data. |

## Development notes

Keep secrets out of Git, use new Flyway migrations for schema changes, and preserve the existing Spring Security session/CSRF/role checks. Feature changes should include appropriate backend and frontend tests and be reviewed before merging. The backend follows the **Boundary–Control–Entity (BCE)** structure, and application routes are defined in `frontend/src/App.jsx`.

For setup problems, check the relevant terminal error and the configuration files above before opening a team issue or pull request. Never include passwords, `.env` contents or private data in logs shared with others.
