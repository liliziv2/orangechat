# Scoped repair checklist

Base: d657575cae653f3eb68da037086f56b970c276e5
Branch: feat/sidebar-memorial-card-and-input

## Included

- [x] Allow interface opacity to reach a real alpha of 1.0.
- [x] Bind previously unconfigured Material dialogs and bottom sheets to popup opacity.
- [x] Prevent a replaced generation job from clearing its successor; share the claim lock.
- [x] Use the current session for title/suggestion updates instead of stale database snapshots.
- [x] Serialize closed-loop writeback entry points.
- [x] Analyze dialogue semantics using the configured title model, with strict numeric validation, a 15-second timeout and the existing structural fallback. This adds an auxiliary model request when that model is configured.
- [x] Add regression tests for task replacement, low-priority claims and semantic parsing.
- [x] Add branch push/PR CI triggers; prevent PR builds from pushing generated signing files.
- [ ] GitHub Actions unit tests pass.
- [ ] GitHub Actions builds and uploads an APK for the exact repair commit.
- [ ] Device validation: popup opacity at 60/90/100%, light/dark themes and nested editors.

## Remaining in the authorized repair scope

- [ ] Complete session lifecycle/deletion/external-update consistency; the current patch only addresses auxiliary title/suggestion writes.
- [ ] Elektron end-to-end persistence/restart validation, assistant state ownership and failure recovery.
- [ ] Memory full-library retrieval/token budgeting and observability.

## Excluded from this patch

No APK size changes, theme redesign, input layout changes, dependency upgrades, assistant database isolation migration or background scheduler replacement.

## Build status

Local Gradle did not reach compilation in the Android/proot environment. No local compilation success is claimed. CI and device results must be recorded separately.
