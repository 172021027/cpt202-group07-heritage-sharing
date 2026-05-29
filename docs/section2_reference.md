# Section 2 — Software Development Process (Reference Outline, v2)

> **What this file is.** A *writing scaffold* for you to rewrite in your
> own English voice. It is **not** a draft to copy. Each subsection
> lists: (a) what to cover, (b) which evidence to cite, (c) optional
> short English phrases you may *re-word* if helpful.
>
> Target length: 1.5–2.5 pages ≈ 700–1000 words for the main text.
> Reflects the corrected scope: team of 9, my responsibility = Module 5
> (Resource Creation & Metadata Maintenance), my two essential PBIs =
> PBI4 (Save Draft + Formal Submission) and PBI5 (Copyright Declaration
> + Validation).

---

## 2.1 Process model adopted by the team  (~110–140 words)

What to cover:
- We followed a **lightweight Scrum-inspired iterative process**, not
  textbook Scrum — say this in one sentence.
- Hard facts to state:
  - team size: 9;
  - delivery window: roughly 4 weeks in April 2026;
  - sprint length: 1 week (4 sprints);
  - roles: rotating Scrum Master, TA acting as Product Owner proxy via
    weekly demos, the remaining 7 split across 9 functional modules
    (see Module 1–9 list in Appendix B.1);
  - tools: Jira / Trello for task tracking, GitHub for version
    control, Spring Boot + MySQL + Redis as the stack.
- Avoid pure textbook definitions ("Scrum is an agile framework
  defined by …") — the rubric rewards "how it was actually applied".

Sample phrase (re-word in your own English):
- *"We adopted a lightweight Scrum-inspired process tailored to a
  nine-person team and a four-week delivery window: weekly sprints,
  weekly demos to the supervising TA, and a single shared Jira
  board."*

Evidence to cite:
- Appendix A.1: Jira board overview screenshot.
- Appendix A.2: module split table (Modules 1–9 with owners).

---

## 2.2 My role and responsibilities  (~90–120 words)

What to cover:
- One sentence: I owned **Module 5 — Resource Creation & Metadata
  Maintenance** (PBIs 4 & 5 in Section 3).
- Concretely list what was in scope:
  - draft creation and metadata entry (title, category, location,
    description),
  - tag / keyword chip UX,
  - copyright declaration entry and server-side non-blank validation,
  - the "revise rejected resource" flow back to PENDING_REVIEW.
- One sentence on collaboration: Module 6 (file upload + submit
  action) shared `ResourceController#submitResource` and
  `ResourceService#submitResource` with me; we agreed on a clear
  contract (Module 5 sets metadata fields, Module 6 sets file paths)
  to keep the merge surface small.

Sample phrase:
- *"My responsibility was Module 5 (Resource Creation & Metadata
  Maintenance), encompassing draft modelling, metadata entry, tag
  input and the copyright declaration end-to-end. Module 6 (file
  upload + the actual submit action) belonged to another team
  member, and we co-authored the two shared methods named below."*

Evidence to cite:
- Appendix B.2: file-by-file ownership map (`docs/section2_ownership.md`).
- Appendix C.3: `git log --author="<you>"` filtered to my files.

---

## 2.3 Sprint planning & task allocation in practice  (~160–190 words)

What to cover:
- Pick **one specific sprint** (recommend Sprint 2, the sprint where
  PBI4 & PBI5 were delivered) and walk the marker through it.
- Structure of the paragraph:
  1. Sprint Planning meeting (when, who, duration);
  2. How PBI4 was decomposed into developer-sized tasks
     (T4.1 entity & status enum; T4.2 `SubmitResourceRequest` DTO;
     T4.3 metadata controller binding; T4.4 form HTML/CSS;
     T4.5 draft auto-save; T4.6 integration test);
  3. How the cards moved across the Jira columns
     (To Do → In Progress → In Review → Done);
  4. The cards I owned in Sprint 2 (cite their Jira IDs once you
     have the screenshot, e.g. SUB-12, SUB-15, SUB-17);
  5. Daily stand-up format (length, channel).

Sample phrases:
- *"PBI4 was decomposed into six developer-sized tasks (T4.1–T4.6)
  during Sprint 2 planning, each entered as a Jira card under the
  `submission` label."*
- *"My personal load for Sprint 2 was 9 story points across cards
  SUB-12, SUB-15 and SUB-17 (see Figure A.3)."*

Evidence to cite:
- Appendix A.3: Sprint 2 board screenshot, filtered by label
  `submission`.
- Appendix A.4: single-card detail screenshot (open one of your
  cards: description + checklist + activity feed).

---

## 2.4 Version control & integration practice  (~150–200 words)

