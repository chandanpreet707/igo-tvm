from pathlib import Path
import csv

RAW_CLASS = Path("metrics/raw/ck_class.csv")
OUT_DIR = Path("metrics/processed"); OUT_DIR.mkdir(parents=True, exist_ok=True)

rows = []
with RAW_CLASS.open() as f:
    r = csv.DictReader(f)
    for row in r:
        cls  = row.get("class") or row.get("Class") or ""
        def to_int(x):
            try: return int(float(x))
            except: return 0
        def to_float(x):
            try: return float(x)
            except: return 0.0

        wmc  = to_int(row.get("wmc")  or row.get("WMC"))
        lcom = to_float(row.get("lcom") or row.get("LCOM"))
        cbo  = to_int(row.get("cbo")  or row.get("CBO"))
        nom  = to_int(row.get("nom")  or row.get("NOM"))
        rows.append((cls, wmc, lcom, cbo, nom))

rows_sorted = sorted(rows, key=lambda t: (-t[1], -t[2], -t[3], t[0]))
with (OUT_DIR / "p8-class-metrics.csv").open("w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["class","wmc","lcom*","cbo","nom"])
    w.writerows(rows_sorted)

N = len(rows)
sum_cbo = sum(r[3] for r in rows)
cf = (sum_cbo / (N * (N - 1))) if N > 1 else 0.0

with (OUT_DIR / "p8-summary.txt").open("w") as f:
    f.write(f"Classes analyzed (N): {N}\n")
    f.write(f"Sum of CBO across classes: {sum_cbo}\n")
    f.write(f"Coupling Factor (CF) = Sum(CBO) / (N*(N-1)) = {sum_cbo} / ({N}*{N-1}) = {cf:.4f}\n")

print("Wrote metrics/processed/p8-class-metrics.csv and p8-summary.txt")
