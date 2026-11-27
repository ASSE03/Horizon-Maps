#!/usr/bin/env python3
"""
Fork FR : liste les textes affichés restés en anglais dans l'interface (après une fusion avec l'original, surtout).

Cherche, dans les fichiers Kotlin de l'appli, les textes passés directement aux éléments d'interface (Text, SToggle, SChoice,
SGroup, SItem, SNote, SSlider, SPercent, SButton, Pill, toast, raiseAlert, setTitle, addText…) qui ressemblent à de l'anglais.
Les textes passés par L.t(...) sont traduits à l'affichage : ils ne sont pas signalés.

Sortie : une ligne par texte suspect (fichier:ligne: texte). Sur GitHub Actions, chacun devient un avertissement visible dans
le résumé du build, et la liste est écrite dans le résumé. Le build n'échoue pas pour autant (code de sortie 0), sauf avec --strict.
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "kotlin")

# les éléments d'interface dont le premier texte (ou les textes de listOf(... to "...")) est montré tel quel
CALLS = r"(?:Text|SToggle\(prefs,|SChoice|SGroup|SItem|SNote|SSlider|SPercent|SButton|SFieldItem|Pill|ImmPill|SetSwitch\([^,]*,|SetChoice\([^,]*,|toast|raiseAlert\(\"[a-z_]+\",|setTitle|addText|setContentTitle|setContentText|speak|aspeak\(\"[a-z_]+\",)"
CALL_RE = re.compile(CALLS + r"\(\s*\"((?:[^\"\\$]|\\.)*)\"")
PAIR_RE = re.compile(r"\bto\s+\"((?:[^\"\\$]|\\.){3,})\"")
FOOTER_RE = re.compile(r"(?:footer|sub|placeholder)\s*=\s*\"((?:[^\"\\$]|\\.){6,})\"")

EN_WORDS = set("""
the you your when with and of on to is not only does show shows hide off turn turns keep keeps make makes this that these those
from for while before after more less than every never always once again stop stops start starts route routes map maps
speed limit limits camera cameras warning warnings sound sounds voice beep beeps button buttons screen car phone app settings
search find found nearby place places home work save saved delete open close closed update updated download downloaded
size bigger smaller larger largest closer closest further usual normal medium slow fast faster fastest shortest simplest
""".split())
FR_HINT = re.compile(r"[éèàçêùôîûëïœÉÈÀÇ«»’]|\b(le|la|les|des|du|un|une|et|est|pour|sur|dans|avec|vous|votre|vos|pas|plus|au|aux|en)\b", re.I)
ALLOW = {"Normal", "Google Maps", "Routes HD", "Home Assistant", "Mapbox", "TomTom", "OpenStreetMap", "Android Auto"}
SKIP = re.compile(r"^(https?:|[a-z_]+$|[A-Z0-9_]+$|#[0-9a-fA-F]+$|%|[\W\d]+$)")


def looks_english(t: str) -> bool:
    t = t.strip()
    if t in ALLOW or any(a in t for a in ("Google Maps", "Home Assistant")) or re.fullmatch(r"[a-z]+(-[a-z]+)+", t):
        return False
    if len(t) < 3 or SKIP.search(t) or FR_HINT.search(t):
        return False
    words = re.findall(r"[A-Za-z']+", t)
    if not words:
        return False
    if len(words) == 1:
        # un seul mot : seulement les mots anglais d'interface connus
        return words[0].lower() in EN_WORDS and words[0][0].isupper()
    hits = sum(1 for w in words if w.lower() in EN_WORDS)
    return hits >= 1


def scan():
    found = []
    for dp, _, fs in os.walk(ROOT):
        for f in fs:
            if not f.endswith(".kt") or f.startswith("I18n"):
                continue
            p = os.path.join(dp, f)
            rel = os.path.relpath(p, os.path.join(ROOT, "..", "..", "..", ".."))
            with open(p, encoding="utf-8") as fh:
                for n, line in enumerate(fh, 1):
                    s = line.strip()
                    if s.startswith("//") or s.startswith("*") or s.startswith("/*") or "L.t(" in line or "Log." in line or "LimitLog" in line:
                        continue
                    texts = [m.group(1) for m in CALL_RE.finditer(line)]
                    if re.search(CALLS + r"\(", line) or "listOf(" in line:
                        texts += [m.group(1) for m in PAIR_RE.finditer(line)]
                    texts += [m.group(1) for m in FOOTER_RE.finditer(line)]
                    for t in texts:
                        if looks_english(t):
                            found.append((rel, n, t))
    return found


def main():
    found = scan()
    gha = os.environ.get("GITHUB_ACTIONS") == "true"
    for rel, n, t in found:
        print(f"{rel}:{n}: {t}")
        if gha:
            print(f"::warning file={rel},line={n},title=Texte en anglais::{t}")
    summ = os.environ.get("GITHUB_STEP_SUMMARY")
    if summ:
        with open(summ, "a", encoding="utf-8") as fh:
            fh.write(f"### Textes en anglais dans l'interface : {len(found)}\n\n")
            for rel, n, t in found[:200]:
                fh.write(f"- `{rel}:{n}` — {t}\n")
            fh.write("\n")
    print(f"{len(found)} texte(s) en anglais", file=sys.stderr)
    return 1 if ("--strict" in sys.argv and found) else 0


if __name__ == "__main__":
    sys.exit(main())
