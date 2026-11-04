import csv, io, re, time, urllib.parse
from pathlib import Path
import requests
from PIL import Image

NAMES = """Adama Science and Technology University
Addis Ababa Science and Technology University
Addis Ababa University
Adigrat University
Ambo University
Arba Minch University
Arsi University
Assosa University
Aksum University
Bahir Dar University
Bonga University
Borana University
Bule Hora University
Dambi Dollo University
Debark University
Debre Berhan University
Debre Markos University
Debre Tabor University
Defence University
Dilla University
Dire Dawa University
Ethiopian Civil Service University
Ethiopian Police University College
Gambella University
Haramaya University
Hawassa University
Injibara University
Jigjiga University
Jimma University
Jinka University
Kebri Dehar University
Kotebe Metropolitan University
Madda Walabu University
Mekdela Amba University
Mekelle University
Mettu University
Mizan-Tepi University
Oda Bultum University
Oromia State University
Raya University
Salale University
Semera University
University of Gondar
Wachemo University
Werabe University
Wolaita Sodo University
Woldia University
Wolkite University
Wollega University
Wollo University""".splitlines()

H = {"User-Agent": "logo-fetcher/1.0 (educational app; Wisdom Tower Academy)"}
WD = "https://www.wikidata.org/w/api.php"
WP = "https://en.wikipedia.org/w/api.php"
OUT = Path("logos")
OUT.mkdir(exist_ok=True)


def slug(n):
    return re.sub(r"[^a-z0-9]+", "-", n.lower()).strip("-")


def wikidata_logo(name):
    r = requests.get(WD, headers=H, timeout=30, params={
        "action": "wbsearchentities", "search": name, "language": "en",
        "format": "json", "limit": 3}).json()
    for hit in r.get("search", []):
        c = requests.get(WD, headers=H, timeout=30, params={
            "action": "wbgetclaims", "entity": hit["id"], "property": "P154",
            "format": "json"}).json().get("claims", {}).get("P154")
        if c:
            f = c[0]["mainsnak"]["datavalue"]["value"]
            return ("https://commons.wikimedia.org/wiki/Special:FilePath/"
                    + urllib.parse.quote(f) + "?width=512"), "wikidata:" + hit["id"]
    return None, None


def wikipedia_image(name):
    r = requests.get(WP, headers=H, timeout=30, params={
        "action": "query", "titles": name, "prop": "pageimages",
        "piprop": "thumbnail", "pithumbsize": 512, "redirects": 1,
        "format": "json"}).json()
    for p in r.get("query", {}).get("pages", {}).values():
        if "thumbnail" in p:
            return p["thumbnail"]["source"], "wikipedia"
    return None, None


def save_png(url, path):
    resp = requests.get(url, headers=H, timeout=60)
    resp.raise_for_status()
    img = Image.open(io.BytesIO(resp.content)).convert("RGBA")
    img.save(path, "PNG")


rows, missing = [], []
for i, name in enumerate(NAMES, 1):
    path = OUT / f"{slug(name)}.png"
    done = False
    for finder in (wikidata_logo, wikipedia_image):
        try:
            url, src = finder(name)
            if url:
                save_png(url, path)
                rows.append([i, name, path.name, src, url])
                print(f"OK   {i:02d} {name} ({src})")
                done = True
                break
        except Exception as e:
            print(f"ERR  {name}: {e}")
        time.sleep(0.3)
    if not done:
        missing.append(name)
        print(f"MISS {i:02d} {name}")

with open("sources.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["#", "university", "file", "source", "url"])
    w.writerows(rows)
Path("missing.txt").write_text("\n".join(missing), encoding="utf-8")
print(f"\nDone: {len(rows)} found, {len(missing)} missing")
