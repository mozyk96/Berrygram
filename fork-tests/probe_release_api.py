import json
import subprocess
import urllib.error
import urllib.request


def credential():
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


token = credential()["password"]
headers = {
    "Authorization": "Bearer " + token,
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "User-Agent": "berrygram-release",
}

checks = [
    ("GET", "/repos/mozyk96/Berrygram/releases/438139214", None),
    ("GET", "/repos/mozyk96/Berrygram/releases/438139214/assets", None),
    ("GET", "/repos/mozyk96/Berrygram/releases?per_page=5", None),
]
for method, path, body in checks:
    request = urllib.request.Request("https://api.github.com" + path, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            payload = response.read().decode("utf-8")
        print(method, path, "->", response.status, "len", len(payload))
    except urllib.error.HTTPError as error:
        print(method, path, "-> HTTP", error.code, error.read().decode("utf-8", "replace")[:200])
    except Exception as error:
        print(method, path, "-> error", type(error).__name__, error)
