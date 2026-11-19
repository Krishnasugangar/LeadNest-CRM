# LeadNest CRM

LeadNest is a small sales CRM for a portfolio or a project walkthrough. An administrator and agents share one workspace: leads, one deal per qualified lead, logged activities, and follow-ups. Data is stored in MySQL. The browser talks only to the frontend origin, and that origin proxies API calls to the Spring Boot backend.



Checked against current Spring docs and Maven Central on 18 October 2026.

| Piece | Version | Why this one |
| --- | --- | --- |
| Java | 21 | Supported by Spring Boot 4.1 (Java 17 through 26) |
| Spring Boot | 4.1.1 | Current stable release (20 August 2026). Needs Spring Framework 7.0.9 or newer |
| Spring Web MVC | `spring-boot-starter-webmvc` | In Boot 4 this replaces `spring-boot-starter-web` |
| Spring Data JPA | Boot-managed | Hibernate, via `spring-boot-starter-data-jpa` |
| Spring Security | Boot-managed (7.x) | Session cookies, BCrypt, CSRF |
| Flyway | Boot-managed, plus `flyway-mysql` | Schema migrations. Hibernate does not create tables |
| MySQL | 8.4 | Server image. Connector is `com.mysql:mysql-connector-j`, managed by Boot |
| springdoc-openapi | 3.1.1 | 3.x line matches Spring Boot 4 |
| Testcontainers | Boot-managed 2.x | `testcontainers-mysql` and `testcontainers-junit-jupiter` |
| React | 19.3.1 | UI |
| React Router | 7.18.4 | Pages |
| Vite | 7.3.7 | Dev server and production build |
| Tailwind CSS | 4.3.3 | Styling, through `@tailwindcss/vite` |

## Folder structure

```text
LeadNest-CRM/
  docker-compose.yml          frontend, backend, MySQL
  .env.example                local values, including the first admin
  backend/
    Dockerfile
    pom.xml
    src/main/java/com/leadnest/crm/
      web/                    controllers
      service/                business rules and transactions
      repository/             Spring Data JPA
      domain/                 entities and enums
      security/               session, CSRF, active-user check
      dto/                    request and response records
      config/                 admin bootstrap and optional demo data
    src/main/resources/db/migration/V1__init.sql
    src/test/java/            policy tests and MySQL API tests
  frontend/
    Dockerfile                builds the SPA, then nginx serves it
    nginx.conf                same-origin proxy for /api and Swagger
    src/pages/                Login, Dashboard, Leads, Lead details, Deals, Follow-ups, Profile, Agents
```

## Database relationships

```text
app_users 1 ──── * leads
leads     1 ──── * activities
leads     1 ──── * follow_ups
leads     1 ──── * lead_history
leads     1 ──── 0..1 deals          deals.lead_id is unique
```

- A lead must have an email or a phone. Lost leads and lost deals require a reason. Deal amounts are `DECIMAL(14,2)` and must be greater than zero.
- Deal access uses the lead’s assigned agent. Reassigning a lead moves the deal with it. Activity rows keep the original `author_id`.
- Leads are archived, not deleted. Related rows stay. Archived leads are excluded from dashboard totals.
- `ddl-auto` is `none`. Flyway applies `V1__init.sql` and does not wipe an existing database.

## How one request travels

Creating a lead is a complete round trip:

1. `LeadsPage` submits the form and calls `api('/api/leads', { method: 'POST', body })` in `frontend/src/api.js`.
2. The helper reads the `XSRF-TOKEN` cookie and sends it as `X-XSRF-TOKEN`. The browser also sends the `HttpOnly` `JSESSIONID` cookie. Both stay on the page origin (`http://localhost:5173`).
3. In local dev, Vite proxies `/api` to `http://localhost:8081`. In Docker, nginx does the same proxy to the `backend` service and re-resolves that name through Docker DNS, so a backend restart does not leave the proxy stuck on an old container address. The React app never calls MySQL and never sees a password hash.
4. Spring Security checks the session and the CSRF header, then `LeadController.create` validates the JSON body.
5. `LeadService.create` runs in a transaction. It loads the signed-in user, checks that a real email or phone was supplied, and assigns the lead to that user. An administrator may instead assign an active user. The service saves the lead and a history row. An agent’s query path never trusts a client-supplied owner.
6. `LeadRepository.save` asks Hibernate to `INSERT` into `leads`. The contact check and the later one-deal-per-lead rule are also enforced by MySQL constraints.
7. The service returns a `LeadResponse`. The page reloads `GET /api/leads`, which again goes through the controller, service, and a filtered repository query.

