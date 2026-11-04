import csv, io, re, time, urllib.parse as up
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

S = requests.Session()
S.headers.update({"User-Agent": "Mozilla/5.0 (compatible; logo-fetcher/2.0; Wisdom Tower Academy)"})
OUT = Path("logos")
OUT.mkdir(exist_ok=True)
GOOD = 300  # min side (px) considered good enough to stop searching
GENERIC = {"logo", "seal", "emblem", "badge", "university", "of", "the", "and", "png",
           "svg", "jpg", "jpeg", "file", "new", "official", "en", "college", "coat", "arms"}


def slug(n):
    return re.sub(r"[^a-z0-9]+", "-", n.lower()).strip("-")


def get(url, **kw):
    kw.setdefault("timeout", 30)
    r = S.get(url, **kw)
    r.raise_for_status()
    return r


def to_image(content):
    try:
        im = Image.open(io.BytesIO(content))
        im.load()
        return im.convert("RGBA")
    except Exception:
        return None


def words(s):
    return set(re.findall(r"[a-z]+", s.lower()))


def commons_candidates(name):
    keys = words(name) - GENERIC
    out = []
    for q in (f"{name} logo", f"{name} emblem seal"):
        r = get("https://commons.wikimedia.org/w/api.php", params={
            "action": "query", "list": "search", "srsearch": q,
            "srnamespace": 6, "srlimit": 8, "format": "json"}).json()
        for hit in r.get("query", {}).get("search", []):
            title = hit["title"][5:]
            toks = words(title)
            if not keys <= toks:
                continue
            if toks - keys - GENERIC:
                continue  # title mentions other words -> probably another institution
            if not re.search(r"logo|seal|emblem|badge|coat", title, re.I):
                continue
            url = ("https://commons.wikimedia.org/wiki/Special:FilePath/"
                   + up.quote(title) + "?width=512")
            if url not in out:
                out.append(url)
    return out


def find_site(name):
    q = f"{name} Ethiopia official website"
    urls = []
    try:
        h = get("https://html.duckduckgo.com/html/", params={"q": q}).text
        urls += [up.unquote(m) for m in re.findall(r"uddg=([^&\"']+)", h)]
    except Exception as e:
        print("   ddg failed:", e)
    try:
        h = get("https://www.bing.com/search", params={"q": q}).text
        urls += re.findall(r'<h2><a href="(https?://[^"]+)"', h)
    except Exception as e:
        print("   bing failed:", e)
    for u in urls:
        host = up.urlparse(u).netloc.lower()
        if host.endswith(".edu.et"):
            return f"https://{host}/"
    return None


def site_candidates(base):
    html = get(base).text
    cands = []
    for m in re.finditer(r"<img\b[^>]*>", html, re.I):
        tag = m.group(0)
        if re.search(r"logo", tag, re.I):
            s = re.search(r"""(?:data-src|src)=["']([^"']+)""", tag, re.I)
            if s:
                cands.append(up.urljoin(base, s.group(1)))
    for m in re.finditer(r"<meta\b[^>]*>", html, re.I):
        if re.search(r"og:image", m.group(0), re.I):
            s = re.search(r"""content=["']([^"']+)""", m.group(0), re.I)
            if s:
                cands.append(up.urljoin(base, s.group(1)))
    for m in re.finditer(r"<link\b[^>]*>", html, re.I):
        if re.search(r"icon", m.group(0), re.I):
            s = re.search(r"""href=["']([^"']+)""", m.group(0), re.I)
            if s:
                cands.append(up.urljoin(base, s.group(1)))
    seen, out = set(), []
    for c in cands:
        if c not in seen and not c.lower().split("?")[0].endswith(".svg"):
            seen.add(c)
            out.append(c)
    return out


def best_of(urls, label, best, limit=6):
    for url in urls[:limit]:
        try:
            im = to_image(get(url, timeout=40).content)
        except Exception:
            continue
        if im is None:
            continue
        w, h = im.size
        if min(w, h) < 48 or max(w, h) / min(w, h) > 5:
            continue
        if best is None or w * h > best["w"] * best["h"]:
            best = {"im": im, "w": w, "h": h, "src": label, "url": url}
        if min(best["w"], best["h"]) >= GOOD:
            break
    return best


rows, missing = [], []
for i, name in enumerate(NAMES, 1):
    best = None
    try:
        best = best_of(commons_candidates(name), "commons", best, limit=3)
    except Exception as e:
        print(f"   commons failed for {name}: {e}")
    if best is None or min(best["w"], best["h"]) < GOOD:
        try:
            base = find_site(name)
            if base:
                best = best_of(site_candidates(base), "site:" + base, best)
        except Exception as e:
            print(f"   site failed for {name}: {e}")
    if best is None:
        missing.append(name)
        print(f"MISS {i:02d} {name}")
        continue
    im = best["im"]
    im.thumbnail((512, 512))
    fn = slug(name) + ".png"
    im.save(OUT / fn, "PNG")
    flag = "LOW-RES" if max(best["w"], best["h"]) < 200 else ""
    rows.append([i, name, fn, best["src"], best["w"], best["h"], flag, best["url"]])
    print(f"OK   {i:02d} {name} ({best['src']}, {best['w']}x{best['h']}) {flag}")
    time.sleep(0.5)

with open("sources.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["#", "university", "file", "source", "width", "height", "flag", "url"])
    w.writerows(rows)
Path("missing.txt").write_text("\n".join(missing), encoding="utf-8")
print(f"\nDone: {len(rows)} found, {len(missing)} missing")
