#!/usr/bin/env python3
"""把 OCR 结果整理成高考词汇/词组两本导入 Excel。

OCR 原始数据在 _ocr_pages/rows2.tsv（page, y, word, phonetic, meaning, number）。
这里只做清洗、词形纠正、按 3859 拆分和输出，不重新跑 OCR。
"""
from pathlib import Path
import re
from difflib import get_close_matches
from openpyxl import Workbook, load_workbook
from openpyxl.styles import Font, PatternFill

ROOT = Path(__file__).resolve().parent.parent
ROWS = ROOT / "wordbooks" / "source" / "gaokao_ocr_rows.tsv"
OUT = ROOT / "wordbooks"
HEADERS = ["序号", "单词", "英式音标", "美式音标", "词性", "释义", "英文例句", "例句翻译"]
POS_RE = re.compile(r"(?<![A-Za-z])(v|n|adj|adv|prep|conj|pron|num|art|int|vt|vi|aux|abbr|a|ad|re)\.?\s*", re.I)

def split_senses(text):
    text=(text or "").strip()
    if not text: return [("", "")]
    ms=list(POS_RE.finditer(text))
    if not ms:
        return [("", text)]
    out=[]
    if ms[0].start()>0:
        pre=text[:ms[0].start()].strip(" ；;，,.")
        if pre: out.append(("", pre))
    for i,m in enumerate(ms):
        end=ms[i+1].start() if i+1<len(ms) else len(text)
        body=text[m.end():end].strip(" ；;，,.")
        pos={"a":"adj.","ad":"adv.","re":"prep."}.get(m.group(1).lower(), m.group(1).lower()+".")
        if body: out.append((pos, body))
    return out or [("", text)]

def load_known():
    known={}
    for fn in ["四级大纲词汇_导入.xlsx","六级大纲词汇_导入.xlsx"]:
        p=OUT/fn
        if not p.exists(): continue
        wb=load_workbook(p,read_only=True); ws=wb.active
        for row in ws.iter_rows(min_row=2,values_only=True):
            w=str(row[1] or "").strip()
            if w and w.lower() not in known:
                known[w.lower()]=(w, row[2] or "", row[5] or "")
    return known

def canonical(word, known, phrase=False):
    raw=re.sub(r"\s+"," ",str(word or "")).strip()
    raw=raw.strip(" .,;，。；：")
    if raw.lower()=="a(an)": raw="a (an)"
    if not raw: return "", "", ""
    key=raw.lower()
    if key in known: return known[key][0], known[key][1], known[key][2]
    alt=raw.replace("0","o").replace("1","l").replace("5","s").replace("|","l")
    alt=alt.lower()
    if alt in known: return known[alt][0], known[alt][1], known[alt][2]
    if phrase:
        fix={
            "a blt(of)":"a bit (of)", "a palr of":"a pair of", "a plece of":"a piece of",
            "abit(of)":"a bit (of)", "afew":"a few", "agreatdeal":"a great deal",
            "a good/greatmany":"a good/great many", "akindof":"a kind of",
            "alittle":"a little", "alotof":"a lot of", "anumberof":"a number of",
            "apairof":"a pair of", "apieceof":"a piece of"
        }
        if key in fix: return fix[key], "", ""
        nospace=re.sub(r"\s+","",raw).lower()
        if nospace in PHRASES_NOSPACE: return PHRASES_NOSPACE[nospace], "", ""
        if key in PHRASES: return PHRASES[key][0], "", ""
        return raw, "", ""
    if " " not in raw and len(raw)>=4:
        cand=get_close_matches(alt, list(known.keys()), n=1, cutoff=0.90)
        if cand:
            w,ph,m=known[cand[0]]
            if w and w.lower()[0]==alt[0] and abs(len(w)-len(raw))<=2:
                return w,ph,m
    return raw, "", ""

def write_book(rows, path):
    wb=Workbook(); ws=wb.active; ws.title="词书"; ws.append(HEADERS)
    for c in ws[1]: c.font=Font(bold=True); c.fill=PatternFill("solid",fgColor="EEF2FF")
    for r in rows: ws.append(r)
    for i,w in enumerate([8,24,18,18,8,65,20,20],1): ws.column_dimensions[ws.cell(1,i).column_letter].width=w
    ws.freeze_panes="A2"; wb.save(path)

def main():
    known=load_known()
    global PHRASES, PHRASES_NOSPACE
    PHRASES={}
    PHRASES_NOSPACE={}
    pp=OUT/"四六级常用词组_导入.xlsx"
    if pp.exists():
        wb=load_workbook(pp,read_only=True); ws=wb.active
        for row in ws.iter_rows(min_row=2,values_only=True):
            w=str(row[1] or "").strip()
            if w:
                PHRASES[w.lower()]=(w,row[2] or "",row[5] or "")
                PHRASES_NOSPACE[re.sub(r"\s+","",w).lower()]=w
    raw=[line.split("\t") for line in ROWS.read_text(encoding="utf-8").splitlines()]
    split=None
    for i,r in enumerate(raw):
        if len(r)>=6 and r[5].strip()=="3859":
            split=i; break
    if split is None: raise SystemExit("cannot find 3859")
    word_rows=[]; phrase_rows=[]; seen=set()
    for i,r in enumerate(raw):
        if len(r)<6: continue
        page,y,word,phonetic,meaning,num=r[:6]
        word=word.strip()
        if not word: continue
        is_phrase = i>=split
        w,ph,known_meaning=canonical(word,known,phrase=is_phrase)
        if not w: continue
        key=w.lower()
        if key in seen: continue
        seen.add(key)
        senses=split_senses(meaning)
        if not meaning.strip() and known_meaning: senses=split_senses(str(known_meaning))
        mcell="\n".join((p+" "+t).strip() for p,t in senses).strip()
        pos=senses[0][0] if senses else ""
        row=[len(word_rows)+1 if not is_phrase else len(phrase_rows)+1,w,ph or "","",pos,mcell,"",""]
        (phrase_rows if is_phrase else word_rows).append(row)
    write_book(word_rows, OUT/"高考精编词汇_导入.xlsx")
    write_book(phrase_rows, OUT/"高考精编词组_导入.xlsx")
    print("words",len(word_rows),"phrases",len(phrase_rows),"split_row",split)

if __name__=="__main__": main()
