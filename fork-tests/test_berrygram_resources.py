from pathlib import Path
import xml.etree.ElementTree as ET
import re
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
assert properties.get('APP_VERSION_NAME') in ('1.3.0', '1.4.0-beta.2')
assert int(properties['APP_VERSION_CODE']) > 7262
assert int(properties.get('APP_VERSION_CODE', '0')) > 0
assert properties.get('APP_PACKAGE') == 'org.example.forkclient'
resources = root / 'TMessagesProj/src/main/res'
def locale_strings(locale):
    values = {}
    for path in sorted((resources / locale).glob('*.xml')):
        for item in ET.parse(path).getroot():
            if item.tag == 'string' and item.attrib.get('name', '').startswith(('Fork', 'BerryGram')):
                values[item.attrib['name']] = ''.join(item.itertext())
    return values
english = locale_strings('values')
ukrainian = locale_strings('values-uk')
russian = locale_strings('values-ru')
action_en = locale_strings('values')
action_uk = locale_strings('values-uk')
action_ru = locale_strings('values-ru')
for key in ('ForkPerformanceTitle', 'ForkPerformanceAutomatic', 'ForkPerformanceSmooth', 'ForkPerformanceBattery', 'ForkPerformanceCustom', 'ForkPerformanceInfo', 'ForkPerformanceLiteMode', 'ForkPerformanceStorage', 'ForkPerformanceCopyDiagnostics'):
    assert key in action_en and key in action_uk and key in action_ru, f'Missing action translation: {key}'
for key in ('ForkGraphicsExperimental',):
    assert key in english and key in ukrainian and key in russian, f'Missing graphics translation: {key}'
for key in ('ForkSettingsTitle', 'ForkVideoTitle', 'ForkVoiceTitle', 'ForkPanelsTitle', 'ForkCustomTextTitle', 'ForkUpdatePanel'):
    assert key in russian, f'Missing Russian setting: {key}'
assert english.keys() == ukrainian.keys(), 'Ukrainian BerryGram translations are incomplete'
format_arg = re.compile(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]')
for key in english:
    assert sorted(format_arg.findall(english[key])) == sorted(format_arg.findall(ukrainian[key])), key
source_root = root / 'TMessagesProj/src/main/java'
active = set()
wrong_api = []
for source in list(source_root.rglob('*.java')) + list(source_root.rglob('*.kt')):
    text = source.read_text(encoding='utf-8', errors='replace')
    active.update(re.findall(r'R\.string\.(Fork\w+)', text))
    wrong_api.extend((source, key) for key in re.findall(r'LocaleController\.(?:getString|formatString|formatSpannable)\(\s*R\.string\.(Fork\w+)', text))
for locale, values in (('default', english), ('Ukrainian', ukrainian), ('Russian', russian)):
    missing = active - values.keys()
    assert not missing, f'Missing {locale} strings used by BerryGram UI: ' + ', '.join(sorted(missing))
assert not wrong_api, 'Android-only BerryGram resources must use ForkLocale, not LocaleController: ' + ', '.join(
    f'{source.relative_to(root)}:{key}' for source, key in wrong_api
)
print('BerryGram backup, manifest, release metadata and localization checks passed')
