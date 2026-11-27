"""Run as root on the deployed host with /opt/certbot/bin/python.

Uses temporary users and a venue, HTTPS, PostgreSQL and real SES delivery.
State is root-only, and cleanup removes only records created by this script.
"""
import base64
import datetime as dt
import hashlib
import hmac
import json
import os
import re
import secrets
import socket
import ssl
import struct
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ORIGIN = sys.argv[2].rstrip("/")
STATE = Path("/var/lib/evmap/smoke-test.json")


def sql(statement):
    return subprocess.check_output(
        ["runuser", "-u", "postgres", "--", "psql", "-d", "evmap", "-At", "-v", "ON_ERROR_STOP=1", "-c", statement],
        text=True,
    ).strip().splitlines()[0]


def save(state):
    descriptor = os.open(STATE, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(descriptor, "w") as output:
        json.dump(state, output)


def request(method, path, payload=None, token=None, expected=200, content_type=None):
    headers = {"X-Evmap-Client-Version": "2"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if payload is not None:
        if not isinstance(payload, bytes):
            payload = json.dumps(payload).encode()
            content_type = "application/json"
        headers["Content-Type"] = content_type
    req = urllib.request.Request(ORIGIN + path, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            status, body = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, body = error.code, error.read()
    if status != expected:
        raise AssertionError(f"{method} {path}: expected {expected}, received {status}; {body[:300]!r}")
    try:
        return json.loads(body) if body else None
    except (ValueError, UnicodeDecodeError):
        return body


class WebSocket:
    def __init__(self, token):
        host = urllib.parse.urlparse(ORIGIN).hostname
        self.connection = ssl.create_default_context().wrap_socket(socket.create_connection((host, 443), timeout=15), server_hostname=host)
        key = base64.b64encode(secrets.token_bytes(16)).decode()
        self.connection.sendall((f"GET /ws HTTP/1.1\r\nHost: {host}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Protocol: v12.stomp\r\n\r\n").encode())
        header = b""
        while not header.endswith(b"\r\n\r\n"):
            header += self.exact(1)
        assert header.startswith(b"HTTP/1.1 101"), header[:200]
        expected = base64.b64encode(hashlib.sha1((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encode()).digest())
        assert expected.lower() in header.lower()
        self.send(f"CONNECT\naccept-version:1.2\nhost:{host}\nAuthorization:Bearer {token}\nheart-beat:0,0\n\n\x00")
        assert "CONNECTED" in self.receive()

    def exact(self, length):
        result = b""
        while len(result) < length:
            part = self.connection.recv(length - len(result))
            if not part:
                raise EOFError("WebSocket closed")
            result += part
        return result

    def send(self, frame):
        data, mask = frame.encode(), secrets.token_bytes(4)
        header = bytes([0x81, 0x80 | len(data)]) if len(data) < 126 else bytes([0x81, 0xFE]) + struct.pack("!H", len(data))
        self.connection.sendall(header + mask + bytes(value ^ mask[index % 4] for index, value in enumerate(data)))

    def receive(self):
        first, second = self.exact(2)
        length = second & 127
        if length == 126:
            length = struct.unpack("!H", self.exact(2))[0]
        elif length == 127:
            length = struct.unpack("!Q", self.exact(8))[0]
        assert not second & 128
        data = self.exact(length)
        if first & 15 == 8:
            return "CLOSED"
        return data.decode()

    def close(self):
        self.connection.close()


def setup():
    if STATE.exists():
        raise RuntimeError("Existing smoke state found; resume or clean it up first")
    assert request("GET", "/health") == {"status": "UP"}
    request("GET", "/api/users/me", expected=401)
    state = {"username": "smoke-" + secrets.token_hex(4), "password": "Aa1!" + secrets.token_urlsafe(20)}
    save(state)
    auth = request("POST", "/api/auth/register", {"user": {
        "name": "DeployTest", "surname": None, "username": state["username"],
        "email": sys.argv[3], "phone": "+48000123456", "dateOfBirth": "1990-01-01",
        "pageDescription": "Temporary deployment verification", "password": state["password"],
    }}, expected=201)
    state.update(auth)
    save(state)
    token, uid = state["accessToken"], state["userId"]
    assert request("POST", "/api/auth/login", {"usernameOrEmail": state["username"], "password": state["password"]})["userId"] == uid
    assert request("GET", "/api/users/me", token=token)["id"] == uid
    peer = int(sql(f"INSERT INTO evmap.app_user(name,email,phone,birthdate,password_hash,username,page_description) SELECT 'SmokePeer','{state['username']}@example.invalid','+48000123457',birthdate,password_hash,'{state['username']}-p','Temporary test peer' FROM evmap.app_user WHERE id={uid} RETURNING id"))
    state["peerId"] = peer
    save(state)
    peer_token = request("POST", "/api/auth/login", {"usernameOrEmail": state["username"] + "-p", "password": state["password"]})["accessToken"]
    state["peerToken"] = peer_token
    location = int(sql("INSERT INTO evmap.location(name,category_id,point) SELECT 'Deploy smoke venue',id,public.ST_SetSRID(public.ST_MakePoint(21.0122,52.2297),4326) FROM evmap.location_category ORDER BY id LIMIT 1 RETURNING id"))
    state["locationId"] = location
    save(state)
    category = request("GET", "/api/events/categories", token=token)[0]["id"]
    start = dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=1)
    event = request("POST", "/api/events", {"name": "Deploy smoke event", "description": "Temporary production deployment test", "startsAt": start.isoformat(), "endsAt": (start + dt.timedelta(hours=1)).isoformat(), "locationId": location, "categoryIds": [category]}, token=token, expected=201)
    state["eventId"] = event
    save(state)
    assert request("GET", f"/api/events/{event}", token=token)["id"] == event
    nearby = request("GET", "/api/map/events/nearby?lat=52.2297&lng=21.0122&radiusMeters=1000", token=token)
    assert any(item.get("id", item.get("eventId")) == event for item in nearby)
    request("GET", "/api/events/feed?page=0&size=2&lat=52.2297&lng=21.0122", token=token)
    image = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")
    boundary = "evmap-" + secrets.token_hex(8)
    data = f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="smoke.png"\r\nContent-Type: image/png\r\n\r\n'.encode() + image + f"\r\n--{boundary}--\r\n".encode()
    photo = request("POST", f"/api/events/{event}/photos", data, token=token, expected=201, content_type=f"multipart/form-data; boundary={boundary}")
    state["mediaPath"] = photo["imagePath"]
    save(state)
    assert request("GET", photo["imagePath"], token=token) == image
    request("GET", photo["imagePath"], expected=401)
    request("DELETE", f"/api/events/{event}/photos/{photo['id']}", token=peer_token, expected=403)
    chat = request("POST", "/api/chats", {"name": "Deploy smoke chat"}, token=token, expected=201)
    state["chatId"] = chat
    save(state)
    request("GET", f"/api/chats/{chat}/search?query=smoke", token=peer_token, expected=403)
    connection = WebSocket(token)
    connection.send(f"SUBSCRIBE\nid:smoke\ndestination:/topic/chats/{chat}/messages\n\n\x00")
    connection.send(f'SEND\ndestination:/app/chats/{chat}/messages\ncontent-type:application/json\n\n{{"text":"Deployment websocket smoke"}}\x00')
    frame = connection.receive()
    assert "MESSAGE" in frame and "Deployment websocket smoke" in frame, frame[:100]
    connection.close()
    finish_setup()


def finish_setup():
    state = json.loads(STATE.read_text())
    token, peer_token = state["accessToken"], state["peerToken"]
    peer, chat = state["peerId"], state["chatId"]
    outsider = WebSocket(peer_token)
    outsider.send(f"SUBSCRIBE\nid:forbidden\ndestination:/topic/chats/{chat}/messages\n\n\x00")
    frame = outsider.receive()
    assert any(value in frame for value in ("ERROR", "CLOSED")), frame[:100]
    outsider.close()
    direct = request("POST", f"/api/chats/direct/{peer}", token=token)
    assert request("POST", f"/api/chats/direct/{peer}", token=token) == direct
    state["directId"] = direct
    recovery = request("POST", "/api/auth/password/forgot", {"email": sys.argv[3]}, expected=202)
    state["recoveryId"] = recovery["requestId"]
    save(state)
    print("PASS: registration, login, profile, event, PostGIS nearby/feed, photo upload/access control, REST chat isolation, WSS messaging/isolation, idempotent direct chat; recovery queued.")


def recovery():
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    state = json.loads(STATE.read_text())
    request_id = state["recoveryId"]
    for _ in range(45):
        sent = sql(f"SELECT sent_at IS NOT NULL FROM evmap.password_reset_mail_outbox WHERE request_id='{request_id}'")
        if sent == "t":
            break
        time.sleep(2)
    assert sent == "t", "SES did not accept the password recovery email"
    env = dict(line.split("=", 1) for line in Path("/etc/evmap/app.env").read_text().splitlines() if "=" in line)
    master = base64.b64decode(env["APP_PASSWORD_RESET_SECRET"])
    key = hmac.new(master, b"mail-encryption", hashlib.sha256).digest()
    encrypted = base64.b64decode(sql(f"SELECT encrypted_body FROM evmap.password_reset_mail_outbox WHERE request_id='{request_id}'"))
    body = AESGCM(key).decrypt(encrypted[:12], encrypted[12:], None).decode()
    code = re.search(r"\b[0-9]{8}\b", body).group()
    reset = request("POST", "/api/auth/password/verify", {"requestId": request_id, "code": code})
    state["password"] = "Bb2!" + secrets.token_urlsafe(20)
    request("POST", "/api/auth/password/reset", {"resetToken": reset["resetToken"], "newPassword": state["password"]}, expected=204)
    request("GET", "/api/users/me", token=state["accessToken"], expected=401)
    request("POST", "/api/auth/refresh", {"refreshToken": state["refreshToken"]}, expected=401)
    state.update(request("POST", "/api/auth/login", {"usernameOrEmail": state["username"], "password": state["password"]}))
    save(state)
    print("PASS: SES SMTP accepted recovery email; recovery survives restart; password changed, previous JWT and refresh token revoked, new login successful.")


def verify():
    state = json.loads(STATE.read_text())
    token = state["accessToken"]
    assert request("GET", "/api/users/me", token=token)["id"] == state["userId"]
    assert request("GET", state["mediaPath"], token=token)
    messages = request("GET", f"/api/chats/{state['chatId']}/messages", token=token)
    assert any(message["text"] == "Deployment websocket smoke" for message in messages)
    print("PASS: persistent tokens, database records, chat messages and media after restart.")


def cleanup():
    state = json.loads(STATE.read_text())
    for key in ("chatId", "directId"):
        if key in state:
            request("DELETE", f"/api/chats/{state[key]}", token=state["accessToken"], expected=204)
    if "eventId" in state:
        request("DELETE", f"/api/events/{state['eventId']}", token=state["accessToken"], expected=204)
    if "peerId" in state:
        request("DELETE", f"/api/users/{state['peerId']}", token=state["peerToken"], expected=204)
    if "userId" in state:
        request("DELETE", f"/api/users/{state['userId']}", token=state["accessToken"], expected=204)
    if "locationId" in state:
        sql(f"DELETE FROM evmap.location WHERE id={state['locationId']} RETURNING id")
    STATE.unlink()
    print("PASS: temporary test records and files removed.")


{"setup": setup, "finish": finish_setup, "recovery": recovery, "verify": verify, "cleanup": cleanup}[sys.argv[1]]()
