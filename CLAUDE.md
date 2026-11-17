# Project rules (mobile-friendly Termux fork)

Goal: a phone-friendly Termux with a simple, nice UI — primarily for using Claude Code from a phone, but understandable for beginners. The owner works from a phone only and talks in Russian; explain things simply.

Hard rules — never break these:
- **Package name stays `com.termux`.** Changing it breaks every prebuilt Termux package (paths are hardcoded to `/data/data/com.termux`).
- **Signing key stays `app/testkey_untrusted.jks`** (the debug `signingConfig` in `app/build.gradle`). The owner installs builds over each other and keeps a hand-configured Ubuntu + Claude Code inside; a different key forces an uninstall and wipes all data.
- **`versionCode` must never go down** (equal is fine). A lower versionCode can't be installed over the existing app.
- Always ship the `apt-android-7` package variant to the owner (don't switch them between bootstrap variants).
- Change only UI/usability. Don't touch bootstrap/package-manager internals.

Build/delivery:
- APKs are built only by GitHub Actions (`.github/workflows/debug_build.yml`): on push to `master`, `claude/**` branches, or manually (workflow_dispatch). The APK is downloaded from the run's Artifacts on the phone. There is no local Android SDK in the dev container.
- Work happens step by step; each step should be a separately testable build.

Localization:
- Russian strings live in `values-ru/` of `app`, `termux-shared`, `terminal-view`. When adding a user-visible string, add it to both `values/` and `values-ru/` — never hardcode UI text in Java. Use `<plurals>` for counts (Russian needs one/few/many).
