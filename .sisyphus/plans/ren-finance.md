# ren-finance - Personal Finance App

## TL;DR

> **Quick Summary**: Build a cross-platform personal finance app ("netwars" dashboard) for net worth tracking and expense planning. Clojure backend with Datahike storage, ClojureDart frontend for mobile + web, MCP server for AI agent integration. Crypto tracking via wallet address lookup (public blockchain APIs).
> 
> **Deliverables**:
> - Clojure backend with Datahike storage
> - Blockchain wallet address lookup (Etherscan, Blockchain.com APIs)
> - CSV import with deduplication
> - MCP server with read-only tools
> - ClojureDart frontend with net worth dashboard
> - User-selectable base currency for net worth calculation
> - VPS/Cloud deployment configuration
> 
> **Estimated Effort**: Large
> **Parallel Execution**: YES - 4 waves
> **Critical Path**: Task 1 → Task 4 → Task 8 → Task 12 → Task 15

---

## Context

### Original Request
User wants to track personal finances with automation (no manual entry). "Netwars" = net worth dashboard (Dota 2 slang). Russian banks have no automatic sync support, so focus on blockchain wallet address lookup + CSV imports. User wants to point to crypto wallet addresses directly, not use exchange APIs.

### Interview Summary
**Key Discussions**:
- **Automation focus**: User explicitly rejected manual entry. Blockchain wallet lookup + CSV imports are primary data sources
- **Russian banks**: No automatic sync available. SimpleFIN (US banks) deferred to future
- **Architecture**: Clojure backend + ClojureDart frontend + Datahike storage + MCP server
- **Crypto tracking**: User wants to point to wallet addresses directly (public blockchain data), not exchange APIs
- **Currency**: User-selectable base currency for net worth calculation (not hardcoded)
- **Deployment**: VPS/Cloud - accessible from anywhere

**Research Findings**:
- ClojureDart: Production-ready (Roam Research ships on App Store), 1,600+ stars
- MCP: Gaiwan SDK v0.2.17 is cleanest, supports latest protocol
- Datahike: 0.7.1663, file-based, Datomic-compatible schema, Norms for migrations
- Blockchain APIs: Public APIs (Etherscan, Blockchain.com) for wallet balance lookup
- Wallet lookup: No authentication needed, just query public blockchain data
- Design System: Revolut DESIGN.md from awesome-design-md (fintech aesthetic, dark theme)

### Metis Review
**Identified Gaps** (addressed):
- **Security**: No API key storage needed - wallet addresses are public data
- **Single-user assumption**: Confirmed - personal app, no multi-user
- **MVP scope**: Blockchain wallet lookup + CSV import
- **MCP access**: Read-only tools for MVP, no write access to financial data
- **Currency**: User-selectable base currency (not hardcoded)
- **Deployment**: VPS/Cloud - accessible from anywhere, needs HTTPS

---

## Work Objectives

### Project File Structure

```
ren-finance/
├── .sisyphus/
│   ├── design/
│   │   └── DESIGN.md                    # Revolut design system tokens
│   ├── evidence/                        # QA screenshots and test outputs
│   └── plans/
│       └── ren-finance.md               # This plan file
│
├── backend/                             # Clojure backend (JVM)
│   ├── deps.edn                         # Dependencies and aliases
│   ├── dev/
│   │   └── user.clj                     # REPL development helpers
│   ├── src/
│   │   └── ren_finance/
│   │       ├── core.clj                 # App entry point, config loading
│   │       ├── config.clj               # Configuration management (env vars, edn)
│   │       │
│   │       ├── db/                      # Datahike database layer
│   │       │   ├── conn.clj             # Database connection lifecycle
│   │       │   ├── schema.clj           # Complete Datomic-style schema
│   │       │   └── migrations.clj       # Norms migration runner
│   │       │
│   │       ├── wallets/                 # Blockchain wallet lookups
│   │       │   ├── protocol.clj         # defprotocol WalletLookup
│   │       │   ├── normalizer.clj       # Unified balance/tx shapes
│   │       │   ├── ethereum.clj         # Etherscan API client
│   │       │   ├── bitcoin.clj          # Blockchain.com API client
│   │       │   └── multi.clj            # Multi-wallet aggregation
│   │       │
│   │       ├── currency/                # Currency conversion
│   │       │   ├── converter.clj        # Exchange rate fetching + caching
│   │       │   └── rates.clj            # Rate storage in Datahike
│   │       │
│   │       ├── import/                  # CSV import
│   │       │   ├── csv.clj              # CSV parsing with column mapping
│   │       │   └── dedup.clj            # Idempotent deduplication logic
│   │       │
│   │       ├── queries/                 # Datalog queries
│   │       │   ├── net_worth.clj        # Net worth calculations
│   │       │   └── transactions.clj     # Transaction queries + filtering
│   │       │
│   │       ├── server/                  # HTTP server
│   │       │   ├── core.clj             # Aleph server lifecycle
│   │       │   ├── routes.clj           # Reitit route definitions
│   │       │   └── middleware.clj        # JSON, CORS, error handling
│   │       │
│   │       └── mcp/                     # MCP server
│   │           ├── server.clj           # Gaiwan SDK initialization
│   │           ├── tools.clj            # 4 read-only tool definitions
│   │           └── resources.clj        # MCP resource definitions
│   │
│   ├── test/
│   │   └── ren_finance/
│   │       ├── db_test.clj              # Schema + migration tests
│   │       ├── wallets_test.clj         # Wallet lookup tests (mocked)
│   │       ├── import_test.clj          # CSV import + dedup tests
│   │       ├── queries_test.clj         # Net worth query tests
│   │       ├── server_test.clj          # HTTP endpoint tests
│   │       └── integration_test.clj     # Full flow integration tests
│   │
│   ├── resources/
│   │   ├── migrations/
│   │   │   ├── 001-initial-schema.edn   # Initial Datahike schema
│   │   │   └── checksums.edn            # Migration checksums
│   │   └── config.edn                   # Default configuration
│   │
│   └── test-fixtures/
│       ├── binance_export.csv            # Sample Binance CSV
│       ├── generic_export.csv            # Sample generic CSV
│       └── mock_balances.edn            # Mock wallet balance data
│
├── frontend/                            # ClojureDart frontend (Flutter)
│   ├── deps.edn                         # Clojure deps + cljd config
│   ├── pubspec.yaml                     # Flutter/Dart dependencies
│   ├── lib/
│   │   └── cljd-out/                    # Generated Dart files (gitignored)
│   ├── src/
│   │   └── ren_finance/
│   │       ├── main.cljd                # App entry point
│   │       │
│   │       ├── model/                   # State management (re-dash)
│   │       │   ├── events.cljd          # Event handlers
│   │       │   ├── subs.cljd            # Subscriptions
│   │       │   └── effects.cljd         # Side effects (HTTP, storage)
│   │       │
│   │       ├── views/                   # UI components
│   │       │   ├── dashboard.cljd       # Net worth dashboard ("Netwars")
│   │       │   ├── import.cljd          # CSV import view
│   │       │   ├── settings.cljd        # Settings (currency, wallets)
│   │       │   └── widgets/             # Reusable widgets
│   │       │       ├── card.cljd        # Styled card component
│   │       │       ├── button.cljd      # Pill button component
│   │       │       ├── chart.cljd       # Net worth chart wrapper
│   │       │       └── nav.cljd         # Navigation bar
│   │       │
│   │       ├── theme/                   # Design tokens
│   │       │   ├── colors.cljd          # Color palette from DESIGN.md
│   │       │   ├── typography.cljd      # Font sizes, weights
│   │       │   └── spacing.cljd         # Spacing scale
│   │       │
│   │       └── api/                     # Backend API client
│   │           └── client.cljd          # HTTP client for backend
│   │
│   ├── android/                         # Generated by flutter create
│   ├── ios/                             # Generated by flutter create
│   ├── web/                             # Generated by flutter create
│   ├── macos/                           # Generated by flutter create
│   ├── linux/                           # Generated by flutter create
│   └── windows/                         # Generated by flutter create
│
├── deployment/                          # Docker + deployment configs
│   ├── Dockerfile                       # Multi-stage Clojure build
│   ├── docker-compose.yml               # Backend + Nginx + Datahike volume
│   ├── nginx/
│   │   ├── nginx.conf                   # Reverse proxy config
│   │   └── ssl.conf                     # SSL/TLS settings
│   ├── scripts/
│   │   ├── setup-vps.sh                 # Initial VPS setup (Docker, firewall)
│   │   ├── deploy.sh                    # Build + deploy script
│   │   └── renew-ssl.sh                 # Certbot renewal cron
│   └── systemd/
│       └── ren-finance.service          # Systemd service file
│
├── .gitignore
├── .env.example                         # Environment variable template
└── README.md                            # Project overview + setup instructions
```

### Key Files Explained

**Backend Core Files**:

| File | Purpose | Key Functions |
|------|---------|---------------|
| `backend/deps.edn` | Dependencies | Datahike 0.7.1663, Gaiwan MCP SDK 0.2.17, Malli, clj-http, Aleph |
| `backend/src/ren_finance/core.clj` | Entry point | `-main`, system start/stop |
| `backend/src/ren_finance/config.clj` | Config | `load-config`, reads from env vars + edn file |
| `backend/src/ren_finance/db/conn.clj` | DB connection | `create-db!`, `connect`, `disconnect` |
| `backend/src/ren_finance/db/schema.clj` | Schema | `finance-schema` vector of all attribute maps |
| `backend/src/ren_finance/wallets/protocol.clj` | Protocol | `defprotocol WalletLookup` with `fetch-balance`, `fetch-transactions` |
| `backend/src/ren_finance/wallets/ethereum.clj` | ETH lookup | `EthereumWallet` record implementing `WalletLookup` |
| `backend/src/ren_finance/wallets/bitcoin.clj` | BTC lookup | `BitcoinWallet` record implementing `WalletLookup` |
| `backend/src/ren_finance/server/core.clj` | HTTP server | `start-server`, `stop-server` using Aleph |
| `backend/src/ren_finance/server/routes.clj` | Routes | Reitit route tree for all API endpoints |
| `backend/src/ren_finance/mcp/server.clj` | MCP server | `start-mcp!` using Gaiwan SDK |

**Frontend Core Files**:

| File | Purpose | Key Functions |
|------|---------|---------------|
| `frontend/deps.edn` | Dependencies | ClojureDart, re-dash |
| `frontend/src/ren_finance/main.cljd` | Entry point | `(defn main [] ...)` |
| `frontend/src/ren_finance/model/events.cljd` | Events | `::load-dashboard`, `::import-csv`, `::set-currency` |
| `frontend/src/ren_finance/model/subs.cljd` | Subscriptions | `::net-worth`, `::accounts`, `::last-sync` |
| `frontend/src/ren_finance/views/dashboard.cljd` | Dashboard | Net worth display, account cards, chart |
| `frontend/src/ren_finance/theme/colors.cljd` | Colors | All color tokens from DESIGN.md |
| `frontend/src/ren_finance/views/settings.cljd` | Settings | Currency selector, wallet management, CSV mapping presets |
| `frontend/src/ren_finance/api/client.cljd` | API client | `fetch-net-worth`, `import-csv`, `sync-wallets` |

