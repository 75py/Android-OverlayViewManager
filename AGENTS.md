# Repository Guidelines

## 3.0.0 Preparation and Coordination

- Read `V3_0_0_PLAN.md` before working on 3.0.0. The user explicitly started coordinated work on 2026-09-12 JST; follow its ownership, review, and validation gates.
- Preparation commit `1646b3b` is shared on `work/3.0.0`. Subsequent changes must use task branches and reciprocal reviewed PRs; the final scope ends at the release PR to main.
- Coordinators: Codex uses Astra (`gpt-6-astra`) / medium; Claude uses Fable 5.1 / high. Verify actual launcher identifiers before starting agents.
- Coordinators choose subagent models and effort for each task from Luna (`gpt-5.6-luna`), Terra (`gpt-5.6-terra`), or Sonnet 5. This supersedes earlier subagent selection guidance for this work. GPT-5.5 and Haiku are not used by default; do not silently substitute other models when an allowed model is unavailable.
- After work starts, each coordinator delegates implementation, investigation, validation, and independent review to subagents. Record model, effort, scope, and role in the task record.
- Coordinate ownership through Orca orchestration and use isolated worktrees. Codex-side PRs require Claude coordinator approval; Claude-side PRs require Codex coordinator approval. Merge only after explicit approval for the current head SHA and successful required checks. Follow the plan for evidence and GitHub account constraints.
- Update `V3_0_0_PLAN.md` at scope, ownership, dependency, review, validation, and integration checkpoints.
- Keep coordinator exchanges and decisions in Git-managed Markdown under `docs/coordination/3.0.0/`. Record proposals, responses, disagreements, rationale, outcomes, and references; do not rely on ephemeral terminal messages alone. Distinguish pending proposals from bilateral agreement. Codex serializes shared plan/log integration to avoid concurrent edits.
- Use `rtk` as the prefix for shell commands (`rtk proxy` for commands without a wrapper).

## Language and Kotlin Migration

- Communicate with the user in Japanese. Write PR titles and bodies in English, including release PRs.
- Write source comments, JavaDoc, and KDoc in English. Keep new or changed comments in build scripts and configuration files in English as well.
- Planning and coordination logs may use Japanese for later review by the user.
- Replace Java with Kotlin where it improves maintainability as part of the planned work. Coordinators agree on migration boundaries and record the rationale; do not mechanically convert the entire repository.
- Separate mechanical conversion from behavior changes where practical. Preserve or explicitly document Java-callable API changes, nullability, JVM signatures, and 2.x migration requirements. Validate both Java and Kotlin consumers for converted public APIs.

## Project Structure & Module Organization

- `core/`: overlay APIs, permission handling, and internal window/lifecycle management.
- `opt-timber/`: optional Timber logging integration, depending on `core`.
- `lint/`: Java library containing custom Android lint detectors and their tests.
- `sample/`: Android demo app; layouts are in `src/main/res/`, and displayed code examples are in `src/main/assets/`.

Java and Kotlin sources live under each module's `src/main/java/` or `src/main/kotlin/`. Local tests use `src/test/java/` or `src/test/kotlin/`; device tests use `src/androidTest/java/` in `core` and `sample`. Documentation animations are in `images/anime/`.

## Build, Test, and Development Commands

Use JDK 17, Android SDK 36 (Build Tools 36.0.0), and the checked-in Gradle wrapper (Gradle 9.4.1). The Android modules use AGP 9.2.1 with built-in Kotlin 2.2.10 and support API 23 or later. Configure your SDK through `ANDROID_HOME` or an untracked `local.properties`.

- `./gradlew :core:assembleDebug :opt-timber:assembleDebug :sample:assembleDebug`: build libraries and the demo APK.
- `./gradlew :core:testDebugUnitTest :opt-timber:testDebugUnitTest :lint:test :sample:testDebugUnitTest`: run local tests.
- `./gradlew :core:lintDebug :sample:lintDebug`: run Android lint checks.
- `./gradlew :core:connectedDebugAndroidTest :sample:connectedDebugAndroidTest`: run instrumentation tests on a connected device or emulator.
- `./gradlew :sample:installDebug`: install the demo; launch it from the device and grant overlay permission when prompted.

## Coding Style & Naming Conventions

Match existing Java style: four-space indentation, same-line opening braces, `UpperCamelCase` classes, `lowerCamelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Keep packages under `com.nagopy.android.overlayviewmanager`. Use `snake_case` resource names and the `overlayviewmanager_` prefix for core resources. Preserve public API JavaDocs and nullability annotations. No dedicated formatter is configured; keep formatting consistent with surrounding code.

## Testing Guidelines

Local tests use JUnit 4, Mockito, and Robolectric; lint tests use Android lint's testing infrastructure. Device tests use AndroidX Test, Espresso, and UI Automator. Name classes `*Test` and use descriptive methods such as `show_isVisibleFalse_isDraggableFalse`. Add regression tests for behavior changes, especially permission, lifecycle, and overlay interactions. Core debug coverage is enabled, but no minimum percentage is configured.

## Commit & Pull Request Guidelines

Recent commits use short imperative subjects, such as `Update JavaDocs` and `Remove redundant timber dependency in tests`. Follow that style and keep commits focused. In pull requests, describe the problem, affected modules, and validation performed; link relevant issues. Include screenshots or recordings for visible sample or overlay changes. Update usage documentation when public APIs change.
