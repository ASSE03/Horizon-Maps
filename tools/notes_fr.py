#!/usr/bin/env python3
"""Notes de version d'Horizon Nav FR : la section « Non publié » de CHANGELOG.fr.md, moins les lignes déjà parues dans les
versions précédentes (fichier passé en 2e argument). Écrit les notes sur la sortie standard ; rien si aucune ligne nouvelle."""
import sys, re

chg = open(sys.argv[1], encoding="utf-8").read()
old = open(sys.argv[2], encoding="utf-8").read() if len(sys.argv) > 2 else ""
m = re.search(r"^## Non publié\s*$(.*?)(?=^## |\Z)", chg, re.S | re.M)
if not m:
    sys.exit(0)

def norm(t):
    return re.sub(r"\s+", " ", t.strip().lstrip("-*• ").strip()).lower()

seen = {norm(l) for l in old.splitlines() if l.strip().startswith(("-", "*", "•"))}
out, head, pend = [], None, []
for line in m.group(1).splitlines():
    if line.startswith("### "):
        if pend:
            out += ([head] if head else []) + pend
        head, pend = line.strip(), []
    elif line.strip().startswith("- "):
        if norm(line) not in seen:
            pend.append(line.rstrip())
    elif line.strip() and pend:
        pend[-1] += " " + line.strip()
if pend:
    out += ([head] if head else []) + pend
if any(l.startswith("- ") for l in out):
    print("\n".join(out))
