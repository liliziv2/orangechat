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
- [x] Memory retrieval: probe the whole library per query token instead of only the decay-ranked 200-row pool, so older but relevant rows can be recalled.
- [x] Memory token budgeting: apply the shared char budget inside recall, so a row that never reaches the prompt does not count as recalled (+1 activation).
- [ ] Memory end-to-end validation on device: write, recall, restart, and confirm the injected set matches the touched set.

## Excluded from this patch

No APK size changes, theme redesign, input layout changes, dependency upgrades, assistant database isolation migration or background scheduler replacement.

## Build status

Local Gradle did not reach compilation in the Android/proot environment. No local compilation success is claimed. CI and device results must be recorded separately.

## Follow-up: sidebar and memory repair
- Restored visible folder creation even with zero existing folders; kept existing folder navigation/rename/delete.
- Removed only the upper new-chat entry; the bottom action stays.
- Replaced the broken 160/200-row probe guard with keyset paging across every active row in the selected memory library.
- Query matching uses all tokens; results still use decay ranking. This is lexical retrieval, not embedding-based semantic search.
- Shared serialization and bounded conservative UTF-8 token-cost estimate; oversized rows do not bypass the budget. Exact model-token counts and total context budgeting are not claimed.
- Memory-disabled chat no longer recalls/touches local records. Mutation and overlays enforce owner boundaries.
- Event memory owner persists in ledger (v34→35 nullable column); historical unknown owners are not guessed.
- Closed-loop source dedupe and writes are transactional; failed emotion writes stop checkpoint advancement for retry.
- Decay refresh updates only scores and does not overwrite content/counters.
- JVM recall/owner/budget regression tests added. Device migration/reopen tests added and compiled in CI; not executed without an emulator/device.
- Drive-state itself remains a global singleton; per-assistant state isolation is a separate schema/behavior change, not claimed by memory-owner fixes.
