# Module 5 — Task Tracking Record (Fan Shuaifei, ID 2362984)

> **Scope.** Personal task ledger maintained for Module 5 (Resource
> Creation & Metadata Maintenance) during Sprints 1–3 of the Heritage
> Sharing Platform project (CPT202 Group 07, April–May 2026).
>
> **Source of truth.** Task IDs (`M5-T*`) trace directly to the
> Conventional-Commits Git history in
> https://github.com/172021027/cpt202-group07-heritage-sharing
> (filter author = Fan Shuaifei, see Figure C.3). Story-point estimates
> were agreed in the Sprint Planning chat; actual hours were
> self-reported at the end of each sprint.

## Sprint mapping to PBIs

| Sprint | Window (2026) | PBIs covered by Module 5 |
|--------|--------------|--------------------------|
| Sprint 1 | week of 13 Apr | PBI4 foundation (entity + status enum) |
| Sprint 2 | week of 20 Apr | PBI4 (submission endpoint + form) + PBI5 (copyright declaration) |
| Sprint 3 | week of 27 Apr | PBI4 (resubmit flow) + bug-fix from change-management case |

## Task ledger

| Task ID | Sprint | PBI | Description | Layer | Est. SP | Actual SP | Status | Linked commit (Conventional) |
|---------|--------|-----|-------------|-------|---------|-----------|--------|------------------------------|
| M5-T01 | 1 | PBI4 | Design `Resource` entity + JPA mapping | backend | 2 | 2 | Done | `feat(submission): resource entity, status state machine and persistence (PBI4 foundation)` |
| M5-T02 | 1 | PBI4 | Implement `ResourceStatus` enum + converter | backend | 1 | 1 | Done | same commit as T01 |
| M5-T03 | 1 | PBI4 | `SubmitResourceRequest` DTO with validation annotations | backend | 1 | 1 | Done | same commit as T01 |
| M5-T04 | 2 | PBI4 | `POST /api/resources/submit` endpoint — metadata binding | backend | 2 | 3 | Done | `feat(submission): submit endpoint, file validation and copyright declaration (PBI4 + PBI5)` |
| M5-T05 | 2 | PBI5 | Backend non-blank validation for `copyrightDeclaration` | backend | 1 | 1 | Done | same commit as T04 |
| M5-T06 | 2 | PBI4 | `submission.html` form layout + field labels | frontend | 2 | 2 | Done | `feat(submission-ui): submission form, copyright field and local draft auto-save` |
| M5-T07 | 2 | PBI4 | `submission.css` styling + responsive layout | frontend | 1 | 1 | Done | same commit as T06 |
| M5-T08 | 2 | PBI4 | Client-side draft auto-save (`localStorage`) | frontend | 2 | 3 | Done | same commit as T06 |
| M5-T09 | 2 | PBI4 | Tag-chip input (max 3, dedupe) | frontend | 1 | 1 | Done | same commit as T06 |
| M5-T10 | 2 | PBI5 | Field-name mismatch fix (`copyright_declaration` → `copyrightDeclaration`) | full-stack | 1 | 2 | Done | bug-fix follow-up (see Section 4 change-management case) |
| M5-T11 | 3 | PBI4 | `RejectedSubmissionEditResponse` DTO | backend | 1 | 1 | Done | `feat(submission): resource entity, status state machine and persistence (PBI4 foundation)` *(extended)* |
| M5-T12 | 3 | PBI4 | `getRejectedSubmissionForEdit` + `resubmitRejectedSubmission` service | backend | 2 | 2 | Done | `feat(submission): submit endpoint, file validation and copyright declaration (PBI4 + PBI5)` *(extended)* |
| M5-T13 | 3 | PBI4 | Revise-mode UI (`submission.html?mode=revise&...`) | frontend | 2 | 2 | Done | `feat(submission-ui): submission form, copyright field and local draft auto-save` *(extended)* |
| M5-T14 | 3 | PBI4 | Soft-delete endpoint `DELETE /api/resources/mine/{id}` | backend | 1 | 1 | Done | same commit as T04 |
| M5-T15 | 3 | PBI4/5 | Integration tests `IT-RES-01`, `IT-RES-02` | test | 2 | 3 | Done | `test: unit and integration tests across auth, submission, review and discovery` |
| M5-T16 | 3 | PBI4/5 | Unit tests `ResourceServiceTest`, `ResourceControllerTest` | test | 1 | 1 | Done | same commit as T15 |
|        |        |     | **Totals** |       | **23**  | **27**    |        |                              |

## Sprint summary (planned vs. actual)

| Sprint | Planned SP | Actual SP | Variance | Note |
|--------|-----------|-----------|----------|------|
| Sprint 1 | 4 | 4 | 0 | Foundation work matched expectation. |
| Sprint 2 | 9 | 13 | +4 (+44%) | Draft auto-save and the `copyrightDeclaration` field-name fix were under-estimated. |
| Sprint 3 | 10 | 10 | 0 | Estimation corrected after Sprint 2 retrospective. |

## Daily progress sync

Task progress was reported daily in the team WeChat group around 21:00
(see Figure A.X — group-chat screenshot of one such sync). Each member
posted: (1) tasks closed today, (2) tasks in progress, (3) blockers. I
ported this format from the daily-stand-up pattern in Scrum (Schwaber
and Sutherland, 2020).

## How this ledger was maintained

- **Source of granular truth** — Git Conventional-Commits history, one
  commit per logically grouped task or task cluster.
- **Source of weekly truth** — Sprint plan published in the group
  report (Group 07, 2026, Table 3).
- **Source of daily truth** — WeChat group chat.
- This Markdown file is a *retrospective consolidation* of those three
  sources, produced for the individual report; it is not a
  substitute for them.
