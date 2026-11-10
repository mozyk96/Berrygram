from pathlib import Path
root=Path(__file__).resolve().parent.parent / 'TMessagesProj/src/main/java/org/telegram'
checks={
 'ui/ForkSettingsActivity.java': ['ForkVideoSettingsActivity', 'ForkVoiceSettingsActivity', 'ForkPanelSettings', 'ForkCustomTextActivity', 'ForkUpdateActivity', 'ForkPerformanceSettingsActivity'],
 'ui/ChatActivity.java': ['CustomTextManager.aliasDisplay', 'consumeForkCameraChoice'],
 'ui/Components/ChatActivityEnterView.java': ['consumeForkCameraChoice'],
 'ui/ForkPerformanceSettingsActivity.java': ['PerformanceSettings', 'PerformancePolicy'],
}
for path,required in checks.items():
 text=(root/path).read_text(encoding='utf-8')
 for term in required:
  assert term in text, (path,term)
retired=('org.telegram.messenger.fork.gestures','org.telegram.messenger.fork.menu','org.telegram.messenger.fork.player','org.telegram.messenger.fork.privacy','org.telegram.messenger.fork.appearance','ForkMessageMenu.', 'new ForkMessageMenu(','ForkUpdaterActivity','ForkQuickReactionShortcuts')
for path in root.rglob('*.java'):
 text=path.read_text(encoding='utf-8',errors='replace')
 for term in retired:
  assert term not in text, (path,term)
passcode = (root / 'ui/Components/PasscodeView.java').read_text(encoding='utf-8')
assert 'passcodeFallbackColor()' in passcode
assert '0xff517c9e' not in passcode
surface = (root / 'ui/ForkGraphicsSurface.java').read_text(encoding='utf-8')
assert 'system_neutral1_' not in surface
popup = (root / 'ui/ActionBar/ActionBarPopupWindow.java').read_text(encoding='utf-8')
assert 'if (swipeBackLayout == null)' in popup
performance = (root / 'ui/ForkPerformanceSettingsActivity.java').read_text(encoding='utf-8')
assert 'removeAllViews' not in performance
assert 'LiteModeSettingsActivity' in performance and 'CacheControlActivity' in performance
updater = (root / 'ui/ForkUpdaterPanel.java').read_text(encoding='utf-8')
for obsolete in ('ForkUpdateChannel', 'ForkUpdateStableRecovery', 'ForkUpdateOlderStable', 'stableRecovery'):
    assert obsolete not in updater, f'obsolete update channel control remains: {obsolete}'
assert 'getBoolean("beta_channel"' not in updater and 'putBoolean("beta_channel"' not in updater
assert 'preferences.contains("beta_channel")' in updater, 'the obsolete channel preference must be migrated'
assert 'GithubUpdateClient.latest(REPOSITORY)' in updater
policy = (root / 'messenger/fork/updater/UpdatePolicy.java').read_text(encoding='utf-8')
for obsolete in ('betaChannel', 'isStableRecovery', 'stableRecovery'):
    assert obsolete not in policy, f'obsolete update channel rule remains: {obsolete}'
print('BerryGram release scope checks passed')
