# 1.3.0 architecture
Android first, Desktop only after owner-approved Android Stable v1.3.0 is actually published. Base60ea4dd Stable1.2.0, runtimea41d059. Preserve identity/key/API23, native headers and current graphics.

Shared ActionRegistry describes stable IDs and native menu option bindings. Native fillMessageMenu produces authoritative availability; processSelectedOption executes, including existing confirmations. Per-account ActionSettings backs gesture mappings, reaction preferences and menu presentation. UI editors use the same IDs, not copied server logic. Body gestures must not intercept media/entities/bot controls; native RecyclerListView tap arbitration and horizontal/vertical threshold remain authoritative.

Order: registry/persistence -> gesture dispatch/editor -> reaction selector/editor -> menu presentation/editor -> target tests/incremental compilation -> signed Beta -> owner acceptance -> Stable. No Stable without owner confirmation; no Desktop implementation beforehand.
