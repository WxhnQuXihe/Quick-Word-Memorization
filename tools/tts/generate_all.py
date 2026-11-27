"""Generate word audio for every active word in words.tsv with Kokoro.

Usage:
  python tools/tts/generate_all.py --limit 30
  python tools/tts/generate_all.py --resume
"""
from pathlib import Path
import argparse, csv, hashlib, re, subprocess, time
import numpy as np
import soundfile as sf
import imageio_ffmpeg
from kokoro_onnx import Kokoro

ROOT = Path(__file__).resolve().parents[2]
MODEL_DIR = ROOT / "_artifacts" / "tts" / "kokoro"
OUT = ROOT / "_artifacts" / "tts" / "audio"
OUT.mkdir(parents=True, exist_ok=True)
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
VOICE = "af_heart"
ACCENT = "us"
MODEL = "kokoro-82m"
MODEL_VERSION = "v1.0"
RAW = np.fromfile(MODEL_DIR / "af_heart.bin", dtype=np.float32).reshape(510, 1, 256)
DUMMY = MODEL_DIR / "voices_dummy.npy"
np.save(DUMMY, RAW[:2])
KOKORO = Kokoro(str(MODEL_DIR / "model.onnx"), str(DUMMY))

def tts_text(word: str) -> str:
    s = word.strip()
    s = s.replace("a/an", "a or an").replace("A/an", "a or an")
    s = s.replace("(", " ").replace(")", " ")
    s = s.replace("/", " or ")
    s = re.sub(r"\s+", " ", s).strip(" .,;:，。；：")
    return s or word

def key_for(text: str) -> str:
    return hashlib.sha256(f"{text}|{ACCENT}|{VOICE}|{MODEL_VERSION}".encode()).hexdigest()

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--resume", action="store_true")
    ap.add_argument("--input", default=str(ROOT / "_artifacts" / "tts" / "words.tsv"))
    args = ap.parse_args()
    words = []
    for line in Path(args.input).read_text(encoding="utf-8").splitlines():
        if "\t" not in line: continue
        wid, word = line.split("\t", 1)
        words.append((int(wid), word))
    if args.limit: words = words[:args.limit]
    manifest = OUT / "manifest.csv"
    done = set()
    if args.resume and manifest.exists():
        with manifest.open(encoding="utf-8-sig") as f:
            for r in csv.DictReader(f): done.add(int(r["word_id"]))
    rows = []
    if manifest.exists() and args.resume:
        with manifest.open(encoding="utf-8-sig") as f: rows = list(csv.DictReader(f))
    t0 = time.time()
    for i, (wid, word) in enumerate(words, 1):
        txt = tts_text(word); checksum = key_for(txt)
        rel = f"{checksum[:2]}/{checksum}.mp3"
        mp3 = OUT / rel; mp3.parent.mkdir(parents=True, exist_ok=True)
        if wid in done and mp3.exists(): continue
        wav = OUT / f".{wid}.wav"
        try:
            audio, sr = KOKORO.create(txt, voice=RAW, speed=1.0, lang="en-us")
            sf.write(wav, audio, sr)
            subprocess.run([FFMPEG, "-y", "-i", str(wav), "-codec:a", "libmp3lame",
                            "-b:a", "64k", "-ac", "1", str(mp3)],
                           check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            rows.append({"word_id": wid, "word": word, "tts_text": txt, "accent": ACCENT,
                         "voice": VOICE, "checksum": checksum, "object_key": rel,
                         "duration_ms": int(len(audio)/sr*1000), "size_bytes": mp3.stat().st_size,
                         "model": MODEL, "model_version": MODEL_VERSION})
        except Exception as e:
            print(f"FAIL {wid} {word!r}: {e}", flush=True)
        finally:
            if wav.exists(): wav.unlink()
        if i % 100 == 0:
            with manifest.open("w", encoding="utf-8-sig", newline="") as f:
                w = csv.DictWriter(f, fieldnames=list(rows[0].keys())); w.writeheader(); w.writerows(rows)
            print(f"{i}/{len(words)} {word} elapsed={time.time()-t0:.0f}s", flush=True)
    if rows:
        with manifest.open("w", encoding="utf-8-sig", newline="") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys())); w.writeheader(); w.writerows(rows)
    print(f"DONE {len(rows)} rows, {time.time()-t0:.0f}s", flush=True)

if __name__ == "__main__":
    main()
