#!/usr/bin/env python3
"""로컬 전용 API 테스트 화면. 브라우저의 CORS 제한을 피하려고 서버와 Supabase를 같은 주소로 중계한다."""
import json
import os
import subprocess
import urllib.error
import urllib.request
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

HERE = Path(__file__).parent
SERVER_DIR = HERE.parents[1]
PORT = int(os.environ.get("TESTER_PORT", "5500"))
API = os.environ.get("API_TARGET", "http://localhost:8080")
SUPABASE = "http://127.0.0.1:54321"
LOCAL_HOSTS = {"localhost", "127.0.0.1"}

# 계정 생성에 관리자 키를 쓰고 데이터를 지울 수 있으므로 운영 서버에는 연결하지 않는다.
if urlsplit(API).hostname not in LOCAL_HOSTS:
    raise SystemExit(f"로컬 서버에만 연결할 수 있습니다. API_TARGET={API}")


def load_keys():
    out = subprocess.run(
        ["npx", "supabase", "status", "-o", "env"],
        cwd=SERVER_DIR, capture_output=True, text=True, check=False,
    ).stdout
    pairs = (line.split("=", 1) for line in out.splitlines() if "=" in line)
    keys = {k: v.strip('"') for k, v in pairs}
    if "SECRET_KEY" not in keys:
        raise SystemExit("Supabase가 꺼져 있습니다. server 폴더에서 npx supabase start 를 먼저 실행하세요.")
    return keys


KEYS = load_keys()


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(HERE), **kwargs)

    def log_message(self, *args):
        pass

    def forward(self):
        path = self.path
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else None
        headers = {}
        for name in ("Authorization", "Content-Type"):
            if self.headers.get(name):
                headers[name] = self.headers[name]
        if path == "/dev/users":
            target = f"{SUPABASE}/auth/v1/admin/users"
            headers = {"apikey": KEYS["SECRET_KEY"], "Content-Type": "application/json"}
        elif path.startswith("/auth/"):
            target = SUPABASE + path
            headers["apikey"] = KEYS["PUBLISHABLE_KEY"]
        else:
            target = API + path
        req = urllib.request.Request(target, data=body, headers=headers, method=self.command)
        try:
            with urllib.request.urlopen(req, timeout=20) as res:
                self.reply(res.status, res.headers.get("Content-Type"), res.read())
        except urllib.error.HTTPError as err:
            self.reply(err.code, err.headers.get("Content-Type"), err.read())
        except OSError as err:
            message = f"{target} 에 연결할 수 없습니다. 서버가 켜져 있는지 확인하세요. ({err})"
            self.reply(502, "application/json", json.dumps({"detail": message}, ensure_ascii=False).encode())

    def reply(self, status, content_type, data):
        self.send_response(status)
        if content_type:
            self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def is_proxied(self):
        return self.path.startswith(("/api/", "/auth/", "/actuator/", "/dev/"))

    def do_GET(self):
        self.forward() if self.is_proxied() else super().do_GET()

    def do_POST(self):
        self.forward()

    def do_PUT(self):
        self.forward()

    def do_PATCH(self):
        self.forward()

    def do_DELETE(self):
        self.forward()


print(f"테스트 화면: http://localhost:{PORT}  (API: {API}, 종료: Ctrl+C)")
ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
