from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main/java/org/telegram/ui'
panel = (root / 'ForkUpdaterPanel.java').read_text(encoding='utf-8')
activity = (root / 'ForkUpdateActivity.java').read_text(encoding='utf-8')
about = (root / 'ForkAboutActivity.java').read_text(encoding='utf-8')
theme = (root / 'ThemeActivity.java').read_text(encoding='utf-8')
settings = (root / 'SettingsActivity.java').read_text(encoding='utf-8')
assert 'presentSettingFragment(new ForkUpdateActivity())' in settings
assert 'new ForkAboutActivity()' in panel
assert 'new ForkUpdateActivity()' not in about
assert 'new ForkUpdaterPanel(context, this)' in activity
start = panel.index('    private void download()')
end = panel.index('    private void fail(', start)
download = panel[start:end]
assert 'downloadAndVerify' in download and 'verified = file' in download
assert 'installIntent.request()' not in download and 'scheduleInstallerCheck()' not in download
assert 'downloadedBytes = received' in download and 'totalBytes = total' in download
assert theme.index('bubbleRadiusInfoRow = rowCount++') < theme.index('forkGraphicsRow = rowCount++') < theme.index('chatListHeaderRow = rowCount++')
print('Update screen routing, explicit installation and graphics placement checks passed')

blocks = (root / 'ForkSettingsBlocks.kt').read_text(encoding='utf-8')
graphics = blocks.split('fun graphics(', 1)[1].split('fun chatSettings(', 1)[0]
performance = blocks.split('fun performance(', 1)[1].split('/* ----- helpers ----- */', 1)[0]
assert 'ForkMiniDock' in graphics and 'ForkMiniDock' not in performance, 'Mini Dock belongs with appearance'