What to cover:
- State the workflow clearly: trunk-based, `main` protected,
  short-lived `feature/*` branches per PBI, squash-merged via Pull
  Request with ≥1 reviewer.
- **Account-loss disclosure.** Recommended honest phrasing:
  > *"The team's original GitHub repository became inaccessible at the
  > end of the project due to an account issue, so the codebase was
  > re-pushed to a new repository for archival. The current
  > repository therefore preserves the final delivered code and the
  > commit messages, organised by feature area, even though the
  > original day-by-day timeline could not be carried over."*
  - This is a clean, factual statement; do **not** claim that the
    current commit timestamps are the original development dates.
- Your personal commit discipline: Conventional Commits (`feat(submission):
  …`, `fix(submission): …`, `test(submission): …`).
- Integration practice: every PR rebased on `main` before merge;
  manual `mvn test` run before approval (no CI was configured —
  flagged as an improvement in §2.6 and §6).

Sample phrase:
- *"All `submission`-scoped commits authored by me follow a
  Conventional-Commits style, which makes the commit log itself a
  traceable contribution record (Appendix C.3)."*

Evidence to cite:
- Appendix C.1: `git log --oneline --graph --decorate --all` screenshot.
- Appendix C.2: GitHub repository main page (commit list + branches).
- Appendix C.3: `git log --author="<you>"` screenshot.

---

## 2.5 Testing & integration evidence  (~140–170 words)

What to cover:
- List concrete submission-scoped tests with IDs and what they verify:
  - `IT-RES-01` — contributor happy path persists `PENDING_REVIEW` +
    normalises duplicate tags;
  - `IT-RES-02` — non-contributor returns 403, anonymous returns
    401/403;
  - Resubmit-flow tests on the metadata-edit path;
  - Unit cases in `ResourceServiceTest` for tag de-duplication and
    copyright rejection.
- Three claims worth making:
  1. **Pyramid shape**: unit (service) → integration (MockMvc + H2)
     → manual browser smoke;
  2. **Reproducible**: tests run on H2 via the `application-test.properties`
     profile, no MySQL needed;
  3. **Both happy and failure paths covered** (access control +
     validation rejection).
- A small concrete story strengthens this section, e.g.:
  *"While writing `IT-RES-02`, I discovered the endpoint returned
  500 instead of 403 for non-contributors because the role check sat
  inside a generic `try/catch (Exception)` block. The bug was fixed
  by promoting the role check above the try-block — the test now
  fails fast and the API contract matches the spec."*

Evidence to cite:
- Appendix D.1: terminal screenshot of `mvnw test` showing
  `BUILD SUCCESS` + "Tests run: N, Failures: 0".
- Appendix D.2: IDE test-runner screenshot, all green for the
  submission package.

---

## 2.6 Reflection on the process  (~110–140 words)

What to cover — three real, specific reflections (do **not** invent
abstract complaints):
1. **Sprint estimation was systematically optimistic.** Sprint 2
   planned 9 SP for Module 5, actual burn-down was ~13 SP — IO
   handling and revise-mode UX were under-estimated. *Action:*
   introduce reference stories; treat IO tasks as a separate
   estimation category.
2. **PR reviews were occasionally rubber-stamped under deadline
   pressure.** A field-name mismatch (`copyrightDeclaration` vs.
   snake-case in an early review payload) slipped through and was
   caught only at integration. *Action:* require the reviewer to
   run the failing module locally before approval. This same
   incident is reused as the change-management example in Section 4.
3. **No CI / automated build.** All tests had to be run manually
   before merge, which discouraged frequent regression runs. *Action:*
   add a minimal GitHub Actions workflow (`mvn -B test`) — listed as
   a concrete improvement in Section 6.

Sample phrase:
- *"Three recurring weaknesses surfaced across the four sprints:
  optimistic estimation, lightweight code review under deadline
  pressure, and the absence of automated CI."*

Evidence to cite:
- Appendix A.5: Sprint 2 done-vs-planned comparison (Jira burn-down
  or the Sprint 2 column).
- Appendix C.4: diff of the field-name fix commit.

---

## Cross-section consistency

- 2.2 names PBI4 + PBI5 — Section 3 details those same two PBIs.
- 2.6 reflection #2 (field-name mismatch) → Section 4
  Change-Management case study.
- 2.6 reflection #3 (no CI) → Section 6 Conclusion improvement list.

## What to do next (in order)

1. Read every subsection above and **rewrite in your own English**.
   Do not copy the sample phrases verbatim.
2. Collect Appendix A/B/C/D screenshots *before* finalising the prose
   so the prose can cite specific figure numbers.
3. Run `mvnw test` on a clean checkout, screenshot the result for
   Appendix D.1.
4. Word-count check: 700–1000 words. If over, cut from 2.1 first,
   never from 2.3 or 2.6.
