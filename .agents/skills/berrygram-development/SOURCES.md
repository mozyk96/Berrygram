# Sources

This skill was written from first-hand inspection of the BerryGram repository and from
public methodology references. No third-party skill text was copied.

## Repository sources (commit `68081a6`, last published commit on `public-1.2.0`)

The branch tip may be ahead with unpublished work; this skill describes only `68081a6`.
`CONTINUE.md` is a local working-copy state file and is not part of that public commit.

- `AGENTS.md` — repository constraints, build rules, credential and release policy.
- `CONTINUE.md` — verified local state, publication state, next action (local working copy;
  not part of the public tree at the documented commit).
- `ROADMAP.md`, `ROADMAP-1.1.md` — scope and direction.
- `TESTING.md` — real test commands and their limits.
- `docs/BUILDING.md` — toolchain, variants, signing, CI secrets and their caveats.
- `docs/PUBLICATION-NOTES.md`, `docs/MODERNIZATION-AUDIT.md`, `docs/UPSTREAM-README.md`.
- `README.md`, `CHANGELOG.md` — product status and history.
- `ARCHITECTURE.md`, `FORK-DESIGN.md`, `FORK-SPEC.md`, `ARCHIVE-DESIGN.md`,
  `BERRYGRAM-SPEC.md`, `BERRYGRAM-1.0-SPEC.md`, `FEATURE_AUDIT_1.1.4.md`.
- `gradle.properties`, `settings.gradle`, `TMessagesProj_App/build.gradle`,
  `TMessagesProj/build.gradle`, `TMessagesProj/proguard-rules.pro`.
- Fork source: `TMessagesProj/src/main/java/org/telegram/ui/Fork*.java` and
  `TMessagesProj/src/main/java/org/telegram/messenger/fork/**`.
- Upstream integration hooks read in `DialogsActivity.java`, `ChatActivity.java`,
  `MainTabsActivity.java`, `ActionBar/ActionBarMenuItem.java`,
  `ActionBar/ActionBarPopupWindow.java`, `Components/ItemOptions.java`.
- Graphics runtime: `TMessagesProj/src/main/java/org/telegram/ui/Components/blur3/**`.
- Tests: `fork-tests/*.java`, `fork-tests/*.ps1`, `fork-tests/*.py`, `fork-tests/*.js`.
- CI: `.github/workflows/berrygram-smoke.yml`.
- Plans: `plans/1.2.0-beta6/PLAN.md`, `plans/1.2.0-beta7/PLAN.md`.
- License: repository `LICENSE` (GNU GPL v2).

## Locally installed skills that informed the structure

These were read for engineering methodology only. Their text is not reproduced.

- `berrygram-maintainer` — project-owned maintainer skill (this repository's workflow,
  release and security expectations). Complementary to this skill; this skill adds the
  architecture, graphics and testing detail.
- `systematic-debugging`, `test-driven-development`, `verification-before-completion`,
  `requesting-code-review`, `receiving-code-review` — locally installed workflow skills,
  part of the Superpowers framework by Jesse Vincent (obra/superpowers, MIT License).
  Used as methodology inspiration for the investigation loop and evidence discipline.
- `android-benchmark-comparison`, `figma-implement-design`, `playwright`,
  `security-best-practices`, `screenshot`, `prompt-master`, `supabase` — reviewed for
  relevant Android/UI/security practices; not applicable to this project's core work and
  not copied.

## Public references

- Agent Skills specification — <https://agentskills.io/specification> (directory layout,
  `SKILL.md` frontmatter, progressive disclosure). Used to validate the structure.
- Superpowers — <https://github.com/obra/superpowers> (MIT License). Methodology reference
  only.

## Deliberately not used

- Third-party Telegram fork source or assets — license and provenance risk; the repository
  itself is the only code authority.
- Unverified blog posts, AI-generated "best practice" collections, and skill marketplaces
  with unclear authorship.
- Any source that recommends bypassing release signing, updater verification, or
  repository security rules.

## Verification note

Every class, method, preference key, path, and command in `references/` was checked against
the public commit named above with `git grep` / `git show`. Where behavior could not be
confirmed without a device, the text says `MANUAL TEST REQUIRED` rather than asserting it.
