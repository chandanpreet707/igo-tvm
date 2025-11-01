# metrics/cc_aggregate.py
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path
import csv, re, os, sys

# --- stable working dir (repo root) ---
repo_root = Path(__file__).resolve().parents[1]
os.chdir(repo_root)

RAW_XML = Path("metrics/raw/pmd.xml")
if not RAW_XML.exists():
    sys.exit("ERROR: metrics/raw/pmd.xml not found. Copy it from target/pmd.xml first.")

OUT_DIR = Path("metrics/processed")
OUT_DIR.mkdir(parents=True, exist_ok=True)

# --- parse PMD XML ---
tree = ET.parse(RAW_XML)
root = tree.getroot()

# robust CC extraction
num_re = re.compile(r"\b(\d+)\b")
msg_re = re.compile(r"cyclomatic\s+complexity\s*(?:is|=)?\s*(\d+)", re.I)

method_rows = []

for file_el in root.findall(".//file"):
    file_path = file_el.get("name", "")
    # a/b/src/main/java/x/y/Z.java -> x.y.Z
    clazz = file_path.replace("\\", "/").split("/src/main/java/")[-1].removesuffix(".java").replace("/", ".")
    for v in file_el.findall("violation"):
        if v.get("rule") != "CyclomaticComplexity":
            continue

        # try multiple ways to get CC
        txt = (v.text or "").strip()
        cc = None

        # PMD 7 sometimes has numbers in the message
        m = msg_re.search(txt)
        if m: cc = int(m.group(1))
        if cc is None:
            # generic number fallback (last number usually is the CC)
            nums = [int(n) for n in num_re.findall(txt)]
            if nums:
                cc = nums[-1]

        # final extra fallback: check common attributes PMD may set
        for attr in ("metricvalue", "metricValue", "value", "violationValue"):
            if cc is None and v.get(attr) and v.get(attr).isdigit():
                cc = int(v.get(attr))

        # if still None, count it as 1 so we don't drop the method entirely
        if cc is None:
            cc = 1

        begin = int(v.get("beginline") or 0)
        end = int(v.get("endline") or begin)
        method_id = f"method@{begin}-{end}"

        method_rows.append((clazz, method_id, cc, begin, end, file_path, txt))

# --- write method-level CSV ---
with open(OUT_DIR / "cc-by-method.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["class","method","cc","beginline","endline","file","message"])
    w.writerows(method_rows)

# --- aggregate by class ---
agg = defaultdict(lambda: {"total":0, "max":0, "count":0, "over10":0, "over15":0})
for clazz, _, cc, *_ in method_rows:
    a = agg[clazz]
    a["total"] += cc
    a["count"] += 1
    a["max"] = max(a["max"], cc)
    if cc > 10: a["over10"] += 1
    if cc > 15: a["over15"] += 1

with open(OUT_DIR / "cc-by-class.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["class","total_cc","avg_cc","max_cc","methods","count_over_10","count_over_15"])
    for clazz, a in sorted(agg.items(), key=lambda kv: (-kv[1]["total"], kv[0])):
        avg = a["total"]/a["count"] if a["count"] else 0
        w.writerow([clazz, a["total"], f"{avg:.2f}", a["max"], a["count"], a["over10"], a["over15"]])

# --- summary file ---
grand_total = sum(a["total"] for a in agg.values())
methods_over10 = sum(a["over10"] for a in agg.values())
methods_over15 = sum(a["over15"] for a in agg.values())

top_methods = sorted(method_rows, key=lambda r: -r[2])[:10]
top_classes = sorted(agg.items(), key=lambda kv: -kv[1]["total"])[:10]

with open(OUT_DIR / "cc-summary.txt", "w") as f:
    f.write(f"Grand total cyclomatic complexity (sum of methods): {grand_total}\n")
    f.write(f"Total methods flagged by PMD: {len(method_rows)}\n")
    f.write(f"Methods with CC > 10: {methods_over10}\n")
    f.write(f"Methods with CC > 15: {methods_over15}\n\n")
    f.write("Top 10 methods by CC:\n")
    for clazz, method_id, cc, b, e, _, msg in top_methods:
        f.write(f"  {clazz} :: {method_id}  CC={cc}  [{b}-{e}]  {msg}\n")
    f.write("\nTop 10 classes by total CC:\n")
    for clazz, a in top_classes:
        f.write(f"  {clazz}  total={a['total']}  avg={a['total']/a['count']:.2f}  max={a['max']}  methods={a['count']}\n")

print(f"[ok] methods={len(method_rows)} classes={len(agg)} grand_total={grand_total}")
print("Wrote metrics/processed/cc-by-method.csv, cc-by-class.csv, cc-summary.txt")
