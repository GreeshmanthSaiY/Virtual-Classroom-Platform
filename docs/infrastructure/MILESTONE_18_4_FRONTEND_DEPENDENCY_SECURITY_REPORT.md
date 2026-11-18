# MILESTONE 18.4 — FRONTEND DEPENDENCY SECURITY REPORT (FINAL — OPTION B IMPLEMENTED)
## Virtual Classroom Platform — Upstream Advisory Investigation, CI Gate Split & Verification

**Execution Date:** 2026-11-18  
**Branch:** `feature/staging-readiness-config`  
**Prior Failing CI Run:** Run `44249038808` (Commit `7cc23ac`, Job `134614920858` — Step 8: `Run npm security audit`)  
**Option B Implementation Commit:** `2201000` (`2201000ecc301904dfd0ec60a51fe4f5c25c8f01`)  
**Verified Green CI Run:** Run `44286907059` (`https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/44286907059`)  
**Remediation Disposition:** **Option B — Explicitly Authorized CI Gate Split** (Mandatory blocking production audit `--omit=dev --audit-level=high` + separate visible warning audit for `devDependencies`; zero forced breaking upgrades; zero advisory suppression)

---

## 1. Executive Summary

Investigation of the `Frontend QA` `npm audit --audit-level=high` failure in GitHub Actions run `44249038808` verified with 100% precision that:

1. **All 12 `HIGH` audit findings stem from a single upstream transitive `devDependencies` package: `picomatch`** (`GHSA-fc5q-39xc-vjpf` / `CVE-2026-112366`, CVSS `7.5`, CWE-1333, ReDoS via single-branch repeated extglobs).
2. **No patched version of `picomatch` exists on the npm registry as of 2026-11-18.** The GitHub Advisory Database entry (updated `2026-11-17T15:56:44Z`) specifies `vulnerable_version_range: "<= 4.0.7"` and **`first_patched_version: null`**, and `4.0.7` is the latest published release on npm.
3. **Even breaking major-version upgrades cannot resolve the advisory.** `vitest@5.1.1` still declares `"picomatch": "^4.0.7"` and `"tinyglobby": "^0.2.17"`, resolving directly to vulnerable `picomatch@4.0.7`.
4. **Production dependencies have ZERO `HIGH` or `CRITICAL` vulnerabilities.** `npm audit --omit=dev --json` confirms **`0` CRITICAL, `0` HIGH, and `2` MODERATE** advisories. `picomatch` is strictly a build/test `devDependency` and is never bundled into `dist/` or shipped to end-user browsers.
5. **Operator-Approved Option B Executed:** `.github/workflows/pr-validation.yml` was updated to enforce `npm audit --omit=dev --audit-level=high` as a **mandatory, blocking CI gate** while running the full `npm audit --audit-level=high` (including `devDependencies`) separately and publishing full audit output + a visible GitHub Actions `::warning` annotation and step summary for `GHSA-fc5q-39xc-vjpf` / `CVE-2026-112366`.
6. **All 4 GitHub Actions CI Checks Passed (`Run 44286907059`):** Backend QA, PostgreSQL 17 Migration & Standalone Runner Gate, Frontend QA (with verified visible warning annotation), and Docker Image Build & Compose Orchestration all completed with conclusion **`success`**.

---

## 2. Complete Vulnerability Inventory (Preserved & Verified Live)

### 2.1 Primary Upstream Unpatched Advisory (`HIGH` — `devDependencies` Only)

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
| **Remediation Status** | **Open / Tracked Upstream.** Surfaced transparently as a CI `::warning` annotation on every run until `picomatch >= 4.0.8` (or backport) is released on npm. |

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

### 2.2 Secondary Non-Blocking Advisories (`MODERATE`)

| Package | Installed Version | Scope | Advisories | Patched Version | Mitigation & Tracking Status |
| :--- | :---: | :---: | :--- | :---: | :--- |
| `react-router` / `react-router-dom` | `6.30.6` *(latest `6.x`)* | **Production** | `GHSA-wrjc-x8rr-h8h6` (Backslash open redirect, CWE-601, MODERATE)<br>`GHSA-337j-9hxr-rhxg` (SSR `deserializeErrors()` constructor injection, CVSS 6.1, CWE-470, MODERATE) | `7.18.0+` (`7.18.5`) — **SemVer MAJOR** | **Mitigated in current SPA:** Pure client-side Vite SPA (zero SSR hydration); `sanitizeRedirectPath()` in `frontend/src/features/auth/LoginPage.tsx` explicitly blocks `\`, `//`, and `:` schemes (tested in `LoginPage.test.tsx`). Major v6 -> v7 upgrade scheduled for a dedicated post-staging milestone. |
| `@vitest/mocker` / `vitest` | `3.2.7` *(latest `3.x`)* | **Dev only** | `GHSA-82fw-gwwq-j7x9` (Path traversal via redirect mock, CVSS 5.9, MODERATE)<br>`GHSA-qxg6-89mg-rqv8` (Manual mock registration, CVSS 4.7, MODERATE) | `4.1.12+` / `5.1.1` — **SemVer MAJOR** | `devDependencies` test runner only. Major upgrade deferred until `picomatch` is patched upstream. |

---

## 3. Implemented Fix (Authorized Option B) & Exact Changed Files

