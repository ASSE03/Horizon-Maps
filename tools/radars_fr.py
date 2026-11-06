#!/usr/bin/env python3
"""Fork FR : fichier des radars fixes de France pour Horizon Nav (sans dépendre d'Overpass depuis le téléphone).

Sources :
  1. OpenStreetMap (highway=speed_camera en France), via les serveurs Overpass, avec plusieurs essais ;
  2. la « Liste des radars fixes en France » du ministère de l'Intérieur (data.gouv.fr).
Les radars officiels absents d'OSM (à plus de 40 m d'un radar OSM) sont ajoutés.
Sortie : radars_fr.json = {"date": "...", "sources": {...}, "r": [[lat, lon, direction ou -1], ...]}
"""
import csv, io, json, math, sys, time, urllib.parse, urllib.request

UA = {"User-Agent": "HorizonNavFR-radars/1.0 (github.com/ASSE03/Horizon-Maps)"}
OVERPASS = ["https://overpass-api.de/api/interpreter", "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter", "https://maps.mail.ru/osm/tools/overpass/api/interpreter"]
DATASET = "https://www.data.gouv.fr/api/1/datasets/6712583387af110196942793/"


def get(url, data=None, timeout=200):
    req = urllib.request.Request(url, data=data, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read()


def osm():
    q = '[out:json][timeout:180];area["ISO3166-1"="FR"][admin_level=2]->.fr;node(area.fr)["highway"="speed_camera"];out;'
    body = urllib.parse.urlencode({"data": q}).encode()
    for attempt in range(3):
        for ep in OVERPASS:
            try:
                els = json.loads(get(ep, body))["elements"]
                if len(els) > 1000:
                    print(f"OSM : {len(els)} radars par {ep}")
                    out = []
                    for e in els:
                        d = (e.get("tags") or {}).get("direction", "")
                        try: dv = int(float(d)) % 360
                        except ValueError: dv = -1
                        out.append([round(e["lat"], 6), round(e["lon"], 6), dv])
                    return out, ep
                print(f"OSM : {ep} n'a renvoyé que {len(els)} radars")
            except Exception as ex:
                print(f"OSM : {ep} : {ex}")
        time.sleep(60)
    return [], "échec"


def official():
    try:
        meta = json.loads(get(DATASET, timeout=60))
        res = [r for r in meta.get("resources", []) if (r.get("format") or "").lower() == "csv"]
        res.sort(key=lambda r: r.get("last_modified") or "", reverse=True)
        if not res: return [], "aucun csv"
        url = res[0]["url"]
        raw = get(url, timeout=120).decode("utf-8-sig", errors="replace")
        sep = ";" if raw.count(";") > raw.count(",") else ","
        rows = list(csv.DictReader(io.StringIO(raw), delimiter=sep))
        if not rows: return [], "csv vide"
        keys = list(rows[0].keys())
        la = next(k for k in keys if "lat" in k.lower())
        lo = next(k for k in keys if "lon" in k.lower() or "lng" in k.lower())
        out = []
        for r in rows:
            try:
                a = float(str(r[la]).replace(",", ".")); o = float(str(r[lo]).replace(",", "."))
                if 41 < a < 52 and -6 < o < 10: out.append([round(a, 6), round(o, 6), -1])
            except (ValueError, TypeError):
                pass
        print(f"Officiel : {len(out)} radars ({url}, colonnes {la}/{lo})")
        return out, res[0].get("last_modified", "")[:10]
    except Exception as ex:
        print(f"Officiel : {ex}")
        return [], "échec"


def main():
    o, osrc = osm()
    g, gsrc = official()
    # grille de 0,001° pour retrouver vite les voisins
    grid = {}
    for p in o:
        grid.setdefault((int(p[0] * 1000), int(p[1] * 1000)), []).append(p)
    added = 0
    for p in g:
        ky, kx = int(p[0] * 1000), int(p[1] * 1000)
        near = False
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                for q in grid.get((ky + dy, kx + dx), []):
                    dlat = (p[0] - q[0]) * 111320; dlon = (p[1] - q[1]) * 111320 * math.cos(math.radians(p[0]))
                    if math.hypot(dlat, dlon) < 40: near = True
        if not near:
            o.append(p); grid.setdefault((ky, kx), []).append(p); added += 1
    if len(o) < 1500:
        print(f"Trop peu de radars ({len(o)}) : fichier non publié"); sys.exit(1)
    out = {"date": time.strftime("%Y-%m-%d"), "sources": {"osm": osrc, "officiel": gsrc, "ajoutés": added}, "r": o}
    with open("radars_fr.json", "w") as f:
        json.dump(out, f, separators=(",", ":"))
    print(f"Total : {len(o)} radars (dont {added} officiels absents d'OSM)")


if __name__ == "__main__":
    main()
