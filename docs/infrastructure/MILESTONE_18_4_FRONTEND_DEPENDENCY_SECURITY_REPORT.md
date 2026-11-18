# MILESTONE 18.4 — FRONTEND DEPENDENCY SECURITY REPORT
## Virtual Classroom Platform — Upstream Advisory Investigation & Risk Disposition

**Investigation Date:** 2026-11-18  
**Branch:** `feature/staging-readiness-config`  
**Triggering CI Run:** GitHub Actions Run `44249038808` (Commit `7cc23ac`, Job `134614920858` — Step 8: `Run npm security audit`)  
**CI Audit Command:** `npm audit --audit-level=high` (`.github/workflows/pr-validation.yml`, line 76)  
**Remediation Decision:** **NO UNSAFE BREAKING UPGRADE OR ADVISORY SUPPRESSION APPLIED — DOCUMENTED UPSTREAM UNPATCHED DEV-ONLY ADVISORY (STOP PER PHASE 3 RULE)**

---

## 1. Executive Summary

Investigation of the `Frontend QA` `npm audit --audit-level=high` failure in GitHub Actions run `44249038808` determined with 100% certainty that:

1. **All 12 `HIGH` audit findings stem from a single upstream transitive package: `picomatch`** (`GHSA-fc5q-39xc-vjpf` / `CVE-2026-112366`, CVSS `7.5`, CWE-1333, ReDoS via single-branch repeated extglobs).
2. **No patched version of `picomatch` exists on the npm registry as of 2026-11-18.** The GitHub Advisory Database entry (updated `2026-11-17T15:56:44Z` — one day after the previous green CI run on `2026-11-16`) lists `vulnerable_version_range: "<= 4.0.7"` and **`first_patched_version: null`**, and `4.0.7` is the latest published version of `picomatch` on npm.
3. **Even breaking major-version upgrades cannot resolve the advisory.** Inspecting `vitest@5.1.1` on npm confirms it still declares `"picomatch": "^4.0.7"` and `"tinyglobby": "^0.2.17"`, resolving to the exact same vulnerable `picomatch@4.0.7`.
4. **Zero `HIGH` or `CRITICAL` vulnerabilities exist in production dependencies.** `npm audit --omit=dev --json` reports **`0` CRITICAL, `0` HIGH, and `2` MODERATE** advisories. `picomatch` is strictly a **`devDependencies` build/test tool** (TailwindCSS, Vite, Vitest) and is never bundled into `dist/` or shipped to end-user browsers.
5. **Strict Compliance with Milestone 18.4 Rules:** Per Phase 2 & Phase 3 directives (*"Do not suppress or ignore advisories merely to make CI green"* and *"If the advisory cannot safely be resolved, document the remaining risk and stop rather than applying a breaking upgrade"*), `frontend/package.json`, `frontend/package-lock.json`, and `.github/workflows/pr-validation.yml` were **not** weakened or forcibly upgraded.

---

## 2. Complete Vulnerability Inventory (Verified Live)

### 2.1 Primary Blocking Advisory (`HIGH` — Causes CI Failure)

| Field | Verified Value |
| :--- | :--- |
| **Vulnerable Package** | `picomatch` (transitive dependency) |
| **Advisory ID** | `GHSA-fc5q-39xc-vjpf` (npm source `1247710`) |
| **CVE Identifier** | `CVE-2026-112366` |
| **Title** | `picomatch vulnerable to ReDoS through single-branch repeated extglobs` |
| **Severity / CVSS** | **HIGH** — CVSS `7.5` (`CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H`), `CWE-1333` |
| **Published / Updated** | Published `2026-10-19T18:32:12Z` / Updated `2026-11-17T15:56:44Z` |
| **Vulnerable Range** | `<= 4.0.7` |
| **Installed Versions** | `2.3.2` (1 copy) and `4.0.7` (3 copies) |
| **Latest npm Version** | `4.0.7` (verified via `npm view picomatch versions --json`) |
| **Patched Version** | **`null` (NO PATCHED RELEASE EXISTS UPSTREAM)** |
| **Environment Scope** | **`devDependencies` ONLY** (never present in production runtime or `dist/` bundle) |
| **Exploitability in This App** | **Negligible / Non-exploitable at runtime.** Only executes during local dev / CI build file-globbing (`tailwindcss`, `vite`, `vitest`) against trusted repository paths; no untrusted external glob input is ever supplied. |

