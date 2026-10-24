"""Run real HTTP checks against three Java processes and real MySQL/Redis.

Requires empty, isolated MARKET_DB_URL / NOTICE_DB_URL schemas on the first run.
--compose checks the Compose stack, including infrastructure restart behavior.
"""
import argparse
import concurrent.futures
import http.cookiejar
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]


class Browser:
    def __init__(self, base="http://127.0.0.1:18080"):
        self.base = base
        self.jar = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.csrf = None

    def call(self, method, path, body=None, expected=200, headers=None):
        headers = dict(headers or {})
        if method != "GET" and self.csrf:
            headers.setdefault("X-CSRF-Token", self.csrf)
        if body is not None:
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self.base + path, data=None if body is None else json.dumps(body).encode(), headers=headers, method=method)
        try:
            response = self.http.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        raw = response.read()
        assert response.code == expected, (method, path, response.code, raw[:250])
        try:
            result = json.loads(raw) if raw else {}
        except ValueError:
            result = {}
        return result.get("data", result)

    def login(self, username):
        self.csrf = self.call("GET", "/api/auth/csrf")["token"]
        self.call("POST", "/api/auth/login", {"username": username, "password": "admin123" if username == "admin" else "123456"})
        self.csrf = self.call("GET", "/api/auth/csrf")["token"]
        return self


