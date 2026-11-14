"""Verify install identity and signer continuity before retaining a release APK."""
import argparse
import re
import subprocess
import sys
from pathlib import Path


def run(*args):
    result = subprocess.run(args, capture_output=True, text=True, errors='replace')
    if result.returncode:
        raise ValueError(f'{Path(args[0]).name} verification failed')
    return result.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('apk')
    parser.add_argument('--aapt', required=True)
    parser.add_argument('--apksigner', required=True)
    parser.add_argument('--zipalign', required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--version-code', type=int, required=True)
    parser.add_argument('--signer-sha256', required=True)
    args = parser.parse_args()

    verifier = Path(__file__).with_name('verify_apk.py')
    run(sys.executable, str(verifier), args.apk, '--aapt', args.aapt,
        '--package', 'org.example.forkclient', '--version', args.version)
    badging = run(args.aapt, 'dump', 'badging', args.apk)
    package = next((line for line in badging.splitlines() if line.startswith('package: ')), '')
    native = next((line for line in badging.splitlines() if line.startswith('native-code:')), '')
    if (not re.search(r"\bversionCode='" + str(args.version_code) + r"'", package)
            or native != "native-code: 'arm64-v8a'"
            or "sdkVersion:'23'" not in badging
            or "targetSdkVersion:'36'" not in badging):
        raise ValueError('Version code, ABI or SDK levels do not match')

    signature = run(args.apksigner, 'verify', '--verbose', '--print-certs', args.apk)
    signers = [value.lower() for value in re.findall(
        r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$', signature, re.MULTILINE)]
    if (signers != [args.signer_sha256.lower()]
            or 'Verified using v1 scheme (JAR signing): true' not in signature
            or 'Verified using v2 scheme (APK Signature Scheme v2): true' not in signature):
        raise ValueError('Signer certificate or signature schemes do not match')
    run(args.zipalign, '-c', '-P', '16', '4', args.apk)
    print('Release identity, SDK levels, ARM64 ABI, signer and alignment verified.')


if __name__ == '__main__':
    try:
        main()
    except ValueError as error:
        print(error, file=sys.stderr)
        sys.exit(1)
