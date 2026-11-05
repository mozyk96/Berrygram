# Workflow and Session Recovery

Scope: how work is planned, committed, and handed off without re-analyzing the repository
each session. Use the project's existing system; do not invent a parallel one.

## Sources of truth

- `AGENTS.md` — repository rules and constraints.
- `ROADMAP.md`, `ROADMAP-1.1.md` — scope and direction.
- `CONTINUE.md` (or the repository's current state/handoff file) — verified state and next
  action. `CONTINUE.md` may be local-only; do not assume it exists in a fresh public clone.
- `plans/<release>/PLAN.md` — per-release checkpoints.
- `CHANGELOG.md` — user-visible history.
- `docs/BUILDING.md`, `docs/PUBLICATION-NOTES.md` — build and publication facts.
- `TESTING.md` — what tests exist and what they do not prove.

If two sources disagree, trust the code and the latest verified state; then fix the stale
document rather than silently ignoring it.

## Standard loop

Canonical rules are in `SKILL.md`; this expands the operating steps.

1. **Restore context** — read the state files, check `git status`, branch, and last commit.
2. **Confirm the public base** — do not assume local HEAD is public.
3. **Read only the needed skill references.**
4. **Plan** — 3–6 checkpoints with verification for each.
5. **Implement** the smallest isolated change.
6. **Test** narrowly, then widen only across touched boundaries.
7. **Review** the diff for collateral damage, secrets, and intended files only.
8. **Commit** logically with a behavior-focused message.
9. **Update** progress/handoff truthfully.
10. **Release** only with explicit authorization.

## Planning template

```md
## Goal
One sentence.

## Constraints
- Preserve upstream behavior.
- Scope boundary (no APK, no unrelated refactor).

## Checkpoints
1. [ ] Locate symbols (files/methods).
2. [ ] Change X in ForkY.
3. [ ] Hook Z in upstream file.
4. [ ] Test: <command> → expected result.
5. [ ] Manual: <scenario> → MANUAL TEST REQUIRED.

## Risks
- ...

## Evidence
- ...
```

## Commit discipline

- One logical change per commit.
- Stage explicit paths; avoid `git add .` when untracked or unrelated files exist.
- Message form: `<area>: <behavior>` plus a short body with evidence level.
- Do not commit: `build/`, `.gradle/`, APKs, keystores, `.env`, IDE files, logs, or
  temporary validation scripts unless they are intentional project files.
- Do not amend or rewrite shared history without approval.

## Handoff format

Record exactly:

```md
- Branch:
- Last commit:
- Completed:
- Incomplete:
- Changed files:
- Tests run (command → result):
- Manual gaps (MANUAL TEST REQUIRED):
- Untested:
- Next action:
```

A new session must be able to continue from this block without re-reading the repository.

## Context economy for a large Android repo

Do:

- Use targeted search for a class or method before reading files.
- Use `git diff` and `git log -p -- <path>` to see change history.
- Read file ranges, not whole directories.
- Read a reference file only when its area is touched.
- Reuse facts already established in the state files.

Do not:

- Read the whole repository.
- Run R8 or a full build after every edit.
- Repeat an unchanged test suite for reassurance.
- Run FPS benchmarks without a performance question.
- Produce long intermediate reports that are not the deliverable.

Savings must never skip a required verification. If a check is required, run it and record
it.

## Documentation maintenance

When code changes, update the affected reference in the same change:

| Code area changed | Reference to update |
| --- | --- |
| Fork classes, packages, hooks | `references/architecture.md` |
| Upstream screen integration | `references/telegram-internals.md` |
| Blur/glass/surfaces/insets | `references/graphics-engine.md` |
| Settings layout/tokens | `references/ui-design-system.md` |
| Motion | `references/animation-system.md` |
| Tests/commands | `references/testing.md` |
| Gradle/signing/release | `references/build-and-release.md` |
| Credentials/updater trust | `references/security.md` |

If a documented symbol no longer exists, remove it or mark it changed. Stale documentation
is a defect.

## Publication workflow for docs

1. Verify the public default branch and the target branch via the remote.
2. Create an isolated clean clone/worktree from the **public** commit, not from a local
   private HEAD.
3. Add only the documentation paths.
4. Verify the diff contains only allowed files.
5. Commit and push the documentation branch, then open a PR — **only after explicit owner
   authorization**. A blocked push is a stop condition, not something to bypass.
6. Do not merge, force push, delete branches, or change settings/Actions without a separate
   authorization.
