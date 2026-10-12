import concurrent.futures
import json
import hashlib

from prepare_demo_images import ROOT, OUTPUT, download


def main():
    sources = json.loads((ROOT / "scripts/banner-image-sources.json").read_text(encoding="utf-8"))
    previous_file = OUTPUT / "banner-download-results.json"
    previous = json.loads(previous_file.read_text(encoding="utf-8")) if previous_file.exists() else []
    cache = {item["name"]: item for item in previous if "errors" not in item}
    def prepare(item):
        existing = cache.get(item["name"])
        if existing and existing["sourceImage"] in [candidate["url"] for candidate in item["candidates"]]:
            image = ROOT / "web/public" / existing["file"].lstrip("/")
            if image.exists() and hashlib.sha256(image.read_bytes()).hexdigest() == existing["sha256"]:
                return existing
        return download(item)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        results = list(executor.map(prepare, sources))
    failures = [result for result in results if "errors" in result]
    (OUTPUT / "banner-download-results.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    if not failures:
        manifest = json.loads((OUTPUT / "manifest.json").read_text(encoding="utf-8"))
        products = [item for item in manifest if "id" in item]
        (OUTPUT / "manifest.json").write_text(json.dumps(products+results, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"saved": len(results)-len(failures), "failures": failures}, ensure_ascii=True))
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
