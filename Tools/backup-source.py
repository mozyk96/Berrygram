"""Create and verify a secret-free, current-tree source snapshot (including submodules)."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    destination = args.destination.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    archive = destination / "berrygram-pre-kotlin-source.zip"
    if archive.exists():
        raise SystemExit("Snapshot already exists; refusing to overwrite")
    commit = git(root, "rev-parse", "HEAD").decode().strip()
    names = git(root, "ls-files", "--recurse-submodules", "-z").decode().split("\0")
    # Source and notes not yet tracked must also survive. Never include build products.
    names += git(root, "ls-files", "--others", "--exclude-standard", "-z").decode().split("\0")
    excluded_parts = {".git", ".gradle", ".kotlin", ".cxx", ".tooling", ".fork-local",
                      "build", "dist", "__pycache__", "node_modules", ".vscode"}
    excluded_names = {"google-services.json", "agconnect-services.json", "local.properties", ".env"}
    excluded_suffixes = {".keystore", ".jks", ".p12", ".pfx", ".apk", ".pyc"}
    files, excluded = {}, []
    with zipfile.ZipFile(archive, "x", zipfile.ZIP_DEFLATED, compresslevel=6) as output:
        for name in sorted(set(names)):
            if not name:
                continue
            relative = Path(name)
            if relative.is_absolute() or ".." in relative.parts:
                raise ValueError("Unsafe source path")
            if (any(part in excluded_parts for part in relative.parts)
                    or relative.name in excluded_names or relative.suffix.lower() in excluded_suffixes):
                excluded.append(name)
                continue
            path = root / relative
            if not path.is_file():
                continue
            payload = path.read_bytes()
            files[name] = {"sha256": hashlib.sha256(payload).hexdigest(), "bytes": len(payload)}
            output.writestr(name, payload)
        manifest = {"sourceCommit": commit, "files": files, "excluded": excluded,
                    "submodules": git(root, "submodule", "status", "--recursive").decode(),
                    "signing": "External DPAPI-protected configuration and permanent key are not included",
                    "storage": "Internal SSD snapshot; not a hardware-independent backup"}
        output.writestr("SNAPSHOT-MANIFEST.json", json.dumps(manifest, ensure_ascii=False, indent=2))
    # Read every member back, testing decompression, CRC and exact byte recovery.
    with zipfile.ZipFile(archive) as source:
        for name, expected in files.items():
            payload = source.read(name)
            if len(payload) != expected["bytes"] or hashlib.sha256(payload).hexdigest() != expected["sha256"]:
                raise RuntimeError(f"Recovery verification failed: {name}")
        recovered = destination / "recovery-check"
        recovered.mkdir(exist_ok=True)
        for name in ("gradle.properties", "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"):
            payload = source.read(name)
            sample = recovered / Path(name).name
            if sample.exists():
                raise RuntimeError("Recovery sample already exists")
            sample.write_bytes(payload)
            if hashlib.sha256(sample.read_bytes()).hexdigest() != files[name]["sha256"]:
                raise RuntimeError("Extracted sample verification failed")
    digest = hashlib.file_digest(archive.open("rb"), "sha256").hexdigest()
    report = {"archive": str(archive), "sourceCommit": commit, "verifiedFiles": len(files),
              "sha256": digest, "hardwareIndependent": False}
    (destination / "verification.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
