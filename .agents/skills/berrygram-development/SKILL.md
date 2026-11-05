---
name: berrygram-development
description: Use when developing, debugging, testing, reviewing, or releasing the BerryGram Android Telegram fork. Covers Telegram upstream architecture, BerryGram fork isolation, Liquid Glass/blur graphics, floating headers, stories, mini dock, custom phrases, camera settings, update center, Gradle/R8 release engineering, and evidence-based verification. Do not use for unrelated Android apps or for general Telegram client questions with no fork component.
license: GPL-2.0
metadata:
  version: "1.0"
  scope: berrygram-android-fork
---

# BerryGram Development

Engineering skill for the BerryGram Android client: an unofficial fork of Telegram
Android that keeps upstream messaging behavior and adds isolated, theme-aware
fork features.

## What this skill is for

- Implementing or modifying a BerryGram feature without breaking upstream Telegram.
- Understanding where a fork feature lives and which upstream screen it touches.
- Diagnosing graphics, layout, inset, animation, and lifecycle regressions.
- Writing targeted JVM tests and running the repository's real verification scripts.
- Preparing a Release/R8 build and validating an APK before publication.
- Handing work to another session with truthful evidence levels.

## When it activates

Activate for work inside a BerryGram checkout, or for a question about BerryGram
architecture, fork classes, build variants, release identity, or acceptance status.
Do not activate for generic Android advice or for the official Telegram client.

## Non-negotiable rules

1. **Preserve upstream behavior.** BerryGram is a fork. A change that fixes a fork
   feature but breaks ordinary Telegram chat, search, stories, archive, accounts, or
   authorization is a regression, not a fix.
2. **Isolate fork code.** Prefer `org.telegram.ui.Fork*`, `org.telegram.messenger.fork.*`,
   and narrow hooks in upstream files. Do not refactor upstream classes for style.
3. **Never fake a feature.** A settings switch must change observable behavior. A class
   that compiles but is never called is not implemented.
4. **Compilation is not verification.** Use the acceptance levels below and state the
   level for every claim.
5. **Respect build identity.** Keep minSdk 23, application ID, signing identity, and
   versioning unless the task explicitly changes them. Never print or commit secrets,
   keystores, API hashes, or service JSON.
6. **Publish only what was authorized.** A docs task publishes docs. It does not
   publish an APK, an unpublished Beta branch, or unfinished code.
7. **Do not touch a physical device without an explicit instruction.** Use an emulator
   or another authorized environment.

## Acceptance levels

Every behavioral claim must carry one of these:

| Level | Meaning |
| --- | --- |
| `TESTED` | A named automated test executed and passed in this session. |
| `CODE VERIFIED` | The code path and call sites were read and are consistent; no runtime test. |
| `MANUAL TEST REQUIRED` | Needs a device/emulator check: rendering, touch, insets, installer, GPU. |
| `UNTESTED` | No verification performed. Never present as complete. |

## Working order

1. Restore context: read `AGENTS.md`, the state/handoff file (`CONTINUE.md` or equivalent),
   `ROADMAP.md`, and the active plan. Check `git status`, branch, and last commit.
2. Confirm which **public base** the work targets. Do not assume local HEAD is public.
3. Read only the references needed for the task (table below).
4. Find the real symbol with targeted search before proposing an edit.
5. Write a short plan with verification steps and expected evidence level.
6. Implement the smallest isolated change.
7. Run the narrowest relevant test, then widen only if the change crosses boundaries.
8. Review the diff for upstream collateral damage, secrets, and accidental files.
9. Commit logically and update the handoff state truthfully.
10. Release only with explicit authorization.

## Reference index

Read on demand; do not load everything.

| File | Read when |
| --- | --- |
| `references/architecture.md` | Locating modules, initialization, accounts, storage, navigation, fork package map. |
| `references/telegram-internals.md` | Touching `DialogsActivity`, `ChatActivity`, `ActionBar`, stories, themes, multi-account. |
| `references/android-engineering.md` | Lifecycle, insets, Views, Canvas, RenderEffect, JNI/native, compatibility. |
| `references/graphics-engine.md` | Blur, Liquid Glass, surfaces, clipping, z-order, fallbacks, popups. |
| `references/ui-design-system.md` | Settings screens, cards, colors, radius, typography, accessibility. |
| `references/animation-system.md` | Popup/surface animations, transitions, interruption, reduced-motion behavior. |
| `references/feature-development.md` | Adding a feature end to end with isolation and acceptance criteria. |
| `references/debugging.md` | Investigating a real bug: disappearance after search, wrong heights, invisible stories, clipped archive. |
| `references/testing.md` | Choosing tests, real commands, regression matrix, evidence recording. |
| `references/build-and-release.md` | Gradle variants, R8, signing, APK verification, SHA-256, release safety. |
| `references/security.md` | Credentials, updater trust, input handling, logging, licensing. |
| `references/workflow.md` | Session recovery, plans, commits, handoff, context economy. |

Examples:

- `examples/feature-plan.md` — turning a feature request into an isolated, testable plan.
- `examples/bug-investigation.md` — a trace-first investigation template with real hooks.
- `examples/release-checklist.md` — pre-publication gate list.

## Core architecture in one paragraph

`TMessagesProj` holds the Android app, UI, JNI, and Telegram core; `TMessagesProj_App*`
hold installable variants; `TMessagesProj_Modules/media` (and other submodules) provide
native/media dependencies. BerryGram fork code lives under
`TMessagesProj/src/main/java/org/telegram/ui/Fork*.java`,
`org/telegram/ui/ForkSettings*.java`, and `org/telegram/messenger/fork/**`
(`auth`, `panels`, `text`, `camera`, `voice`, `updater`). Integration into upstream
screens is deliberately narrow: a handful of hook lines in `DialogsActivity`,
`ChatActivity`, `ActionBar*`, `ItemOptions`, and `MainTabsActivity`.

## Done criteria

A task is complete only when:

- The change is isolated and upstream behavior is preserved.
- The relevant reference-level constraint was checked (graphics, insets, lifecycle, security).
- A named test ran, or the limitation is stated as `MANUAL TEST REQUIRED`/`UNTESTED`.
- The diff contains only intended files, with no secrets or generated artifacts.
- The handoff records branch, commit, changed files, test results, gaps, and next action.
