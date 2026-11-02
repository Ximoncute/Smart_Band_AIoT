from __future__ import annotations

import json
import math
import os
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse, parse_qs
from urllib.parse import urlencode
from urllib.request import Request, urlopen
import auth

ROOT = Path(__file__).resolve().parent
DATA = ROOT / "data"
DATA.mkdir(exist_ok=True)
TELEMETRY = DATA / "telemetry.jsonl"
ALERTS = DATA / "alerts.jsonl"
STOPS = DATA / "stops.jsonl"
LOCK = threading.Lock()
STOP_CANDIDATES = {}
LAST_ALERT_EVENT = {}


def distance_m(a, b):
    if not a or not b:
        return 0.0
    r = 6371000.0
    p1, p2 = math.radians(a["lat"]), math.radians(b["lat"])
    dp = math.radians(b["lat"] - a["lat"])
    dl = math.radians(b["lon"] - a["lon"])
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(h))


def read_jsonl(path, limit=500):
    if not path.exists():
        return []
    with LOCK, path.open(encoding="utf-8") as f:
        rows = [json.loads(line) for line in f if line.strip()]
    return rows[-limit:]


def reverse_place(point):
    query = urlencode({"lat": point["lat"], "lon": point["lon"], "format": "jsonv2", "zoom": 18})
    try:
        request = Request("https://nominatim.openstreetmap.org/reverse?" + query,
                          headers={"User-Agent": "SmartbandIoT/1.0 (device stop log)"})
        with urlopen(request, timeout=3) as response:
            return json.load(response).get("display_name") or "Chưa xác định tên điểm dừng"
    except Exception:
        return "Chưa xác định tên điểm dừng"


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def send_json(self, status, value):
        body = json.dumps(value, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type,Authorization")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_json(204, {})

    def do_POST(self):
        path = urlparse(self.path).path
        if path.startswith("/api/v1/auth/") or path.startswith("/api/v1/admin/"):
            try:
                size = int(self.headers.get("Content-Length", 0))
                data = json.loads(self.rfile.read(size))
                if path == "/api/v1/auth/request-otp":
                    status, value = auth.request_otp(data.get("email", ""), data.get("purpose", "register"))
                elif path == "/api/v1/auth/register":
                    status, value = auth.register(data.get("email", ""), data.get("name", ""), data.get("password", ""), data.get("otp", ""))
                elif path == "/api/v1/auth/login":
                    status, value = auth.login(data.get("email", ""), data.get("password", ""))
                elif path == "/api/v1/auth/reset-password":
                    email = data.get("email", "").strip().lower()
                    password = data.get("password", "")
                    if len(password) < 6:
                        return self.send_json(400, {"error": "password_too_short"})
                    if not auth.verify_otp(email, "reset", data.get("otp", "")):
                        return self.send_json(403, {"error": "invalid_otp"})
                    auth.users.update_one({"email": email}, {"$set": {"password_hash": auth.hash_password(password)}})
                    auth.sessions.delete_many({"email": email})
                    status, value = 200, {"ok": True}
                elif path == "/api/v1/auth/change-password":
                    user = auth.authenticated(self.headers.get("Authorization", "").removeprefix("Bearer "))
                    if not user:
                        return self.send_json(401, {"error": "unauthorized"})
                    if not auth.verify_password(data.get("old_password", ""), user["password_hash"]):
                        return self.send_json(401, {"error": "invalid_password"})
                    if len(data.get("new_password", "")) < 6:
                        return self.send_json(400, {"error": "password_too_short"})
                    auth.users.update_one({"email": user["email"]}, {"$set": {"password_hash": auth.hash_password(data["new_password"])}})
                    status, value = 200, {"ok": True}
                elif path == "/api/v1/auth/profile":
                    user = auth.authenticated(self.headers.get("Authorization", "").removeprefix("Bearer "))
                    if not user:
                        return self.send_json(401, {"error": "unauthorized"})
                    auth.users.update_one({"email": user["email"]}, {"$set": {"profile": data.get("profile", {})}})
                    status, value = 200, {"ok": True}
                elif path == "/api/v1/admin/user":
                    if not auth.admin_authorized(self.headers.get("Authorization", "").removeprefix("Bearer ")):
                        return self.send_json(403, {"error": "forbidden"})
                    status, value = auth.admin_update(data.get("email", ""), data)
                else:
                    return self.send_json(404, {"error": "not_found"})
                return self.send_json(status, value)
            except Exception as exc:
                return self.send_json(503, {"error": str(exc)})
        if path != "/api/v1/telemetry":
            return self.send_json(404, {"error": "not_found"})
        try:
            size = int(self.headers.get("Content-Length", 0))
            item = json.loads(self.rfile.read(size))
            item["received_at"] = datetime.now(timezone.utc).isoformat()
            item.setdefault("device_id", "smartband-001")
            with LOCK, TELEMETRY.open("a", encoding="utf-8") as f:
                f.write(json.dumps(item, ensure_ascii=False) + "\n")
            gps = item.get("gps") or {}
            device = item["device_id"]
            if gps.get("valid") and gps.get("lat") is not None and gps.get("lon") is not None:
                point = {"lat": gps["lat"], "lon": gps["lon"]}
                candidate = STOP_CANDIDATES.get(device)
                now = datetime.now(timezone.utc)
                if candidate and (now - candidate["last_seen"]).total_seconds() > 10:
                    candidate = None
                    STOP_CANDIDATES.pop(device, None)
                moving = float(gps.get("speed_kmh") or 0) >= 2.0
                if candidate and distance_m(candidate["point"], point) >= 20:
                    moving = True
                if moving:
                    STOP_CANDIDATES.pop(device, None)
                else:
                    if candidate is None:
                        candidate = {"point": point, "since": now, "last_seen": now, "logged": False}
                        STOP_CANDIDATES[device] = candidate
                    candidate["last_seen"] = now
                    if not candidate["logged"] and (now - candidate["since"]).total_seconds() >= 180:
                        event = {"device_id": device, "gps": candidate["point"],
                                 "place_name": reverse_place(candidate["point"]),
                                 "stopped_at": candidate["since"].isoformat(),
                                 "received_at": item["received_at"], "duration_seconds": 180}
                        with LOCK, STOPS.open("a", encoding="utf-8") as f:
                            f.write(json.dumps(event, ensure_ascii=False) + "\n")
                        candidate["logged"] = True
            else:
                STOP_CANDIDATES.pop(device, None)
            alert_type = item.get("alert_type") or ("FALL" if item.get("state") == "EMERGENCY" else "")
            alert_id = (device, alert_type, item.get("alert_event_ms"))
            if alert_type and alert_id != LAST_ALERT_EVENT.get(device):
                with LOCK, ALERTS.open("a", encoding="utf-8") as f:
                    f.write(json.dumps(item, ensure_ascii=False) + "\n")
                LAST_ALERT_EVENT[device] = alert_id
            self.send_json(201, {"ok": True})
        except Exception as exc:
            self.send_json(400, {"ok": False, "error": str(exc)})

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/admin":
            body = (ROOT / "admin.html").read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            return self.wfile.write(body)
        if path in {"/api/v1/auth/me", "/api/v1/admin/users", "/api/v1/admin/user-data"}:
            token = self.headers.get("Authorization", "").removeprefix("Bearer ")
            try:
                if path == "/api/v1/admin/users":
                    return self.send_json(200, auth.admin_users()) if auth.admin_authorized(token) else self.send_json(403, {"error": "forbidden"})
                if path == "/api/v1/admin/user-data":
                    if not auth.admin_authorized(token):
                        return self.send_json(403, {"error": "forbidden"})
                    email = parse_qs(urlparse(self.path).query).get("email", [""])[0]
                    user = auth.users.find_one({"email": email.lower()})
                    if not user:
                        return self.send_json(404, {"error": "user_not_found"})
                    device = user.get("device_id")
                    return self.send_json(200, {"user": auth.public_user(user),
                                                "telemetry": [r for r in read_jsonl(TELEMETRY, 100) if r.get("device_id") == device],
                                                "alerts": [r for r in read_jsonl(ALERTS, 100) if r.get("device_id") == device],
                                                "stops": [r for r in read_jsonl(STOPS, 100) if r.get("device_id") == device]})
                user = auth.authenticated(token)
                return self.send_json(200, {"user": auth.public_user(user)}) if user else self.send_json(401, {"error": "unauthorized"})
            except Exception as exc:
                return self.send_json(503, {"error": str(exc)})
        rows = read_jsonl(TELEMETRY)
        if path.startswith("/api/v1/devices/") and path.endswith("/snapshot"):
            device = path.split("/")[4]
            current = next((x for x in reversed(rows) if x.get("device_id") == device), None)
            received = datetime.fromisoformat(current["received_at"]) if current else None
            fresh = received is not None and (datetime.now(timezone.utc) - received).total_seconds() < 10
            return self.send_json(200, {"device_id": device, "connected": fresh, "latest": current})
        if path == "/api/v1/route":
            points = [x for x in rows if x.get("gps", {}).get("valid")]
            total = sum(distance_m(points[i - 1]["gps"], points[i]["gps"]) for i in range(1, len(points)))
            return self.send_json(200, {"points": points, "distance_m": round(total, 1)})
        if path == "/api/v1/alerts":
            return self.send_json(200, {"alerts": read_jsonl(ALERTS)})
        if path == "/api/v1/stops":
            return self.send_json(200, {"stops": read_jsonl(STOPS)})
        if path == "/health":
            return self.send_json(200, {"ok": True})
        self.send_json(404, {"error": "not_found"})


if __name__ == "__main__":
    host = os.getenv("BACKEND_HOST", "0.0.0.0")
    port = int(os.getenv("BACKEND_PORT", "8787"))
    print(f"Smartband backend listening on http://{host}:{port}")
    ThreadingHTTPServer((host, port), Handler).serve_forever()
