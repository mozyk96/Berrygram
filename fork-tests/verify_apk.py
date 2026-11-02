"""Check install-critical manifest resources and ABI in a built BerryGram APK."""
import argparse
import subprocess
import sys
import re

parser = argparse.ArgumentParser()
parser.add_argument('apk')
parser.add_argument('--aapt', required=True)
parser.add_argument('--package', required=True)
parser.add_argument('--version', required=True)
args = parser.parse_args()
result = subprocess.run([args.aapt, 'dump', 'badging', args.apk], text=True, capture_output=True, errors='replace')
if result.returncode:
    print('APK badging failed: manifest resources may be missing.', file=sys.stderr)
    sys.exit(1)
lines = result.stdout.splitlines()
package = next((line for line in lines if line.startswith('package: ')), '')
checks = ["name='" + args.package + "'" in package,
          "versionName='" + args.version + "'" in package,
          any(line == "application-label:'BerryGram'" for line in lines),
          any(line.startswith('native-code:') and "'arm64-v8a'" in line for line in lines)]
if not all(checks):
    print('APK identity, label, version or ARM64 ABI did not match.', file=sys.stderr)
    sys.exit(1)
xml = subprocess.run([args.aapt, 'dump', 'xmltree', args.apk, 'AndroidManifest.xml'], text=True, capture_output=True, errors='replace')
if xml.returncode:
    print('APK manifest inspection failed.', file=sys.stderr)
    sys.exit(1)
for attribute in ('testOnly', 'debuggable'):
    if re.search(r'android:' + attribute + r'\([^\n]+\)=\(type 0x12\)0x(?:1|ffffffff)\b', xml.stdout, re.IGNORECASE):
        print('Release APK has android:' + attribute + '=true.', file=sys.stderr)
        sys.exit(1)
configs = subprocess.run([args.aapt, 'dump', 'configurations', args.apk], text=True, capture_output=True, errors='replace')
if configs.returncode or any(not any(line.strip() == language or line.strip().startswith(language + '-') for line in configs.stdout.splitlines()) for language in ('uk', 'ru')):
    print('Release APK is missing Ukrainian or Russian resources.', file=sys.stderr)
    sys.exit(1)
print('APK manifest resources, identity, version and ARM64 ABI passed.')