#### Exact Transitive Dependency Tree (`npm ls picomatch --all`)
```text
virtual-classroom-frontend@1.0.0
+-- tailwindcss@3.4.19 [dev]
|   +-- chokidar@3.6.0
|   |   +-- anymatch@3.1.3 -> picomatch@2.3.2 (deduped)
|   |   `-- readdirp@3.6.0 -> picomatch@2.3.2 (deduped)
|   +-- fast-glob -> micromatch@4.0.8
|   `-- micromatch@4.0.8 -> picomatch@2.3.2
+-- vite@6.4.6 [dev]
|   +-- fdir@6.5.0 -> picomatch@4.0.7 (deduped)
|   +-- picomatch@4.0.7
|   `-- tinyglobby@0.2.17
|       +-- fdir@6.5.0 -> picomatch@4.0.7 (deduped)
|       `-- picomatch@4.0.7
`-- vitest@3.2.7 [dev]
    `-- picomatch@4.0.7
```

*(Because `picomatch` propagates upward through 11 ancestor packages — `anymatch`, `chokidar`, `fast-glob`, `fdir`, `micromatch`, `readdirp`, `sucrase`, `tailwindcss`, `tinyglobby`, `vite`, `vitest` — `npm audit` counts 12 `HIGH` entries, all rooted in this single `CVE-2026-112366` advisory.)*

### 2.2 Secondary Non-Blocking Advisories (`MODERATE` — Do Not Fail `--audit-level=high`)

| Package | Installed Version | Scope | Advisories | Patched Version | Compatibility & Application Risk Assessment |
| :--- | :---: | :---: | :--- | :---: | :--- |
| `react-router` / `react-router-dom` | `6.30.6` *(latest `6.x`)* | **Production** | `GHSA-wrjc-x8rr-h8h6` (Backslash open redirect, CWE-601, MODERATE)<br>`GHSA-337j-9hxr-rhxg` (SSR `deserializeErrors()` constructor injection, CVSS 6.1, CWE-470, MODERATE) | `7.18.0+` (`7.18.5`) — **SemVer MAJOR** | **Mitigated in current SPA:**<br>1. `GHSA-337j-9hxr-rhxg` applies exclusively to React Router SSR hydration (`deserializeErrors()`); this application is a pure client-side Vite SPA (`BrowserRouter`) with zero SSR.<br>2. `GHSA-wrjc-x8rr-h8h6` is mitigated by `sanitizeRedirectPath()` in `frontend/src/features/auth/LoginPage.tsx` (lines 18–30), which explicitly rejects backslashes (`!trimmed.includes('\\')`), `//`, and `:` schemes (verified by `LoginPage.test.tsx`); all other `<Link>` targets are static internal routes.<br>3. Upgrading `react-router-dom` v6 -> v7 is a breaking major upgrade deferred to a dedicated compatibility milestone. |
| `@vitest/mocker` / `vitest` | `3.2.7` *(latest `3.x`)* | **Dev only** | `GHSA-82fw-gwwq-j7x9` (Path traversal via redirect mock, CVSS 5.9, MODERATE)<br>`GHSA-qxg6-89mg-rqv8` (Manual mock registration, CVSS 4.7, MODERATE) | `4.1.12+` / `5.1.1` — **SemVer MAJOR** | `devDependencies` test runner only; never exposed to network or production runtime. Major upgrade deferred (and `vitest@5.1.1` still pulls vulnerable `picomatch@4.0.7`). |

---

## 3. Fix Evaluation & Why No Forced Upgrade Was Applied

| Candidate Option | Outcome | Evaluation |
| :--- | :---: | :--- |
| **Option 1: Patch `picomatch` via `npm overrides`** | **Impossible Today** | Latest npm version is `4.0.7`; `first_patched_version` is `null`. No patched release exists to override to. |
| **Option 2: `npm audit fix --force` (`tailwindcss@4.3.5`, `vitest@5.1.1`, `vite@8.3.8`, `react-router-dom@7.18.5`)** | **Rejected (Fails + Breaks App)** | 1. **Still fails `npm audit`:** `vitest@5.1.1` still depends on `picomatch@^4.0.7` (`CVE-2026-112366`).<br>2. Introduces 4 simultaneous breaking major upgrades (Tailwind CSS v3 -> v4 breaks `tailwind.config.js` & `@tailwindcss/forms`; React Router v6 -> v7; Vitest v3 -> v5). |
| **Option 3: Suppress advisory or alter CI `pr-validation.yml` to `--omit=dev`** | **Not Applied Without Approval** | Explicitly prohibited by Milestone 18.4 rules (*"Do not suppress or ignore advisories merely to make CI green"*). Available only as an explicit operator policy decision (see Section 6). |
| **Option 4: Document upstream unpatched status, verify production safety, preserve lockfile stability** | **Selected (Per Phase 3 Directive)** | Follows *"If the advisory cannot safely be resolved, document the remaining risk and stop rather than applying a breaking upgrade."* Zero destabilization, 100% reproducible `package-lock.json`. |

