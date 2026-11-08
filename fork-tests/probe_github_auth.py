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


cred = credential()
secret = cred.get("password", "")
print("username:", cred.get("username"))
print("secret_length:", len(secret))
print("secret_prefix_kind:", "pat" if secret.startswith(("ghp_", "github_pat_")) else ("gho" if secret.startswith("gho_") else "other"))

for scheme in ("Bearer", "token"):
    request = urllib.request.Request(
        "https://api.github.com/user",
        headers={
            "Authorization": scheme + " " + secret,
            "Accept": "application/vnd.github+json",
            "User-Agent": "berrygram-release",
        },
    )
    try:
        with urllib.request.urlopen(request) as response:
            payload = json.loads(response.read().decode("utf-8"))
        print(scheme, "->", payload.get("login"))
    except urllib.error.HTTPError as error:
        print(scheme, "-> HTTP", error.code)
    except Exception as error:
        print(scheme, "-> error", type(error).__name__)
