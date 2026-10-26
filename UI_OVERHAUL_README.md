# Lucky Dog — Complete Sprint 3 UI overhaul

Source base: user-provided `314-main(1).zip` (24 October 2026).

## What changed

- **Public navigation**: anonymous visitors see one Log in entry (registration remains on `/login`); authenticated roles use a profile dropdown with logout. Small screens have a collapsible navigation menu.
- **Homepage**: removed the How Lucky Dog works section; existing hero, monthly project and editorial content remain.
- **Designer directory**: unchanged.
- **Public designer profile**: review/report buttons are not shown to visitors; customers can still write reviews and report designers. Project cards now show their first available photo.
- **Project search and customer favourites**: image-led cards using the first public project image.
- **Customer (`/customer`)**: full dashboard, three distinct action cards, real recent favourite thumbnails, no registration/logout duplicate.
- **Designer (`/designer`)**: dashboard with portfolio/project-interest statistics, actions and real recent portfolio thumbnails; former placeholder removed.
- **User Admin**: header is reduced to designer reports and Account Management. `/admin/accounts` is the admin landing page, with Create Designer Account next to the title.
- **Platform Manager**: new `/manager` management homepage links to existing category, project, article, recognition and activity tools; public browse links remain in header.
- **Authentication**: valid credentials for suspended accounts produce HTTP 423 and the requested suspension message. Incorrect passwords (including for suspended accounts) still return generic invalid-credential feedback. The address `admin@luckydog.com` is a requested display contact; verify its operational use before production.

## Backend API changes (no migration needed)

- Public `/api/projects` responses include nullable `coverImageUrl`.
- Public designer profile project entries include nullable `coverImageUrl`.
- Customer favourite listing response includes nullable `coverImageUrl`.
- Cover URLs are derived using batched image queries and public routes and do **not** read a project detail or add views.
- Login can return HTTP 423 for a suspended account when its password is correct.

## Install / test on Windows

1. Back up your current working source or commit it. This ZIP contains a complete `314-main` folder, not just patch fragments. Copy the updated source files into your working checkout if you prefer to keep local Git history. **Do not overwrite your own uncommitted changes without comparing first.**
2. Keep your existing `csit314_test` database and image files. No re-seeding or Flyway database changes are needed.
3. In `frontend`, run `npm.cmd ci`, then `npm.cmd run check` and `npm.cmd run dev`.
4. In `backend`, set your usual `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` to `csit314_test`; disable unwanted bootstrap accounts; run `./mvnw.cmd spring-boot:run`.
5. Test guest homepage, projects, public designer profiles; customer favourites/dashboard; designer dashboard/edit portfolio; admin accounts/creation; manager hub; profile dropdown; active and suspended login; and mobile navigation.

**Important:** The backend integration tests can modify/delete records in your disposable `_test` database. Use a backup or clean disposable instance if you run backend integration tests.

## Verification performed in this environment

- Frontend Node unit suite: **135 passed**.
- Full `npm ci` and Vite build **not executed** because Vite 8.3.2 was not cached (network retrieval unavailable).
- Maven compile/backend tests **not executed** because the Maven wrapper could not download Maven 3.9.15.
- No live database or browser verification performed; the user will complete final tests locally.
