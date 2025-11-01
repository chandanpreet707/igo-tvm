import pandas as pd, matplotlib.pyplot as plt
from pathlib import Path
out = Path("poster"); out.mkdir(parents=True, exist_ok=True)

dfm = pd.read_csv("metrics/processed/cc-by-method.csv")
dft = dfm.sort_values("cc", ascending=False).head(10)
plt.figure(); plt.barh(range(len(dft)), dft["cc"])
plt.yticks(range(len(dft)), dft["class"] + " • " + dft["method"])
plt.gca().invert_yaxis(); plt.title("Top 10 Methods by Cyclomatic Complexity")
plt.tight_layout(); plt.savefig(out/"fig-top10-method-cc.png"); plt.close()

dfc = pd.read_csv("metrics/processed/cc-by-class.csv")
dftc = dfc.sort_values("total_cc", ascending=False).head(10)
plt.figure(); plt.barh(range(len(dftc)), dftc["total_cc"])
plt.yticks(range(len(dftc)), dftc["class"])
plt.gca().invert_yaxis(); plt.title("Top 10 Classes by Total Cyclomatic Complexity")
plt.tight_layout(); plt.savefig(out/"fig-top10-class-totalcc.png"); plt.close()

print("Saved poster/fig-top10-method-cc.png and poster/fig-top10-class-totalcc.png")
