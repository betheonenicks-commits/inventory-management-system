# IAMS Installation Guide

Deploys the full stack — backend API, frontend, PostgreSQL, MinIO object store,
and an nginx reverse proxy — with a single command (US-PLAT-01 / NFR-DEPLOY-01).

## Host requirements

- Any Docker-capable host: Docker Engine 24+ (or Docker Desktop) with the
  Compose plugin v2+. Linux, Windows, and macOS all work.
- Sizing: 2 CPU cores, 4 GB RAM, and 10 GB free disk is comfortable for a
  small deployment. The first build additionally downloads Maven/npm
  dependencies (~1 GB, one-time).
- Free ports on the host (all remappable, see Configuration):
  | Port | Service | Override variable |
  |------|---------|-------------------|
  | 80   | Reverse proxy — the only port users need | `IAMS_HTTP_PORT` |
  | 5432 | PostgreSQL (dev-tool access only) | `DB_HOST_PORT` |
  | 9000/9001 | MinIO API / console | `MINIO_API_PORT` / `MINIO_CONSOLE_PORT` |

## Install

```bash
git clone <this repository> && cd <repository>
cp .env.example .env     # then edit .env - see Configuration below
docker compose up -d --build
```

That is the whole installation. The backend runs its own database migrations
(Flyway) and seeds the initial administrator on first start — there are no
manual post-start steps.

Watch it come up (order is health-gated: postgres → backend → proxy):

```bash
docker compose ps        # wait until every service shows "healthy"
```

Then open `http://<host>/` (or `http://<host>:$IAMS_HTTP_PORT/`) and log in
with the administrator credentials from your `.env`.

## Configuration

All configuration lives in `.env` (copied from `.env.example`). Before any
non-throwaway deployment, change at minimum:

- `DB_PASSWORD` — PostgreSQL password (also injected into the backend).
- `IAMS_JWT_SECRET` — token-signing secret, 32+ bytes. **Never ship the
  default.**
- `IAMS_DEV_ADMIN_PASSWORD` — the seeded administrator's password.
- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` — object-store credentials.

Port overrides (`IAMS_HTTP_PORT`, `DB_HOST_PORT`, `MINIO_API_PORT`,
`MINIO_CONSOLE_PORT`) are only needed when the defaults collide with something
already running on the host.

## Verify / troubleshoot

Each service has a real health probe, so a broken deployment is visible — a
failing service shows `unhealthy` or `restarting` in `docker compose ps`, and
dependents deliberately do not start on top of it (no silent partial stack).

```bash
docker compose ps                    # who is up, who is healthy
docker compose logs backend          # the usual suspect: DB credentials/config
docker compose logs postgres
docker compose logs proxy
```

Common causes, and what they look like:

- **Wrong `DB_PASSWORD` after the database volume already exists** — the
  Postgres volume keeps the password it was initialized with; changing `.env`
  later does not change the database. `backend` logs show
  `FATAL: password authentication failed for user "iams_app"` and the
  container restarts until fixed. Either restore the original password or, if
  the data is disposable, `docker compose down -v` and start fresh.
- **Host port already taken** — `docker compose up` fails immediately with
  `bind: Only one usage of each socket address` (Windows) or
  `address already in use` (Linux), naming the port. Set the matching override
  in `.env`.
- **Backend unhealthy while postgres is healthy** — read
  `docker compose logs backend`; Flyway and Spring fail fast with the concrete
  cause (bad credentials, incompatible schema, exhausted connections).

## Updating

```bash
git pull
docker compose up -d --build     # rebuilds changed images, migrates the DB
```

Database and object-store data live in named volumes
(`iams_postgres_data`, `iams_minio_data`) and survive rebuilds and
`docker compose down`. Only `docker compose down -v` deletes them.
