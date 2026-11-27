"""Upload changed manual overrides to the Shorthand admin API.

Used by .github/workflows/audio-overrides.yml. Only files listed in --changed
are uploaded; the server writes them to /opt/shorthand/audio/manual and updates
word_audio through the admin API.
"""
from pathlib import Path
import argparse, csv, os, sys
import requests

BASE = os.environ.get("API_BASE", "http://47.95.66.191:8082").rstrip("/")
USER = os.environ["ADMIN_USERNAME"]
PASS = os.environ["ADMIN_PASSWORD"]
ROOT = Path(__file__).resolve().parents[2]
DIR = ROOT / "audio-overrides"
MANIFEST = DIR / "manifest.csv"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--changed", default="", help="comma/newline separated paths")
    args = ap.parse_args()
    changed = {Path(x.strip()).name for x in args.changed.replace("\n", ",").split(",") if x.strip()}
    if not MANIFEST.exists():
        print("no manifest"); return
    with MANIFEST.open(encoding="utf-8-sig") as f:
        rows = list(csv.DictReader(f))
    login = requests.post(f"{BASE}/api/admin/auth/login", json={"username": USER, "password": PASS}, timeout=30)
    login.raise_for_status()
    token = login.json()["data"]["token"]
    headers = {"Authorization": f"Bearer {token}"}
    count = 0
    for r in rows:
        if changed and r["file"] not in changed:
            continue
        path = DIR / r["file"]
        if not path.exists():
            print("missing", path); continue
        with path.open("rb") as f:
            mime = "audio/wav" if path.suffix.lower() == ".wav" else "audio/mpeg"
            res = requests.post(f"{BASE}/api/admin/words/{r['word_id']}/audio",
                                headers=headers,
                                files={"file": (path.name, f, mime)},
                                timeout=120)
        res.raise_for_status()
        body = res.json()
        if body.get("code") != 0: raise RuntimeError(body)
        print("uploaded", r["word_id"], r["word"], body["data"]["audioUrl"])
        count += 1
    print("done", count)

if __name__ == "__main__":
    main()
