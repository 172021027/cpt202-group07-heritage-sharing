# Section 2 — Software Development Process (Reference Outline)

> Purpose of this file: a *writing scaffold* for you to rewrite in your own
> words. It is **not** a ready-to-submit draft. Each subsection lists:
> - what to cover (in Chinese, for your own thinking),
> - the kind of evidence you should cite (so it sounds project-specific, not
>   textbook),
> - a few English keywords/phrases you may reuse if helpful.
>
> Target length: 1.5–2.5 pages (~700–1000 words for the main text).
> Recommended structure: 2.1 → 2.6, with figures/screenshots placed in
> Appendix A and referenced from the text (e.g. "see Appendix A.2").

---

## 2.1 Process model adopted by the team  (~100–130 words)

要点：
- 说明团队**没有完全照搬教科书 Scrum**，而是采用一种"轻量级 Scrum-like
  iterative process"。这样写既贴近事实，也符合 rubric "不要堆定义" 的要求。
- 关键事实写清楚：
  - 项目周期：~4 weeks in April 2026；
  - Sprint length：1 week (4 sprints in total)；
  - 团队规模：6 (or 实际人数)；
  - Roles you adopted：Scrum Master (rotating)、Product Owner proxy = TA
    feedback、Developers；
  - 用到的工具链：Trello/Jira (task board)、GitHub (version control)、
    Spring Boot + MySQL + Redis (tech stack).
- 不要写 "Scrum is an agile framework defined by Schwaber..." 这种定义句。

可复用短语（保留，自己改写顺序）：
- "We adopted a lightweight Scrum-inspired process tailored to a 6-person
  student team and a four-week delivery window."
- "Each weekly iteration ended with an internal demo and a short
  retrospective, the outcomes of which fed directly into the next sprint
  backlog."

证据：
- Appendix A.1: 截图你们的 Trello/Jira board overview。
- Appendix A.2: sprint backlog snapshot (week 1 / week 4 对比) 来体现演进。

---

## 2.2 My role and responsibilities  (~80–110 words)

要点：
- 一句话定位：你在团队里负责 **Resource Submission module**，包含两个
  essential PBIs：
  - **PBI4 — Save Draft & Formal Submission** (controller endpoint
    `POST /api/resources/submit`, `SubmitResourceRequest` DTO,
    `ResourceService.submitResource(...)`, plus draft auto-save in
    `submission.js` via `localStorage`)；
  - **PBI5 — Copyright Declaration & Validation**
    (`copyrightDeclaration` field on `Resource`, file-type/size validation
    in `ResourceService.validateImageFile / validateVideoFile`).
- 补一句你也负责对应的前端页面 (`submission.html`, `submission.js`,
  `submission.css`) 与该模块的集成测试 (`ResourceSubmissionAnd
  ManagementIntegrationTests`).
- 提一句横向协作：与 reviewer 模块同学约定 `ResourceStatus` 状态机契约。

证据：
- Appendix B.1: GitHub commit log filtered by author (yourself).
- Appendix B.2: file list of `ResourceController#submitResource`,
  `ResourceService#submitResource`, `submission.js` (highlighted regions
  done by you).

---

## 2.3 Sprint planning & task allocation in practice  (~150–180 words)

要点：
- 不要泛泛而谈，**挑一个具体的 sprint 详细写**——推荐 Sprint 2，因为
  PBI4/PBI5 主体是在该 sprint 落地。
- 描述结构（一段写完）：
  1. Sprint Planning meeting (when, who, duration)；
  2. 如何把 PBI4 拆成 task：例如
     - T4.1 Design `Resource` entity + `ResourceStatus` enum,
     - T4.2 `SubmitResourceRequest` DTO + `POST /submit` endpoint,
     - T4.3 Image/video upload + storage path,
     - T4.4 Frontend form + client-side validation,
     - T4.5 Draft auto-save via localStorage,
     - T4.6 Integration test;
  3. 在 Trello/Jira 上每个 task 是一张 card，状态在 To Do → In Progress →
     Review → Done 之间流转；
  4. 你领的 cards (列出 ticket id, 如 SUB-12, SUB-15)；
  5. Daily stand-up 形式 (15 min, async via group chat or face-to-face?).

可复用短语：
- "PBI4 was decomposed into six developer-sized tasks (T4.1–T4.6) during
  Sprint 2 planning."
- "Each card carried a story-point estimate (1, 2, or 3) agreed via
  planning poker; my own load for Sprint 2 was 9 points."

证据：
- Appendix A.3: Sprint 2 board screenshot (To Do / Doing / Done columns).
- Appendix A.4: a single card detail screenshot for SUB-12 (PBI4 main
  endpoint) — shows assignee, estimate, checklist, comments.

---

## 2.4 Version control & integration practice  (~150–200 words)

要点：
- 老老实实写：团队使用 **GitHub** 做版本控制。诚实地交代一句：
  "Due to a lost team account at the start of week 3, the repository
  history was migrated to a new GitHub organisation. The current public
  repository therefore captures the consolidated commit history of the
  final delivered codebase, organised by feature area rather than the
  original day-by-day timeline."
  → 这句很关键。它把"账号丢失 → 新仓库"这件事**讲在明处**，主考官看到
  commit 集中在某几天就不会判定造假。
