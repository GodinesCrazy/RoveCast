# Repository Guidelines

## Project Structure & Modules
- App module: `app`.
- Source: `app/src/main/java/com/ivanmarty/rovecast/` organized by feature: `api/`, `ui/`, `data/`, `player/`, `billing/`, `ads/`, `cast/`, `util/`, `model/`, `repo/`.
- Resources: `app/src/main/res` (layouts, drawables, menus, values). Assets: `app/src/main/assets/`. Manifest: `app/src/main/AndroidManifest.xml`.
- Tests: unit in `app/src/test/java/…`; instrumented in `app/src/androidTest/java/…`.
- Build/config: root `build.gradle`, `app/build.gradle`, `gradle.properties`, `app/proguard-rules.pro`, `app/lint.xml`, `res/xml/network_security_config.xml`.

## Build, Test, and Run
- Build debug: `./gradlew assembleDebug` — compiles APK/AAB for debug.
- Install debug: `./gradlew installDebug` — installs on connected device/emulator.
- Unit tests: `./gradlew testDebugUnitTest` — runs JVM tests under `test/`.
- Instrumented tests: `./gradlew connectedDebugAndroidTest` — runs Android tests on device/emulator.
- Lint: `./gradlew lint` (module: `./gradlew :app:lint`). Clean: `./gradlew clean`.
- Android Studio: open project, select `app`, press Run or Debug.

## Coding Style & Naming
- Language: Java (Android). Indentation: 4 spaces; no tabs. Keep methods small and cohesive.
- Names: Classes `PascalCase`; methods/fields `camelCase`; constants `UPPER_SNAKE_CASE`.
- Resources: snake_case (e.g., `fragment_home.xml`, `ic_play_arrow.xml`, `menu_top.xml`). IDs start with view or feature prefix when helpful.
- Packages by feature (e.g., `ui/home`, `data/…`). Use Android Studio formatter; optimize imports.

## Testing Guidelines
- Frameworks: JUnit 4 for unit; AndroidX/Espresso for instrumented UI.
- Location: pure JVM tests in `test/`; Android-dependent or UI tests in `androidTest/`.
- Naming: mirror class under test, suffix with `Test` (e.g., `HomeViewModelTest`).
- Aim to cover ViewModels, repositories, and utilities. Prefer deterministic tests; avoid real network/audio.

## Commit & PR Guidelines
- Use Conventional Commits: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`; scope by feature (e.g., `feat(search): add region filter`).
- PRs: clear description, rationale, linked issues, test steps; include screenshots for UI changes.
- Keep changes focused; update strings/resources consistently; pass build, tests, and lint before requesting review.

## Security & Configuration
- Do not commit secrets or private keys; prefer `local.properties`/Gradle properties. Review `google-services.json` handling as needed.
- Keep permissive `network_security_config` only for debug builds.
- Verify release with `proguard-rules.pro` and test on a physical device.

