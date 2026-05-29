# My Ownership — Module 5: Resource Creation & Metadata Maintenance

> **Scope.** Per the team's 9-person module split, I own **Module 5
> (资源创建与元数据维护 / Resource Creation & Metadata Maintenance)**,
> which maps to part of Project Option A "B. Resource Submission &
> Management". My responsibility covers:
>
> - drafting a resource (contributor-facing draft model & UX),
> - filling metadata (title, category, location, description),
> - tag / keyword input and persistence,
> - **copyright declaration entry**.
>
> The adjacent **Module 6** (file/image upload + the "submit to
> pending-review" action) is owned by another team member. Both modules
> share the same `Resource` entity and the same `POST /api/resources/submit`
> endpoint, so several files below are jointly touched — the lists flag
> which parts are mine vs. collaborative.
>
> The two essential PBIs I detail in Section 3 of the report:
> - **PBI4 — Save Draft & Formal Submission** (metadata-side half of
>   the submit flow, plus the resubmit-after-rejection flow),
> - **PBI5 — Copyright Declaration & Validation**
>   (`copyrightDeclaration` field end-to-end + the server-side
>   non-blank check).

---

## 1. Backend files

### 1.1 Entities — primarily mine
- `entity/Resource.java` — aggregate root. Metadata columns
  (`title`, `description`, `categoryId`, `location`,
  `copyrightDeclaration`) and the `submittedAt`/`approvedAt` timeline
  are mine. `picturePath` / `videoPath` are populated by Module 6 but
  live on the row I designed.
- `entity/ResourceStatus.java` — `DRAFT` is mine (Module 5);
  `PENDING_REVIEW` onwards is touched by Modules 6/7.
- `entity/ResourceStatusConverter.java` — mine.
- `entity/ResourceAction.java`, `entity/ResourceActionType.java` —
  shared audit row; the `RESUBMIT` action type and its emit-point are
  mine.

### 1.2 DTOs — mine
- `dto/SubmitResourceRequest.java` — every metadata input field
  (`title`, `location`, `categoryId`, `description`, `tags`,
  `copyrightDeclaration`, `contributorId`).
- `dto/RejectedSubmissionEditResponse.java` — pre-fill payload for
  the "revise rejected resource" page.

### 1.3 Repository — shared
- `repository/ResourceRepository.java` (shared with Modules 6/7/8/9).
- `repository/ResourceActionRepository.java` (shared with Module 7).

### 1.4 Service — split inside one file
`service/ResourceService.java` hosts methods from several modules.
The methods that are **mine** (Module 5):
- `submitResource(...)` — *collaborative*: metadata mapping
  (`setTitle`, `setLocation`, `setCategoryId`, `setDescription`,
  `setCopyrightDeclaration`, `saveResourceTags`) is mine; the file
  save calls (`saveFile(image, ...)`, `saveFile(video, ...)`) are
  Module 6.
- `getRejectedSubmissionForEdit(...)` — mine.
- `resubmitRejectedSubmission(...)` — *collaborative*: metadata-edit
  half mine, file-replacement half Module 6.
- `saveResourceTags(...)` — mine.
- `toRejectedSubmissionEditResponse(...)` — mine.

Module 6 (not mine): `validateImageFile(...)`,
`validateVideoFile(...)`, `saveFile(...)`.

### 1.5 Controller — collaborative
`controller/ResourceController.java`:
- `POST /api/resources/submit` — the endpoint physically accepts
  multipart files (Module 6), but every metadata parameter binding
  and the DTO construction below is mine.
- `DELETE /api/resources/mine/{id}` — soft-delete a draft I own.

Other endpoints in the same file belong to Modules 7/8/9 (search,
filter, listing, action history, offline / archive / restore).

## 2. Frontend — primarily mine (metadata surface)

- `resources/html/submission.html` — metadata form (title, category,
  location, description, tags, copyright declaration). File inputs
  for image/video live on the same page but are Module 6.
- `resources/static/css/submission.css` — mine.
- `resources/static/js/submission.js`:
  - mine: tag chip input (`tags = ...slice(0, 3)`), draft auto-save
    (`getDraftStorageKey`, `loadDraft`, `clearDraft`), role gating
    (`isContributorRole`, `lockSubmissionForm`), revise-mode UX
    (`setReviseModeUi`), pre-fill from `RejectedSubmissionEditResponse`.
  - Module 6: multipart construction for the file blobs and upload
    progress handling.

## 3. Tests — mine (Module 5 surface)
- `integration/resource/ResourceSubmissionAndManagementIntegrationTests.java`
  - `IT-RES-01` happy path (metadata + tags + copyright);
  - `IT-RES-02` access control (non-contributor / anonymous);
  - resubmit-flow tests on the metadata-edit path.
- `unit/ResourceServiceTest.java` — metadata mapping, tag
  de-duplication, copyright rejection.
- `unit/ResourceControllerTest.java` — MockMvc on
  `POST /api/resources/submit` (parameter binding, role check) and
  `DELETE /api/resources/mine/{id}`.

## 4. NOT mine — name the right owner in the report

- Modules 1–4 (auth / login / profile / contributor approval):
  `entity/User.java`, `entity/UserRole.java`, `security/*`,
  `service/UserService.java`, `service/EmailVerificationService.java`,
  `controller/AuthController.java`, `controller/UserController.java`,
  `controller/AdminContributorRequestController.java`.
- Module 6 (file upload / submit action):
  `validateImageFile`, `validateVideoFile`, `saveFile` in
  `ResourceService.java`; multipart param handling in
  `ResourceController#submitResource`.
- Module 7 (review): `controller/ReviewController.java`,
  `service/ReviewService.java`, all DTOs under `dto/admin/`.
- Module 8 (browse / search / comments):
  `controller/ResourceCommentController.java`,
  `service/ResourceCommentService.java`,
  `entity/ResourceComment.java`, `dto/ResourceComment*.java`,
  `dto/PublicResourceDto.java`, search/filter endpoints in
  `ResourceController`.
- Module 9 (taxonomy + archive/restore):
  `controller/CategoryController.java`,
  `controller/TagController.java`,
  `controller/TaxonomyController.java`,
  `service/CategoryService.java`, `service/TagService.java`,
  `entity/Category.java`, `entity/Tag.java`,
  `entity/ResourceTag.java`, `entity/ResourceTagId.java`,
  `entity/ContributorRequestStatus*`, archive/offline/restore
  endpoints in `ResourceController`.

## 5. Citing this in the report

- **Section 2.2** ("My role"): name the module exactly as
  *"Module 5 — Resource Creation & Metadata Maintenance"*, team size 9.
  Refer the reader to *Appendix B.2 (`docs/section2_ownership.md`)*
  for the file-by-file map.
- **Section 3** ("Software Design"): keep PBI4 and PBI5 as your two
  detailed PBIs, but in the opening paragraph of each, clearly mark
  which methods/files are yours vs. shared with Module 6. Honesty
  about scope **strengthens** the mark — the rubric asks for PBIs
  *to which you have made the main and evidenced contribution*.
- **Appendix C.3** — author-filtered git log:
  ```powershell
  git log --author="<your name>" --oneline -- `
    src/main/java/com/example/heritage_sharing_api/entity/Resource.java `
    src/main/java/com/example/heritage_sharing_api/entity/ResourceStatus.java `
    src/main/java/com/example/heritage_sharing_api/dto/SubmitResourceRequest.java `
    src/main/java/com/example/heritage_sharing_api/dto/RejectedSubmissionEditResponse.java `
    src/main/java/com/example/heritage_sharing_api/service/ResourceService.java `
    src/main/java/com/example/heritage_sharing_api/controller/ResourceController.java `
    src/main/resources/html/submission.html `
    src/main/resources/static/css/submission.css `
    src/main/resources/static/js/submission.js
  ```
