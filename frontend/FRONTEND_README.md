# SmartATS Frontend

Plain HTML, CSS, and JavaScript frontend for SmartATS.

## UX Architecture

```
Browser UI -> ui.js + navbar.js + page modules -> api.js -> Spring Boot Backend -> Gemini API
```

The frontend keeps shared polish in a small set of cross-page layers:

- `frontend/js/ui.js`
  Shared toast notifications, confirmation modal, skeleton helpers, empty-state helpers, and global UI readiness hooks.
- `frontend/js/navbar.js`
  Role-aware navigation, mobile navigation behavior, dropdown accessibility, and session-aware profile actions.
- `frontend/css/landing.css`
  Shared application layout, design tokens, focus states, motion rules, responsive breakpoints, card/system styling, and notification/modal polish.
- Page modules such as `ats.js`, `jobs.js`, `interview.js`, `applicants.js`, `recruiter.js`, and `candidate-dashboard.js`
  Feature logic plus page-specific rendering on top of the shared UX foundation.

The frontend never stores or sends Gemini API keys.

## Responsive Strategy

The application is optimized around a small set of responsive rules instead of one-off per-page overrides:

- shared page shells (`page-wrapper`, `page-hero`, `panel`, `grid` primitives) collapse cleanly from desktop to tablet and mobile
- navigation switches to a mobile menu below the tablet breakpoint with role-aware links preserved
- cards, analytics panels, applicant/job/interview grids, and ATS upload surfaces use flexible `minmax(...)` layouts to avoid overflow
- mobile actions stack into full-width buttons where interaction density is high
- ATS, candidate analytics, recruiter analytics, interview, applicants, and create-job flows were all smoke-tested at desktop, tablet, and mobile widths

## Loading And Empty States

Loading behavior avoids blank surfaces and layout jumps:

- shared skeleton cards for job/applicant/interview/metric loading
- analytics chart skeletons before data hydration
- ATS upload and analysis progress surfaces
- interview setup, generation, live-session, and submission transitions
- richer empty states with clearer guidance and contextual CTAs for jobs, applicants, recruiter jobs, and interviews

## Animation System

Animations are intentionally subtle and short:

- page-entry fade/slide transitions
- faster hover elevation and button transitions
- polished toast entry/exit motion
- modal and confirmation dialog transitions
- shimmer skeletons for perceived progress
- reduced-motion support via `prefers-reduced-motion`

## Accessibility Improvements

The production polish pass added a stronger accessibility baseline:

- keyboard-friendly profile dropdown and mobile navigation behavior
- visible focus rings across links, buttons, fields, and custom controls
- improved button semantics for profile actions and confirmation UX
- better toast roles (`status` / `alert`) and live-region friendliness
- safer mobile nav state with `aria-expanded`
- consistent field sizing and touch targets for mobile interaction

## Performance Improvements

Frontend performance improvements focus on perceived speed and duplicate-work reduction:

- shared role-resolution caching in `auth.js` to reduce repeated `/api/users/all` fetches
- skeleton-first rendering before async content loads
- match-score, resume-status, and application-score caching remain in place and were preserved
- chart rendering stays dependency-free and lightweight through `analytics-charts.js`
- shared UI helpers centralize repeated toast/empty/loading logic instead of duplicating DOM work in every page script

## AI-Connected Pages

- `interview.html`
- `ats.html`
- `jobs.html`
- `recruiter.html`
- `dashboard.html`

## Real Backend Integrations

Interview flow:

- `POST /api/interview/sessions`
- `GET /api/interview/sessions/{sessionId}`
- `PUT /api/interview/sessions/{sessionId}/answers`
- `POST /api/interview/sessions/{sessionId}/submit`
- `GET /api/interview/sessions/{sessionId}/result`

Structured AI endpoints also available:

- `POST /api/ai/interview/generate`
- `POST /api/ai/interview/evaluate`

ATS flow:

- `POST /api/resumes/upload`
- `POST /api/ai/ats/analyze`

Job matching:

- `GET /api/match/{jobId}`
- `POST /api/ai/match/score`

Analytics:

- `GET /api/analytics/recruiter`
- `GET /api/analytics/recruiter/export.csv`
- `GET /api/analytics/candidate`

## How To Run

1. Start the Spring Boot backend on `http://localhost:8080`.
2. Serve the `frontend/` directory:

```powershell
cd frontend
python -m http.server 5500
```

3. Open:

```text
http://localhost:5500/index.html
```

## Verification Summary

Syntax checks run clean for:

```powershell
node --check frontend\js\ui.js
node --check frontend\js\navbar.js
node --check frontend\js\jobs.js
node --check frontend\js\applicants.js
node --check frontend\js\interviews.js
node --check frontend\js\interview.js
node --check frontend\js\recruiter.js
node --check frontend\js\candidate-dashboard.js
```

Manual browser verification covered:

- candidate dashboard on desktop and mobile
- candidate jobs and applications views
- ATS upload/history surfaces on desktop and mobile
- full mock interview flow through results
- interview exit confirmation modal
- candidate settings
- recruiter dashboard
- recruiter applicants on mobile
- recruiter create-job flow
- recruiter interviews
- recruiter settings

## Safety Notes

- `api.js` maps backend failures to safe UI messages instead of raw server payloads.
- frontend error states avoid raw stack traces and backend HTML error bodies.
- ATS and interview pages render normalized backend responses only.
