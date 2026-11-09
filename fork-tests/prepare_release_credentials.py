"""Prepares the release credentials from the generated BuildConfig of the fork.

The value is taken from the build tree of the previous release of the same
fork, it is written into a temporary environment file for the release build and
is never printed or committed.
"""
import io
import os
import re
import sys

root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
source = os.path.join(root, 'TMessagesProj', 'build', 'generated', 'source', 'buildConfig',
                       'release', 'org', 'telegram', 'messenger', 'BuildConfig.java')
target = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.environ.get('TEMP', '.'), 'apienv.txt')

if not os.path.exists(source):
    print('The generated BuildConfig of the previous release is missing', file=sys.stderr)
    sys.exit(1)

with io.open(source, 'r', encoding='utf-8') as handle:
    text = handle.read()

api_id = re.search(r'FORK_TELEGRAM_API_ID\s*=\s*([1-9][0-9]*)', text)
api_hash = re.search(r'FORK_TELEGRAM_API_HASH\s*=\s*"([0-9a-fA-F]{32})"', text)
if not api_id or not api_hash:
    print('The generated BuildConfig has no usable credential', file=sys.stderr)
    sys.exit(1)

with io.open(target, 'w', encoding='utf-8') as handle:
    handle.write('FORK_TELEGRAM_API_ID=%s\n' % api_id.group(1))
    handle.write('FORK_TELEGRAM_API_HASH=%s\n' % api_hash.group(1))

print('release credential prepared in %s (value not printed)' % target)
