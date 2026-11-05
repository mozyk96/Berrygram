# BerryGram continuation — Android 1.3.0 Beta 2

## State

- Branch: `dev/1.3.0`; Beta 1 tag `v1.3.0-beta.1` and Stable 1.2.0 remain immutable.
- Version: `1.3.0-beta.2`, base code `7259` → ARM64 APK `72599`.
- Phase: P0 fixes and Performance Engine implemented; full smoke and Java compilation passed. Release/R8 and APK checks pending.
- Additional prior Beta2 work was recovered from `work/public-candidate` (gesture native-path gate/fixes, `test_gesture_native_path.js`, BETA2 notes) and integrated here; it is backed up at `work/public-candidate-beta2-prior`.

## Completed

- Gesture dispatch: one re-entry-safe path; message/owner/account validation; correct native action gate for type≥2 messages; native option tuples for explicit actions; no recursive reaction callback; unavailable-action feedback; selected state cleanup; cached gesture preferences.
- Reaction panel: `ReactionFilterPolicy` over native allowed emoji, stable order, duplicate suppression, empty-result fallback, defensive list copy, layout-params null guard, full native picker retained.
- Performance Engine: `PerformancePolicy`/`PerformanceSettings` (automatic, smooth, battery, custom), Performance Center with diagnostics, runtime effect reduction integrated into `ForkGraphicsSurface`; Telegram Lite Mode storage is not overwritten.
- Tests: gesture dispatch + native-path, reaction filtering, performance policy/settings, updated smoke/resource/release-scope checks.

## Verified

- `fork-tests/smoke.ps1`: passed fully (Python 3.12 installed for fixtures).
- `:TMessagesProj_App:compileAfatDebugJavaWithJavac -PforkAbi=arm64-v8a`: passed.
- No physical device, real Telegram account or crash log used. Device touch/reaction/menu behavior remains owner-tested.

## Next

- Build Release/R8 in the cached native build workspace (`work/public-candidate`) from the exact committed source, with the existing release keystore, then verify APK metadata/signature/ABI/source/checksum and publish `v1.3.0-beta.2` only if valid.
- If Release/R8 fails, report the exact blocker rather than publishing.