| File | Action | Exact Change Summary |
| :--- | :---: | :--- |
| `.github/workflows/pr-validation.yml` | **Modified** (`2201000`) | Split single `npm audit --audit-level=high` step into:<br>1. **Step 8 (`Run production npm security audit (blocking)`):** `npm audit --omit=dev --audit-level=high` (mandatory blocking gate).<br>2. **Step 9 (`Run full npm security audit including devDependencies (visible warning)`):** executes full `npm audit --audit-level=high`, prints complete audit output to job logs, emits `::warning title=Frontend Dev-Dependency Security Advisory::...` referencing `GHSA-fc5q-39xc-vjpf` / `CVE-2026-112366`, and writes summary status to `$GITHUB_STEP_SUMMARY`. |
| `docs/infrastructure/MILESTONE_18_4_FRONTEND_DEPENDENCY_SECURITY_REPORT.md` | **Created / Updated** | Preserves complete advisory analysis, dependency tree, verification evidence, CI run results, and remaining risk tracking. |
| `frontend/package.json` | **Unchanged** | Zero forced or breaking dependency upgrades applied. |
| `frontend/package-lock.json` | **Unchanged** | Reproducible lockfile preserved intact. |

*(Zero application source files, zero database configurations, zero Supabase objects, and zero Render/Vercel deployment settings were modified.)*

---

## 4. Verification Results (Local & Live GitHub Actions CI)

### 4.1 Local Frontend Verification (`frontend/`)
- **Production-Only Security Audit (`npm audit --omit=dev --audit-level=high`):** **PASS** (`0` CRITICAL, `0` HIGH, `2` MODERATE).
- **TypeScript & Vite Production Build (`npm run build` / `tsc -b && vite build`):** **PASS** (`1887` modules transformed, built in `8.00s`).
- **Vitest Unit & Contract Test Suite (`npm test -- --run`):** **PASS** — **`10/10` test files passed, `37/37` tests passed, `0` failed** (`18.71s`), covering `ApiContract.test.ts`, `TokenSecurity.test.ts`, `LoginPage.test.tsx`, `RoleLayouts.test.tsx`, and all component/unit suites.

### 4.2 GitHub Actions CI Evidence (Run `44286907059` — Commit `2201000`)
- **Workflow Run URL:** `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/44286907059`
- **Commit SHA:** `2201000ecc301904dfd0ec60a51fe4f5c25c8f01` (`feature/staging-readiness-config`)

| Job ID | CI Job Name | Status | Conclusion |
| :---: | :--- | :---: | :---: |
| `134741799754` | **Frontend QA (TypeScript, Vitest, Build, Audit)** | Completed | **`SUCCESS`** |
| `134741798961` | **Backend QA (JUnit 5, Mockito, Spring Security, Package)** | Completed | **`SUCCESS`** |
| `134741799662` | **PostgreSQL 17 Migration & Standalone Runner Gate** | Completed | **`SUCCESS`** |
| `134741799828` | **Docker Image Build & Compose Orchestration** | Completed | **`SUCCESS`** |

#### Verified Frontend QA Steps (`Job 134741799754`)
- Step 5 — `Install dependencies from lockfile` (`npm ci`): **`SUCCESS`**
- Step 6 — `TypeScript type-check and production build` (`npm run build`): **`SUCCESS`**
- Step 7 — `Run Vitest unit & contract test suite` (`npm test -- --run`): **`SUCCESS`**
- Step 8 — `Run production npm security audit (blocking)` (`npm audit --omit=dev --audit-level=high`): **`SUCCESS`**
- Step 9 — `Run full npm security audit including devDependencies (visible warning)`: **`SUCCESS`** *(emitted visible check-run warning annotation verified via GitHub Check Runs API)*:
  ```text
  annotation_level: "warning"
  title:            "Frontend Dev-Dependency Security Advisory"
  message:          "Full npm audit (including devDependencies) reported HIGH/CRITICAL findings
                     (tracked upstream unpatched GHSA-fc5q-39xc-vjpf / CVE-2026-112366 on
                     picomatch <= 4.0.7). Production audit (--omit=dev --audit-level=high) passed.
                     See docs/infrastructure/MILESTONE_18_4_FRONTEND_DEPENDENCY_SECURITY_REPORT.md."
  ```

---

## 5. Remaining Risks & Remediation Trigger

1. **Unpatched Upstream `picomatch <= 4.0.7` (`GHSA-fc5q-39xc-vjpf` / `CVE-2026-112366`, HIGH, Dev-Only):**
   - *Residual Risk:* Low / build-time only (not shipped to production runtime; processes only trusted repo globs).
   - *Remediation Trigger:* As soon as upstream publishes `picomatch >= 4.0.8` (or a `2.3.x` backport) on npm, pin the patched version via `frontend/package.json` `overrides` / lockfile refresh and verify zero `HIGH` findings on full `npm audit`.
2. **`react-router-dom@6.30.6` (`GHSA-wrjc-x8rr-h8h6` & `GHSA-337j-9hxr-rhxg`, MODERATE, Production):**
   - *Residual Risk:* Mitigated by client-only SPA architecture (no SSR) and `sanitizeRedirectPath()` backslash rejection.
   - *Remediation Trigger:* Schedule a dedicated compatibility milestone for the React Router v6 -> v7 major upgrade.
3. **`@vitest/mocker` / `vitest@3.2.7` (`GHSA-82fw-gwwq-j7x9` & `GHSA-qxg6-89mg-rqv8`, MODERATE, Dev-Only):**
   - *Residual Risk:* Dev test runner only; schedule Vitest major upgrade after `picomatch` is patched upstream.

---

## 6. Operator Handoff

Milestone 18.4 (Option B) is complete and verified across all 4 GitHub Actions CI checks (`Run 44286907059`). Strict boundaries were observed: no merges to `develop`/`main`, no Render/Vercel deployments, no database/Supabase/production changes.

**Stopping here and awaiting Operator Review.**
