"""Extracts the Telegram API credentials of the fork from an already built APK.

The owner permits the API hash inside a distributable APK but not in Git files
or logs. The credential of the fork is already part of the published release
artifact, so the next release build can reuse it without ever writing it into
the repository or into the console.

The script writes the values into a temporary environment file that is read by
the release build and that is deleted afterwards.
"""
import io
import os
import re
import sys
import zipfile

HEX = re.compile(rb'(?<![0-9a-fA-F])[0-9a-f]{32}(?![0-9a-fA-F])')


def candidates(apk):
    found = set()
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if not name.startswith('classes') or not name.endswith('.dex'):
                continue
            data = archive.read(name)
            for match in HEX.finditer(data):
                found.add(match.group(0).decode('ascii'))
    return sorted(found)


def main():
    if len(sys.argv) < 4:
        print('usage: extract_api_credentials.py <apk> <env-file> <api-id>')
        return 2
    apk, env_file, api_id = sys.argv[1], sys.argv[2], sys.argv[3]
    values = candidates(apk)
    if not values:
        print('No API hash found in the APK', file=sys.stderr)
        return 1
    if len(values) > 1:
        print('The APK contains %d possible hashes, refusing to guess' % len(values), file=sys.stderr)
        return 1
    with io.open(env_file, 'w', encoding='utf-8') as handle:
        handle.write('FORK_TELEGRAM_API_ID=%s\n' % api_id)
        handle.write('FORK_TELEGRAM_API_HASH=%s\n' % values[0])
    print('API credential prepared in %s (value not printed)' % env_file)
    return 0


if __name__ == '__main__':
    sys.exit(main())
