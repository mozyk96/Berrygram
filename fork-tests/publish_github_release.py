import json
import os
import subprocess
import sys
import urllib.request

REPO = "mozyk96/Berrygram"
TAG = "v1.4.0-beta.2"
NAME = "Berrygram 1.4.0 Beta 2"
APK_NAME = "BerryGram-1.4.0-beta.2-arm64-v8a.apk"


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


def api(method, path, token, data=None, raw=None, content_type=None):
    url = "https://api.github.com" + path
    body = None
    headers = {
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "berrygram-release",
    }
    if raw is not None:
        body = raw
        headers["Content-Type"] = content_type or "application/octet-stream"
    elif data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(request) as response:
        payload = response.read()
    if not payload:
        return {}
    return json.loads(payload.decode("utf-8"))


def main():
    apk_path = sys.argv[1]
    checksums_path = sys.argv[2]
    body_path = sys.argv[3]
    token = git_credential()["password"]

    existing = api("GET", "/repos/%s/releases/tags/%s" % (REPO, TAG), token)
    if existing.get("id"):
        print("release_exists", existing["id"])
        release_id = existing["id"]
    else:
        with open(body_path, "r", encoding="utf-8") as handle:
            body = handle.read()
        created = api(
            "POST",
            "/repos/%s/releases" % REPO,
            token,
            data={
                "tag_name": TAG,
                "name": NAME,
                "body": body,
                "prerelease": True,
                "draft": False,
                "target_commitish": "feature/berrygram-1.4.0-beta2",
            },
        )
        release_id = created["id"]
        print("release_created", release_id, created.get("html_url"))

    assets = api("GET", "/repos/%s/releases/%s/assets" % (REPO, release_id), token)
    existing_names = {asset["name"] for asset in assets}

    for path, upload_name in ((apk_path, APK_NAME), (checksums_path, "SHA256SUMS")):
        if upload_name in existing_names:
            print("asset_present", upload_name)
            continue
        with open(path, "rb") as handle:
            payload = handle.read()
        uploaded = api(
            "POST",
            "/repos/%s/releases/%s/assets?name=%s" % (REPO, release_id, upload_name),
            token,
            raw=payload,
        )
        print("asset_uploaded", uploaded["name"], uploaded.get("size"), uploaded.get("digest"))


if __name__ == "__main__":
    main()