## Windows PowerShell setup

From the project directory:

```powershell
cd C:\Users\krishna\Projects\LeadNest-CRM
Copy-Item .env.example .env
notepad .env
```

Set `LEADNEST_ADMIN_PASSWORD` to a private password of 8 to 72 characters before the first start. Leave `LEADNEST_DEMO_DATA=false` unless you want a few fictional leads on the bootstrap admin.

Start everything:

```powershell
docker compose up --build
```

When the frontend is up:

- App: http://localhost:5173
- API directly: http://localhost:8081
- Swagger UI: http://localhost:5173/swagger-ui.html
- MySQL from the host: `localhost` port `3307`

Containers reach MySQL at `mysql:3306`. The host port `3307` is only for tools on your machine.

Stop the stack without removing data:

```powershell
docker compose stop
```

Start it again:

```powershell
docker compose start
```

The database lives in the named volume `leadnest-mysql`. Keep that volume. Recreating containers does not require deleting it.

## Native development

Run only MySQL in Docker, then the two apps on the host.

```powershell
cd C:\Users\krishna\Projects\LeadNest-CRM
Copy-Item .env.example .env -ErrorAction SilentlyContinue
docker compose up mysql -d
```

Wait until MySQL is healthy:

```powershell
docker compose ps
```

Backend, in a second terminal:

```powershell
cd C:\Users\krishna\Projects\LeadNest-CRM\backend
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3307/leadnest?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectionTimeZone=UTC&characterEncoding=utf8"
$env:SPRING_DATASOURCE_USERNAME = "leadnest"
$env:SPRING_DATASOURCE_PASSWORD = "leadnest"
$env:LEADNEST_ADMIN_EMAIL = "admin@leadnest.local"
$env:LEADNEST_ADMIN_PASSWORD = "change-me-admin-password"
$env:LEADNEST_ADMIN_NAME = "Avery Admin"
$env:LEADNEST_DEMO_DATA = "false"
mvn spring-boot:run
```

Use the password you actually put in `.env` for `MYSQL_PASSWORD` and `LEADNEST_ADMIN_PASSWORD`. The defaults above match `.env.example`.

Frontend, in a third terminal:

```powershell
cd C:\Users\krishna\Projects\LeadNest-CRM\frontend
npm install
npm run dev
```

Vite listens on port 5173 and proxies `/api` to `http://localhost:8081`. Open http://localhost:5173.

Build checks:

```powershell
cd C:\Users\krishna\Projects\LeadNest-CRM\backend
mvn test

cd C:\Users\krishna\Projects\LeadNest-CRM\frontend
npm run build
```

`mvn test` starts a temporary MySQL 8.4 container through Testcontainers, so Docker must be running.

## Environment variables

| Variable | Used by | Purpose |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL container | Root password for the local database |
| `MYSQL_PASSWORD` | MySQL and backend | Password for the `leadnest` database user |
| `LEADNEST_ADMIN_NAME` | Backend | Display name for the first administrator |
| `LEADNEST_ADMIN_EMAIL` | Backend | Email for the first administrator |
| `LEADNEST_ADMIN_PASSWORD` | Backend | Initial password. Required when the email is new |
| `LEADNEST_DEMO_DATA` | Backend | `true` inserts a few fictional leads. Default `false` |
| `SPRING_DATASOURCE_URL` | Backend | JDBC URL. Compose points this at `mysql:3306` |
| `SPRING_DATASOURCE_USERNAME` | Backend | Database user. Compose uses `leadnest` |
| `SPRING_DATASOURCE_PASSWORD` | Backend | Same value as `MYSQL_PASSWORD` inside Compose |
| `SERVER_PORT` | Backend | Defaults to `8081` |

