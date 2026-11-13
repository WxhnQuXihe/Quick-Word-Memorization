#!/usr/bin/env python3
"""把三本词书源文件转成 Shortand 导入模板。

源文件优先使用同目录下的 .xlsx（序号/单词/音标/释义 或 序号/词组/释义），
也保留 PDF 入口的占位：当前先处理已整理好的 Excel。

用法：
  python tools/build_wordbooks.py
输出：
  wordbooks/四级大纲词汇_导入.xlsx
  wordbooks/六级大纲词汇_导入.xlsx
  wordbooks/四六级常用词组_导入.xlsx
  wordbooks/词书导入模板.xlsx
"""
from pathlib import Path
import re
from openpyxl import Workbook, load_workbook
from openpyxl.styles import Font, PatternFill, Alignment

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "wordbooks"
OUT.mkdir(exist_ok=True)

HEADERS = ["序号", "单词", "英式音标", "美式音标", "词性", "释义", "英文例句", "例句翻译"]
POS_RE = re.compile(
    r"(?<![A-Za-z])(v|n|adj|adv|prep|conj|pron|num|art|int|vt|vi|aux|abbr)\.\s*",
    re.IGNORECASE,
)


def split_senses(meaning: str):
    """把 'v. 放弃…n. 放任…' 拆成 [(pos, text), ...]。"""
    meaning = (meaning or "").strip()
    if not meaning:
        return []
    matches = list(POS_RE.finditer(meaning))
    if not matches:
        return [("", meaning)]
    result = []
    if matches[0].start() > 0:
        prefix = meaning[: matches[0].start()].strip(" ；;，,")
        if prefix:
            result.append(("", prefix))
    for idx, match in enumerate(matches):
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(meaning)
        text = meaning[match.end():end].strip(" ；;，,")
        if text:
            result.append((match.group(1).lower() + ".", text))
    return result


def write_book(rows, output: Path):
    wb = Workbook()
    ws = wb.active
    ws.title = "词书"
    ws.append(HEADERS)
    for cell in ws[1]:
        cell.font = Font(bold=True)
        cell.fill = PatternFill("solid", fgColor="EEF2FF")
        cell.alignment = Alignment(vertical="center")
    for row in rows:
        ws.append(row)
    widths = [8, 22, 18, 18, 8, 60, 28, 28]
    for i, width in enumerate(widths, 1):
        ws.column_dimensions[ws.cell(row=1, column=i).column_letter].width = width
    ws.freeze_panes = "A2"
    wb.save(output)


def build_from_xlsx(source: Path, output: Path, phrase: bool = False):
    wb = load_workbook(source, read_only=True)
    ws = wb.active
    seen = set()
    rows = []
    duplicate = 0
    total = 0
    for index, raw in enumerate(ws.iter_rows(min_row=2, values_only=True), start=1):
        total += 1
        if phrase:
            seq, word, meaning = (raw + (None, None, None))[:3]
            phonetic_uk = phonetic_us = pos = ""
            examples = ("", "")
        else:
            seq, word, phonetic_uk, meaning = (raw + (None, None, None, None))[:4]
            phonetic_us = ""
            examples = ("", "")
        word = str(word or "").strip()
        if not word:
            continue
        key = word.lower()
        if key in seen:
            duplicate += 1
            continue
        seen.add(key)
        senses = split_senses(str(meaning or ""))
        meaning_cell = "\n".join(f"{p} {t}".strip() for p, t in senses) if senses else ""
        pos_cell = senses[0][0] if senses else ""
        rows.append([seq or index, word, phonetic_uk or "", phonetic_us or "", pos_cell,
                     meaning_cell, examples[0], examples[1]])
    write_book(rows, output)
    print(f"{source.name}: 源 {total} 行，去重 {duplicate}，输出 {len(rows)} 词 -> {output.name}")
    return len(rows)


def write_template():
    wb = Workbook()
    ws = wb.active
    ws.title = "词书导入模板"
    ws.append(HEADERS)
    for cell in ws[1]:
        cell.font = Font(bold=True)
        cell.fill = PatternFill("solid", fgColor="EEF2FF")
    ws.append([1, "abandon", "/əˈbændən/", "/əˈbændən/", "v.", "v. 放弃；遗弃；离开\nn. 放任；狂热", "", ""])
    for i, width in enumerate([8, 22, 18, 18, 8, 60, 28, 28], 1):
        ws.column_dimensions[ws.cell(row=1, column=i).column_letter].width = width
    ws.freeze_panes = "A2"
    wb.save(OUT / "词书导入模板.xlsx")


if __name__ == "__main__":
    write_template()
    build_from_xlsx(ROOT / "wordbooks/source/四级大纲词汇.xlsx", OUT / "四级大纲词汇_导入.xlsx")
    build_from_xlsx(ROOT / "wordbooks/source/六级大纲词汇.xlsx", OUT / "六级大纲词汇_导入.xlsx")
    build_from_xlsx(ROOT / "wordbooks/source/四六级常用词组.xlsx", OUT / "四六级常用词组_导入.xlsx", phrase=True)