---

## 4. Changed Files

| File | Action | Reason |
| :--- | :---: | :--- |
| `frontend/package.json` | **Unchanged** | No compatible patched version exists for `picomatch` (`CVE-2026-112366`). |
| `frontend/package-lock.json` | **Unchanged** | Lockfile reproducibility preserved. |
| `.github/workflows/pr-validation.yml` | **Unchanged** | No audit suppression applied. |
| `docs/infrastructure/MILESTONE_18_4_FRONTEND_DEPENDENCY_SECURITY_REPORT.md` | **Created** | Formal advisory analysis, risk evidence, and operator decision options. |

---

## 5. Verification Results (Local & CI)

### 5.1 Local Verification (`frontend/`)
- **`npm audit --omit=dev --json` (Production Dependencies Only):**
  - `CRITICAL`: **`0`**
  - `HIGH`: **`0`**
  - `MODERATE`: `2` (`react-router` / `react-router-dom` `6.30.6` — mitigated in client-only SPA)
  - `LOW`: `0`
- **`npm audit --json` (All Dependencies incl. Dev):**
  - `CRITICAL`: **`0`**
  - `HIGH`: `12` (100% caused by unpatched dev-only `picomatch <= 4.0.7`, `CVE-2026-112366`)
  - `MODERATE`: `3` (`react-router-dom@6.30.6`, `@vitest/mocker@3.2.7`)
- **TypeScript + Production Build (`npm run build` / `tsc -b && vite build`):**
  - Status: **`PASS`** (`1887` modules transformed, built cleanly in `15.37s`).
- **Vitest Unit & Contract Test Suite (`npm test -- --run`):**
  - Status: **`PASS`** — **`10` test files passed (`10/10`), `37` tests passed (`37/37`), `0` failed** in `15.31s` (including `ApiContract.test.ts`, `TokenSecurity.test.ts`, `LoginPage.test.tsx`, `RoleLayouts.test.tsx`).

### 5.2 GitHub Actions CI Evidence
- **Run `44249038808` (Commit `7cc23ac`):**
  - `PostgreSQL 17 Migration & Standalone Runner Gate`: **SUCCESS**
  - `Backend QA (JUnit 5, Mockito, Spring Security, Package)`: **SUCCESS**
  - `Docker Image Build & Compose Orchestration`: **SUCCESS**
  - `Frontend QA`: `npm ci` (**SUCCESS**), `npm run build` (**SUCCESS**), `npm test -- --run` (**SUCCESS**), `npm audit --audit-level=high` (**FAILURE** solely due to unpatched upstream dev-only `picomatch <= 4.0.7`, `CVE-2026-112366`).

---

## 6. Recommended Next Steps & Operator Decision Options

Because `CVE-2026-112366` (`picomatch <= 4.0.7`) has **no upstream patch published yet** and **zero production runtime impact**, two operator-governed paths are available:

1. **Path A — Wait for Upstream `picomatch >= 4.0.8` Patch (Zero Policy Change):**
   - As soon as `picomatch@4.0.8` (or a `2.3.x` backport) is published to npm, add `"picomatch": "^4.0.8"` under `frontend/package.json` `overrides` (or update `package-lock.json` via `npm update picomatch`), re-run `npm ci`, `npm audit --audit-level=high`, `npm run build`, and `npm test`, and confirm green CI.
2. **Path B — Explicit Operator Authorization to Scope CI Blocking Gate to Production Dependencies (`npm audit --omit=dev --audit-level=high`) While Reporting Full Audit as Advisory:**
   - If staging merge/deployment should proceed before upstream publishes `picomatch@4.0.8`, the operator may explicitly authorize updating `.github/workflows/pr-validation.yml` Step 8 to:
     - **Fail CI** on `npm audit --omit=dev --audit-level=high` (strictly blocking any `HIGH`/`CRITICAL` production dependency vulnerability), AND
     - **Log/report** full `npm audit --audit-level=high` (including `devDependencies`) as a visible CI warning annotation so unpatched dev-tool CVEs remain tracked transparently without blocking staging releases.
3. **Planned Post-Staging Maintenance Item:**
   - Schedule a dedicated compatibility milestone for upgrading `react-router-dom` `6.30.6` -> `7.18.5+` (resolving the 2 `MODERATE` advisories permanently) and `vitest` `3.x` -> `5.x` once `picomatch` is patched upstream.

---

## 7. Sign-Off

Milestone 18.4 investigation and documentation are **COMPLETE**. No breaking upgrades, advisory suppressions, database changes, or deployments were performed. Awaiting operator decision between **Path A** (wait for upstream `picomatch` patch) and **Path B** (operator-authorized CI gate split between blocking production audit and visible dev-dependency audit reporting).
