# Local message archive — investigation, not implemented

The feature must remain off until its compatibility with Telegram API Terms and content-licensing restrictions has been reviewed for ordinary deleted messages. Telegram API Terms §1.4 explicitly prohibit preventing self-destructing content from disappearing. No inference that ordinary-message preservation is permitted is made here. See https://core.telegram.org/api/terms and https://telegram.org/tos/content-licensing.

## Boundaries

Archive only eligible received ordinary messages after the owner opts in. Exclude encrypted/secret dialogs, any self-destructing or TTL-bearing message or media, unsupported or ambiguous types, outgoing messages, and content subject to a protected-content policy until specifically reviewed. Do not modify Telegram's main message store, updates, deletion logic, read receipts, or server state. Start collecting only after opt-in; no backfill by default. Disabling stops collection; clearing irreversibly removes data and associated encryption keys. Retention must be configurable and enforced during access as well as periodic cleanup, since background scheduling is not exact.

## Event identity and correctness

Telegram's common updateDeleteMessages contains message IDs without a peer; updateDeleteChannelMessages contains a channel ID and message IDs. Store account identity and peer identity separately, plus message IDs and the message-ID namespace required to map deletion updates. Maintain a unique account/common-message-ID index for non-channel messages only where Telegram's documented sequence makes that unambiguous; otherwise do not assign a deletion event to a peer. For channels, use account, channel, and message ID. Follow upstream's already-reconciled update processing and pts gap recovery; don't create a second MTProto synchronization loop. Accept idempotent insert/update/delete-event processing, and record deletion as observed rather than claiming when or by whom it occurred. Lost or unavailable updates mean deletion state can be unknown. Never infer deletion merely from a missing message.

Sources: https://core.telegram.org/api/updates and https://core.telegram.org/constructor/updateDeleteChannelMessages.

## Storage and key lifecycle proposal

Use a separate app-private archive store, independently scoped by stable account ID. Encrypt each content record with an authenticated encryption scheme and fresh unique nonce, and protect a per-account data key with Android Keystore; bind record identity and schema version as associated data. Keep key operations and disk work off the UI thread. Exclude archive files and keys from system/cloud backup and diagnostic logs. A deleted Keystore key makes the corresponding encrypted archive unrecoverable; uninstall, device restore, key invalidation and partial deletion must be handled explicitly. Keystore limits key extraction but does not protect data from an attacker controlling the running app. Source: https://developer.android.com/privacy-and-security/keystore.

Do not implement until the data model, cryptography, recovery behavior, consent and Terms questions receive a focused review. Required verification includes TTL and encrypted-chat exclusions, multiple accounts, channel versus common ID collisions, missed/reordered updates, restart and crash recovery, key invalidation, retention, clearing, backup exclusion and no content in logs. No claim of Telegram Terms compliance or security audit is made.