### Design System
**Source**: Revolut DESIGN.md from [awesome-design-md](https://github.com/VoltAgent/awesome-design-md)

**Key Design Principles**:
- **Two-mode canvas**: Near-black (`#000000`) for storytelling/dashboard, white (`#ffffff`) for forms/catalogue
- **Typography**: Aeonik Pro for display (20-136px, weight 500), Inter for body/UI (weight 400/600)
- **Primary CTA**: White pill on dark canvas (`#ffffff` bg, `#000000` text)
- **Brand accent**: Cobalt violet (`#494fdf`) - reserved for featured elements only
- **Buttons**: Pill-shaped (`rounded: 9999px`), 48px height
- **Cards**: Rounded 20px (`rounded.lg`), 32px padding
- **Inputs**: Rounded 12px (`rounded.md`), 56px height
- **Spacing**: 4px base unit, scale: 4/8/16/24/32/48/80/88/120px
- **No drop shadows**: Depth via canvas/surface-luminance shifts
- **Material 3**: All UI components MUST follow Material 3 (Material Design 3) guidelines for accessibility, touch targets, and interaction patterns

**Color Palette**:
- Canvas Dark: `#000000` (main dashboard background)
- Canvas Light: `#ffffff` (forms, settings)
- Surface Elevated: `#16181a` (cards on dark background)
- Primary: `#494fdf` (brand accent, featured cards)
- Ink: `#191c1f` (text on light)
- On-Dark: `#ffffff` (text on dark)
- Accent Teal: `#00a87e` (positive/gains)
- Accent Danger: `#e23b4a` (negative/losses)
- Accent Warning: `#ec7e00` (warnings)

**Component Tokens**:
- `button-primary`: White pill on dark, 48px height, `rounded.full`
- `button-dark`: Dark pill on light, 48px height, `rounded.full`
- `feature-card-dark`: `#16181a` bg, 20px rounded, 32px padding
- `plan-card-featured`: `#494fdf` bg, 20px rounded, 32px padding
- `text-input`: White bg, 12px rounded, 56px height
- `nav-bar`: Dark bg, 64px height

**Design File**: `.sisyphus/design/DESIGN.md` (copy of Revolut DESIGN.md)

### Core Objective
Build a personal finance tracking app that automatically fetches crypto wallet balances via public blockchain APIs and imports CSV files, displays net worth on a cross-platform dashboard, and exposes financial data via MCP for AI agent integration.

### Concrete Deliverables
- Clojure HTTP backend with Datahike storage
- Blockchain wallet address lookup (Etherscan, Blockchain.com APIs)
- CSV import with idempotent deduplication
- MCP server with 4 read-only tools
- ClojureDart mobile + web app with net worth dashboard
- User-selectable base currency for net worth calculation
- VPS/Cloud deployment configuration

### Definition of Done
- [ ] Given a valid wallet address, system fetches balance via public blockchain API
- [ ] Given a CSV file, system imports transactions without duplicates
- [ ] MCP server exposes `get_net_worth` tool returning current net worth breakdown
- [ ] ClojureDart app displays net worth dashboard with last-sync timestamp
- [ ] App works offline showing last-known state
- [ ] User can select base currency for net worth calculation

### Must Have
- Wallet address lookup via public blockchain APIs
- User-selectable base currency for net worth calculation
- Idempotent CSV import (no duplicates)
- Datalog queries for net worth calculation
- MCP read-only tools (no write access to financial data)
- Offline support in mobile app
- HTTPS for VPS/Cloud deployment

### Must NOT Have (Guardrails)
- **No bank API integration** (except deferred SimpleFIN)
- **No multi-user support** - single-user personal app
- **No real-time trading / order execution** - read-only portfolio tracking
- **No tax calculation / reporting** - net worth and expense planning only
- **No AI financial advice** - MCP provides data access, not recommendations
- **No bank-grade audit trails** - no immutable ledger design
- **No exchange API integration** - wallet address lookup only (public data)
- **No API key storage needed** - wallet addresses are public
- **No multi-currency conversion in MVP** - pick one base currency

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: NO (greenfield project)
- **Automated tests**: YES (Tests-after)
- **Framework**: clojure.test (standard Clojure testing)

### QA Policy
Every task MUST include agent-executed QA scenarios.
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Backend**: Use Bash (curl) - Send requests, assert status + response fields
- **Frontend**: Use Playwright (playwright skill) - Navigate, interact, assert DOM, screenshot
- **MCP**: Use MCP Inspector or curl - Test tool invocations

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - foundation):
├── Task 1: Project scaffolding + deps.edn [quick]
├── Task 2: Datahike schema + migrations [unspecified-high]
├── Task 3: Blockchain wallet lookup protocol [quick]
├── Task 4: Wallet address lookup implementation [deep]
├── Task 5: CSV import with deduplication [unspecified-high]
└── Task 6: Currency conversion module [quick]

Wave 2 (After Wave 1 - backend core):
├── Task 7: HTTP server + routes [unspecified-high]
├── Task 8: Net worth calculation queries [deep]
├── Task 9: MCP server setup [unspecified-high]
├── Task 10: MCP tools implementation [unspecified-high]
└── Task 11: Backend integration tests [unspecified-high]

Wave 3 (After Wave 2 - frontend):
├── Task 12: ClojureDart project setup [quick]
├── Task 13: Net worth dashboard UI [visual-engineering]
├── Task 14: CSV import UI [visual-engineering]
├── Task 14a: Settings view (currency + wallets) [visual-engineering]
└── Task 15: Offline support + state management [unspecified-high]

Wave 4 (After Wave 3 - deployment):
├── Task 16: VPS deployment configuration [unspecified-high]
├── Task 17: HTTPS + reverse proxy setup [unspecified-high]
└── Task 18: Multi-wallet aggregation [unspecified-high]

Wave FINAL (After ALL tasks):
├── Task F1: Plan compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Real manual QA (unspecified-high)
└── Task F4: Scope fidelity check (deep)
-> Present results -> Get explicit user okay
```

### Dependency Matrix

| Task | Depends On | Blocks |
|------|------------|--------|
| 1 | - | 2-6 |
| 2 | 1 | 7, 8 |
| 3 | 1 | 4 |
| 4 | 3 | 7, 11 |
| 5 | 1 | 7, 11 |
| 6 | 1 | 8, 13 |
| 7 | 2, 4, 5 | 9, 10, 11 |
| 8 | 2, 6 | 10, 13 |
| 9 | 7 | 10 |
| 10 | 8, 9 | 11 |
| 11 | 7, 10 | F1-F4 |
| 12 | - | 13, 14, 15 |
| 13 | 8, 12 | F1-F4 |
| 14 | 5, 12 | F1-F4 |
| 14a | 7, 12 | F1-F4 |
| 15 | 12 | F1-F4 |
| 16 | 11 | F1-F4 |
| 17 | 16 | F1-F4 |
| 18 | 4 | F1-F4 |

### Agent Dispatch Summary

- **Wave 1**: 6 tasks - T1 `quick`, T2 `unspecified-high`, T3 `quick`, T4 `deep`, T5 `unspecified-high`, T6 `quick`
- **Wave 2**: 5 tasks - T7 `unspecified-high`, T8 `deep`, T9 `unspecified-high`, T10 `unspecified-high`, T11 `unspecified-high`
- **Wave 3**: 5 tasks - T12 `quick`, T13 `visual-engineering`, T14 `visual-engineering`, T14a `visual-engineering`, T15 `unspecified-high`
- **Wave 4**: 3 tasks - T16 `unspecified-high`, T17 `unspecified-high`, T18 `unspecified-high`
- **FINAL**: 4 tasks - F1 `oracle`, F2 `unspecified-high`, F3 `unspecified-high`, F4 `deep`

---

## Parallel Execution Strategy

> **Problem**: Multiple agents writing to shared filesystem causes race conditions and overwrites.
> **Solution**: Each task gets its own git branch + worktree via **Work Trunk** (`wt` CLI). Agents work in isolation. Orchestrator merges.

### Workflow

```
┌──────────────────────────────────────────────────────────┐
│ Orchestrator (Sisyphus)                                  │
│  1. `wt create task-N-description`  → new branch + dir   │
│  2. Delegate agent to that worktree                      │
│  3. Agent works, commits, signals done                   │
│  4. Orchestrator: `wt merge`  → merge to main + cleanup  │
│  5. Next task(s)                                         │
└──────────────────────────────────────────────────────────┘
```

### Setup (before any task executes)

```bash
# Install Work Trunk (one-time)
# From: https://github.com/gItNbY/work-trunk (or as shown in video)
curl -sSf https://raw.githubusercontent.com/gItNbY/work-trunk/main/install.sh | sh

# Initialize repo
git init
git add .sisyphus/plans/ren-finance.md
git commit -m "chore: initial plan"
git branch -M main
```

### Per-Task Execution

For each task, the orchestrator follows this exact sequence:

1. **Create worktree**: `wt create task-{N}-{slug}`
   - Creates branch `task-{N}-{slug}` from `main`
   - Creates worktree at `../ren-finance/task-{N}-{slug}/`
   - Auto-cds into worktree

2. **Dispatch agent** to worktree directory:
   - Agent uses `write`/`edit` tools inside worktree
   - Agent does NOT run git commands (orchestrator handles commits)
   - Agent completes work, verifies with QA scenarios

3. **Commit**: Orchestrator stages all changes in worktree branch, commits with task message

4. **Merge**: `wt merge` in the worktree
   - Stashes any pending changes (should be none — agent committed)
   - Switches to main branch
   - Merges task branch into main
   - Restores any stashed changes
   - Removes worktree and branch

### Branch Naming Convention

| Task | Branch | Worktree Path |
|------|--------|---------------|
| 1 | `task-1-scaffolding` | `../ren-finance/task-1-scaffolding/` |
| 2 | `task-2-schema` | `../ren-finance/task-2-schema/` |
| 3 | `task-3-protocol` | `../ren-finance/task-3-protocol/` |
| 4 | `task-4-wallets` | `../ren-finance/task-4-wallets/` |
| 5 | `task-5-csv-import` | `../ren-finance/task-5-csv-import/` |
| 6 | `task-6-currency` | `../ren-finance/task-6-currency/` |
| 7 | `task-7-http-server` | `../ren-finance/task-7-http-server/` |
| 8 | `task-8-queries` | `../ren-finance/task-8-queries/` |
| 9 | `task-9-mcp-server` | `../ren-finance/task-9-mcp-server/` |
| 10 | `task-10-mcp-tools` | `../ren-finance/task-10-mcp-tools/` |
| 11 | `task-11-integration-tests` | `../ren-finance/task-11-integration-tests/` |
| 12 | `task-12-cljd-setup` | `../ren-finance/task-12-cljd-setup/` |
| 13 | `task-13-dashboard` | `../ren-finance/task-13-dashboard/` |
| 14 | `task-14-csv-ui` | `../ren-finance/task-14-csv-ui/` |
| 14a | `task-14a-settings` | `../ren-finance/task-14a-settings/` |
| 15 | `task-15-offline` | `../ren-finance/task-15-offline/` |
| 16 | `task-16-deployment` | `../ren-finance/task-16-deployment/` |
| 17 | `task-17-https` | `../ren-finance/task-17-https/` |
| 18 | `task-18-multi-wallet` | `../ren-finance/task-18-multi-wallet/` |

### File Collision Rules

"Can Run In Parallel: YES" means **disjoint file writes** — no two tasks in a parallel group write to the same file path. Verified by collision matrix:

| Parallel Group | Tasks | Files Written | Collision Risk |
|----------------|-------|---------------|----------------|
| Wave 1 (post T1) | 2,3,4,5,6 | `db/`, `wallets/`, `import/`, `currency/` — all separate dirs | **None** — fully parallel safe |
| Wave 2 | 7,8,9,10,11 | `server/`, `queries/`, `mcp/`, `test/` — all separate dirs | **9→10 sequential** (T9 creates `tools.clj`, T10 fills it). Per matrix, T9 blocks T10. |
| Wave 3 | 12,13,14,14a,15 | `main.cljd`, `model/`, `views/`, `theme/`, `api/` | **12 blocks all** (creates project scaffold). **13→14→14a→15 sequential** within Wave 3 — they share `frontend/` tree and Task 12's generated files. |
| Wave 4 | 16,17,18 | `deployment/`, `nginx/`, `scripts/` + `wallets/multi.clj` | **None** — fully parallel safe. |

### Merge Conflict Resolution

Since tasks in a parallel group write to **disjoint file trees**, merge conflicts should not occur. If they do:

1. Orchestrator resolves by taking both changes (different files → auto-merge)
2. If same file conflict detected: sequential re-merge, taking later task's version
3. Escalate: run `git mergetool` and manually review

### Execution Order Per Wave

```
Wave 1:
  1. Task 1 (blocks 2-6) → wt create → agent → wt merge
  2. Tasks 2,3,4,5,6 in parallel → each wt create → each agent → each wt merge
     (merge order: 2→3→4→5→6 — any order works, disjoint files)

