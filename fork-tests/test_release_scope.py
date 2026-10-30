from pathlib import Path
root=Path('TMessagesProj/src/main/java/org/telegram')
checks={
 'ui/ForkSettingsActivity.java': ['ForkVideoSettingsActivity', 'ForkVoiceSettingsActivity', 'ForkPanelSettings', 'ForkCustomTextActivity'],
 'ui/ChatActivity.java': ['CustomTextManager.aliasDisplay', 'consumeForkCameraChoice'],
 'ui/Components/ChatActivityEnterView.java': ['consumeForkCameraChoice'],
}
for path,required in checks.items():
 text=(root/path).read_text(encoding='utf-8')
 for term in required:
  assert term in text, (path,term)
retired=('org.telegram.messenger.fork.gestures','org.telegram.messenger.fork.menu','org.telegram.messenger.fork.updater','org.telegram.messenger.fork.player','org.telegram.messenger.fork.performance','org.telegram.messenger.fork.privacy','org.telegram.messenger.fork.appearance','ForkMessageMenu','ForkUpdaterActivity','ForkQuickReactionShortcuts')
for path in root.rglob('*.java'):
 text=path.read_text(encoding='utf-8',errors='replace')
 for term in retired:
  assert term not in text, (path,term)
print('BerryGram release scope checks passed')
