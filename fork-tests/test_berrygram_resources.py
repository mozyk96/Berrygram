from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parent.parent
res = root / 'TMessagesProj/src/main/res/xml'
old = ET.parse(res / 'berrygram_backup_rules.xml').getroot()
new = ET.parse(res / 'berrygram_data_extraction_rules.xml').getroot()
def excludes(node):
    return any(e.get('domain') == 'sharedpref' and e.get('path') == 'fork_custom_text.xml' for e in node.findall('exclude'))
assert excludes(old)
assert excludes(new.find('cloud-backup'))
assert excludes(new.find('device-transfer'))
android = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root / 'TMessagesProj/src/main/AndroidManifest.xml').getroot().find('application')
assert manifest.get(android + 'fullBackupContent') == '@xml/berrygram_backup_rules'
assert manifest.get(android + 'dataExtractionRules') == '@xml/berrygram_data_extraction_rules'
for path in [root / 'TMessagesProj/config/debug/AndroidManifest.xml', root / 'TMessagesProj/config/debug/AndroidManifest_SDK23.xml', root / 'TMessagesProj/config/release/AndroidManifest.xml', root / 'TMessagesProj/config/release/AndroidManifest_SDK23.xml', root / 'TMessagesProj/config/release/AndroidManifest_standalone.xml']:
    app = ET.parse(path).getroot().find('application')
    assert app.get(android + 'icon') == '@mipmap/berrygram_launcher'
    assert app.get(android + 'label') == '@string/ForkAppName'
properties = {}
for line in (root / 'gradle.properties').read_text(encoding='utf-8').splitlines():
    if '=' in line:
        key, value = line.split('=', 1)
        if key in {'APP_VERSION_NAME', 'APP_VERSION_CODE', 'APP_PACKAGE'}:
            properties[key] = value.strip()
assert properties.get('APP_VERSION_NAME') == '1.0.0'
assert int(properties.get('APP_VERSION_CODE', '0')) > 0
assert properties.get('APP_PACKAGE') == 'org.example.forkclient'
print('BerryGram backup, manifest and release metadata checks passed')