Wave 2:
  1. Tasks 7,8 in parallel → separate worktrees → agents → merge
  2. Task 9 → (requires Task 7) → agent → merge
  3. Task 10 → (requires Tasks 8,9) → agent → merge
  4. Task 11 → (requires Tasks 7,10) → agent → merge

Wave 3:
  1. Task 12 → (creates frontend scaffold) → agent → merge
  2. Tasks 13,14,14a in parallel → each wt create → each agent → each wt merge
     (disjoint views/ files: dashboard.cljd, import.cljd, settings.cljd)
  3. Task 15 → (modifies model/effects.cljd, needs all views done) → agent → merge

Wave 4:
  1. Tasks 16,18 in parallel → separate worktrees → agents → merge
  2. Task 17 → (requires Task 16) → agent → merge

Final Wave:
  F1→F2→F3→F4 sequential (each reads previous result)
```

### Commit Strategy

Each worktree merge produces a single commit on `main`:
- Task branch commits are squashed into one commit per task
- Commit message from task's "**Commit**" section
- This keeps `main` history clean: 19 commits (one per task) + 4 final commits

---

## TODOs

- [ ] 1. Project Scaffolding + deps.edn

  **What to do**:
  - Create `backend/deps.edn` with all dependencies (Datahike, Gaiwan MCP SDK, Malli, clj-http, Aleph, buddy)
  - Create `frontend/deps.edn` with ClojureDart dependency:
    ```clojure
    {:deps {io.github.tensegritics/clojuredart
            {:git/url "https://github.com/tensegritics/ClojureDart"
             :git/sha "e81f1eb2e8fe0ce46cc3e74e4f5131ffcf4939c5"}}}
    ```
    (Pin SHA at planning time. Update if ClojureDart has breaking changes.)
  - **Dev aliases** in `backend/deps.edn` for test/coverage/lint:
    ```clojure
    :aliases {:test {:extra-deps {lambdaisland/kaocha {:mvn/version "1.91.1392"}}
                     :main-opts ["-m" "kaocha.runner"]}
              :coverage {:extra-deps {cloverage/cloverage {:mvn/version "1.2.4"}}
                         :main-opts ["-m" "cloverage.coverage"]}
              :lint {:extra-deps {clj-kondo/clj-kondo {:mvn/version "2024.11.14"}}
                     :main-opts ["-m" "clj-kondo.main" "--lint" "src" "test"]}}
    ```
  - Create directory structure: `backend/src/ren_finance/`, `backend/test/`, `backend/resources/migrations/`
  - Create `frontend/src/ren_finance/` directory structure
  - Create `deployment/` directory with placeholder files
  - Create `backend/dev/user.clj` for REPL development
  - Create `.gitignore` with Clojure/Java/Flutter ignores
  - Create `README.md` with project overview
  - Create `.env.example` with environment variable template
  - **Config structure** — define in `resources/config.edn`:
    ```clojure
    ;; Default configuration (overridden by env vars)
    {:db/storage-path  "./data/ren-finance"   ;; File path for Datahike storage
     :server/http-port 3000                    ;; Backend HTTP server port
     :mcp/transport    :stdio                  ;; :stdio or :http
     :mcp/http-port    3999                    ;; MCP HTTP transport port (when :http)
     :cors/allowed-origins ["http://localhost:*"]
     :log/level        :info                   ;; :debug, :info, :warn, :error
     :currency/default :USD                    ;; Fallback base currency
     :sync/wallet-refresh-hours 6}             ;; Hours between wallet auto-refresh
    ```
  - **`.env.example`** — environment variable overrides:
    ```
    REN_STORAGE_PATH=/var/lib/ren-finance/data
    REN_HTTP_PORT=3000
    REN_MCP_TRANSPORT=stdio
    REN_MCP_HTTP_PORT=3999
    REN_LOG_LEVEL=info
    REN_DEFAULT_CURRENCY=USD
    REN_CORS_ORIGINS=http://localhost:*,https://example.com
    ```
    Config priority: env vars > config.edn > hardcoded defaults

  **Must NOT do**:
  - No frontend code yet (Wave 3)
  - No exchange implementations yet (Wave 1 Task 4)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple scaffolding task, no complex logic
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 5, 6)
  - **Blocks**: Tasks 2-6
  - **Blocked By**: None (can start immediately)

  **References**:
  - Datahike: `org.replikativ/datahike {:mvn/version "0.7.1663"}`
  - MCP SDK: `co.gaiwan/mcp-sdk {:mvn/version "0.2.17"}`
  - Malli: `metosin/malli {:mvn/version "0.16.0"}`
  - HTTP client: `clj-http {:mvn/version "3.12.3"}`
  - HTTP server: `aleph/aleph {:mvn/version "0.8.3"}`
  - Routing: `metosin/reitit {:mvn/version "0.7.0"}`
  - Crypto: `buddy/buddy-core {:mvn/version "1.10.413"}`
  - ClojureDart: `io.github.tensegritics/clojuredart {:git/url "https://github.com/tensegritics/ClojureDart" :git/sha "e81f1eb..."}`
  - Logging: `org.clojure/tools.logging {:mvn/version "1.3.0"}`, `ch.qos.logback/logback-classic {:mvn/version "1.5.12"}`
  - Test runner: `lambdaisland/kaocha {:mvn/version "1.91.1392"}`
  - Coverage: `cloverage/cloverage {:mvn/version "1.2.4"}`
  - Linter: `clj-kondo/clj-kondo {:mvn/version "2024.11.14"}`
  - CSV: `org.clojure/data.csv {:mvn/version "1.1.0"}`

  **Acceptance Criteria**:
  - [ ] `backend/deps.edn` exists with all dependencies
  - [ ] `frontend/deps.edn` exists with ClojureDart dependency
  - [ ] Directory structure created as specified
  - [ ] `clojure -Spath` resolves without errors in backend/
  - [ ] `.gitignore` covers Clojure, Java, Flutter artifacts

  **QA Scenarios**:
  ```
  Scenario: Dependencies resolve correctly
    Tool: Bash
    Steps:
      1. Run `clojure -Spath` in project root
      2. Verify output contains datahike, mcp-sdk, malli paths
    Expected Result: Command exits 0, paths contain expected libraries
    Evidence: .sisyphus/evidence/task-1-deps-resolve.txt
  ```

  **Commit**: YES
  - Message: `feat(core): add project scaffolding and deps.edn`
  - Files: `backend/deps.edn`, `frontend/deps.edn`, `.gitignore`, `README.md`, `.env.example`

- [ ] 2. Datahike Schema + Migrations

  **What to do**:
  - Create `backend/src/ren_finance/db/schema.clj` with complete finance schema
  - Schema entities: Account, Category, Transaction, Payee, Wallet, ExchangeRate (Budget in schema but no API/UI in MVP)
  - Use `:db.type/bigdec` for monetary amounts
  - Index `:transaction/date`, `:transaction/account`, `:transaction/category`
  - Create `backend/resources/migrations/001-initial-schema.edn`
  - Create `backend/src/ren_finance/db/conn.clj` for database connection management
    - **Datahike config format**:
      ```clojure
      (def db-config
        {:store {:backend :file
                 :path (or (System/getenv "REN_STORAGE_PATH") "./data/ren-finance")}
         :keep-history? true           ;; REQUIRED — Task 8 depends on d/as-of
         :schema-flexibility :read     ;; Allow adding attributes post-creation
         :initial-data (load-migration "resources/migrations/001-initial-schema.edn")})
      ```
    - `:keep-history? true` is **non-negotiable** — historical net worth queries fail silently without it
    - Lifecycle: `create-db!` (if-not-exists → `d/create-database`) → `connect` (`d/connect`) → `disconnect` (`d/release`)
  - Create `backend/src/ren_finance/db/migrations.clj` for Norms migration runner
  - **Schema specification** (all attributes per entity):

    **Account** — prefix `:account/`:
    | Attribute | Value Type | Cardinality | Unique | Notes |
    |---|---|---|---|---|
    | `:name` | `:db.type/string` | `:one` | — | Display name |
    | `:type` | `:db.type/keyword` | `:one` | — | `:checking`, `:savings`, `:crypto`, `:cash`, `:credit` |
    | `:balance` | `:db.type/bigdec` | `:one` | — | Current balance |
    | `:currency` | `:db.type/keyword` | `:one` | — | `:USD`, `:EUR`, `:BTC`, `:ETH`, `:RUB` |
    | `:opened-date` | `:db.type/instant` | `:one` | — | When account opened |

    **Transaction** — prefix `:transaction/`:
    | Attribute | Value Type | Cardinality | Unique | Notes |
    |---|---|---|---|---|
    | `:date` | `:db.type/instant` | `:one` | — | Indexed. When transaction occurred |
    | `:amount` | `:db.type/bigdec` | `:one` | — | Positive=income, Negative=expense |
    | `:description` | `:db.type/string` | `:one` | — | Merchant or description |
    | `:account` | `:db.type/ref` | `:one` | — | Ref to `:account/db` |
    | `:category` | `:db.type/ref` | `:one` | — | Ref to `:category/db` |
    | `:type` | `:db.type/keyword` | `:one` | — | `:income`, `:expense`, `:transfer` |
    | `:import-hash` | `:db.type/string` | `:one` | `:db.unique/identity` | SHA-256 of date+amount+description+account |
    | `:import-source` | `:db.type/keyword` | `:one` | — | `:csv`, `:manual`, `:wallet-sync` |

    **Category** — prefix `:category/`:
    | Attribute | Value Type | Cardinality | Unique | Notes |
    |---|---|---|---|---|
    | `:name` | `:db.type/string` | `:one` | — | "Groceries", "Salary" |
    | `:type` | `:db.type/keyword` | `:one` | — | `:income`, `:expense` |
    | `:parent` | `:db.type/ref` | `:one` | — | Optional — ref to `:category/db` for hierarchy |
    | `:color` | `:db.type/string` | `:one` | — | Optional hex `"#00a87e"` |

    **Wallet** — prefix `:wallet/`:
    | Attribute | Value Type | Cardinality | Unique | Notes |
    |---|---|---|---|---|
    | `:address` | `:db.type/string` | `:one` | `:db.unique/identity` | Blockchain address |
    | `:chain-type` | `:db.type/keyword` | `:one` | — | `:ETH`, `:BTC` |
    | `:label` | `:db.type/string` | `:one` | — | Optional user label |
    | `:last-balance` | `:db.type/bigdec` | `:one` | — | Cached last-fetched balance |
    | `:last-sync` | `:db.type/instant` | `:one` | — | Timestamp of last fetch |

    **ExchangeRate** — prefix `:rate/`:
    | Attribute | Value Type | Cardinality | Unique | Notes |
    |---|---|---|---|---|
    | `:base-currency` | `:db.type/keyword` | `:one` | — | `:USD` |
    | `:target-currency` | `:db.type/keyword` | `:one` | — | `:EUR` |
    | `:rate` | `:db.type/double` | `:one` | — | Exchange rate value |
    | `:timestamp` | `:db.type/instant` | `:one` | — | When rate was fetched |

    **Payee** — prefix `:payee/` (reserved for future):
    | Attribute | Value Type | Cardinality | Unique | Notes |
    |---|---|---|---|---|
    | `:name` | `:db.type/string` | `:one` | — | Merchant/counterparty name |

    **Budget** (reserved for future — in schema only, no API endpoints or UI in MVP)

  **Seed default categories** in migration `001-initial-schema.edn`:
  ```clojure
  ;; Income categories
  {:db/id :category-1, :category/name "Salary", :category/type :income, :category/color "#00a87e"}
  {:db/id :category-2, :category/name "Freelance", :category/type :income, :category/color "#00a87e"}
  {:db/id :category-3, :category/name "Investment", :category/type :income, :category/color "#494fdf"}
  ;; Expense categories
  {:db/id :category-4, :category/name "Housing", :category/type :expense, :category/color "#e23b4a"}
  {:db/id :category-5, :category/name "Food", :category/type :expense, :category/color "#ec7e00"}
  {:db/id :category-6, :category/name "Transport", :category/type :expense, :category/color "#494fdf"}
  {:db/id :category-7, :category/name "Shopping", :category/type :expense, :category/color "#e23b4a"}
  {:db/id :category-8, :category/name "Entertainment", :category/type :expense, :category/color "#00a87e"}
  {:db/id :category-9, :category/name "Bills", :category/type :expense, :category/color "#e23b4a"}
  {:db/id :category-10, :category/name "Other", :category/type :expense, :category/color "#ffffff"}
  ```

  **Must NOT do**:

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Schema design requires careful thought about data modeling
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3, 5, 6)
  - **Blocks**: Tasks 7, 8
  - **Blocked By**: Task 1 (needs deps.edn)

  **References**:
  - Datahike schema docs: `https://github.com/replikativ/datahike/blob/main/doc/schema.md`
  - Datahike norms: `https://github.com/replikativ/datahike/blob/main/doc/norms.md`
  - Schema pattern: `:db/ident`, `:db/valueType`, `:db/cardinality`, `:db/unique`

  **Acceptance Criteria**:
  - [ ] Schema file exists with all entity definitions
  - [ ] Migration file exists in `backend/resources/migrations/`
  - [ ] `(d/create-database cfg)` succeeds with schema
  - [ ] `(d/transact conn schema)` succeeds

  **QA Scenarios**:
  ```
  Scenario: Schema transacts successfully
    Tool: Bash
    Steps:
      1. Run `clojure -M -m ren-finance.db.conn` in backend/
      2. Verify database created at configured path
      3. Verify schema entities exist in database
    Expected Result: Database created, schema transacted without errors
    Evidence: .sisyphus/evidence/task-2-schema-transact.txt

  Scenario: Migration runs idempotently
    Tool: Bash
    Steps:
      1. Run migrations twice
      2. Verify no duplicate schema entities
    Expected Result: Second run is no-op
    Evidence: .sisyphus/evidence/task-2-migration-idempotent.txt
  ```

  **Commit**: YES (groups with Task 1)
  - Message: `feat(db): add Datahike schema and migration system`
  - Files: `backend/src/ren_finance/db/`, `backend/resources/migrations/`

