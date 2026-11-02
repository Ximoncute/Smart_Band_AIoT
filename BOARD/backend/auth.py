import hashlib
import hmac
import os
import secrets
from datetime import datetime, timedelta, timezone
from pathlib import Path

from pymongo import MongoClient
from pymongo.errors import DuplicateKeyError

DATA = Path(__file__).resolve().parent / "data"
DATA.mkdir(exist_ok=True)
KEY_FILE = DATA / "admin.key"
if not KEY_FILE.exists():
    KEY_FILE.write_text(secrets.token_urlsafe(32), encoding="ascii")
ADMIN_KEY = KEY_FILE.read_text(encoding="ascii").strip()

client = MongoClient(os.getenv("MONGODB_URI", "mongodb://127.0.0.1:27017"), serverSelectionTimeoutMS=2000)
db = client["smartband"]
users = db.users
sessions = db.sessions
otps = db.otps
users.create_index("email", unique=True)
sessions.create_index("expires_at", expireAfterSeconds=0)
otps.create_index("expires_at", expireAfterSeconds=0)


def hash_password(password, salt=None):
    salt = salt or secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 260000)
    return salt.hex() + ":" + digest.hex()


def verify_password(password, stored):
    try:
        salt, digest = stored.split(":", 1)
        return hmac.compare_digest(hash_password(password, bytes.fromhex(salt)).split(":")[1], digest)
    except (ValueError, AttributeError):
        return False


def public_user(user):
    return {key: user.get(key) for key in ("email", "name", "blocked", "created_at", "profile", "device_id")}


def request_otp(email, purpose):
    email = email.strip().lower()
    if "@" not in email or purpose not in {"register", "reset"}:
        return 400, {"error": "invalid_request"}
    exists = users.find_one({"email": email}) is not None
    if purpose == "register" and exists:
        return 409, {"error": "email_exists"}
    if purpose == "reset" and not exists:
        return 404, {"error": "user_not_found"}
    code = str(secrets.randbelow(900000) + 100000)
    otps.update_one({"email": email, "purpose": purpose},
                    {"$set": {"code_hash": hashlib.sha256(code.encode()).hexdigest(),
                              "expires_at": datetime.now(timezone.utc) + timedelta(minutes=5)}}, upsert=True)
    return 200, {"ok": True, "client_delivery_code": code}


def verify_otp(email, purpose, code):
    record = otps.find_one({"email": email.strip().lower(), "purpose": purpose,
                            "expires_at": {"$gt": datetime.now(timezone.utc)}})
    if not record or not hmac.compare_digest(record["code_hash"], hashlib.sha256(code.encode()).hexdigest()):
        return False
    otps.delete_one({"_id": record["_id"]})
    return True


def register(email, name, password, otp):
    email = email.strip().lower()
    if not email or "@" not in email or len(password) < 6:
        return 400, {"error": "invalid_user"}
    if not verify_otp(email, "register", otp):
        return 403, {"error": "invalid_otp"}
    user = {"email": email, "name": name.strip(), "password_hash": hash_password(password),
            "blocked": False, "created_at": datetime.now(timezone.utc).isoformat(), "profile": {},
            "device_id": "smartband-001"}
    try:
        users.insert_one(user)
    except DuplicateKeyError:
        return 409, {"error": "email_exists"}
    return login(email, password)


def login(email, password):
    user = users.find_one({"email": email.strip().lower()})
    if not user or not verify_password(password, user.get("password_hash")):
        return 401, {"error": "invalid_credentials"}
    if user.get("blocked"):
        return 403, {"error": "account_blocked"}
    token = secrets.token_urlsafe(32)
    sessions.insert_one({"token_hash": hashlib.sha256(token.encode()).hexdigest(), "email": user["email"],
                         "expires_at": datetime.now(timezone.utc) + timedelta(days=30)})
    return 200, {"token": token, "user": public_user(user)}


def authenticated(token):
    if not token:
        return None
    session = sessions.find_one({"token_hash": hashlib.sha256(token.encode()).hexdigest(),
                                 "expires_at": {"$gt": datetime.now(timezone.utc)}})
    if not session:
        return None
    return users.find_one({"email": session["email"], "blocked": False})


def admin_authorized(token):
    return hmac.compare_digest(token or "", ADMIN_KEY)


def admin_users():
    rows = [public_user(user) for user in users.find({}, {"password_hash": 0})]
    return {"count": len(rows), "users": rows}


def admin_update(email, patch):
    update = {}
    if "blocked" in patch:
        update["blocked"] = bool(patch["blocked"])
    if patch.get("password"):
        if len(patch["password"]) < 6:
            return 400, {"error": "password_too_short"}
        update["password_hash"] = hash_password(patch["password"])
    if not update:
        return 400, {"error": "empty_update"}
    result = users.update_one({"email": email.lower()}, {"$set": update})
    if not result.matched_count:
        return 404, {"error": "user_not_found"}
    sessions.delete_many({"email": email.lower()})
    return 200, {"ok": True}
