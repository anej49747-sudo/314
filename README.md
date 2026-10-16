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

Open the Vite URL printed in the terminal (usually `http://localhost:5173`). It shows a **setup placeholder only**. Use Ctrl+C to stop the server.

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

The initial `baseline-checks.yml` workflow validates repository files. The additional `scaffold-ci.yml` workflow runs npm lint/build and a Maven PostgreSQL integration test using an ephemeral GitHub-hosted PostgreSQL service account. This CI workflow only runs on GitHub after the branch is pushed; it is not a deployment pipeline. Tests and builds **must be run locally before handing off to CI or manual UAT**.

Use one feature branch/PR per Taiga story, and prefer a squash merge to `main` only after CI passes and manual tester UAT approval. Documentation/infrastructure PRs do not require feature UAT unless user-visible behavior changes. No GitHub repository settings or tester approvals are assumed to be in place.

No user stories have been implemented or accepted in this scaffold. Business classes, database tables, endpoints, pages, and role-based authorization will be added through separately scoped work. The assigned diagram owner retains responsibility for UML diagrams.