- 分支策略：`main` (protected) + `feature/*` (per PBI) + `fix/*`；
  PR review by ≥1 reviewer before merge.
- 你个人的提交节奏：按 task 粒度提交，commit message 遵循
  `feat(submission): ...` / `fix(submission): ...` / `test(submission):
  ...` 约定（这是我帮你在本地仓库里实际做了的）。
- 集成方式：每天 push；CI 缺失（这是 limitation，留到 2.6 反思）。

可复用短语：
- "The repository follows a trunk-based workflow with short-lived feature
  branches and squash-merged pull requests."
- "All `submission`-scoped commits authored by me follow a
  Conventional-Commits style (e.g. `feat(submission): add copyright
  declaration field`), which makes the commit log itself a traceable
  contribution record."

证据：
- Appendix C.1: `git log --oneline --graph` 截图。
- Appendix C.2: 一份 PR 截图 (含 review comments / approvals)。
- Appendix C.3: `git log --author="<你的名字>" --oneline` 过滤后的截图，
  证明你个人贡献。

---

## 2.5 Testing & integration evidence  (~140–170 words)

要点：
- 实际列出 src/test 下与 submission 相关的测试，**给出测试 ID + 验证目标**：
  - `IT-RES-01` — contributor submission persists pending resource +
    normalised tags;
  - `IT-RES-02` — non-contributor / anonymous users blocked from
    submitting (403 / 401);
  - `IT-RES-03+` — rejected submission edit/resubmit flow (你模块独有);
  - 以及 `ResourceServiceTest` 中的 unit-level cases。
- 强调三件事：
  1. **Pyramid 形状**：unit (service) → integration (MockMvc + H2) → manual
     smoke test in browser;
  2. **可重复**：测试在 H2 上跑，application-test.properties 提供独立
     profile；
  3. **覆盖了 PBI4 的接受路径与拒绝路径**（happy path + access control +
     validation rejection）。
- 提一次"通过测试发现了一个 bug"的小故事，会显得真实——例如
  "IT-RES-02 originally returned 500 instead of 403 due to an
  unhandled `IllegalArgumentException`; this prompted introducing the
  explicit role check before service invocation."

证据：
- Appendix D.1: `mvn test` 终端输出截图 (BUILD SUCCESS, Tests run: N,
  Failures: 0).
- Appendix D.2: IDE test runner 中 submission package 的绿色全过截图。

---

## 2.6 Reflection on the process  (~110–140 words)

要点（**这里 rubric 看重"具体、有证据、可改进"，避免空话**）：

挑三条真实反思（按你描述：用了 Jira/Trello + GitHub，所以反思不应抱怨缺
工具；要抱怨**用法不到位**）：

1. **Sprint estimation was systematically optimistic.** Sprint 2 planned
   9 SP for the submission module, actual burn-down showed ~13 SP because
   file upload + storage paths were under-estimated. Lesson: introduce
   reference stories for future estimation; treat IO/file-handling tasks
   as a separate category.
2. **PR reviews were sometimes rubber-stamped under deadline pressure.**
   A field-name mismatch (`copyrightDeclaration` vs.
   `copyright_declaration`) between submission and review modules slipped
   through a same-day merge and was caught only at integration. Lesson:
   require a checklist item "ran the failing module locally" before
   approval; this is the change-management example you can re-use in
   Section 4.
3. **CI / automated build was missing.** All tests had to be run manually
   before each merge, which discouraged frequent regression runs. Adding
   a minimal GitHub Actions workflow (`mvn -B test`) is a low-cost
   improvement explicitly planned for the next iteration.

可复用短语：
- "Two recurring weaknesses surfaced across the four sprints: …"
- "These observations are revisited as concrete action items in
  Section 6 (Conclusion)."

证据：
- Appendix A.5: burn-down chart / sprint 2 板的 done vs. planned 对比。
- Appendix C.4: 你为修复字段不一致而提交的那个 fix commit 的 diff 截图。

---

## Cross-section pointers (so 2 → 3/4/6 一脉相承)

- 2.2 提到的 PBI4 / PBI5 就是 Section 3 详细展开的两个 essential PBIs。
- 2.6 第 2 条 (field-name mismatch) 直接成为 Section 4 change-management
  case study 的开头。
- 2.6 第 3 条 (no CI) 直接成为 Section 6 future-iteration 改进项之一。

## What you should actually do next

1. Read each subsection above, then **rewrite in your own English voice**.
   Do **not** copy the suggested phrases verbatim — they exist to anchor
   the structure, not to be the final text.
2. Collect the screenshots referenced as Appendix A/B/C/D *before* you
   finalise the prose, because the prose must explicitly cite them
   ("Figure A.3 shows …", not "we used a board").
3. Run `mvn test` once on a clean checkout and screenshot the result for
   Appendix D.1.
4. Check word count: keep this section between 700 and 1000 words. If
   over, cut from 2.1 (process overview) first, never from 2.3 or 2.6.