- [ ] 3. Blockchain Wallet Lookup Protocol

  **What to do**:
  - Create `backend/src/ren_finance/wallets/protocol.clj` with `defprotocol WalletLookup`
  - Methods: `fetch-balance`, `fetch-transactions`, `wallet-address`
  - Create `backend/src/ren_finance/wallets/normalizer.clj` for unified data format
  - Define normalized balance and transaction shapes for wallet data

  **Must NOT do**:
  - No exchange API integration (wallet address lookup only)
  - No authentication handling (public data)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Protocol definition is straightforward
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 5, 6)
  - **Blocks**: Task 4
  - **Blocked By**: Task 1

  **References**:
  - Clojure protocols: `clojure.core/defprotocol`
  - Normalized shape: `{:asset :btc, :balance 1.5M, :wallet "0x123..."}`

  **Acceptance Criteria**:
  - [ ] Protocol file exists with `WalletLookup` definition
  - [ ] Normalizer file exists with conversion functions
  - [ ] Protocol compiles without errors

  **QA Scenarios**:
  ```
  Scenario: Protocol compiles
    Tool: Bash
    Steps:
      1. Run `clojure -M -e "(require 'ren-finance.wallets.protocol)"` in backend/
      2. Verify no compilation errors
    Expected Result: Namespace loads successfully
    Evidence: .sisyphus/evidence/task-3-protocol-compile.txt
  ```

  **Commit**: YES (groups with Tasks 1, 2)
  - Message: `feat(wallets): add wallet lookup protocol and normalizer`
  - Files: `backend/src/ren_finance/wallets/protocol.clj`, `backend/src/ren_finance/wallets/normalizer.clj`

- [ ] 4. Wallet Address Lookup Implementation

  **What to do**:
  - Create `backend/src/ren_finance/wallets/ethereum.clj` for Ethereum wallet lookup
  - Create `backend/src/ren_finance/wallets/bitcoin.clj` for Bitcoin wallet lookup
  - Implement `WalletLookup` protocol for both chains
  - Use public APIs: Etherscan for ETH, Blockchain.com for BTC
  - Fetch wallet balances via public REST APIs
  - Handle rate limiting (public APIs have limits)
  - Normalize responses to unified format
  - **Rate limiting**: Implement exponential backoff with jitter
    - Etherscan: 5 calls/sec limit, delay 200ms + random jitter on 429 responses
    - Blockchain.com: free tier rate limits, delay 1s + random jitter on 429
    - Circuit breaker: stop retrying after 5 consecutive failures, log warning, return `:rate-limited` status
  - **Logging**: Log all API calls with `:wallet/address`, `:api/response-time-ms`, `:api/status` at INFO level
  - **Error handling**: Wrap all HTTP calls in try/catch, return `:api-error` status with error message for network failures
  - **Sync concurrency**: Fetch multiple wallets **sequentially**, not in parallel
    - Rationale: Public APIs (Etherscan, Blockchain.com) rate-limit per IP
    - Sequential avoids hitting per-IP limits when multiple wallets share same chain
    - Insert 500ms delay between each wallet fetch (API fair use)
    - Task 18 (multi-wallet) builds batch orchestration on top of this

  **Must NOT do**:
  - No exchange API integration
  - No private key handling (public address only)

  **References**:
  - Etherscan API: `https://docs.etherscan.io/`
  - Blockchain.com API: `https://www.blockchain.com/api`
  - Rate limits: respect public API limits

  **Acceptance Criteria**:
  - [ ] Given valid ETH address, `fetch-balance` returns ETH balance
  - [ ] Given valid BTC address, `fetch-balance` returns BTC balance
  - [ ] Rate limiting handled gracefully
  - [ ] Responses normalized to unified format

  **QA Scenarios**:
  ```
  Scenario: Fetch ETH balance
    Tool: Bash
    Preconditions: Valid Ethereum address
    Steps:
      1. Call `(fetch-balance adapter "0x...")`
      2. Verify response contains `:asset`, `:balance` keys
      3. Verify balance is BigDecimal
    Expected Result: Balance returned with correct shape
    Evidence: .sisyphus/evidence/task-4-eth-balance.txt

  Scenario: Fetch BTC balance
    Tool: Bash
    Preconditions: Valid Bitcoin address
    Steps:
      1. Call `(fetch-balance adapter "1...")`
      2. Verify response contains `:asset`, `:balance` keys
    Expected Result: Balance returned with correct shape
    Evidence: .sisyphus/evidence/task-4-btc-balance.txt
  ```

  **Commit**: YES
  - Message: `feat(wallets): add Ethereum and Bitcoin wallet lookup`
  - Files: `backend/src/ren_finance/wallets/ethereum.clj`, `backend/src/ren_finance/wallets/bitcoin.clj`

- [ ] 5. CSV Import with Deduplication

  **What to do**:
  - Create `backend/src/ren_finance/import/csv.clj`
  - Parse CSV files with configurable column mapping
  - Idempotent import: detect duplicates by `transaction/id` or hash of `date+amount+description`
  - Support common CSV formats: Binance export, generic bank export
  - Create `backend/src/ren_finance/import/dedup.clj` for deduplication logic
  - Create test fixtures: `backend/test-fixtures/binance_export.csv`, `backend/test-fixtures/generic_export.csv`
  - **Column mapping persistence**: Save user's column mapping configuration to Datahike
    - Schema: `{:csv-mapping/name string?, :csv-mapping/columns {:date string, :amount string, :description string, :account string}}`
    - Auto-apply last-used mapping when importing from the same CSV format
    - Allow naming and switching between multiple mapping presets
    - API endpoints: `GET /api/settings/csv-mappings`, `PUT /api/settings/csv-mappings`
  - **CSV format specifications**:
    - **Binance export** columns (in order): `Date(UTC),Pair,Type,Order Price,Order Amount,Avarage Txn Price,Filled,Total,Status`
      - Date format: `"2026-01-15 10:30:00"` (UTC)
      - Mapping: `:date` → `Date(UTC)`, `:amount` → `Total`, `:description` → `Pair`, account inferred from context
      - Dedup by `:transaction/import-hash` (hash of date+amount+pair)
    - **Generic format** columns: `date,amount,description`
      - Date format: `"2026-01-15"` (auto-detect YYYY-MM-DD or MM/DD/YYYY)
      - Amount: signed decimal — negative = expense, positive = income, unsigned = expense
      - Dedup by `:transaction/import-hash` (hash of date+amount+description)
    - **Column mapping config format**: user-provided EDN/map
      ```clojure
      {:date "Date(UTC)", :amount "Total", :description "Pair", :account "Binance"}
      ```
    - **Test fixture formats**:
      - `binance_export.csv`: 10 rows mimicking real Binance trade export
      - `generic_export.csv`: 10 rows with `date,amount,description` format, mix of income (+) and expense (-)
    - **CSV partial failure handling**: All-or-nothing per file
      - Wrap entire import in `(d/with-transaction [conn] ...)` — rollback on any row failure
      - If any row fails (bad date, missing field, parse error): roll back all rows, return error details
      - Error response: `{:errors [{:row 5, :field :date, :message "Invalid date format: 'bad-date'"}]}`
      - User fixes CSV and re-imports — no partial state left in DB
      - This guarantees idempotency: failed import leaves zero trace

  **Must NOT do**:
  - No auto-detection of CSV format (explicit mapping required)
  - No streaming import (batch is fine for MVP)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Deduplication logic requires careful design
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 4, 6)
  - **Blocks**: Tasks 7, 11, 14
  - **Blocked By**: Task 1

  **References**:
  - CSV parsing: `clojure.data.csv`
  - Dedup strategy: hash of `date+amount+description+account`

  **Acceptance Criteria**:
  - [ ] Given a CSV file, imports transactions to Datahike
  - [ ] Running import twice creates no duplicates
  - [ ] Supports Binance export format
  - [ ] Supports generic `date,amount,description` format

  **QA Scenarios**:
  ```
  Scenario: Import CSV successfully
    Tool: Bash
    Preconditions: Test CSV file with 10 rows
    Steps:
      1. Call `(import-csv conn "test/fixtures/binance_export.csv" mapping)`
      2. Query Datahike for imported transactions
      3. Verify count matches CSV rows
    Expected Result: 10 transactions imported
    Evidence: .sisyphus/evidence/task-5-import-csv.txt

  Scenario: Idempotent import (no duplicates)
    Tool: Bash
    Steps:
      1. Import same CSV twice
      2. Query transaction count
      3. Verify count unchanged after second import
    Expected Result: Same count after re-import
    Evidence: .sisyphus/evidence/task-5-idempotent.txt
  ```

  **Commit**: YES
  - Message: `feat(import): add CSV import with deduplication`
  - Files: `backend/src/ren_finance/import/csv.clj`, `backend/src/ren_finance/import/dedup.clj`