def wait_for(action, timeout=60):
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        try:
            value = action()
            if value:
                return value
        except (OSError, AssertionError, ValueError) as error:
            last = error
        time.sleep(0.5)
    raise AssertionError(f"Service did not become ready: {last}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--compose", action="store_true")
    parser.add_argument("--keep-running", action="store_true")
    args = parser.parse_args()
    logs = ROOT / "tmp" / "smoke"
    logs.mkdir(parents=True, exist_ok=True)
    processes = {}
    env = os.environ.copy()
    env.setdefault("SPRING_PROFILES_ACTIVE", "demo")
    env.setdefault("NOTICE_INTERNAL_TOKEN", "campus-test-internal-token-at-least-32-characters")
    java = str(Path(env["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java")) if "JAVA_HOME" in env else "java"

    def start(name, module, artifact, extra=None):
        process_env = {**env, **(extra or {})}
        with (logs / (name + ".log")).open("ab") as out:
            processes[name] = subprocess.Popen([java, "-jar", str(ROOT / module / "target" / (artifact + "-1.0.0-SNAPSHOT.jar"))], cwd=ROOT / module, env=process_env, stdout=out, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)

    def notice_up():
        if args.compose:
            subprocess.run(["docker", "compose", "start", "notice"], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
        else:
            start("notice", "notice-service", "notice-service", {"SPRING_PROFILES_ACTIVE": "default"})

    def notice_down():
        if args.compose:
            subprocess.run(["docker", "compose", "stop", "notice"], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
        else:
            processes["notice"].terminate()
            processes["notice"].wait(15)

    checks = []
    succeeded = False
    try:
        if not args.compose:
            notice_up()
            start("market", "server", "market-service")
            start("gateway", "gateway", "gateway", {"SPRING_PROFILES_ACTIVE": "default"})
        anonymous = Browser()
        wait_for(lambda: anonymous.call("GET", "/api/health"))
        seller, buyer, other, admin = [wait_for(lambda name=name: Browser().login(name)) for name in ("zhou", "student", "chen", "admin")]
        anonymous.call("GET", "/api/admin/users", expected=401, headers={"X-User-Id": "2", "X-User-Role": "ADMIN"})
        buyer.call("GET", "/api/admin/users", expected=403, headers={"X-User-Role": "ADMIN"})
        anonymous.call("GET", "/internal/users/1/notifications", expected=404)
        saved = buyer.csrf
        buyer.csrf = None
        buyer.call("POST", "/api/reports", {}, expected=403)
        buyer.csrf = saved
        checks.append("gateway, forged identity, CSRF and internal route isolation")

        if not args.compose:
            start("market2", "server", "market-service", {"SERVER_PORT": "18086", "OUTBOX_ENABLED": "false"})
            second = Browser("http://127.0.0.1:18086")
            second.jar = buyer.jar
            second.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(second.jar))
            assert wait_for(lambda: second.call("GET", "/api/auth/me"))["username"] == "student"
            checks.append("two market processes share the same Redis Session")

        def good():
            return seller.call("POST", "/api/goods", {"title": "HTTP闭环 " + uuid.uuid4().hex[:8], "price": 12.34, "category": "教材书籍", "conditionLevel": "九成新", "location": "图书馆", "description": "真实网关与独立通知进程测试"}, expected=201)["id"]

        gid = good()
        key = uuid.uuid4().hex
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as pool:
            results = list(pool.map(lambda _: buyer.call("POST", f"/api/orders?goodsId={gid}&requestKey={key}"), range(20)))
        assert len({row["id"] for row in results}) == 1
        order = results[0]
        buyer.call("POST", f"/api/orders?goodsId={good()}&requestKey={key}", expected=409)
        seller.call("PATCH", f"/api/orders/{order['id']}/confirm")
        buyer.call("PATCH", f"/api/orders/{order['id']}/complete")
        buyer.call("PATCH", f"/api/orders/{order['id']}/cancel", expected=409)
        assert seller.call("GET", f"/api/goods/{gid}")["status"] == "SOLD"
        checks.append("20 identical requests, payload conflict, seller confirmation and buyer completion")

        wait_for(lambda: seller.call("GET", "/api/notifications")["total"] > 0)
        notices = seller.call("GET", "/api/notifications")["records"]
        notice_id = notices[0]["id"]
        buyer.call("PUT", f"/api/notifications/{notice_id}/read", expected=404)
        seller.call("PUT", f"/api/notifications/{notice_id}/read")
        seller.call("PUT", f"/api/notifications/{notice_id}/read")
        checks.append("real OpenFeign delivery, recipient isolation and idempotent read")

        notice_down()
        outage_gid = good()
        before = time.monotonic()
        outage_order = buyer.call("POST", f"/api/orders?goodsId={outage_gid}&requestKey={uuid.uuid4().hex}")
        elapsed = time.monotonic() - before
        assert elapsed < 3, elapsed
        seller.call("GET", "/api/notifications", expected=503)
        notice_up()
        wait_for(lambda: any(row["businessId"] == outage_order["id"] and row["type"] == "ORDER_CREATED" for row in seller.call("GET", "/api/notifications?size=100")["records"]), timeout=60)
        checks.append("notice outage does not block transactions; persisted Outbox resumes delivery")

        if args.compose:
            total = anonymous.call("GET", "/api/goods")["total"]
            subprocess.run(["docker", "compose", "restart", "mysql"], cwd=ROOT, check=True)
            wait_for(lambda: anonymous.call("GET", "/api/goods")["total"] == total)
            subprocess.run(["docker", "compose", "stop", "redis"], cwd=ROOT, check=True)
            buyer.call("GET", "/api/auth/me", expected=503)
            subprocess.run(["docker", "compose", "start", "redis"], cwd=ROOT, check=True)
            checks.append("Compose MySQL restart preserves rows; Redis failure returns 503")
        else:
            start("market-bad-redis", "server", "market-service", {"SERVER_PORT": "18087", "REDIS_PORT": "1", "OUTBOX_ENABLED": "false"})
            broken = Browser("http://127.0.0.1:18087")
            wait_for(lambda: broken.call("GET", "/api/health"))
            broken.jar = buyer.jar
            broken.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(broken.jar))
            broken.call("GET", "/api/auth/me", expected=503)
            checks.append("Redis Session failure returns 503 without anonymous fallback")

        report = {"passed": len(checks), "failed": 0, "checks": checks, "outageOrderSeconds": round(elapsed, 3), "compose": args.compose}
        (logs / "result.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(report, ensure_ascii=False))
        succeeded = True
        if args.keep_running:
            (logs / "pids.json").write_text(json.dumps({name: p.pid for name, p in processes.items()}), encoding="utf-8")
    finally:
        if not args.keep_running or not succeeded:
            for process in processes.values():
                if process.poll() is None:
                    process.terminate()
            for process in processes.values():
                try:
                    process.wait(15)
                except subprocess.TimeoutExpired:
                    process.kill()


if __name__ == "__main__":
    main()
