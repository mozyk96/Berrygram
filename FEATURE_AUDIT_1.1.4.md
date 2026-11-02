# BerryGram 1.1.4 functional evidence

Status is conservative: source connections and mock tests do not prove device behavior. PARTIAL means implementation exists but an end-to-end result remains unverified. UNTESTED means the stated scenario has not been executed.

| Feature | Status | Evidence and remaining check |
| --- | --- | --- |
| Starting camera, last camera and gesture | PARTIAL | Settings persist; both `InstantCameraView` implementations read them and chat input handles the gesture. Policy and preference fixtures pass. Real cameras, permissions, gestures and restart require device validation. |
| Round-video bitrate | PARTIAL | `RoundVideoQualityPolicy` feeds both recorder paths and supported encoder bounds; adaptive paths attempt bitrate changes. Policy fixtures pass. Actual hardware encoder output and fallback remain unverified. |
| Voice bitrate | PARTIAL | Settings feed both `MediaController.startRecord` and `AudioRecorderNative.nCreate`; `audio.c` applies `OPUS_SET_BITRATE`. Policy and preference fixtures pass. Recorded Opus output, device behavior and restart require validation. |
| Updated menus and Liquid Glass | PARTIAL | Preference controls shared popup rendering and eligible blur capture, with power/device fallback. Policy fixtures pass. Screenshot fidelity, frame times and all popup variants remain unverified. A null-capture recovery is fixed in this change. |
| Custom phrases and per-chat aliases | PARTIAL | Editor and persistent manager connect to `LocaleController` and dialog display. Scope, formatting and manager fixtures pass. Actual language changes, accessibility, status correctness and restarts on Android remain unverified. |
| GitHub updater | PARTIAL | Network, metadata, SHA-256, package and signer checks exist; mock download and package fixtures pass. A real Android update and installer interaction remain unverified. |
| Russian, Ukrainian, English settings | PARTIAL | Ukrainian and English fork resources exist; Russian covers the active `Fork*.java` settings resource references, with format-argument validation. Some inactive fork resources lack Russian translations; Android language switching needs validation. |
| Startup, chats, calling, media, notifications | UNTESTED | No account or real device was used. These are Telegram core scenarios and are not certified by unit fixtures. |
| CPU, RAM, GPU, jank, battery and background work | UNTESTED | No comparable Android baseline or after measurements exist. The isolated software emulator remained offline; no performance improvement is quantified. |

The release must not be described as fully verified or as a measured performance improvement until those checks are performed. No physical phone is used without explicit permission.
