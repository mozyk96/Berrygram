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
path = "C:/Users/Keoo2/Documents/SHA256SUMS-1.4.0-beta.2.txt"
with open(path, "rb") as handle:
    payload = handle.read()

for scheme in ("Bearer", "token"):
    headers = {
        "Authorization": scheme + " " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "berrygram-release",
        "Content-Type": "application/octet-stream",
    }
    request = urllib.request.Request(
        "https://api.github.com/repos/mozyk96/Berrygram/releases/438139214/assets?name=SHA256SUMS",
        data=payload,
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(request) as response:
            body = response.read().decode("utf-8")
        parsed = json.loads(body)
        print(scheme, "-> uploaded id", parsed.get("id"), parsed.get("name"), parsed.get("size"))
        break
    except urllib.error.HTTPError as error:
        print(scheme, "-> HTTP", error.code, error.read().decode("utf-8", "replace")[:300])
    except Exception as error:
        print(scheme, "-> error", type(error).__name__, error)
