"""Create a SQL upsert file from the generated audio manifest."""
from pathlib import Path
import csv

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "_artifacts" / "tts" / "audio" / "manifest.csv"
OUT = ROOT / "_artifacts" / "tts" / "audio-import.sql"

def q(s):
    return "'" + str(s).replace("\\", "\\\\").replace("'", "\\'") + "'"

rows = list(csv.DictReader(MANIFEST.open(encoding="utf-8-sig")))
with OUT.open("w", encoding="utf-8") as f:
    for r in rows:
        f.write(
            "INSERT INTO word_audio "
            "(word_id,accent,voice,object_key,checksum,duration_ms,size_bytes,model,model_version,status) VALUES ("
            f"{int(r['word_id'])},{q(r['accent'])},{q(r['voice'])},{q(r['object_key'])},{q(r['checksum'])},"
            f"{int(r['duration_ms'])},{int(r['size_bytes'])},{q(r['model'])},{q(r['model_version'])},2) "
            "ON DUPLICATE KEY UPDATE object_key=VALUES(object_key),checksum=VALUES(checksum),"
            "duration_ms=VALUES(duration_ms),size_bytes=VALUES(size_bytes),model=VALUES(model),"
            "model_version=VALUES(model_version),status=2;\n"
        )
print(f"rows={len(rows)} sql={OUT} size={OUT.stat().st_size}")