- [ ] 6. Currency Conversion Module

  **What to do**:
  - Create `backend/src/ren_finance/currency/converter.clj`
  - Support user-selectable base currency (USD, EUR, RUB, etc.)
  - Fetch exchange rates from public APIs (exchangerate-api.com, open.er-api.com)
  - Cache exchange rates in Datahike (update daily)
  - Convert crypto balances to base currency
  - Convert fiat amounts to base currency
  - Create `backend/src/ren_finance/currency/rates.clj` for rate storage
  - **Exchange rate API integration**:
    - Primary fiat: `https://open.er-api.com/v6/latest/{BASE}` (free, no API key, ~1000 req/day)
    - Fallback fiat: `https://api.exchangerate-api.com/v4/latest/{BASE}` (free, no key, 1500 req/month)
    - Crypto prices: `https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies={BASE}` (free tier, 10-30 req/min)
    - Cache strategy: store rates in Datahike with `:rate/timestamp`, refresh on app startup and every 6 hours
    - Graceful degradation: show cached rates when API unavailable, display "Stale rates" warning in dashboard
    - Retry: one retry with 2s delay on failure, then use cache

  **Must NOT do**:
  - No real-time rate updates (daily is fine)
  - No hardcoded currency (user selects)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Standard HTTP calls with caching
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 4, 5)
  - **Blocks**: Tasks 8, 13
  - **Blocked By**: Task 1

  **References**:
  - Exchange rate API: `https://open.er-api.com/v6/latest/USD`
  - Caching: store rates in Datahike with timestamp

  **Acceptance Criteria**:
  - [ ] Given base currency "USD", converts all amounts to USD
  - [ ] Given base currency "EUR", converts all amounts to EUR
  - [ ] Exchange rates cached and updated daily
  - [ ] Crypto prices fetched from CoinGecko or similar

  **QA Scenarios**:
  ```
  Scenario: Convert to USD
    Tool: Bash
    Steps:
      1. Set base currency to "USD"
      2. Convert 1 BTC to USD
      3. Verify returns current BTC/USD rate
    Expected Result: Converted amount is BigDecimal
    Evidence: .sisyphus/evidence/task-6-convert-usd.txt

  Scenario: Convert to EUR
    Tool: Bash
    Steps:
      1. Set base currency to "EUR"
      2. Convert 1000 USD to EUR
      3. Verify returns current USD/EUR rate
    Expected Result: Converted amount is BigDecimal
    Evidence: .sisyphus/evidence/task-6-convert-eur.txt
  ```

  **Commit**: YES
  - Message: `feat(currency): add currency conversion module`
  - Files: `backend/src/ren_finance/currency/converter.clj`, `backend/src/ren_finance/currency/rates.clj`

- [ ] 7. HTTP Server + Routes

  **What to do**:
  - Create `backend/src/ren_finance/server/core.clj` with Aleph HTTP server
  - Create `backend/src/ren_finance/server/routes.clj` with Reitit route definitions
  - Create `backend/src/ren_finance/server/middleware.clj` for JSON, CORS, error handling
  - Routes: `GET /api/net-worth`, `GET /api/accounts`, `GET /api/transactions`
  - Routes: `POST /api/import/csv`, `POST /api/wallets/sync`
  - Routes: `GET /api/settings`, `PUT /api/settings/base-currency`
  - Routes: `GET /api/settings/wallets`, `PUT /api/settings/wallets`, `DELETE /api/settings/wallets/:id`
  - Routes: `GET /api/settings/csv-mappings`, `PUT /api/settings/csv-mappings`
  - **API contract — request/response shapes**:
    ```
    GET  /api/net-worth
         → 200 {:data {:total 8000M, :by-type [{:type :crypto, :balance 2000M} {:type :cash, :balance 6000M}], :last-sync "2026-05-10T12:00:00Z"}}
    GET  /api/accounts
         → 200 {:data [{:id ..., :name "Checking", :type :checking, :balance 5000M, :currency :USD}]}
    GET  /api/transactions?limit=20&offset=0&from=2026-01-01&to=2026-05-10&category=food
         → 200 {:data {:transactions [{:id ..., :date ..., :amount -50M, :description "Coffee", :type :expense, :category "Food"}], :total 150}}
    POST /api/import/csv  (multipart form, field name "file")
         → 200 {:data {:imported 10, :duplicates 0, :errors []}}
         → 422 {:error {:code :import-error, :message "Invalid CSV: missing date column"}}
    POST /api/wallets/sync  {:addresses ["0x742d...", "1A1zP1..."]}
         → 200 {:data {:synced 2, :results [{:address "0x742d...", :balance 1.5M, :asset :ETH} {:address "1A1zP1...", :balance 0.5M, :asset :BTC}]}}
         → 429 {:error {:code :rate-limited, :message "API rate limit reached. Try again in 60s."}}
    GET  /api/settings
         → 200 {:data {:base-currency :USD, :wallets [{:id ..., :address "0x...", :chain-type :ETH, :label "Main", :balance 1.5M}]}}
    PUT  /api/settings/base-currency  {:currency :EUR}
         → 200 {:data {:currency :EUR}}
    GET  /api/settings/wallets
         → 200 {:data [{:id ..., :address "0x...", :chain-type :ETH, :label "Main", :balance 1.5M}]}
    PUT  /api/settings/wallets  {:address "0x...", :chain-type :ETH, :label "Main"}
         → 201 {:data {:id ..., :address "0x..."}}
    DELETE /api/settings/wallets/:id
         → 200 {:data {:deleted "0x..."}}
         → 404 {:error {:code :not-found, :message "Wallet not found"}}
    ```
    Response envelope: `{:status 200, :body {:data ...}}` success, `{:status 4xx/5xx, :body {:error {:code keyword, :message string, :details map?}}}` error.
    Success responses always under `:data` key. Never return arrays at top level.

  - **Logging setup** in `core.clj`:
    - Use `clojure.tools.logging` with Logback backend (logback.xml in resources/)
    - Log requests: `[method] [path] [status] [duration-ms]` at INFO
    - Log errors with full stack trace at ERROR level, never expose stack traces to client
    - Log wallet/currency API calls at INFO level
    - Configurable log level via environment variable `LOG_LEVEL` (default: INFO)
  - **Standard JSON error envelope** in `middleware.clj`:
    - Success: `{:status 200, :body {:data {...}}}`
    - Error: `{:status 4xx/5xx, :body {:error {:code string?, :message string?, :details map?}}}`
    - Error codes: `:validation-error`, `:not-found`, `:rate-limited`, `:import-error`, `:internal-error`
    - All errors return consistent JSON envelope, never plain text, HTML, or stack traces
    -     Wrap Ring handler in `try/catch` at middleware level for unhandled exceptions → 500 with `:internal-error`
  - **`GET /api/health`** — Docker health check endpoint (no auth, always 200 if DB connected):
    ```clojure
    {:data {:status "ok", :db-connected true, :uptime-seconds <int>}}
    ```
  - **Startup sequence** in `core.clj` `-main`:
    1. Load config (config.edn + env var overrides)
    2. Init Datahike: `create-db!` → `connect`
    3. Start Aleph HTTP server (blocking — keeps process alive)
    4. Start MCP server (STDIO or HTTP based on config `:mcp/transport`)
    5. Register JVM shutdown hook: SIGTERM/SIGINT → stop MCP → stop HTTP → `disconnect` Datahike → exit
  - **Graceful shutdown**: On SIGTERM, stop accepting new requests, wait up to 10s for in-flight, then release all resources

  **Must NOT do**:
  - No authentication middleware (single-user, local-only for MVP)
  - No WebSocket endpoints
  - **Category**: `unspecified-high`
    - Reason: HTTP server setup with middleware
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 8, 9, 10, 11)
  - **Blocks**: Tasks 9, 10, 11
  - **Blocked By**: Tasks 2, 4, 5

  **References**:
  - Aleph: `aleph.http/start-server`
  - Reitit: `reitit.ring/router`

  **Acceptance Criteria**:
  - [ ] Server starts on configurable port
  - [ ] `GET /api/net-worth` returns JSON with net worth data
  - [ ] `POST /api/import/csv` accepts file upload
  - [ ] CORS headers present for local development

  **QA Scenarios**:
  ```
  Scenario: Server starts and responds
    Tool: Bash (curl)
    Steps:
      1. Start server on port 3000
      2. `curl http://localhost:3000/api/net-worth`
      3. Verify JSON response with 200 status
    Expected Result: `{"net_worth": 0, "accounts": []}`
    Evidence: .sisyphus/evidence/task-7-server-start.txt

  Scenario: CSV import endpoint works
    Tool: Bash (curl)
    Steps:
      1. `curl -F "file=@test.csv" http://localhost:3000/api/import/csv`
      2. Verify 200 response with import count
    Expected Result: `{"imported": 10, "duplicates": 0}`
    Evidence: .sisyphus/evidence/task-7-csv-import.txt
  ```

  **Commit**: YES
  - Message: `feat(server): add HTTP server with REST API routes`
  - Files: `backend/src/ren_finance/server/core.clj`, `backend/src/ren_finance/server/routes.clj`, `backend/src/ren_finance/server/middleware.clj`

- [ ] 8. Net Worth Calculation Queries

  **What to do**:
  - Create `backend/src/ren_finance/queries/net_worth.clj`
  - Datalog query for total net worth (sum of all account balances)
  - Datalog query for net worth by account type
  - Datalog query for historical net worth (using `d/as-of`)
  - Datalog query for spending by category
  - Create `backend/src/ren_finance/queries/transactions.clj` for transaction queries

  **Must NOT do**:
  - No multi-currency conversion (single base currency for MVP)

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Complex Datalog queries with aggregations
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 7, 9, 10, 11)
  - **Blocks**: Tasks 10, 13
  - **Blocked By**: Task 2

  **References**:
  - Datahike queries: `d/q` with `:find (sum ?amount) .`
  - Time travel: `(d/as-of @conn date)` — requires `:keep-history? true` in Datahike config (set in Task 2)
  - Aggregation gotcha: use `:with ?entity` for correct sums

  **Acceptance Criteria**:
  - [ ] `(net-worth conn)` returns total net worth as BigDecimal
  - [ ] `(net-worth-by-type conn)` returns breakdown by account type
  - [ ] `(net-worth-at conn date)` returns historical net worth
  - [ ] `(spending-by-category conn start-date end-date)` returns category breakdown

  **QA Scenarios**:
  ```
  Scenario: Calculate net worth correctly
    Tool: Bash
    Preconditions: Datahike DB with 3 accounts (checking: 1000, savings: 5000, crypto: 2000)
    Steps:
      1. Call `(net-worth conn)`
      2. Verify returns 8000M
    Expected Result: 8000M
    Evidence: .sisyphus/evidence/task-8-net-worth.txt

  Scenario: Historical net worth
    Tool: Bash
    Steps:
      1. Add transaction dated 2026-01-01
      2. Call `(net-worth-at conn #inst "2025-12-31")`
      3. Verify returns balance before transaction
    Expected Result: Pre-transaction balance
    Evidence: .sisyphus/evidence/task-8-historical.txt
  ```

  **Commit**: YES
  - Message: `feat(queries): add net worth and transaction queries`
  - Files: `backend/src/ren_finance/queries/net_worth.clj`, `backend/src/ren_finance/queries/transactions.clj`

- [ ] 9. MCP Server Setup

  **What to do**:
  - Create `backend/src/ren_finance/mcp/server.clj`
  - Initialize Gaiwan MCP SDK with server info
  - Configure STDIO transport (for Claude Desktop)
  - Configure HTTP transport (for development/Inspector)
  - Create `backend/src/ren_finance/mcp/tools.clj` for tool definitions
  - Create `backend/src/ren_finance/mcp/resources.clj` for resource definitions

  **Must NOT do**:
  - No tool implementations yet (Task 10)
  - No write access tools (read-only for MVP)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: MCP SDK integration requires understanding protocol
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 7, 8, 10, 11)
  - **Blocks**: Task 10
  - **Blocked By**: Task 7

  **References**:
  - Gaiwan SDK: `co.gaiwan/mcp-sdk {:mvn/version "0.2.17"}`
  - Server init: `(mcp/run-stdio! {})` or `(mcp/run-http! {:port 3999})`

  **Acceptance Criteria**:
  - [ ] MCP server starts without errors
  - [ ] Server responds to `initialize` request
  - [ ] Server responds to `tools/list` request (empty initially)

  **QA Scenarios**:
  ```
  Scenario: MCP server starts
    Tool: Bash
    Steps:
      1. Run `clojure -M -m ren-finance.mcp.server`
      2. Send initialize JSON-RPC request via stdin
      3. Verify response contains server info
    Expected Result: `{"result": {"serverInfo": {"name": "ren-finance"}}}`
    Evidence: .sisyphus/evidence/task-9-mcp-start.txt

  Scenario: MCP Inspector connects
    Tool: Bash
    Steps:
      1. Run `npx @modelcontextprotocol/inspector clojure -M -m ren-finance.mcp.server`
      2. Verify Inspector UI loads
    Expected Result: Inspector shows connected server
    Evidence: .sisyphus/evidence/task-9-mcp-inspector.txt
  ```

  **Commit**: YES
  - Message: `feat(mcp): add MCP server setup with Gaiwan SDK`
  - Files: `backend/src/ren_finance/mcp/server.clj`, `backend/src/ren_finance/mcp/tools.clj`, `backend/src/ren_finance/mcp/resources.clj`

- [ ] 10. MCP Tools Implementation

  **What to do**:
  - Implement 4 read-only tools in `backend/src/ren_finance/mcp/tools.clj`:
    1. `get_net_worth` - Returns current net worth breakdown
    2. `fetch_wallet_data` - Triggers wallet address sync
    3. `list_transactions` - Returns recent transactions
    4. `generate_report` - Generates spending/income report
  - Use Malli schemas for tool input validation
  - Return `{:content [{:type "text" :text ...}] :isError false}` format

  **Must NOT do**:
  - No write access tools (no delete/update transactions)
  - No AI categorization (defer to MCP client)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Tool implementation with proper error handling
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 7, 8, 9, 11)
  - **Blocks**: Task 11
  - **Blocked By**: Tasks 8, 9

  **References**:
  - Tool format: `{:name "tool_name", :description "...", :schema ..., :tool-fn fn}`
  - Malli schema: `(mjs/transform [:map [:param string?]])`

  **Acceptance Criteria**:
  - [ ] `tools/list` returns 4 tools
  - [ ] `get_net_worth` returns net worth data
  - [ ] `fetch_wallet_data` triggers sync and returns result
  - [ ] `list_transactions` returns transaction list
  - [ ] `generate_report` returns formatted report

  **QA Scenarios**:
  ```
  Scenario: get_net_worth tool works
    Tool: Bash (MCP Inspector or curl)
    Steps:
      1. Call `get_net_worth` tool via MCP
      2. Verify response contains net worth value
    Expected Result: `{"content": [{"type": "text", "text": "Net worth: $8000"}]}`
    Evidence: .sisyphus/evidence/task-10-get-net-worth.txt

  Scenario: list_transactions tool works
    Tool: Bash
    Steps:
      1. Call `list_transactions` with `{:limit 5}`
      2. Verify response contains transaction list
    Expected Result: Transaction list with 5 items
    Evidence: .sisyphus/evidence/task-10-list-transactions.txt
  ```

  **Commit**: YES
  - Message: `feat(mcp): add read-only MCP tools`
  - Files: `backend/src/ren_finance/mcp/tools.clj`

- [ ] 11. Backend Integration Tests

  **What to do**:
  - Create `backend/test/ren_finance/integration_test.clj`
  - Test full flow: wallet lookup → net worth query → currency conversion
  - Test full flow: CSV import → transaction query → report generation
  - Test MCP tool invocations end-to-end
  - Create test fixtures in `backend/test-fixtures/`

  **Must NOT do**:
  - No UI tests (Wave 3)
  - No real API calls (use mocks/fixtures)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Integration testing requires understanding full flow
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 7, 8, 9, 10)
  - **Blocks**: Tasks F1-F4
  - **Blocked By**: Tasks 7, 10

  **References**:
  - Test framework: `clojure.test`
  - Mock exchange: return fixed data from `fetch-balances`

  **Acceptance Criteria**:
  - [ ] All integration tests pass
  - [ ] Test coverage > 80% for backend code
  - [ ] Mock exchange adapter works correctly

  **QA Scenarios**:
  ```
  Scenario: Full flow integration test
    Tool: Bash
    Steps:
      1. Run `clojure -M:test` in backend/
      2. Verify all tests pass
    Expected Result: `Ran N tests, N assertions, 0 failures`
    Evidence: .sisyphus/evidence/task-11-integration-tests.txt
  ```

  **Commit**: YES
  - Message: `test(backend): add integration tests`
  - Files: `backend/test/ren_finance/integration_test.clj`, `backend/test-fixtures/`

- [ ] 12. ClojureDart Project Setup

  **What to do**:
  - Create `frontend/deps.edn` with ClojureDart dependency
  - Run `clj -M:cljd init` in frontend/ to generate Flutter project
  - Create `frontend/src/ren_finance/main.cljd` entry point
  - Create `frontend/src/ren_finance/model/events.cljd` for re-dash events
  - Create `frontend/src/ren_finance/model/subs.cljd` for re-dash subscriptions
  - Create `frontend/src/ren_finance/model/effects.cljd` for side effects
  - Configure `frontend/pubspec.yaml` with required Flutter packages
  - **Copy Revolut DESIGN.md** to `.sisyphus/design/DESIGN.md`
  - **Flutter package versions** in `pubspec.yaml`:
    - `flutter: ">=3.22.0"`
    - `syncfusion_flutter_charts: "^24.2.9"` (net worth chart)
    - `file_picker: "^8.0.0"` (CSV file selection)
    - `shared_preferences: "^2.2.0"` (offline cache)
    - `http: "^1.2.0"` (HTTP client)
    - `flutter_lint: "^5.0.0"` (dev dependency)
  - **Navigation structure**: Bottom navigation tab bar (Material 3 NavigationBar)
    - Tab 1: Dashboard — net worth display, chart, account cards
    - Tab 2: Import — CSV file picker, column mapping, preview, execute
    - Tab 3: Settings — base currency selector, wallet management, CSV mapping presets
    - Active tab: `{colors/primary}` (`#494fdf`) indicator
    - Inactive tabs: muted white (`#ffffff` at 60% opacity)
    - Bottom bar: 64px height, `{colors/canvas-dark}` (`#000000`) background
    - Tab icons: Dashboard (home icon), Import (upload icon), Settings (gear icon)
    - State: current tab index persisted in re-dash app-db under `:active-tab`
  - **re-dash app-db shape** — exact top-level keys (events write, subs read):
    ```clojure
    {:active-tab 0                               ;; 0=Dashboard, 1=Import, 2=Settings
     :net-worth {:total nil, :by-type [], :last-sync nil}
     :accounts [{:db/id ..., :name ... :type :checking :balance ... :currency :USD}]
     :transactions {:items [], :total 0, :loading? false}
     :settings {:base-currency :USD
                :wallets [{:address "0x...", :chain-type :ETH, :label "Main", :balance nil}]}
     :import {:status :idle, :preview-rows [], :result nil, :error nil}
     :cache {:net-worth {...}, :settings {...}, :cached-at nil}
     :ui {:loading? false, :error nil, :offline? false}}
    ```
    Events write to these keys. Subs read from them. Must match exactly — events/subs agents work independently.
  - **API base URL configuration** in `client.cljd`:
    - Web: relative path `/api` (same-origin via Nginx reverse proxy)
    - Mobile: `http://<host>:3000/api` — configurable at build time
    - Default logic: try `/api` first (web), fall back to `http://localhost:3000/api` (mobile dev)
    - Override via environment variable or Dart `--dart-define=API_BASE_URL=http://host:3000/api`

  **Design System Setup**:
  - Download Revolut DESIGN.md from: `https://raw.githubusercontent.com/VoltAgent/awesome-design-md/main/design-md/revolut/DESIGN.md`
  - Save to: `.sisyphus/design/DESIGN.md`
  - This file defines ALL visual tokens for the frontend

  **Must NOT do**:
  - No actual UI implementation yet (Tasks 13, 14, 15)
  - No backend integration yet

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Project scaffolding
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 13, 14, 15)
  - **Blocks**: Tasks 13, 14, 15
  - **Blocked By**: None (can start immediately)

  **References**:
  - ClojureDart: `tensegritics/clojuredart {:git/url "..."}`
  - re-dash: `hti/re-dash {:git/url "https://github.com/htihospitality/re-dash.git"}`
  - **Design system**: `.sisyphus/design/DESIGN.md` (Revolut DESIGN.md)

  **Acceptance Criteria**:
  - [ ] `clj -M:cljd compile` succeeds in frontend/
  - [ ] Flutter app builds for web: `flutter build web --release` in frontend/
  - [ ] App shows "Ren Finance" title
  - [ ] `.sisyphus/design/DESIGN.md` exists with Revolut design tokens
  - [ ] `frontend/src/ren_finance/theme/colors.cljd` exists with color tokens

  **QA Scenarios**:
  ```
  Scenario: ClojureDart compiles
    Tool: Bash
    Steps:
      1. Run `clj -M:cljd compile` in frontend/
      2. Verify no compilation errors
    Expected Result: Dart files generated in frontend/lib/cljd-out/
    Evidence: .sisyphus/evidence/task-12-cljd-compile.txt

  Scenario: Web build succeeds
    Tool: Bash
    Steps:
      1. Run `flutter build web --release` in frontend/
      2. Verify frontend/build/web/ directory created
    Expected Result: Web build completes without errors
    Evidence: .sisyphus/evidence/task-12-web-build.txt

  Scenario: Design system file exists
    Tool: Bash
    Steps:
      1. Verify `.sisyphus/design/DESIGN.md` exists
      2. Verify file contains Revolut color tokens
    Expected Result: File exists with design tokens
    Evidence: .sisyphus/evidence/task-12-design-system.txt
  ```

  **Commit**: YES
  - Message: `feat(frontend): add ClojureDart project scaffolding`
  - Files: `frontend/deps.edn`, `frontend/pubspec.yaml`, `frontend/src/ren_finance/main.cljd`

