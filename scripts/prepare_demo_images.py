import concurrent.futures
import hashlib
import io
import json
from pathlib import Path
import urllib.request

from PIL import Image, ImageOps


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "web/public/images"
Image.MAX_IMAGE_PIXELS = 25000000


def download(item):
    target = OUTPUT / ("goods/%03d.webp" % item["id"] if "id" in item else "banners/" + item["name"] + ".webp")
    target.parent.mkdir(parents=True, exist_ok=True)
    errors = []
    for candidate in item["candidates"]:
        try:
            request = urllib.request.Request(candidate["url"], headers={"User-Agent": "Mozilla/5.0", "Accept": "image/*"})
            with urllib.request.urlopen(request, timeout=10) as response:
                content = response.read(15000000)
            with Image.open(io.BytesIO(content)) as original:
                original.load()
                if original.width < 120 or original.height < 120:
                    raise ValueError("image is too small")
                photo = ImageOps.exif_transpose(original).convert("RGBA")
            if "id" in item:
                photo.thumbnail((740, 550))
                result = Image.new("RGB", (800, 600), "#ffffff")
                result.paste(photo, ((800-photo.width)//2, (600-photo.height)//2), photo)
            else:
                result = ImageOps.fit(photo.convert("RGB"), (1600, 700), centering=(0.5, 0.5))
            result.save(target, "WEBP", quality=87, method=5)
            return {**{key: value for key, value in item.items() if key != "candidates"},
                    "file": "/" + target.relative_to(ROOT / "web/public").as_posix(),
                    "source": candidate.get("source", candidate["url"]), "sourceImage": candidate["url"],
                    "sha256": hashlib.sha256(target.read_bytes()).hexdigest(), "width": result.width, "height": result.height}
        except Exception as error:
            errors.append(type(error).__name__ + ": " + str(error)[:120])
    return {"id": item.get("id"), "title": item["title"], "errors": errors}


def main():
    products = json.loads((ROOT / "scripts/demo-image-sources.json").read_text(encoding="utf-8"))
    banners = json.loads((ROOT / "scripts/banner-image-sources.json").read_text(encoding="utf-8"))
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        results = list(executor.map(download, products + banners))
    (OUTPUT / "manifest.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    failures = [result for result in results if "errors" in result]
    print(json.dumps({"saved": len(results)-len(failures), "failures": failures}, ensure_ascii=True))
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
