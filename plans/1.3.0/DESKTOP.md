# Desktop: gated plan, not started
Gate: Android1.3.0 Stable published after owner acceptance. Until then do not clone/build/change Telegram Desktop.

After gate inspect official telegramdesktop/tdesktop architecture, actual C++/Qt components, licenses and Windows build documentation. Prefer independently built sibling checkout/repository for large upstream history/CMake dependencies; decide after real investigation. Share behavior/schema documentation, not Android Java or SharedPreferences.

Stages: reproducible Windows x64 upstream build/branding -> shared native UI tokens/components -> floating vertical dock with actual native navigation -> adaptive workspace -> Settings Studio with all native settings/search -> anchored menus/profile/chat surfaces -> real supported Blur/Glass/fallback and reduced motion -> compatible action/reaction/menu/text settings -> packaged Windows Alpha -> owner tests -> Beta -> approved Stable. No Electron, invented signature or Android key reuse. Linux portability retained; real build paths/methods determined after gate.