- [ ] 13. Net Worth Dashboard UI

  **What to do**:
  - Create `frontend/src/ren_finance/views/dashboard.cljd`
  - Display total net worth prominently ("Netwars" title)
  - Show breakdown by account type (crypto, cash, etc.)
  - Show last-sync timestamp
  - Use `syncfusion_flutter_charts` for net worth trend chart
  - Pull-to-refresh for manual sync trigger
  - **Dashboard states**:
    - **Loading** (initial fetch): Show 3 skeleton placeholder cards (same layout as real cards, pulsing animation, Material 3 skeleton pattern). No header/net-worth number visible until data arrives.
    - **Empty** (no accounts, no wallets): Show centered hero section: title "Welcome to Ren Finance", subtitle "Connect your first wallet or import a CSV to get started", primary CTA button "Add Wallet" (navigates to Settings tab), secondary CTA "Import CSV" (navigates to Import tab).
    - **Error** (backend unreachable): Show inline error banner at top of screen: yellow/amber background, icon, text "Unable to connect to backend. Check that the server is running." + "Retry" button. Cached data (if any) shown below banner.
    - **Data loaded** (normal state): Net worth figure (80px display), breakdown cards (crypto, cash), chart, last-sync timestamp, pull-to-refresh hint.
    - **Stale data** (cache older than 6h): Subtle banner below net worth: muted orange text "Data synced X hours ago — may be stale". Auto-dismisses after fresh sync.
  - **Follow Revolut DESIGN.md** for all styling (see `.sisyphus/design/DESIGN.md`)
  - Create reusable widgets in `frontend/src/ren_finance/views/widgets/`:
    - `card.cljd` - Styled card component
    - `button.cljd` - Pill button component
    - `chart.cljd` - Net worth chart wrapper
    - `nav.cljd` - Navigation bar
  - Create theme files in `frontend/src/ren_finance/theme/`:
    - `colors.cljd` - Color palette from DESIGN.md
    - `typography.cljd` - Font sizes, weights
    - `spacing.cljd` - Spacing scale

  **Design Specifications** (from Revolut DESIGN.md):
  - **Background**: `{colors.canvas-dark}` (`#000000`) for main dashboard
  - **Net worth display**: `{typography.display-xl}` (80px, weight 500, lineHeight 1.0)
  - **Section titles**: `{typography.heading-lg}` (32px, weight 500)
  - **Account cards**: `{component.feature-card-dark}` (`#16181a` bg, 20px rounded, 32px padding)
  - **Positive values**: `{colors.accent-teal}` (`#00a87e`)
  - **Negative values**: `{colors.accent-danger}` (`#e23b4a`)
  - **Sync button**: `{component.button-primary}` (white pill on dark, 48px height)
  - **Last-sync text**: `{typography.body-sm}` (14px, `{colors.on-dark-mute}`)
  - **Spacing**: `{spacing.section}` (88px) between sections, `{spacing.xxl}` (32px) card padding
  - **Material 3**: Use Material 3 component patterns for accessibility (min 48px touch targets, proper focus states, semantic labels)

  **Must NOT do**:
  - No drop shadows (use canvas/surface-luminance shifts)
  - No accent colors as button surfaces (only in illustrations)
  - No near-black canvas (`#0a0a0a`) - use true black (`#000000`)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: UI design with charts and responsive layout
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 12, 14, 15)
  - **Blocks**: Tasks F1-F4
  - **Blocked By**: Tasks 8, 12

  **References**:
  - ClojureDart widgets: `cljd.flutter/f/widget`
  - Charts: `syncfusion_flutter_charts`
  - re-dash subscriptions: `(rd/subscribe [::net-worth])`
  - **Design system**: `.sisyphus/design/DESIGN.md` (Revolut DESIGN.md)

  **Acceptance Criteria**:
  - [ ] Dashboard displays net worth value
  - [ ] Shows breakdown by account type
  - [ ] Shows last-sync timestamp
  - [ ] Pull-to-refresh triggers sync
  - [ ] Chart shows net worth trend (if data available)
  - [ ] Follows Revolut DESIGN.md color palette
  - [ ] Uses correct typography tokens
  - [ ] No drop shadows (depth via surface-luminance)

  **QA Scenarios**:
  ```
  Scenario: Dashboard displays net worth
    Tool: Playwright
    Preconditions: Backend running with test data
    Steps:
      1. Open app in browser
      2. Verify "Netwars" title visible
      3. Verify net worth value displayed
      4. Verify account breakdown visible
    Expected Result: Dashboard shows $8000 net worth
    Evidence: .sisyphus/evidence/task-13-dashboard.png

  Scenario: Pull-to-refresh works
    Tool: Playwright
    Steps:
      1. Perform pull-to-refresh gesture
      2. Verify loading indicator appears
      3. Verify data updates
    Expected Result: Sync triggered, timestamp updated
    Evidence: .sisyphus/evidence/task-13-pull-refresh.png

  Scenario: Design system compliance
    Tool: Playwright
    Steps:
      1. Screenshot dashboard
      2. Verify background is #000000 (true black)
      3. Verify cards use #16181a background
      4. Verify buttons are pill-shaped (rounded)
    Expected Result: Matches Revolut DESIGN.md
    Evidence: .sisyphus/evidence/task-13-design-compliance.png
  ```

  **Commit**: YES
  - Message: `feat(dashboard): add net worth dashboard with charts`
  - Files: `frontend/src/ren_finance/views/dashboard.cljd`, `frontend/src/ren_finance/views/widgets/`, `frontend/src/ren_finance/theme/`

