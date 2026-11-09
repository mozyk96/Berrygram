"""Uploads the release assets with curl, the Python resolver is unreliable here.

The token is read from the git credential store and is never printed.
"""
import json
import subprocess
import sys

REPO = "mozyk96/Berrygram"
TAG = sys.argv[1] if len(sys.argv) > 1 else "v1.4.2"
ASSETS = sys.argv[2:]


def git_credential():
    out = subprocess.run(
        ["git", "credential", "fill"],
        input="protocol=https\nhost=github.com\n\n",
        capture_output=True,
        text=True,
        check=True,
    ).stdout
    fields = {}
    for line in out.splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            fields[key] = value
    return fields


def curl_json(args):
    result = subprocess.run(["curl.exe", "-sS", "-H", "Accept: application/vnd.github+json",
                             "-H", "X-GitHub-Api-Version: 2022-11-28",
                             "-H", "User-Agent: berrygram-release"] + args,
                            capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError("curl failed: " + result.stderr.strip())
    return json.loads(result.stdout) if result.stdout.strip() else {}


def main():
    token = git_credential()["password"]
    auth = "Authorization: Bearer " + token
    release = curl_json(["-H", auth, "https://api.github.com/repos/%s/releases/tags/%s" % (REPO, TAG)])
    release_id = release["id"]
    upload_url = release["upload_url"].split("{")[0]
    print("release", release_id, release.get("html_url"))

    existing = {asset["name"] for asset in curl_json(
        ["-H", auth, "https://api.github.com/repos/%s/releases/%s/assets" % (REPO, release_id)])}
    for path in ASSETS:
        import os
        name = os.path.basename(path)
        if name in existing:
            print("asset_present", name)
            continue
        result = subprocess.run(
            ["curl.exe", "-sS", "--fail-with-body", "-X", "POST",
             "-H", auth,
             "-H", "Accept: application/vnd.github+json",
             "-H", "X-GitHub-Api-Version: 2022-11-28",
             "-H", "User-Agent: berrygram-release",
             "-H", "Content-Type: application/octet-stream",
             "--data-binary", "@" + path,
             upload_url + "?name=" + name],
            capture_output=True, text=True)
        if result.returncode != 0:
            print("asset_failed", name, result.stdout[:300], result.stderr[:300])
            raise SystemExit(1)
        payload = json.loads(result.stdout)
        print("asset_uploaded", payload.get("name"), payload.get("size"), payload.get("state"))


if __name__ == "__main__":
    main()
