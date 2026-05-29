# Branching Strategy — Module 5 Personal Working Repository

> **Repository scope.** This GitHub repository is the *personal working
> copy* maintained by Fan Shuaifei (Student ID 2362984) for **Module 5
> — Resource Creation & Metadata Maintenance** of the CPT202 Group 07
> Heritage Sharing Platform. The full nine-person team coordinates
> through a separate shared codebase and a group chat; this repository
> exists to provide reproducible evidence for the individual report
> (Assignment 3).
>
> Because only one author contributes here, the branching model below
> is intentionally lightweight — it follows the spirit of *GitHub
> Flow* (Chacon and Straub, 2014) rather than a heavyweight Git-Flow
> with release branches.

## Branches

| Branch | Purpose | Protection |
|--------|---------|------------|
| `main` | Always-deployable snapshot. Each commit represents a known-good state of Module 5. | Direct pushes discouraged; changes arrive via merged pull requests from `develop`. |
| `develop` | Integration line where completed features accumulate before being promoted to `main`. | Receives merges from `feature/*` branches. |
| `feature/*` | Short-lived branches, one per logical task or task cluster. Examples: `feature/docs-branching-strategy`, `feature/submission-resubmit`. | Deleted after the corresponding pull request is merged. |

## Workflow

1. Pick a task from the personal task ledger
   (`docs/section2_task_tracking.md`).
2. Create a `feature/<short-slug>` branch from the current tip of
   `develop`.
3. Implement the task using **Conventional Commits** for every commit
   message (`feat`, `fix`, `docs`, `test`, `refactor`, `ci`, `chore`).
4. Push the branch and open a pull request *into `develop`*. The PR
   description references the task ID(s) from the ledger.
5. Once CI (`.github/workflows/ci.yml`) is green and the change has
   been self-reviewed, squash-merge into `develop` and delete the
   feature branch.
6. At the end of each sprint, fast-forward `main` from `develop` so
   `main` reflects the latest stable Module 5 increment.

## Why not Git-Flow?

A full Git-Flow (with `release/*` and `hotfix/*` branches) was
considered and rejected for two reasons:

- The repository has a single contributor, so the coordination
  overhead of long-running release branches yields no benefit.
- The module ships into the team's shared codebase rather than to an
  external production environment, so there are no version tags to
  stabilise.

## Honesty note

The early commits on `main` (15 commits dated 28–29 May 2026) were
made on a single branch because the branching strategy described above
was formalised only after the personal task ledger was consolidated.
From this point onwards, all new work follows the workflow above. The
historical commits are not retroactively rewritten — preserving an
accurate Git history is preferred over presenting an idealised one.

## References

- Chacon, S. and Straub, B. (2014) *Pro Git*. 2nd edn. Berkeley, CA:
  Apress.
- GitHub (2017) *Understanding the GitHub flow*. Available at:
  https://docs.github.com/en/get-started/quickstart/github-flow.
