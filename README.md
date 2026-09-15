# JARVIS — real Android UI agent

JARVIS is an Android application built around the real `AccessibilityService` API. It observes the active app's semantic UI tree (text, content descriptions, resource IDs, classes, clickability and bounds), performs actions through accessibility APIs, and verifies foreground-app transitions before reporting success. It does not use fake success messages or coordinate macros.

## Current working slice

- Kotlin + Jetpack Compose premium assistant UI
- microphone speech recognition and spoken responses via Android SpeechRecognizer/TTS
- `JarvisAccessibilityService` with semantic tree inspection, click, type, scroll, swipe, back, home and app launch primitives
- safe action model (`AgentAction`, `TaskPlan`, `Observation`) and provider boundary (`AiPlanner`)
- language-tolerant starter planner for opening installed apps and search tasks
- high-impact actions represented as confirmation-required actions
- explicit error when the required UI is not exposed
- no API keys or sensitive screen contents are persisted

The planner is intentionally a safe baseline, not a fake universal LLM. Replace `SafeCommandPlanner` with a local model or an authenticated `AiPlanner` implementation for broader language and task planning. Keep the action executor's confirmation policy in front of any provider output.

## Run on a real phone

1. Open this folder in Android Studio (Ladybug or newer) and sync Gradle.
2. Build and install the `app` debug variant on a real Android 8+ phone.
3. Grant microphone permission.
4. Android Settings → Accessibility → **JARVIS UI control** → enable it. This is required for real device control.
5. Tap the microphone and say `Jarvis, Instagram kholo` (Instagram must be installed). JARVIS launches the package, waits, and only reports completion when the foreground package is verified.
6. Try `YouTube par Arijit Singh search karo`. Search succeeds only when the target app exposes an accessible search control and editable field.

Android security screens, OTPs, passwords, biometrics, CAPTCHAs and inaccessible custom-rendered controls are never bypassed. If a target app does not expose a semantic interface, JARVIS reports the limitation rather than claiming success.

The repository does not contain an Android SDK/Gradle installation in this environment, so APK compilation and real-device testing must be performed in Android Studio or a CI runner with the Android SDK installed.
