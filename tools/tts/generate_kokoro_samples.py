"""Generate a small Kokoro TTS sample set as MP3."""
from pathlib import Path
import csv, subprocess, zipfile
import numpy as np
import soundfile as sf
import imageio_ffmpeg
from kokoro_onnx import Kokoro

ROOT = Path(__file__).resolve().parents[2]
MODEL_DIR = ROOT / "_artifacts" / "tts" / "kokoro"
OUT = ROOT / "_artifacts" / "tts" / "samples-kokoro"
OUT.mkdir(parents=True, exist_ok=True)
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()

WORDS = [
    "abandon", "ability", "abnormal", "abolish", "abrupt",
    "absolute", "absorb", "abstract", "academic", "accelerate",
    "access", "accommodate", "accompany", "accomplish", "accurate",
    "accuse", "achieve", "acknowledge", "acquire", "adapt",
    "read", "lead", "present", "record", "object",
    "a bit (of)", "a few", "a great deal", "a kind of", "a lot of",
]

# The HF individual voice file is raw float32, shape (510, 256).
raw = np.fromfile(MODEL_DIR / "af_heart.bin", dtype=np.float32).reshape(510, 1, 256)
dummy = MODEL_DIR / "voices_dummy.npy"
np.save(dummy, raw[:2])
kokoro = Kokoro(str(MODEL_DIR / "model.onnx"), str(dummy))

rows = []
for i, word in enumerate(WORDS, 1):
    wav = OUT / f"{i:02d}.wav"
    mp3 = OUT / f"{i:02d}_{word.replace(' ', '_').replace('/', '-')}.mp3"
    audio, sr = kokoro.create(word, voice=raw, speed=1.0, lang="en-us")
    sf.write(wav, audio, sr)
    subprocess.run([FFMPEG, "-y", "-i", str(wav), "-codec:a", "libmp3lame",
                    "-b:a", "64k", "-ac", "1", str(mp3)],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    wav.unlink()
    rows.append({"index": i, "word": word, "voice": "af_heart", "duration_sec": round(len(audio)/sr, 3),
                 "file": mp3.name, "size_bytes": mp3.stat().st_size})
    print(f"{i:02d} {word} {len(audio)/sr:.2f}s", flush=True)

with (OUT / "manifest.csv").open("w", encoding="utf-8-sig", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
    writer.writeheader(); writer.writerows(rows)

html = """<!doctype html><meta charset="utf-8"><title>Kokoro samples</title>
<style>body{font-family:system-ui;max-width:760px;margin:30px auto;padding:0 16px}h1{font-size:22px}
.row{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid #eee}
.word{width:170px;font-weight:600}audio{height:34px}</style>
<h1>Kokoro 30 samples · af_heart · US English</h1>
"""
for r in rows:
    html += f'<div class="row"><span class="word">{r["word"]}</span><audio controls preload="none" src="{r["file"]}"></audio><small>{r["duration_sec"]}s</small></div>\n'
(OUT / "samples.html").write_text(html, encoding="utf-8")

zip_path = OUT.parent / "kokoro-samples-30.zip"
with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
    for p in OUT.iterdir():
        if p.is_file():
            z.write(p, p.name)
print("zip", zip_path, zip_path.stat().st_size)