- [ ] 14. CSV Import UI

  **What to do**:
  - Create `frontend/src/ren_finance/views/import.cljd`
  - File picker for CSV selection
  - Column mapping configuration UI
  - Import preview (show first 5 rows)
  - Import progress indicator
  - Success/error feedback
  - **Import view states**:
    - **Initial** (default): Centered view with file picker card. Title "Import CSV", subtitle "Select a CSV file to import transactions". Large pill button "Select CSV File". Hint text below: "Supports Binance export and generic date,amount,description format".
    - **Preview** (file selected): Table showing first 5 rows parsed from CSV. Column mapping dropdowns below each column header. "Import" button at bottom.
    - **Importing** (in progress): Progress bar (indeterminate animation), text "Importing N rows...", Cancel button (fires abort, but graceful — waits for current row).
    - **Success**: Green checkmark animation (1s), text "Imported 10 transactions (2 duplicates skipped)". "Done" button (navigates to Dashboard). "Import Another" link.
    - **Error**: Red error card, text from backend (e.g. "Invalid CSV: missing 'date' column"), "Try Again" button (resets to initial state).
    - **Empty** (file has no parseable rows): Warning card "No transactions found in file. Check the column mapping." + "Edit Mapping" button.
  - **Follow Revolut DESIGN.md** for all styling (see `.sisyphus/design/DESIGN.md`)

  **Design Specifications** (from Revolut DESIGN.md):
  - **Background**: `{colors.canvas-light}` (`#ffffff`) for import form
  - **Form title**: `{typography.heading-lg}` (32px, weight 500)
  - **File picker button**: `{component.button-dark}` (dark pill on light, 48px height)
  - **Column mapping inputs**: `{component.text-input}` (white bg, 12px rounded, 56px height)
  - **Preview table**: `{component.feature-card-light}` (white bg, 20px rounded, 32px padding)
  - **Import button**: `{component.button-primary}` (white pill on dark, 48px height)
  - **Progress indicator**: `{colors.primary}` (`#494fdf`) accent
  - **Success message**: `{colors.accent-teal}` (`#00a87e`)
  - **Error message**: `{colors.accent-danger}` (`#e23b4a`)
  - **Spacing**: `{spacing.xl}` (24px) between form sections
  - **Material 3**: Use Material 3 form patterns (proper input labels, error states, focus indicators)

  **Must NOT do**:
  - No drag-and-drop (keep it simple)
  - No auto-detection of CSV format

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: File handling UI with progress feedback
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 12, 13, 15)
  - **Blocks**: Tasks F1-F4
  - **Blocked By**: Tasks 5, 12

  **References**:
  - File picker: `file_picker` Flutter package
  - re-dash effects: `(rd/reg-fx ::import-csv ...)`
  - **Design system**: `.sisyphus/design/DESIGN.md` (Revolut DESIGN.md)

  **Acceptance Criteria**:
  - [ ] File picker opens for CSV selection
  - [ ] Column mapping UI works
  - [ ] Import preview shows first 5 rows
  - [ ] Progress indicator during import
  - [ ] Success message after import
  - [ ] Follows Revolut DESIGN.md color palette
  - [ ] Uses correct typography tokens

  **QA Scenarios**:
  ```
  Scenario: CSV import flow
    Tool: Playwright
    Preconditions: Test CSV file available
    Steps:
      1. Navigate to import view
      2. Click "Select CSV"
      3. Choose test file
      4. Configure column mapping
      5. Click "Import"
      6. Verify success message
    Expected Result: "Imported 10 transactions" message
    Evidence: .sisyphus/evidence/task-14-csv-import.png

  Scenario: Design system compliance
    Tool: Playwright
    Steps:
      1. Screenshot import view
      2. Verify background is #ffffff (white)
      3. Verify inputs use 56px height
      4. Verify buttons are pill-shaped
    Expected Result: Matches Revolut DESIGN.md
    Evidence: .sisyphus/evidence/task-14-design-compliance.png
  ```

  **Commit**: YES
  - Message: `feat(import): add CSV import UI`
  - Files: `frontend/src/ren_finance/views/import.cljd`

- [ ] 14a. Settings View (Base Currency + Wallet Management)

  **What to do**:
  - Create `frontend/src/ren_finance/views/settings.cljd`
  - **Base currency selector**: Dropdown UI for selecting base currency from major ISO 4217 currencies (USD, EUR, RUB, GBP, JPY, CNY, BTC, ETH)
  - Show current exchange rates relative to selected base currency (read-only display, fetched from backend)
  - Persist selected currency to backend: `PUT /api/settings/base-currency`
  - **Wallet address management**: List of configured wallet addresses with add/remove
  - Each wallet entry: address text input, blockchain type toggle (ETH/BTC), optional user label
  - Validate address format on input:
    - ETH: `^0x[a-fA-F0-9]{40}$` (42-char hex starting with 0x)
    - BTC legacy: `^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$`
    - BTC bech32: `^bc1[a-zA-Z0-9]{39,59}$`
  - Show last-known balance for each wallet (fetched from `GET /api/settings/wallets`)
  - Persist wallet addresses to backend: `PUT /api/settings/wallets` and `DELETE /api/settings/wallets/:id`
  - **CSV mapping presets**: View saved column mapping presets (created during CSV import, stored by Task 5)
  - **Import history**: Show recent CSV imports with timestamp, row count, duplicate count
  - **Settings view states**:
    - **Loading**: 3 skeleton rows (same layout as setting sections, pulsing animation)
    - **Normal**: Sections rendered: base currency selector, wallet list, CSV mapping presets, import history
    - **Empty wallets**: "No wallets configured. Add your first wallet below." helper text + "Add Wallet" button prominent
    - **Saving**: Save button shows spinning indicator, disabled state
    - **Save success**: Green checkmark toast at bottom "Saved" (auto-dismiss 2s)
    - **Save error**: Red inline text below save button "Failed to save. Server returned: [error message]"
    - **Delete wallet**: Confirmation dialog "Remove wallet [label]?" with Cancel and Delete buttons. On confirm: brief loading, wallet removed from list.
  - **Follow Revolut DESIGN.md** for all styling (see `.sisyphus/design/DESIGN.md`)

  **Design Specifications** (from Revolut DESIGN.md):
  - **Background**: `#ffffff` (forms/light canvas)
  - **Section headers**: `{typography.heading-md}` (24px, weight 500)
  - **Input labels**: `{typography.label}` (14px, weight 600, uppercase)
  - **Input fields**: `{component.text-input}` (white bg, 12px rounded, 56px height)
  - **Wallet cards**: `{component.feature-card-light}` (white bg, 20px rounded, 32px padding)
  - **Delete wallet**: `{colors.accent-danger}` (`#e23b4a`) text button
  - **Add wallet button**: `{component.button-dark}` (dark pill on light, 48px height)
  - **Save button**: `{component.button-primary}` (white pill on dark, 48px height)
  - **Spacing**: `{spacing.xl}` (24px) between form sections
  - **Material 3**: Use Material 3 form patterns with proper input labels, error states, focus indicators, min 48px touch targets

  **Must NOT do**:
  - No user authentication (single-user)
  - No multi-profile / multi-user support
  - No address book / contacts
  - No scheduled sync configuration (always manual trigger from dashboard)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Form UI with validation and dynamic wallet list
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 12, 13, 14, 15)
  - **Blocks**: Tasks F1-F4
  - **Blocked By**: Tasks 7 (backend settings API exists), 12 (ClojureDart project setup)

  **References**:
  - Backend endpoints: `GET /api/settings`, `PUT /api/settings/base-currency`, `GET|PUT|DELETE /api/settings/wallets`
  - Currency list: ISO 4217 major currencies
  - Wallet address validation regexs (see format rules above)
  - **Design system**: `.sisyphus/design/DESIGN.md` (Revolut DESIGN.md)

  **Acceptance Criteria**:
  - [ ] Base currency selector dropdown works and persists via API
  - [ ] Wallet address can be added with label and chain type
  - [ ] Wallet address can be removed
  - [ ] Invalid address format shows inline validation error
  - [ ] Settings persist across app restarts (loaded from backend on startup)
  - [ ] Follows Revolut DESIGN.md color palette and typography
  - [ ] All touch targets ≥ 48px (Material 3 accessibility)

  **QA Scenarios**:
  ```
  Scenario: Configure base currency
    Tool: Playwright
    Steps:
      1. Navigate to settings view
      2. Select "EUR" from currency dropdown
      3. Click Save
      4. Navigate to dashboard
      5. Verify net worth displayed in EUR (€ symbol)
    Expected Result: Dashboard shows € values
    Evidence: .sisyphus/evidence/task-14a-currency-setting.png

  Scenario: Add wallet address
    Tool: Playwright
    Steps:
      1. Navigate to settings view
      2. Click "Add Wallet"
      3. Enter valid ETH address: 0x742d35Cc6634C0532925a3b844Bc9e7595f2bD18
      4. Select "Ethereum" from chain type dropdown
      5. Enter label "My Main Wallet"
      6. Click Save
      7. Verify wallet appears in wallet list with balance
    Expected Result: Wallet added and balance displayed
    Evidence: .sisyphus/evidence/task-14a-add-wallet.png

  Scenario: Invalid address validation
    Tool: Playwright
    Steps:
      1. Navigate to settings view
      2. Click "Add Wallet"
      3. Enter "not-a-valid-address" in wallet field
      4. Click outside input (trigger blur validation)
      5. Verify inline error message visible
    Expected Result: Red error text: "Invalid address format"
    Evidence: .sisyphus/evidence/task-14a-validation.png

  Scenario: Delete wallet address
    Tool: Playwright
    Steps:
      1. Navigate to settings view (precondition: wallet exists)
      2. Click delete icon/button on existing wallet
      3. Confirm deletion in dialog
      4. Verify wallet removed from list
    Expected Result: Wallet removed, confirmed by API response
    Evidence: .sisyphus/evidence/task-14a-delete-wallet.png

  Scenario: Design system compliance
    Tool: Playwright
    Steps:
      1. Screenshot settings view
      2. Verify background is #ffffff (white canvas)
      3. Verify input fields are 56px height with 12px border-radius
      4. Verify buttons are pill-shaped (rounded.full)
      5. Verify wallet cards have 20px rounded corners
    Expected Result: Matches Revolut DESIGN.md tokens
    Evidence: .sisyphus/evidence/task-14a-design-compliance.png
  ```

  **Commit**: YES
  - Message: `feat(settings): add settings view for currency and wallet management`
  - Files: `frontend/src/ren_finance/views/settings.cljd`

