#!/usr/bin/env python3
"""Fork FR : fichier des radars fixes de France pour Horizon Nav (sans dépendre d'Overpass depuis le téléphone).

Sources :
  1. OpenStreetMap (highway=speed_camera en France), via les serveurs Overpass, avec plusieurs essais ;
  2. la « Liste des radars fixes en France » du ministère de l'Intérieur (data.gouv.fr).
La liste officielle fait foi ; OpenStreetMap ne donne que la direction des radars qu'il connaît.
Sortie : radars_fr.json = {"date": "...", "sources": {...}, "r": [[lat, lon, direction ou -1, vitesse contrôlée ou 0], ...]}
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
        vk = next((k for k in keys if "vma" in k.lower() or "vitesse" in k.lower()), None)
        tk = next((k for k in keys if "type" in k.lower()), None)
        types = {}
        for r in rows:
            t = (r.get(tk) or "").strip() if tk else ""; types[t] = types.get(t, 0) + 1
        print("Types :", types)
        out = []
        for r in rows:
            try:
                a = float(str(r[la]).replace(",", ".")); o = float(str(r[lo]).replace(",", "."))
                try: v = int(float(str(r.get(vk) or "0").replace(",", "."))) if vk else 0
                except ValueError: v = 0
                t = (r.get(tk) or "").lower() if tk else ""
                # fork FR : tourelles (ETT) et radars « double sens » peuvent contrôler les deux sens : jamais de direction pour eux
                both = any(w in t for w in ("double", "deux sens", "bidirection", "tourelle", "ett"))
                if 41 < a < 52 and -6 < o < 10: out.append([round(a, 6), round(o, 6), -1, v if 20 <= v <= 130 else 0, both])
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
    if len(g) < 1500:
        print(f"Liste officielle trop courte ({len(g)}) : fichier non publié"); sys.exit(1)
    # Fork FR : la liste officielle du ministère fait foi (un radar retiré disparaît de cette liste, alors qu'il reste souvent des
    # années dans OpenStreetMap : radar annoncé à la sortie de La Versanne, retiré depuis 3 ans). OpenStreetMap ne sert plus qu'à
    # donner la direction d'un radar officiel quand il le connaît à moins de 60 m.
    grid = {}
    for q in o:
        grid.setdefault((int(q[0] * 1000), int(q[1] * 1000)), []).append(q)
    out = []; withdir = 0
    for p in g:
        ky, kx = int(p[0] * 1000), int(p[1] * 1000)
        best = None; bd = 60.0
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                for q in grid.get((ky + dy, kx + dx), []):
                    dlat = (p[0] - q[0]) * 111320; dlon = (p[1] - q[1]) * 111320 * math.cos(math.radians(p[0]))
                    dd = math.hypot(dlat, dlon)
                    if dd < bd: bd = dd; best = q
        d = best[2] if best is not None and not p[4] else -1
        if d >= 0: withdir += 1
        out.append([p[0], p[1], d, p[3]])
    # contrôle : ce qu'OpenStreetMap a en plus (radars retirés, ou trop récents pour la liste) n'est pas repris
    near = [q for q in o if abs(q[0] - 45.3155) < 0.01 and abs(q[1] - 4.533) < 0.01]
    print("OSM près de La Versanne :", near)
    out_ = {"date": time.strftime("%Y-%m-%d"), "sources": {"officiel": gsrc, "osm (directions)": osrc, "avec direction": withdir}, "r": out}
    with open("radars_fr.json", "w") as f:
        json.dump(out_, f, separators=(",", ":"))
    print(f"Total : {len(out)} radars officiels, dont {withdir} avec la direction d'OpenStreetMap")


if __name__ == "__main__":
    main()
