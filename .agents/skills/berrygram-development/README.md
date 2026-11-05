# BerryGram Development Skill

## Українською

`berrygram-development` — це інженерний скіл для AI-агентів і розробників, які працюють
з Android-клієнтом BerryGram (неофіційний форк Telegram Android). Він описує реальну
архітектуру репозиторію: модулі, форкові класи, точки інтеграції з upstream-кодом
Telegram, графічний рушій Liquid Glass/blur, плаваючі поверхні, історії, налаштування,
збирання Release/R8 та перевірку APK.

Головна ідея — **доказовість**. Кожне твердження про поведінку має один із рівнів:
`TESTED`, `CODE VERIFIED`, `MANUAL TEST REQUIRED` або `UNTESTED`. Успішна компіляція не
вважається доказом правильної поведінки UI.

Скіл складається з компактного `SKILL.md` і тематичних довідників у `references/`, які
агент читає лише за потреби (progressive disclosure). Приклади робочих процесів — у
`examples/`.

## English

### What it is

A modular Agent Skill for engineering work on BerryGram. It is documentation, not
executable code: no scripts are bundled, and nothing is installed automatically.

It covers:

- repository architecture and fork isolation rules;
- Telegram Android internals that the fork touches;
- Liquid Glass, blur, surfaces, clipping, z-order, insets, fallbacks;
- settings UI, theming, accessibility;
- animation policy and lifecycle;
- feature development, debugging, testing, build/release, security, workflow.

### Who it is for

- AI coding agents working in a BerryGram checkout.
- Contributors who need the fork's real class/method map before editing.
- Reviewers checking that a change does not break upstream Telegram.
- Release engineers validating artifacts.

### Layout

```text
.agents/skills/berrygram-development/
├── SKILL.md
├── README.md
├── SOURCES.md
├── references/
│   ├── architecture.md
│   ├── telegram-internals.md
│   ├── android-engineering.md
│   ├── graphics-engine.md
│   ├── ui-design-system.md
│   ├── animation-system.md
│   ├── feature-development.md
│   ├── debugging.md
│   ├── testing.md
│   ├── build-and-release.md
│   ├── security.md
│   └── workflow.md
└── examples/
    ├── feature-plan.md
    ├── bug-investigation.md
    └── release-checklist.md
```

### Activation

The frontmatter `description` is the activation contract. An agent should load this skill
when the task mentions BerryGram development, the fork's classes or graphics, its build,
tests, or release. It should not activate for unrelated Android projects.

### Using it with OpenCode

Agent Skills are discovered from configured skill directories. Either:

- keep the directory inside a project that has `.agents/skills` on its skill path, or
- add the skill root to the `skills` array in `opencode.json`.

Then ask for BerryGram work normally; the agent loads `SKILL.md` and opens a reference only
when needed. To verify discovery, ask the agent to name the reference file it would read
for a graphics task — it should answer `references/graphics-engine.md`.

### Using it with Codex

Same format: a directory with `SKILL.md` plus optional `references/` and `examples/`. Place
it in a skills directory that the Codex configuration scans, or reference it from the
project instructions. Keep the directory name identical to the `name` field.

### How to read the references

Do not read all of them. Use the index in `SKILL.md`:

- new feature → `feature-development.md`, then the specific area;
- visual bug → `graphics-engine.md` and `debugging.md`;
- test work → `testing.md`;
- build/release → `build-and-release.md`;
- session start → `workflow.md`.

### Keeping it accurate

The skill documents commit `68081a6` (version `1.2.0-beta.7`), the last published commit
on branch `public-1.2.0`. The branch tip may contain unpublished work.

When a change affects a documented area:

1. Verify the symbol still exists at the public base (`git grep <Symbol> 68081a6`).
2. Update the reference in the same commit as the code.
3. Remove or mark removed symbols; do not leave stale names.
4. Re-check commands against `TESTING.md` and the Gradle files.
5. Do not document unpublished local work as public behavior.

### Adding a section

1. Create `references/<topic>.md` with a scope line and verified symbols.
2. Add a row to the reference index in `SKILL.md` with a "read when" condition.
3. Keep it focused; split rather than grow one file without limit.
4. Add an example under `examples/` only if it demonstrates a distinct workflow.
5. Verify every path and command before committing.

### Proposing changes

Open a pull request against the active public branch. Keep the diff to documentation
unless the PR explicitly covers code. Describe what changed, why, which symbols you
verified, and what remains unverified. Do not force push or rewrite shared history.

### Acceptance levels

Canonical definition is in `SKILL.md`; repeated here for contributors.

| Level | Meaning |
| --- | --- |
| `TESTED` | A named test ran and passed. |
| `CODE VERIFIED` | The code path was read and is consistent. |
| `MANUAL TEST REQUIRED` | Needs a device/emulator check. |
| `UNTESTED` | No verification. |

### License and sources

The skill is distributed with BerryGram under GPL-2.0. It was written for this project;
see `SOURCES.md` for repository documents and external methodology references. No
third-party skill text is copied.
