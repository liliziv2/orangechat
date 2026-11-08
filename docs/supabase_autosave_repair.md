# Supabase automatic message upload repair

- User, successful assistant and proactive replies publish durable upload intents in private `noBackupFilesDir`, using AtomicFile. No credentials are persisted in the outbox.
- WorkManager performs connected-network uploads, exponential retries, startup recovery and periodic recovery. Only successful HTTP responses remove intents; failures remain pending.
- Requests supply the original UIMessage UUID as `id`, use `on_conflict=id` and `resolution=ignore-duplicates`. Requires a UUID primary/unique id and INSERT permission. No UPDATE permission is required. Existing remote records are never rewritten.
- Original creation time is converted from UIMessage's local timestamp to UTC using the device timezone at enqueue time; the model does not retain an original timezone.
- Regeneration creates new UIMessage identities; selecting a branch does not enqueue history. Message edits are not remote updates. Multiple attempts with the same UUID are insert-once.
- Disabled/deleted or URL/table-reconfigured destinations retain pending jobs without redirecting them. Current credentials are resolved by configuration id at upload time.
- External Memories page exposes global pending count and retry action. Logs do not contain message payloads or remote response bodies; there is not yet a per-item error detail UI.

## Verification boundaries

- JVM tests cover durable intent JSON round-trip, content preservation, absence of credential fields, and stable id/time on recovery.
- Local offline Gradle compilation was blocked by missing Kotlin serialization plugin 2.3.21, before Kotlin source compilation.
- Real device/network and Supabase RLS verification remain necessary: offline send -> restart -> reconnect -> pending reaches zero; retry after server insert but lost response produces one row; normal and proactive messages both arrive.
- Intents are separate from conversation persistence, not one database transaction. Abrupt process death in the small interval between conversation commit and intent publication, disk write failure, and interrupted partial generations remain limitations. Existing historical gaps are NOT backfilled by this patch.
- No remote schema/data changes or historical imports are performed.
