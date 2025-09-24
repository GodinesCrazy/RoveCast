# Repository Guidelines

## Project Structure & Module Organization
- Module: `app` (Android).
- Source: `app/src/main/java/com/ivanmarty/rovecast/` grouped by feature: `api/`, `ui/`, `data/`, `player/`, `billing/`, `ads/`, `cast/`, `util/`, `model/`, `repo/`.
- Resources: `app/src/main/res/` (layouts, drawables, menus, values); assets in `app/src/main/assets/`; manifest at `app/src/main/AndroidManifest.xml`.
- Tests: unit in `app/src/test/java/...`; instrumented in `app/src/androidTest/java/...`.

## Build, Test, and Development Commands
- Build debug: `./gradlew assembleDebug` — compiles a debug APK/AAB.
- Install debug: `./gradlew installDebug` — installs to a connected device/emulator.
- Unit tests: `./gradlew testDebugUnitTest` — runs JVM tests in `test/`.
- Instrumented tests: `./gradlew connectedDebugAndroidTest` — runs Android tests on device/emulator.
- Lint: `./gradlew :app:lint`; Clean: `./gradlew clean`.
- Android Studio: open project, select `app`, use Run/Debug to launch.

## Coding Style & Naming Conventions
- Language: Java; indentation: 4 spaces; no tabs. Keep methods small and cohesive.
- Names: Classes `PascalCase`; methods/fields `camelCase`; constants `UPPER_SNAKE_CASE`.
- Resources: snake_case (e.g., `fragment_home.xml`, `ic_play_arrow.xml`, `menu_top.xml`). IDs use helpful prefixes.
- Packages by feature (e.g., `ui/home`). Use Android Studio formatter; optimize imports.

## Testing Guidelines
- Frameworks: JUnit 4 for unit tests; AndroidX/Espresso for instrumented UI.
- Location: pure JVM tests in `test/`; Android-dependent/UI tests in `androidTest/`.
- Naming: mirror class under test, suffix with `Test` (e.g., `HomeViewModelTest`).
- Scope: focus on ViewModels, repositories, and utilities; prefer deterministic tests (avoid real network/audio).

## Commit & Pull Request Guidelines
- Commits: Conventional Commits (e.g., `feat(search): add region filter`, `fix(player): handle pause crash`).
- PRs: include clear description, rationale, linked issues, and screenshots for UI changes.
- Quality bar: changes focused; strings/resources updated; build, tests, and lint must pass before review.

## Security & Configuration
- Never commit secrets or private keys; prefer `local.properties`/Gradle properties. Review `google-services.json` handling.
- Keep permissive `network_security_config` for debug only.
- Verify release with `app/proguard-rules.pro` and test builds on a physical device.
