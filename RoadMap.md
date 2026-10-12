# Hashimoto Tracker: tentative 12-month roadmap

## Purpose and planning assumptions

This is a learning and product-building plan, not a delivery promise. Month 1 starts when we agree on the first product goal. The month numbers are checkpoints, not deadlines; we can slow down, reorder work, or drop a feature when user feedback or technical learning changes the plan.

The target for the first year is a useful, secure web app that helps people record relevant parts of daily life and review understandable trends. It is **not** a promise to build every service in the target architecture, integrate every wearable, or deliver a clinical decision system within one year.

The architecture diagram is a map of possible growth. We will build each capability when a user need justifies it. A domain name in the diagram does not automatically mean it needs its own microservice.

## What is already in place

- A React/Vite frontend, API Gateway, and Patient Service.
- Keycloak login/registration and role-protected API access.
- PostgreSQL, Kafka, and local Docker Compose/K3d environments.
- Kubernetes manifests and local deployment commands.
- A working authenticated patient creation flow demonstrated in Kubernetes.

This is a platform foundation. The core Hashimoto tracking features—symptom, routine, meal, and medication logging; a daily timeline; and useful personal summaries—still need product definition and implementation.

## How we will work through the roadmap

For each feature, follow the same small loop:

1. State the user's problem and the situation in which it occurs.
2. Agree on one user story and how we will know it works.
3. Sketch the screen and decide the minimum data to keep.
4. Implement one complete path through frontend, API, authorization, and storage.
5. Review it together, learn from it, and only then choose the next feature.

Prefer a small number of clear domain boundaries. Begin with the existing application structure and split out another service only when independent ownership, deployment, scaling, or security requirements justify that cost.

## Month-by-month outline

| Checkpoint | Focus | Intended outcome |
| --- | --- | --- |
| **1 — Product discovery** | Choose the first user and first problem to solve. Discuss the daily situations, current workarounds, and what a useful result would look like. | A short MVP brief, one primary user journey, a small feature list, and a clear list of items explicitly postponed. |
| **2 — UX and data design** | Sketch the key screens and define the minimum data model for a daily check-in. Decide how users correct/delete entries and how timestamps, units, provenance, and time zones are represented. | Clickable or paper prototype; agreed field definitions and acceptance criteria for the first end-to-end feature. |
| **3 — Delivery foundation** | Make builds and deployments repeatable. Add the CI checks needed for the existing app, then introduce Argo CD in a local development cluster when its source-of-truth and image flow are understood. | A code change can be built and delivered to the development environment through a documented pipeline; local Docker/Kubernetes workflows remain reproducible. |
| **4 — First tracking workflow** | Build one complete symptom check-in: create, validate, save, list, and edit/delete it with the signed-in user's authorization. | A person can record a symptom and see it again in a chronological view. This is the first product vertical slice. |
| **5 — Daily context** | Add only the contextual details selected during discovery, such as activity/routine notes, meals, or medication taken. Keep these as user records; do not make treatment recommendations. | A check-in can capture the agreed daily context without overwhelming the user with a large form. |
| **6 — Personal history** | Improve the timeline, filters, corrections, and data export/delete behavior. Clarify manual versus imported entries in the design, even before imports exist. | Users can review and correct their own history; the app gives clear feedback when data is missing. |
| **7 — First summaries** | Add simple descriptive views over the user's own records: counts, time trends, and comparisons over explicitly chosen periods. | A user can answer a small agreed question from their history. Summaries show their time range and data completeness; they do not claim causation. |
| **8 — Reliability and privacy review** | Review authorization, per-user data isolation, secret handling, backups/restore, error handling, log redaction, deletion, and operational alerts. | A written review with prioritized fixes; a tested local recovery path and no accidental exposure of health data in logs. |
| **9 — Small usability pilot** | Let a few willing testers try the agreed workflow under a clear privacy notice and appropriate consent. Use synthetic data for demos and debugging wherever possible. | A prioritized list of usability issues and evidence about which fields/features help users; revise the MVP before adding more domains. |
| **10 — Mobile and data-source decision** | Decide whether mobile access is necessary now, which mobile platform to support first, and which source data would answer a specific user question. Recheck provider access and current terms. | A decision record: mobile framework/platform, first data types, permissions, connector path, and a go/no-go decision for an integration. |
| **11 — One health-data connector (conditional)** | If Month 10 justifies it, implement one source adapter and sync path. Keep it behind a provider-neutral ingestion contract with consent, provenance, deduplication, sync status, revocation, and deletion. | One selected source can sync a limited set of user-approved records without overwriting manual entries. If no source is ready or useful, use this checkpoint to improve the core tracking workflow instead. |
| **12 — Evaluate and choose the next year** | Review user value, data quality, security, reliability, and maintenance cost. Decide whether analytics should remain descriptive, whether a mobile app is justified, and whether any AI experiment is safe and useful. | A year-one review, updated architecture/roadmap, and a realistic next-stage scope based on evidence rather than the original diagram alone. |

## Product principles and safety boundaries

- Make manual symptom and routine tracking useful even when device integrations are unavailable.
- Ask permission for specific data types only when a feature needs them. Show what source a record came from and when it last synchronized.
- Preserve provenance, original timestamps/units, and user corrections. Retries must not create duplicate records.
- Show patterns as possible associations, with time windows, sources, missing data, and uncertainty. Correlation is not proof of cause.
- Do not recommend changing, starting, or stopping medication. Do not present automated summaries as diagnosis or clinical decisions.
- Treat health information as sensitive. Before a pilot using real health data or any external AI provider, review privacy, security, clinical-safety, and provider-contract requirements with qualified people.
- Build the provider connection later through the health-data integration layer in the [target architecture](docs/fullArchitectureHashimoto%20.md), not directly into each business service or the browser.

## Connected health data: explicitly later

This capability is intentionally scheduled after the core tracking workflow and initial user feedback. For Android, evaluate Health Connect through a mobile app; for iOS, evaluate HealthKit through a mobile app. They are not features the current browser frontend can provide by itself.

Do not begin a new integration using the legacy Google Fit APIs: Google says they will stop being available after the end of 2026. Google Health API is a possible cloud-oriented route, but its current developer documentation says new projects are not being onboarded; verify access again at the Month 10 decision point. Prefer one valuable, available source over several speculative integrations.

References: [Google Fit migration guidance](https://developer.android.com/health-and-fitness/guides/health-connect/migrate/fit-apis-end-of-service), [Google Health API availability](https://developers.google.com/health/get-started), and [Apple HealthKit](https://developer.apple.com/documentation/healthkit).

## Deferred until there is evidence and a clear need

- Building all the future domain services as separate microservices.
- Connecting every wearable, activity, nutrition, or sleep provider at once.
- Real-time AI processing of identifiable customer health data.
- AI-generated treatment or medication advice.
- Production-scale high availability and infrastructure beyond the needs of an initial, controlled pilot.

These items may become appropriate later. They are postponed so the first year can establish a valuable product flow and trustworthy data before adding integrations and complexity.