- [ ] 15. Offline Support + State Management

  **What to do**:
  - Create `frontend/src/ren_finance/model/effects.cljd` for storage effects
  - Cache last-known state in `shared_preferences`
  - Show cached data when offline
  - Display "Last synced: X minutes ago" indicator (dashboard header, muted secondary text)
  - Re-sync automatically when connection restored (network connectivity listener in `main.cljd`)
  - Ensure navigation tab bar state persists when switching between Dashboard/Import/Settings (current tab index in re-dash app-db)
  - **State hydration on startup** (init sequence):
    1. Load cached state from `shared_preferences`
    2. Show cached data immediately (reduced latency perception)
    3. Fire `::fetch-net-worth`, `::fetch-settings` effects in parallel
    4. On live data response: merge into re-dash db (live wins, cache fills gaps)
    5. Save updated cache via `::save-cache` effect
    6. On network error: keep cached state, show "Offline — last synced X ago" banner
  - **Effects system** in `effects.cljd`:
    - `::fetch-net-worth` → `GET /api/net-worth` → dispatch `:net-worth-loaded`
    - `::import-csv` → `POST /api/import/csv` → dispatch `:import-completed`
    - `::sync-wallets` → `POST /api/wallets/sync` → dispatch `:wallets-synced`
    - `::fetch-settings` → `GET /api/settings` → dispatch `:settings-loaded`
    - `::save-setting` → `PUT /api/settings/base-currency` → dispatch `:setting-saved`
    - `::save-wallet` → `PUT /api/settings/wallets` → dispatch `:wallet-saved`
    - `::delete-wallet` → `DELETE /api/settings/wallets/:id` → dispatch `:wallet-deleted`
    - `::save-cache` → `shared_preferences.setString` (fire-and-forget)
    - `::load-cache` → `shared_preferences.getString` → dispatch `:cache-loaded`

  **Must NOT do**:
  - No conflict resolution (last-write-wins)
  - No offline write queue

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Offline state management with re-dash
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 12, 13, 14)
  - **Blocks**: Tasks F1-F4
  - **Blocked By**: Task 12

  **References**:
  - `shared_preferences` Flutter package
  - re-dash effects for async storage

  **Acceptance Criteria**:
  - [ ] App shows cached data when offline
  - [ ] "Last synced" timestamp displayed
  - [ ] Auto-sync when connection restored
  - [ ] No crash when network unavailable

  **QA Scenarios**:
  ```
  Scenario: Offline mode works
    Tool: Playwright
    Steps:
      1. Load app with data
      2. Disable network
      3. Verify cached data still displays
      4. Verify "Offline" indicator visible
      5. Re-enable network
      6. Verify auto-sync triggers
    Expected Result: App works offline with cached data
    Evidence: .sisyphus/evidence/task-15-offline-mode.png
  ```

  **Commit**: YES
  - Message: `feat(offline): add offline support with cached state`
  - Files: `frontend/src/ren_finance/model/effects.cljd`, `frontend/src/ren_finance/api/client.cljd`

- [ ] 16. VPS Deployment Configuration

  **What to do**:
  - Create `deployment/Dockerfile` for Clojure backend (multi-stage build)
  - Create `deployment/docker-compose.yml` with backend + Nginx + Datahike volume
  - Create `deployment/nginx/nginx.conf` for reverse proxy configuration
  - Create `deployment/nginx/ssl.conf` for SSL/TLS settings
  - Create `deployment/scripts/setup-vps.sh` for initial VPS setup
  - Create `deployment/scripts/deploy.sh` for build + deploy
  - Create `deployment/systemd/ren-finance.service` for auto-restart
  - **Docker build context**: Set `context: ..` in docker-compose.yml (parent dir contains `backend/`)
    - Dockerfile located at `deployment/Dockerfile` uses `COPY ../backend/ /app/backend/`
    - Or better: Dockerfile lives at project root, compose `context: .` with `dockerfile: deployment/Dockerfile`
  - **Database backup**: Create `deployment/scripts/backup-db.sh`
    - Rsync Datahike storage directory to `./backups/datahike-{date}/`
    - Cron job recommended: daily at 3am via host crontab (not container cron)
    - Keep last 7 daily + 4 weekly backups (rotate script)
    - Restore instructions in README.md: stop backend, copy backup dir back, restart

  **Must NOT do**:
  - No cloud-specific services (keep it generic)
  - No CI/CD pipeline (manual deployment for MVP)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Docker and deployment configuration
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 17, 18)
  - **Blocks**: Task F1-F4
  - **Blocked By**: Task 11

  **References**:
  - Docker: multi-stage build for Clojure
  - Nginx: reverse proxy for HTTP server

  **Acceptance Criteria**:
  - [ ] `docker-compose up -d` starts backend
  - [ ] Backend accessible on configured port
  - [ ] Nginx reverse proxy works

  **QA Scenarios**:
  ```
  Scenario: Docker deployment works
    Tool: Bash
    Steps:
      1. Run `docker-compose up -d` in deployment/
      2. Verify container running
      3. `curl http://localhost:3000/api/net-worth`
      4. Verify 200 response
    Expected Result: Backend responds via Docker
    Evidence: .sisyphus/evidence/task-16-docker-deploy.txt
  ```

  **Commit**: YES
  - Message: `feat(deploy): add Docker deployment configuration`
  - Files: `deployment/Dockerfile`, `deployment/docker-compose.yml`, `deployment/nginx/`, `deployment/scripts/`

- [ ] 17. HTTPS + Reverse Proxy Setup

  **What to do**:
  - Configure `deployment/nginx/nginx.conf` with SSL/TLS certificates
  - Use Let's Encrypt for free SSL certificates
  - Create `deployment/scripts/renew-ssl.sh` for certbot renewal
  - Configure HTTPS redirect in Nginx
  - Create security headers configuration in `deployment/nginx/ssl.conf`

  **Must NOT do**:
  - No custom domain (use IP or self-signed for MVP)
  - No CDN configuration

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: SSL/TLS configuration
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 16, 18)
  - **Blocks**: Task F1-F4
  - **Blocked By**: Task 16

  **References**:
  - Let's Encrypt: `certbot --nginx`
  - Nginx SSL: `ssl_certificate`, `ssl_certificate_key`

  **Acceptance Criteria**:
  - [ ] HTTPS works on port 443
  - [ ] HTTP redirects to HTTPS
  - [ ] SSL certificate valid

  **QA Scenarios**:
  ```
  Scenario: HTTPS works
    Tool: Bash
    Steps:
      1. `curl https://your-vps-ip/api/net-worth`
      2. Verify 200 response
      3. Verify SSL certificate valid
    Expected Result: HTTPS connection succeeds
    Evidence: .sisyphus/evidence/task-17-https.txt
  ```

  **Commit**: YES
  - Message: `feat(deploy): add HTTPS with Let's Encrypt`
  - Files: `deployment/nginx/nginx.conf`, `deployment/nginx/ssl.conf`, `deployment/scripts/renew-ssl.sh`

- [ ] 18. Multi-Wallet Aggregation

  **What to do**:
  - Create `backend/src/ren_finance/wallets/multi.clj`
  - Aggregate balances across all configured wallet addresses
  - Handle duplicate assets (ETH on multiple wallets)
  - Unified net worth calculation across all wallets
  - Update MCP tools to support multi-wallet queries

  **Must NOT do**:
  - No cross-chain trading
  - No wallet management (read-only)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Aggregation logic across multiple wallets
  - **Skills**: []
    - No specialized skills needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 16, 17)
  - **Blocks**: Tasks F1-F4
  - **Blocked By**: Task 4

  **References**:
  - Dedup strategy: sum balances by asset across wallets
  - Net worth: sum of all wallet balances in base currency
  - Concurrency: sync wallets **sequentially** with 500ms gap (rate limit protection, see Task 4)

  **Acceptance Criteria**:
  - [ ] `(multi-wallet-net-worth conn)` returns total across all wallets
  - [ ] Duplicate assets aggregated correctly
  - [ ] MCP `get_net_worth` tool includes all wallets

  **QA Scenarios**:
  ```
  Scenario: Multi-wallet aggregation
    Tool: Bash
    Preconditions: ETH + BTC wallet addresses configured
    Steps:
      1. Call `(multi-wallet-net-worth conn)`
      2. Verify includes balances from both wallets
    Expected Result: Combined net worth value
    Evidence: .sisyphus/evidence/task-18-multi-wallet.txt
  ```

  **Commit**: YES
  - Message: `feat(wallets): add multi-wallet net worth aggregation`
  - Files: `backend/src/ren_finance/wallets/multi.clj`

---

## Final Verification Wave

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists. For each "Must NOT Have": search codebase for forbidden patterns. Check evidence files exist in .sisyphus/evidence/.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run linter + tests. Review all changed files for: empty catches, console.log in prod, commented-out code, unused imports.
  Output: `Build [PASS/FAIL] | Lint [PASS/FAIL] | Tests [N pass/N fail] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high` (+ `playwright` skill if UI)
  Start from clean state. Execute EVERY QA scenario from EVERY task. Test cross-task integration.
  Output: `Scenarios [N/N pass] | Integration [N/N] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff. Verify 1:1 - everything in spec was built, nothing beyond spec was built.
  Output: `Tasks [N/N compliant] | VERDICT`

---

## Commit Strategy

- **Wave 1**: `feat(core): add project scaffolding, Datahike schema, wallet lookup, and currency conversion`
- **Wave 2**: `feat(backend): add HTTP server, net worth queries, and MCP server`
- **Wave 3**: `feat(frontend): add ClojureDart dashboard, CSV import UI, and settings view`
- **Wave 4**: `feat(deploy): add Docker deployment, HTTPS, and multi-wallet aggregation`

---

## Success Criteria

### Verification Commands
```bash
# Backend tests
cd backend && clojure -M:test

# MCP server test
cd backend && npx @modelcontextprotocol/inspector clojure -M -m ren-finance.mcp.server

# Frontend build
cd frontend && clj -M:cljd compile && flutter build web --release

# Docker deployment
cd deployment && docker-compose up -d
curl http://localhost:3000/api/net-worth
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] All tests pass
- [ ] MCP server responds to tool invocations
- [ ] ClojureDart app builds for web + mobile
- [ ] Docker deployment works
- [ ] HTTPS configured with valid certificate
- [ ] UI follows Revolut DESIGN.md (dark theme, fintech aesthetic)
- [ ] UI components follow Material 3 guidelines (accessibility, touch targets, focus states)
