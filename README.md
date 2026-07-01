# PBMP Developer Workbench + Cursor Launch (Demo)

This demo orchestrates your full development lifecycle inside PBMP.
**PBMP is the owner (orchestrator); Cursor is just a pluggable tool.**

## Lifecycle Flow (stage gates)

```
Business Requirements -> Business Analysis -> Solution Design ->
Pseudocode -> Coding (React/PHP/Kotlin) -> Testing -> Deployment
```

Each stage is a **gate**: the next stage cannot unlock until the previous one is approved.

## Standard governance included

1. **Stage gates / approvals** — the next stage unlocks only when the current one is approved, plus an audit entry.
2. **Pre-Coding Checklist (Definition of Ready)** — Requirement, Business object, UML,
   API, Design approved, Security, Coding standards.
3. **Coding stage = Cursor** — "Open in Cursor" is enabled only when
   - all stages up to Pseudocode are **approved**, and
   - the pre-coding checklist is **100%** complete.
   PBMP then generates `.cursor/rules/pbmp-context.mdc` and launches `cursor <folder>`.
4. **Validate & Complete (Definition of Done)** — after coding, standard checks run
   (coding standards/lint, security scan, unit tests, API contract). Only on pass does the
   coding stage get approved -> Testing unlocks.
5. **Audit Trail** — who did what, and when (governance log).
6. **Artifacts** — each stage has an artifact (requirement note, analysis, design, pseudocode).
7. **Role-based approval** — each stage can only be approved by the correct role:
   - Requirements/Analysis → **Business Analyst**
   - Design/Pseudocode → **Architect**
   - Coding → **Developer** (via validation)
   - Testing → **QA**, Deployment → **DevOps**
   Switch roles via the top-right "Logged in as"; approving with the wrong role is blocked.
8. **Git post-commit hook** — on "Open in Cursor", PBMP turns the project folder into a git
   repo and installs `.git/hooks/post-commit`. When the developer runs **git commit**, the
   hook automatically calls PBMP's `/validate` API -> on pass, the coding stage is approved
   and Testing unlocks.
9. **Real PHP backend integration** — validation now runs through PBMP's real PHP component
   (`pbmp-php/validate.php`). It runs **real `php -l` syntax lint**, coding standards
   (no var_dump/die/eval), and a hardcoded-secret security scan on the project's PHP files.
   If PHP is unavailable, it falls back gracefully.
10. **Real deploy at the Deployment stage** — the DevOps role clicks "🚀 Deploy to Production"
    -> the backend runs the real `deploy.sh`, which packages the project (`tar.gz`) and
    deploys it to `releases/<TASK>/<timestamp>/`, creating `manifest.json` + a `current`
    symlink. On success the deployment stage is approved (lifecycle complete).

## How to run (local)

```bash
node server.js
```

Open in your browser: http://localhost:4000  (to change the port: `PORT=5000 node server.js`)

## Deploy to the cloud (e.g. Zeabur)

The server reads `process.env.PORT` and binds to `0.0.0.0`, so it runs on Zeabur with start command `npm start`.

### Zeabur checklist (fixes 502)

1. **Variables** — delete any manual `PORT` override (e.g. `PORT=${WEB_PORT}`). Let Zeabur inject `PORT` automatically. Remove leftover vars from old apps (e.g. `PASSWORD`).
2. **Settings** — clear any custom Dockerfile snippet in the dashboard, or set it to **Load from GitHub** (this repo includes a `Dockerfile`). Leave Startup Command empty (`npm start` is used).
3. **Redeploy** — trigger a new deploy from the latest `main` commit and wait until status is **Running** (not Building/Canceled).
4. **Verify** — open `https://your-app.zeabur.app/health` — should return `{"ok":true,"port":8080}`.

Optional: set `PUBLIC_URL=https://your-app.zeabur.app` so git post-commit hooks call the cloud URL instead of localhost.

