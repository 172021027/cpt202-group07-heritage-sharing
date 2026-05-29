# My Ownership — Submission Module (PBI4 + PBI5)

> Purpose: list every file in this repository that is part of "my"
> submission module, so I can (a) cite them in the report as
> Appendix B, and (b) prove individual contribution.
>
> "My" means: I wrote or substantially modified these files for PBI4
> (Save Draft + Formal Submission) and PBI5 (Copyright Declaration +
> File Validation). Files only **touched** for cross-module compatibility
> are listed in the "shared / interface" section.

## 1. Backend — primary ownership

### 1.1 Entities (data model)
- src/main/java/com/example/heritage_sharing_api/entity/Resource.java
  - The aggregate root for a submitted resource (`resourceId`,
    `contributorId`, `title`, `description`, `categoryId`, `location`,
    `picturePath`, `videoPath`, `copyrightDeclaration`, `status`,
    `submittedAt`, `approvedAt`).
- src/main/java/com/example/heritage_sharing_api/entity/ResourceStatus.java
  - Canonical state enum: `DRAFT`, `PENDING_REVIEW`, `REJECTED`,
    `APPROVED`, `ARCHIVED`, `UNPUBLISHED`, `DELETED`. The `fromDbValue`
    method gives backward-compatibility against legacy string values
    that appeared earlier in the project.
- src/main/java/com/example/heritage_sharing_api/entity/ResourceStatusConverter.java
- src/main/java/com/example/heritage_sharing_api/entity/ResourceAction.java
  - Immutable audit row written whenever the resource transitions
    state (used by the `RESUBMIT` action emitted from my service).
- src/main/java/com/example/heritage_sharing_api/entity/ResourceActionType.java

### 1.2 DTOs
- src/main/java/com/example/heritage_sharing_api/dto/SubmitResourceRequest.java
  - Payload for the submit + resubmit endpoints; includes `tags`,
    `copyrightDeclaration`, `contributorId`.
- src/main/java/com/example/heritage_sharing_api/dto/RejectedSubmissionEditResponse.java
  - Response used by the "Revise rejected resource" page so the
    contributor can pre-fill the form.
- src/main/java/com/example/heritage_sharing_api/dto/ResourceDetailResponse.java
  - Full resource view used by submission-side detail pages (carries
    `copyrightDeclaration` back to the UI).

### 1.3 Repository
- src/main/java/com/example/heritage_sharing_api/repository/ResourceRepository.java
  - JPA repository used by submitResource / resubmit.
- src/main/java/com/example/heritage_sharing_api/repository/ResourceActionRepository.java
  - Used to persist the `RESUBMIT` action row.

### 1.4 Service (the core of PBI4 + PBI5)
- src/main/java/com/example/heritage_sharing_api/service/ResourceService.java
  - Methods that are "mine":
    - `submitResource(SubmitResourceRequest, MultipartFile image, MultipartFile video)` — PBI4 main path.
    - `getRejectedSubmissionForEdit(Long contributorId, Long resourceId)` — PBI4 resubmit pre-fill.
    - `resubmitRejectedSubmission(...)` — PBI4 resubmit happy path,
      validates file presence (image + video required), writes
      audit row, resets `submittedAt`, clears `approvedAt`.
    - `validateImageFile(...)`, `validateVideoFile(...)` — PBI5 file-type
      / size guards.
    - `saveResourceTags(...)` — supporting helper used by both paths.
    - `deleteOwnResource(...)` — submission-side soft delete
      (DRAFT → DELETED).
  - Methods owned by other team members but referenced here for
    contract: `getAllResources()`, `searchResources(...)`,
    `archiveResource(...)`, `offlineResource(...)`,
    `restoreResource(...)`.

### 1.5 Controller
- src/main/java/com/example/heritage_sharing_api/controller/ResourceController.java
  - Endpoints owned by me (submission scope):
    - `POST /api/resources/submit` — PBI4 main path (multipart upload).
    - `DELETE /api/resources/mine/{id}` — contributor soft delete.
  - Endpoints owned by other team members but in the same controller
    (kept in one file to share the security helper
    `resolveCurrentUserId()`): `GET /{id}`, `GET /`, `GET /approved`,
    `GET /search`, `GET /filter`, `GET /search-and-filter`,
    `GET /list-frontend`, `GET /actions/history`, `PUT /offline/{id}`,
    `PUT /archive/{id}`, `PUT /restore/{id}`.

## 2. Frontend — primary ownership

- src/main/resources/html/submission.html
  - The submit / revise page (single page, switches by `?mode=revise`
    query string).
- src/main/resources/static/css/submission.css
- src/main/resources/static/js/submission.js
  - Notable concerns:
    - Role gating (`isContributorRole`, `lockSubmissionForm`).
    - Local draft auto-save via `localStorage`
      (`getDraftStorageKey`, `loadDraft`, `clearDraft`); the draft
      key differentiates "fresh submission" and "revise <resourceId>"
      so two drafts cannot collide.
    - Tag chip handling (`tags = Array.isArray(draft.tags) ?
      draft.tags.slice(0, 3) : []`) — enforces the 3-tag UX rule
      before the request is sent.
    - Submit flow: multipart POST → on success, clears the draft and
      redirects.

## 3. Tests — primary ownership

- src/test/java/com/example/heritage_sharing_api/integration/resource/ResourceSubmissionAndManagementIntegrationTests.java
  - `IT-RES-01` happy path,
  - `IT-RES-02` access control (non-contributor / anonymous),
  - plus the resubmit flow.
- src/test/java/com/example/heritage_sharing_api/unit/ResourceServiceTest.java
  - Unit-level coverage of `submitResource` / `resubmit` /
    validation helpers.
- src/test/java/com/example/heritage_sharing_api/unit/ResourceControllerTest.java
  - MockMvc-level coverage of `POST /api/resources/submit` and
    `DELETE /api/resources/mine/{id}`.

## 4. Shared / interface — touched but not owned

These are listed only so the report can be accurate about what
**other** team members own. Do NOT claim authorship of these in the
report:
- `entity/User.java`, `entity/UserRole.java` — owned by the auth lead.
- `entity/Category.java`, `entity/Tag.java`, `entity/ResourceTag.java`,
  `entity/ResourceTagId.java` — taxonomy module.
- `service/UserService.java#isContributor(...)` — auth module, consumed
  by my controller for role check.
- `service/ReviewService.java` — review module, consumes the
  `PENDING_REVIEW` status produced by my service (this is the
  interface contract referenced in Section 4 "Change Management").

## 5. How to use this file in the report

- In Section 2.2 ("My role"), cite this file as a whole:
  *"A complete file-level breakdown of my submission-module ownership
  is given in Appendix B.2 (see `docs/section2_ownership.md` in the
  repository)."*
- In Section 3 ("Software Design"), reuse the bullet under each of
  1.4, 1.5, 2 as the structure for the PBI4/PBI5 deep dive.
- For the GitHub evidence (Appendix C.3), run:
  ```powershell
  git log --author="<your name>" --oneline -- `
    src/main/java/com/example/heritage_sharing_api/dto/SubmitResourceRequest.java `
    src/main/java/com/example/heritage_sharing_api/service/ResourceService.java `
    src/main/java/com/example/heritage_sharing_api/controller/ResourceController.java `
    src/main/resources/static/js/submission.js `
    src/main/resources/html/submission.html
  ```
  and screenshot the result.
