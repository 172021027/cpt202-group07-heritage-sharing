# Trello Board Setup — DIY Guide

> **Important honesty note.** I cannot create a Trello board for you,
> and I cannot generate fake screenshots showing a board "as it looked
> in April 2026". If your team genuinely used Trello, log into that
> workspace and screenshot what is there. If your team did not use it,
> then **do not invent one for the report** — the rubric explicitly
> rewards honest description of what you actually did.
>
> What this file gives you is the *structure* of a credible Trello
> board for your submission module — the same structure a real team
> would have used. If you decide to use Trello going forward (for
> Assignment-3-supporting evidence going forward, NOT for
> retroactive claims), this is what to build.

---

## Board layout

**Board name:** `CPT202 Group 07 — Heritage Sharing Platform`

**Lists (columns) from left to right:**
1. `Backlog`
2. `Sprint 2 — To Do`
3. `In Progress`
4. `In Review`
5. `Done`

## Labels (colour-coded)

| Label | Colour | Used on cards that are… |
|-------|--------|-------------------------|
| `PBI4` | green | part of "Save Draft + Formal Submission" |
| `PBI5` | yellow | part of "Copyright Declaration + Validation" |
| `submission` | blue | scoped to my module |
| `backend` | purple | server-side work |
| `frontend` | orange | UI / JS work |
| `test` | red | testing work |

## Cards to create (for the submission module only)

Each card title follows the pattern `SUB-<n> · <short title>`. Put
descriptions, checklists and labels exactly as below.

### SUB-12 · POST /api/resources/submit endpoint
- Labels: `PBI4`, `submission`, `backend`
- Estimate: 3 SP
- Description:
  > Implement multipart endpoint that accepts title, location,
  > categoryId, description, tags, copyrightDeclaration, image, video.
  > Reject unauthenticated and non-contributor users.
- Checklist:
  - [ ] DTO `SubmitResourceRequest`
  - [ ] Controller method with `@RequestParam` for multipart fields
  - [ ] Service method persists `Resource(status=PENDING_REVIEW)`
  - [ ] Returns `{ success, resourceId }`

### SUB-13 · Resource entity + status enum + audit row
- Labels: `PBI4`, `submission`, `backend`
- Estimate: 2 SP
- Description:
  > Define `Resource` JPA entity, `ResourceStatus` enum with backward
  > compatibility against legacy string values, and the
  > `ResourceAction` audit row written on every state transition.

### SUB-14 · Image + video file validation
- Labels: `PBI5`, `submission`, `backend`
- Estimate: 2 SP
- Description:
  > Reject files exceeding the configured size limit; reject
  > unsupported MIME types; short-circuit cheap checks before writing
  > to disk so failed requests don't leave orphan files.

### SUB-15 · Copyright declaration field end-to-end
- Labels: `PBI5`, `submission`, `backend`, `frontend`
- Estimate: 2 SP
- Description:
  > Add `copyrightDeclaration` to the Resource entity, DTOs, controller
  > param list, submission form, and detail page. Server-side
  > rejection if blank.

### SUB-16 · Submission form (HTML + CSS)
- Labels: `PBI4`, `submission`, `frontend`
- Estimate: 2 SP

### SUB-17 · Draft auto-save (localStorage)
- Labels: `PBI4`, `submission`, `frontend`
- Estimate: 2 SP
- Description:
  > Persist the in-progress form to `localStorage` under
  > `heritage_draft` (or `heritage_draft_revise_<id>` in revise mode);
  > restore on next visit; clear on successful submit.

### SUB-18 · Resubmit-after-rejection flow
- Labels: `PBI4`, `submission`, `backend`, `frontend`
- Estimate: 3 SP
- Checklist:
  - [ ] `GET /api/resources/rejected/{id}` returns
    `RejectedSubmissionEditResponse`
  - [ ] `POST /api/resources/{id}/resubmit` only allowed if status =
    REJECTED
  - [ ] Front-end revise mode (`submission.html?mode=revise`)

### SUB-19 · Integration tests (happy + access control)
- Labels: `PBI4`, `submission`, `test`
- Estimate: 2 SP
- Description:
  > Add `IT-RES-01` (contributor happy path) and `IT-RES-02`
  > (non-contributor / anonymous blocked).

### SUB-20 · Soft-delete own resource
- Labels: `PBI4`, `submission`, `backend`
- Estimate: 1 SP
- Description:
  > `DELETE /api/resources/mine/{id}` flips status to DELETED if and
  > only if the caller is the contributor.

## What to screenshot (only if this board genuinely existed)

If — and only if — you have a real board to screenshot:
1. **Board overview** (Appendix A.1): zoom out so all five columns +
   ~6 cards per column are visible.
2. **Sprint 2 snapshot** (Appendix A.3): filter by label `submission`
   so reviewers can see the cards I actually worked on.
3. **Single card detail** (Appendix A.4): open SUB-12, screenshot the
   description + checklist + activity feed.

## Strongly recommended honest fallback

If the team did not actually use a Trello/Jira board, rewrite 2.3
in the report as follows (this is the kind of phrasing that survives
scrutiny):

> *"Task allocation was conducted in weekly face-to-face planning
> sessions and tracked in a shared spreadsheet rather than a dedicated
> board tool such as Jira or Trello. While this kept overhead low for
> a six-person student team, it limited visibility of in-progress
> work; the spreadsheet snapshot at the end of Sprint 2 is reproduced
> in Appendix A.3. Introducing a real Kanban board is one of the
> concrete improvements identified in Section 6."*

This is **better** than a fabricated board, because:
- it gives the marker a real artefact (the spreadsheet),
- it scores rubric point #2 ("how Scrum was *actually* applied"),
- it sets up Section 6 (Conclusion) with a genuine improvement item.