No secret belongs in frontend code. The browser uses relative `/api` URLs.

## Administrator initialization

There is no registration page and no registration API.

On startup, `AdminBootstrap` reads `LEADNEST_ADMIN_EMAIL`, `LEADNEST_ADMIN_PASSWORD`, and `LEADNEST_ADMIN_NAME`. If that email is not already in `app_users`, it inserts one active `ADMIN` with a BCrypt hash. If the email already exists, the password is left alone. If the email is set and the password is shorter than 8 characters, startup fails. Credentials are not written in Java source.

Sign in at http://localhost:5173 with that email and password. From Agents, create each agent with a name, email, and initial password. Agents change that password from Profile after they sign in. Deactivating an agent rejects the next request on an existing session.

## Swagger

Open http://localhost:5173/swagger-ui.html (or http://localhost:8081/swagger-ui.html). The raw document is at `/v3/api-docs`.

Swagger is public so the API can be inspected. Calling a protected operation still needs a session cookie and the `X-XSRF-TOKEN` header. The application UI does that for you.

## Port conflicts

Default host ports are frontend `5173`, backend `8081`, and MySQL `3307`.

See what is listening:

```powershell
netstat -ano | findstr ":5173 :8081 :3307"
```

Stop the other program, or change only the host side of the mapping in `docker-compose.yml`, for example `"8082:8081"`. Inside the Compose network the backend still listens on `8081` and MySQL still listens on `3306`. If you change the host MySQL port, point native `SPRING_DATASOURCE_URL` at that port. If you change the host frontend port, open that port in the browser.

Do not delete the `leadnest-mysql` volume to fix a port clash. `docker compose down` stops containers and keeps the volume. Avoid `docker compose down -v` unless you intend to discard the database.

## Demo walkthrough

1. Sign in as the bootstrap administrator.
2. Open Agents and create an agent.
3. Sign out, then sign in as that agent.
4. Create a lead. It is assigned to the agent. Name is required, plus an email or a phone.
5. Open the lead. Move it from New to Contacted, then to Qualified.
6. Convert it to one deal. A second conversion is rejected.
7. Add a call, meeting, note, or email log. Email here is a record of a conversation, not a sent message.
8. Schedule a follow-up and mark it complete. Overdue items show on Follow-ups and the dashboard.
9. Move the deal from Proposal to Negotiation, then to Won. Won and Lost stay closed. Lost asks for a reason.
10. Sign back in as the administrator. The same lead, deal, activity, and follow-up are visible. Dashboard totals follow the signed-in user’s scope.

An agent who calls another agent’s lead URL receives `403`. A lost lead needs a reason. A qualified lead that already has a deal cannot be marked lost.

## Tests

`PolicyTest` checks lead and deal transitions, the lost-reason rule, and contact validation without a database.

`CrmApiIntegrationTest` runs against MySQL 8.4 and covers:

- the walkthrough above
- an agent blocked from another agent’s lead, from agent management, and from reassignment
- a deactivated agent’s existing session
- illegal status jumps, missing contact details, and a zero deal amount
- two concurrent conversions, where the unique `deals.lead_id` constraint leaves one row

## Limitations

- Sessions are stored in backend memory. Restarting the backend signs people out. Rows in MySQL remain.
- One backend instance. Sessions are not shared across replicas.
- There is no self-service registration, file upload, notification, payment, or outbound email.
- Optional demo data does not create accounts. It only adds fictional leads when `LEADNEST_DEMO_DATA=true` and the database has no leads yet.
- This project is meant to run locally. It does not include cloud hosting, a domain, Kubernetes, or CI.
