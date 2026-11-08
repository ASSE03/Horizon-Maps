#!/usr/bin/env python3
"""Fork FR : fichier des radars fixes de France pour Horizon Nav (sans dépendre d'Overpass depuis le téléphone).

Sources :
  1. OpenStreetMap (highway=speed_camera en France), via les serveurs Overpass, avec plusieurs essais ;
  2. la « Liste des radars fixes en France » du ministère de l'Intérieur (data.gouv.fr).
  3. Lufop (lufop.net, CC BY-SA 4.0), avec la clé du secret LUFOP_KEY : azimut, sens du flash, radars de chantier.
La liste officielle fait foi ; Lufop, puis OpenStreetMap, donnent la direction.
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


LUFOP = "https://api.lufop.net/api"


def lufop(refs):
    """Fork FR : les radars de Lufop (lufop.net, CC BY-SA 4.0) : azimut, sens du flash (F devant, B derrière, D les deux) et
    radars de chantier. Offre gratuite : 200 requêtes par jour, 10 par minute, 200 radars par réponse ; la France est donc
    parcourue par cercles d'environ 80 km, redécoupés quand une réponse est pleine."""
    import os
    key = os.environ.get("LUFOP_KEY", "").strip()
    if not key: print("Lufop : pas de clé (secret LUFOP_KEY)"); return [], "pas de clé"
    seen = {}; calls = [0]

    def ask(lat, lon, km):
        if calls[0] >= 185: return None
        calls[0] += 1
        q = urllib.parse.urlencode({"key": key, "format": "json", "pays": "FR", "nbr": "200", "q": f"{lat:.4f},{lon:.4f}", "m": str(int(math.ceil(km * 10)))})
        for attempt in range(2):
            try:
                time.sleep(6.5)
                d = json.loads(get(f"{LUFOP}?{q}", timeout=60))
                if d is None: return []
                if isinstance(d, list): return d
                print("Lufop : réponse", str(d)[:200]); return None
            except Exception as ex:
                print(f"Lufop : {ex}"); time.sleep(10)
        return None

    def cover(lat, lon, km, depth):
        rs = ask(lat, lon, km)
        if rs is None: return
        for x in rs:
            try:
                k = (round(float(x["lat"]), 5), round(float(x["lng"]), 5)); seen[k] = x
            except (KeyError, ValueError, TypeError): pass
        if len(rs) >= 195 and depth < 2:
            h = km / 2.0; dl = h / 111.0; dn = h / (111.0 * math.cos(math.radians(lat)))
            for a, b in ((dl, dn), (dl, -dn), (-dl, dn), (-dl, -dn)): cover(lat + a, lon + b, h * 1.15, depth + 1)

    # un cercle seulement là où la liste officielle a des radars (pas en mer)
    cells = sorted({(int((p[0] - 41.0) // 1.0), int((p[1] + 5.5) // 1.3)) for p in refs})
    for cy, cx in cells:
        cover(41.0 + cy + 0.5, -5.5 + cx * 1.3 + 0.65, 80.0, 0)
    out = list(seen.values())
    print(f"Lufop : {len(out)} radars en {calls[0]} requêtes ; exemple : {out[:1]}")
    return out, f"{len(out)} radars"


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
                if 41 < a < 52 and -6 < o < 10: out.append([round(a, 6), round(o, 6), -1, v if 20 <= v <= 130 else 0, both, t])
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
    L, lsrc = lufop(g)
    if lsrc != "pas de clé" and len(L) < 3000:
        # quota du jour épuisé ou Lufop en panne : l'ancien fichier (avec ses directions) est gardé plutôt qu'un fichier appauvri
        print(f"Lufop n'a renvoyé que {len(L)} radars : fichier non publié"); sys.exit(1)
    lgrid = {}
    for x in L:
        try:
            q = [float(x["lat"]), float(x["lng"])]
            az = x.get("azimut"); fl = (x.get("flash") or "").strip().upper()[:1]
            azv = float(az) % 360.0 if az not in (None, "", "-") else -1.0
            v = x.get("vitesse"); v = int(float(v)) if v not in (None, "", "-") else 0
            q += [azv, fl, v, (x.get("name") or "")]
            lgrid.setdefault((int(q[0] * 1000), int(q[1] * 1000)), []).append(q)
        except (KeyError, ValueError, TypeError):
            pass

    def near(gr, p, lim):
        best = None; bd = lim
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                for q in gr.get((int(p[0] * 1000) + dy, int(p[1] * 1000) + dx), []):
                    dd = math.hypot((p[0] - q[0]) * 111320, (p[1] - q[1]) * 111320 * math.cos(math.radians(p[0])))
                    if dd < bd: bd = dd; best = q
        return best

    # l'azimut de Lufop est-il le sens de circulation contrôlé, ou l'inverse ? On le compare aux directions d'OpenStreetMap
    agree = [0, 0]
    for p in g:
        q = near(grid, p, 60.0); l = near(lgrid, p, 60.0)
        if q is not None and l is not None and q[2] >= 0 and l[2] >= 0 and l[3] != "D":
            dd = abs((q[2] - l[2] + 540.0) % 360.0 - 180.0)
            if dd < 50: agree[0] += 1
            elif dd > 130: agree[1] += 1
    flip = agree[1] > agree[0] * 2
    print(f"Lufop/OSM : même sens {agree[0]}, sens opposé {agree[1]} : azimut {'inversé' if flip else 'gardé'}")

    out = []; withdir = 0; fromL = 0; used = set()
    for p in g:
        l = near(lgrid, p, 60.0)
        q = near(grid, p, 60.0)
        d = -1
        if l is not None:
            used.add(id(l))
            if l[3] == "D": d = -1
            elif l[2] >= 0: d = (l[2] + (180.0 if flip else 0.0)) % 360.0; fromL += 1
            elif q is not None and not p[4]: d = q[2]
        elif q is not None and not p[4]:
            d = q[2]
        if d >= 0: withdir += 1
        out.append([p[0], p[1], round(d, 1) if d >= 0 else -1, p[3] or (l[4] if l is not None and 20 <= l[4] <= 130 else 0)])
    # les radars de chantier de Lufop (absents de la liste officielle)
    works = 0
    for cell in lgrid.values():
        for l in cell:
            if id(l) in used or "chantier" not in l[5].lower(): continue
            d = -1 if l[3] == "D" or l[2] < 0 else (l[2] + (180.0 if flip else 0.0)) % 360.0
            out.append([round(l[0], 6), round(l[1], 6), round(d, 1) if d >= 0 else -1, l[4] if 20 <= l[4] <= 130 else 0]); works += 1
    print(f"Lufop : {fromL} directions, {works} radars de chantier ajoutés")
    # contrôle : ce qu'OpenStreetMap a en plus (radars retirés, ou trop récents pour la liste) n'est pas repris
    near = [q for q in o if abs(q[0] - 45.3155) < 0.01 and abs(q[1] - 4.533) < 0.01]
    print("OSM près de La Versanne :", near)
    out_ = {"date": time.strftime("%Y-%m-%d"), "sources": {"officiel": gsrc, "osm (directions)": osrc, "lufop": lsrc, "chantiers": works, "avec direction": withdir}, "r": out}
    with open("radars_fr.json", "w") as f:
        json.dump(out_, f, separators=(",", ":"))
    print(f"Total : {len(out)} radars, dont {withdir} avec une direction")


if __name__ == "__main__":
    main()
