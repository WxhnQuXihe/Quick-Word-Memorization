"""Create a local manual audio override for one word.

Examples:
  python tools/audio/make_override.py --word abandon --file ~/Downloads/abandon.mp3
  python tools/audio/make_override.py --word-id 1 --file ~/Downloads/abandon.mp3
"""
from pathlib import Path
import argparse, csv, hashlib, re, shutil
from datetime import datetime

ROOT = Path(__file__).resolve().parents[2]
OVERRIDES = ROOT / "audio-overrides"
MANIFEST = OVERRIDES / "manifest.csv"
GENERATED_MANIFEST = ROOT / "_artifacts" / "tts" / "audio" / "manifest.csv"
FIELDS = ["word_id", "word", "accent", "voice", "file", "checksum", "updated_at"]

def slug(word: str) -> str:
    return re.sub(r"[^a-zA-Z0-9._-]+", "_", word).strip("_")[:48] or "word"

def load_rows():
    if not MANIFEST.exists(): return []
    with MANIFEST.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))

def find_word(word=None, word_id=None):
    rows = []
    if GENERATED_MANIFEST.exists():
        with GENERATED_MANIFEST.open(encoding="utf-8-sig") as f:
            rows = list(csv.DictReader(f))
    if word_id is not None:
        for r in rows:
            if int(r["word_id"]) == int(word_id): return int(r["word_id"]), r["word"]
        return int(word_id), word or str(word_id)
    if not word: raise SystemExit("请传 --word 或 --word-id")
    matches = [r for r in rows if r["word"].lower() == word.strip().lower()]
    if not matches: raise SystemExit(f"找不到单词 {word!r}，请改用 --word-id")
    if len(matches) > 1: raise SystemExit(f"{word!r} 匹配到多条，请改用 --word-id")
    return int(matches[0]["word_id"]), matches[0]["word"]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--word")
    ap.add_argument("--word-id", type=int)
    ap.add_argument("--file", required=True)
    ap.add_argument("--accent", default="us")
    ap.add_argument("--voice", default="af_heart")
    args = ap.parse_args()
    src = Path(args.file).expanduser()
    if not src.exists(): raise SystemExit(f"文件不存在: {src}")
    if not src.name.lower().endswith(".mp3"): raise SystemExit("只支持 MP3 文件")
    if src.stat().st_size > 5 * 1024 * 1024: raise SystemExit("MP3 不能超过 5MB")
    wid, word = find_word(args.word, args.word_id)
    OVERRIDES.mkdir(exist_ok=True)
    name = f"{wid}-{slug(word)}.mp3"
    dst = OVERRIDES / name
    shutil.copyfile(src, dst)
    checksum = hashlib.sha256(dst.read_bytes()).hexdigest()
    rows = [r for r in load_rows() if int(r["word_id"]) != wid]
    rows.append({"word_id": wid, "word": word, "accent": args.accent, "voice": args.voice,
                 "file": name, "checksum": checksum, "updated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")})
    rows.sort(key=lambda r: int(r["word_id"]))
    with MANIFEST.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS); w.writeheader(); w.writerows(rows)
    print(f"override: {dst}")
    print(f"word_id={wid} word={word} checksum={checksum}")
    print("\n下一步：")
    print("  git add audio-overrides")
    print(f"  git commit -m \"audio: 修正 {word}\"")
    print("  git push")

if __name__ == "__main__":
    main()