### Multi-user Cursor access (no Gmail login on every machine)

**You cannot store a Gmail password in PBMP** — Cursor does not allow third-party apps to log users into the desktop app with email/password.

The supported approach for multiple users:

1. **Cursor Team** — use org email `grow24.ai.collaboration@gmail.com` (or your team owner account).
2. **Service account API key** — in [Cursor Dashboard](https://cursor.com/dashboard) → Team → Service accounts, create a key.
3. **Zeabur Variables** (secrets, never commit):

| Variable | Example | Purpose |
|----------|---------|---------|
| `CURSOR_TEAM_EMAIL` | `grow24.ai.collaboration@gmail.com` | Shown in UI (display only) |
| `CURSOR_API_KEY` | `cursor_...` | Server-side; all PBMP users share cloud agents |
| `CURSOR_MODEL` | `auto` (default) | Optional; use `composer-2.5` if your plan supports it |

4. Redeploy. Users click **☁ Run in Cursor Cloud** — coding runs on Cursor’s servers via the team API key. **No Gmail login on each developer machine.**

**Open in Cursor (local)** still opens the folder on the user’s PC (requires Cursor installed). Use **Cloud** for true multi-user access from Zeabur.

**Important — how "Open in Cursor" works in the cloud:** the server runs in the cloud,
so it cannot launch Cursor on a visitor's computer, and a browser cannot write files to a
visitor's disk. The project must first reach the user's machine. So "Open in Cursor" offers
three ways to open **only that one project** on the user's own machine:

- **A) ZIP download** — download the project as a `.zip`, extract it, then run `cursor .` in that folder (recommended on cloud/Zeabur).
- **B) Git clone** — `git clone https://github.com/Grow24/bpmpcursor.git` then open `projects/<folder>` in Cursor.

## The two demo tasks

- **TASK-101** (Invoice) — currently at the `Business Analysis` stage. Approve the stages
  yourself -> fill the checklist -> launch Cursor -> validate.
- **TASK-102** (Attendance) — already at the `Coding` stage (design/pseudocode approved,
  checklist 100%). You can go straight to launching Cursor + validating.

## Structure

```
server.js                  # Node backend: stage gates, role-approval, validate, deploy, audit
pbmp-php/validate.php      # PBMP's PHP validation backend (real php -l + scans)
deploy.sh                  # Real deploy script (package -> releases/ + manifest)
public/                    # Developer Workbench UI (lifecycle, checklist, deploy log, audit)
data/tasks.json            # Tasks + 7-stage lifecycle + audit (PBMP's "knowledge")
projects/<feature>/        # The folder that opens in Cursor
  src/                     # Sample code (e.g. Invoice.php) validated by the PHP backend
  .cursor/rules/           # PBMP auto-generates context here
releases/<TASK>/<ts>/      # Deploy artifacts (tar.gz + manifest.json), 'current' symlink
```

## API

```bash
curl http://localhost:4000/api/tasks
curl -X POST http://localhost:4000/api/tasks/TASK-101/stages/analysis/approve -d '{"actor":"ba.rekha","role":"Business Analyst"}'
curl -X POST http://localhost:4000/api/tasks/TASK-101/checklist -d '{"key":"api","done":true}'
curl -X POST http://localhost:4000/api/tasks/TASK-101/launch
curl -X POST http://localhost:4000/api/tasks/TASK-101/validate   # runs the PHP backend
curl -X POST http://localhost:4000/api/tasks/TASK-101/stages/testing/approve -d '{"actor":"qa.neha","role":"QA"}'
curl -X POST http://localhost:4000/api/tasks/TASK-101/deploy -d '{"actor":"ops.raj","role":"DevOps"}'   # real deploy.sh
```

## Can also be run standalone

```bash
php pbmp-php/validate.php projects/invoice-feature "React + PHP"   # PHP validation
sh deploy.sh invoice-feature TASK-101 ./releases                  # manual deploy
```
