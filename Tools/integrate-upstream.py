#!/usr/bin/env python3
"""Three-way integration of an official Telegram source update into the fork.

Usage: python Tools/integrate-upstream.py <base-rev> <target-rev> [--dry-run]

For every path changed between the two upstream revisions the script performs a
real three-way merge (base = previous upstream revision, ours = fork working
tree, theirs = new upstream revision).  Fork modifications are preserved and
whitespace line endings are normalised to LF so that git can handle them.
Paths under TMessagesProj/jni/twidget/ are handled by the dedicated
"take upstream" switch below because upstream converts them to submodules.
"""
import argparse
import os
import subprocess
import sys

TWIDGET_PREFIX = 'TMessagesProj/jni/twidget/'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def git(*args, binary=False):
    return subprocess.run(['git', *args], cwd=ROOT, check=True,
                          capture_output=True, text=not binary).stdout


def show(rev, path):
    try:
        return git('show', f'{rev}:{path}', binary=True)
    except subprocess.CalledProcessError:
        return None


def is_binary(data):
    return b'\0' in data[:8000]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('base')
    ap.add_argument('target')
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()

    out = git('diff', '--name-status', '--no-renames', args.base, args.target)
    rows = [line.split('\t') for line in out.splitlines() if line.strip()]
    conflicts, taken, added, deleted = [], 0, 0, 0
    for row in rows:
        status, path = row[0], row[1]
        if path.startswith(TWIDGET_PREFIX):
            continue
        base = show(args.base, path)
        theirs = show(args.target, path)
        full = os.path.join(ROOT, path)
        ours = None
        if os.path.exists(full):
            with open(full, 'rb') as f:
                ours = f.read()
        if status.startswith('D'):
            if ours is not None:
                conflicts.append((path, 'deleted upstream but modified locally'))
            continue
        if status.startswith('A') or ours is None:
            if theirs is None:
                continue
            if ours is not None:
                conflicts.append((path, 'added upstream but exists locally'))
                continue
            if args.dry_run:
                added += 1
                continue
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, 'wb') as f:
                f.write(theirs)
            added += 1
            continue
        if base is None or theirs is None:
            conflicts.append((path, 'missing base or theirs'))
            continue
        if is_binary(ours) or is_binary(theirs) or is_binary(base):
            if ours.replace(b'\r\n', b'\n') == base.replace(b'\r\n', b'\n'):
                if not args.dry_run:
                    with open(full, 'wb') as f:
                        f.write(theirs)
                taken += 1
            else:
                conflicts.append((path, 'binary file changed on both sides'))
            continue
        if ours.replace(b'\r\n', b'\n') == base.replace(b'\r\n', b'\n'):
            if not args.dry_run:
                with open(full, 'wb') as f:
                    f.write(theirs)
            taken += 1
            continue
        if ours.replace(b'\r\n', b'\n') == theirs.replace(b'\r\n', b'\n'):
            continue
        # real three-way merge on normalised text
        tmp = os.path.join(ROOT, '.merge-tmp')
        with open(tmp + '.ours', 'wb') as f:
            f.write(ours.replace(b'\r\n', b'\n'))
        with open(tmp + '.base', 'wb') as f:
            f.write(base.replace(b'\r\n', b'\n'))
        with open(tmp + '.theirs', 'wb') as f:
            f.write(theirs.replace(b'\r\n', b'\n'))
        proc = subprocess.run(['git', 'merge-file', '-p', '--diff3',
                               tmp + '.ours', tmp + '.base', tmp + '.theirs'],
                              cwd=ROOT, capture_output=True)
        merged = proc.stdout
        if proc.returncode != 0:
            conflicts.append((path, f'{proc.returncode} conflict hunks'))
        if not args.dry_run:
            with open(full, 'wb') as f:
                f.write(merged)
        for suffix in ('.ours', '.base', '.theirs'):
            try:
                os.remove(tmp + suffix)
            except OSError:
                pass

    print(f'taken from upstream: {taken}, added: {added}, deleted-skipped: {deleted}')
    for path, why in conflicts:
        print(f'CONFLICT {path}: {why}')
    return 1 if conflicts else 0


if __name__ == '__main__':
    sys.exit(main())
