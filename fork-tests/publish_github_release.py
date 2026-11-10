import json
import subprocess
import sys
import time
import urllib.error
import urllib.request

REPO = "mozyk96/Berrygram"
TAG = "v1.4.0-beta.2"
NAME = "Berrygram 1.4.0 Beta 2"
APK_NAME = "BerryGram-1.4.0-beta.2-arm64-v8a.apk"

if len(sys.argv) > 4:
    TAG = sys.argv[4]
if len(sys.argv) > 5:
    NAME = sys.argv[5]
if len(sys.argv) > 6:
    APK_NAME = sys.argv[6]
# The official stream publishes normal releases, a prerelease is only used for
# an explicit owner request.
PRERELEASE = True
if len(sys.argv) > 7:
    PRERELEASE = sys.argv[7].strip().lower() not in ("false", "0", "no")


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
    url = path if path.startswith("https://uploads.github.com/") else "https://api.github.com" + path
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
    last_error = None
    for attempt in range(4):
        request = urllib.request.Request(url, data=body, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=600) as response:
                payload = response.read()
            break
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", "replace")[:400]
            raise RuntimeError("api %s %s failed: %s %s" % (method, path, error.code, detail))
        except (urllib.error.URLError, OSError) as error:
            # A transient network or name resolution failure must not abort the
            # publication of a verified artifact.
            last_error = error
            time.sleep(5 * (attempt + 1))
    else:
        raise RuntimeError("api %s %s failed: %s" % (method, path, last_error))
    if not payload:
        return {}
    return json.loads(payload.decode("utf-8"))


def main():
    apk_path, checksums_path, body_path = sys.argv[1], sys.argv[2], sys.argv[3]
    token = git_credential()["password"]
    user = api("GET", "/user", token)
    print("authenticated_as", user.get("login"), "scopes_ok", bool(user.get("login")))

    release_id = None
    try:
        existing = api("GET", "/repos/%s/releases/tags/%s" % (REPO, TAG), token)
        release_id = existing.get("id")
        print("release_exists", release_id)
    except RuntimeError as error:
        if "404" not in str(error):
            raise
        print("release_not_published_yet")

    if release_id is None:
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
                "prerelease": PRERELEASE,
                "draft": False,
                "target_commitish": "feature/berrygram-1.4.1",
            },
        )
        release_id = created["id"]
        print("release_created", release_id, created.get("html_url"))

    assets = api("GET", "/repos/%s/releases/%s/assets" % (REPO, release_id), token)
    existing_names = {asset["name"] for asset in assets}
    upload_url = api("GET", "/repos/%s/releases/%s" % (REPO, release_id), token).get(
        "upload_url", ""
    ).split("{")[0]
    for path, upload_name in ((apk_path, APK_NAME), (checksums_path, "SHA256SUMS")):
        if upload_name in existing_names:
            print("asset_present", upload_name)
            continue
        with open(path, "rb") as handle:
            payload = handle.read()
        uploaded = api(
            "POST",
            upload_url + "?name=" + upload_name,
            token,
            raw=payload,
        )
        print("asset_uploaded", uploaded["name"], uploaded.get("size"), uploaded.get("digest"))


if __name__ == "__main__":
    main()
