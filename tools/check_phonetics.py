#!/usr/bin/env python3
"""生成音标校对清单：空音标 + 四级/六级同词音标不一致。"""
from pathlib import Path
import csv
from openpyxl import load_workbook

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "wordbooks" / "音标校对清单.csv"


def read(path, word_col, phonetic_col):
    wb = load_workbook(path, read_only=True)
    ws = wb.active
    data = {}
    for row in ws.iter_rows(min_row=2, values_only=True):
        word = str(row[word_col] or "").strip()
        phonetic = str(row[phonetic_col] or "").strip()
        if word:
            data.setdefault(word.lower(), (word, phonetic))
    return data


four = read(ROOT / "wordbooks/source/四级大纲词汇.xlsx", 1, 2)
six = read(ROOT / "wordbooks/source/六级大纲词汇.xlsx", 1, 2)

rows = []
for key in sorted(set(four) & set(six)):
    a = four[key][1]
    b = six[key][1]
    if a and b and a != b:
        rows.append([four[key][0], a, b, "四级/六级音标不一致"])
for key, (word, p) in sorted(four.items()):
    if not p:
        rows.append([word, "", "", "四级音标为空"])
for key, (word, p) in sorted(six.items()):
    if not p:
        rows.append([word, "", "", "六级音标为空"])

with OUT.open("w", encoding="utf-8-sig", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(["单词", "四级音标", "六级音标", "问题"])
    writer.writerows(rows)
print(f"common={len(set(four) & set(six))} issues={len(rows)} -> {OUT}")
