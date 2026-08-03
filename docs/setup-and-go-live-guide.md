# Setup & Go-Live Guide

Everything needed to run the University Asset Management System from scratch and share
it on the internet through a temporary tunnel.

---

## 1. Prerequisites

The only hard requirement is **Docker Desktop** (macOS/Windows) or Docker Engine + the
compose plugin (Linux). Everything else — Java, Node, PostgreSQL — runs inside
containers.

For sharing the app publicly you also need **cloudflared** (free, no account required):

```bash
# macOS
brew install cloudflared
# Windows
winget install Cloudflare.cloudflared
# Linux (Debian/Ubuntu)
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb -o cf.deb && sudo dpkg -i cf.deb
```

---

## 2. First-time setup

```bash
cd UniAssetManagement            # the project folder (unzip it anywhere first)
cp .env.example .env
```

Open `.env` and change at least these two lines:

| Variable | What to set |
|---|---|
| `JWT_SECRET` | any long random string, 48+ characters |
| `ADMIN_PASSWORD` | the password every seeded account will use |

Then start everything:

```bash
docker compose up --build -d
```

First build takes a few minutes (Maven and npm downloads). Watch progress with
`docker compose logs -f backend` — when you see `Started AssetManagementApplication`,
it's ready.

## 3. Using the app

| URL | What |
|---|---|
| http://localhost | The application |
| http://localhost/swagger-ui.html | Interactive API docs |
| http://localhost:8025 | MailHog — password-reset & notification emails land here |

Sign in with any seeded account (password = your `ADMIN_PASSWORD`):

| Email | Role |
|---|---|
| admin@university.local | Super Admin (everything) |
| asset.admin@university.local | Asset Administrator |
| lab.manager@university.local | Lab Manager (approvals, check-outs, issuing) |
| storekeeper@university.local | Storekeeper (stock) |
| maintenance@university.local | Maintenance Officer |
| finance@university.local | Finance Officer (payments, refunds) |
| lecturer@university.local | Lecturer (reserve, report faults) |
| student@university.local | Student |
| auditor@university.local | Auditor (read-only) |

A typical demo flow: sign in as **lecturer** → create a reservation → sign in as
**lab.manager** → approve it → Check-Out & Returns → check it out → record the return.
Watch the dashboard, notifications and audit log update along the way.

## 4. Everyday commands

```bash
docker compose up -d        # start (after the first build)
docker compose down         # stop, keep data
docker compose down -v      # stop and WIPE the database (re-seeds on next start)
docker compose logs -f backend
docker compose build backend && docker compose up -d backend   # redeploy after code changes
```

---

## 5. Going live with a temporary tunnel

A **Cloudflare quick tunnel** gives you a public HTTPS URL that forwards to your
machine. No account, no signup, free. The URL only works while the tunnel process runs.

**Start it** (with the app already running on port 80):

```bash
cloudflared tunnel --url http://localhost:80
```

Within a few seconds it prints your public URL, e.g.:

```
https://random-words-here.trycloudflare.com
```

Share that link — the whole app (frontend, API, Swagger) works through it.

**Stop it:** press `Ctrl+C` in that terminal, or `pkill cloudflared`. The link dies
immediately.

**Run it in the background instead:**

```bash
nohup cloudflared tunnel --url http://localhost:80 > tunnel.log 2>&1 &
grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" tunnel.log   # prints your URL
```

### Things to know about quick tunnels

- **New URL every time.** Restarting the tunnel produces a different random address.
- **It's public.** Anyone with the link can reach the login page, and the demo
  credentials are well-known. Before sharing beyond a quick demo, set a strong
  `ADMIN_PASSWORD` in `.env` and re-seed (`docker compose down -v && docker compose up -d`),
  or set `SEED_DEMO_DATA=false` so only the admin account exists.
- **Don't leave it running unattended** with demo data — kill it when the demo is done.
- MailHog (port 8025) is *not* exposed through the tunnel; it stays local.

### Want a permanent URL instead?

Use a **named tunnel** (free Cloudflare account + a domain you own):

```bash
cloudflared tunnel login                 # one-time browser auth
cloudflared tunnel create uniassets
cloudflared tunnel route dns uniassets assets.yourdomain.com
cloudflared tunnel run --url http://localhost:80 uniassets
```

The URL stays `https://assets.yourdomain.com` forever, and
`brew services start cloudflared` (with a config file) keeps it running across reboots.
For a real deployment, see `docs/deployment-guide.md`.

---

## 6. Troubleshooting

| Symptom | Fix |
|---|---|
| "You do not have permission…" on login through a tunnel | Already fixed in this build (forwarded-header handling). If it reappears, make sure you rebuilt: `docker compose build backend nginx && docker compose up -d` |
| Port 80 already in use | Change the nginx port mapping in `docker-compose.yml` to e.g. `"8088:80"` and tunnel to `http://localhost:8088` |
| Backend "unhealthy" | `docker compose logs backend` — usually a bad `DB_*`/`JWT_SECRET` value in `.env` |
| Forgot the admin password | Set a new `ADMIN_PASSWORD` in `.env`, then `docker compose down -v && docker compose up -d` (wipes data) |
| Want a clean slate | `docker compose down -v` removes the database and uploads volumes |
