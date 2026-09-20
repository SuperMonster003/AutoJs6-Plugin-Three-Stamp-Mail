"""Obtains an OAuth 2.0 access token (plus a refresh token) for an Outlook.com / Hotmail or Microsoft 365
mailbox on this PC, for the XOAUTH2 rows of the provider matrix (mail roadmap P6 Outlook.com column;
the in-app flow is roadmap P9).

Usage:
  py .python/outlook_oauth_login.py --client-id <application (client) id> [--tenant consumers|common|<tenant id>]
                                    [--out build/outlook-token.properties] [--suffix A] [--timeout 300]
  py .python/outlook_oauth_login.py --refresh [--out build/outlook-token.properties] [--suffix A]

One-time registration (Microsoft Entra admin center, https://entra.microsoft.com, any Microsoft account):
  1. Identity > Applications > App registrations > New registration. Name: anything (for example
     "AutoJs6 mail matrix"). Supported account types: "Personal Microsoft accounts only" for Outlook.com /
     Hotmail test accounts (or "Accounts in any organizational directory and personal Microsoft accounts"
     when Microsoft 365 accounts will be tested too). Redirect URI: platform "Public client/native (mobile &
     desktop)", value exactly `http://localhost` (no port; the loopback port is chosen at run time).
  2. After registration: Authentication > Advanced settings > "Allow public client flows" = Yes. Do not
     create a client secret; the flow below is a public client with PKCE.
  3. Copy the "Application (client) ID" from the Overview page. That id is not a secret.

The script asks the browser for the delegated Exchange scopes `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All`
and `SMTP.Send` plus `offline_access`, catches the redirect on a loopback port, exchanges the code with PKCE,
and writes the tokens to the (git-ignored) output file as
`OUTLOOK_ACCESS_TOKEN_<suffix>`, `OUTLOOK_REFRESH_TOKEN_<suffix>`, `OUTLOOK_TOKEN_EXPIRES_AT_<suffix>`
(Unix seconds), `OUTLOOK_CLIENT_ID_<suffix>` and `OUTLOOK_TENANT_<suffix>`. `--refresh` reads the refresh token
and the client id back from that file and replaces the access token (Outlook access tokens last about an
hour, refresh tokens about 90 days of inactivity). Nothing secret is printed.
"""
import argparse
import base64
import hashlib
import http.server
import io
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import webbrowser

SCOPES = [
    "https://outlook.office.com/IMAP.AccessAsUser.All",
    "https://outlook.office.com/POP.AccessAsUser.All",
    "https://outlook.office.com/SMTP.Send",
    "offline_access",
]
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def authority(tenant):
    return f"https://login.microsoftonline.com/{tenant}/oauth2/v2.0"


def read_properties(path):
    props = {}
    if os.path.exists(path):
        with io.open(path, encoding="utf-8") as handle:
            for line in handle:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, value = line.split("=", 1)
                    props[key.strip()] = value.strip()
    return props


def write_properties(path, props):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Written by .python/outlook_oauth_login.py; keep this file out of git.\n")
        for key in sorted(props):
            handle.write(f"{key}={props[key]}\n")


def token_request(tenant, form):
    data = urllib.parse.urlencode(form).encode("ascii")
    request = urllib.request.Request(f"{authority(tenant)}/token", data=data, method="POST")
    request.add_header("Content-Type", "application/x-www-form-urlencoded")
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")
        try:
            detail = json.loads(body)
            message = f"{detail.get('error')}: {detail.get('error_description', '')}"
        except ValueError:
            message = body[:500]
        raise SystemExit(f"token endpoint answered HTTP {error.code}: {message}")


def store(args, tenant, client_id, answer, props):
    suffix = args.suffix
    expires_at = int(time.time()) + int(answer.get("expires_in", 0))
    props[f"OUTLOOK_ACCESS_TOKEN_{suffix}"] = answer["access_token"]
    if answer.get("refresh_token"):
        props[f"OUTLOOK_REFRESH_TOKEN_{suffix}"] = answer["refresh_token"]
    props[f"OUTLOOK_TOKEN_EXPIRES_AT_{suffix}"] = str(expires_at)
    props[f"OUTLOOK_CLIENT_ID_{suffix}"] = client_id
    props[f"OUTLOOK_TENANT_{suffix}"] = tenant
    write_properties(args.out, props)
    granted = answer.get("scope", "").split()
    missing = [s for s in SCOPES if s != "offline_access" and s not in granted]
    print(f"tokens written to {args.out} (suffix {suffix}); access token expires in {answer.get('expires_in')} s "
          f"({time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(expires_at))}); refresh token: "
          f"{'yes' if answer.get('refresh_token') else 'no'}")
    print("granted scopes:", " ".join(granted) or "(none reported)")
    if missing:
        print("WARNING: not granted:", " ".join(missing))


class RedirectHandler(http.server.BaseHTTPRequestHandler):
    result = None

    def do_GET(self):
        query = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        RedirectHandler.result = {key: values[0] for key, values in query.items()}
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(b"<html><body><p>AutoJs6 mail matrix: the sign-in result was received, you can close this window.</p></body></html>")

    def log_message(self, *_):
        pass


def login(args):
    if not args.client_id:
        raise SystemExit("--client-id is required (the Application (client) ID of the Entra app registration)")
    verifier = base64.urlsafe_b64encode(secrets.token_bytes(48)).rstrip(b"=").decode("ascii")
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode("ascii")).digest()).rstrip(b"=").decode("ascii")
    state = secrets.token_urlsafe(24)
    server = http.server.HTTPServer(("127.0.0.1", 0), RedirectHandler)
    server.timeout = 5
    redirect_uri = f"http://localhost:{server.server_address[1]}"
    url = f"{authority(args.tenant)}/authorize?" + urllib.parse.urlencode({
        "client_id": args.client_id,
        "response_type": "code",
        "redirect_uri": redirect_uri,
        "response_mode": "query",
        "scope": " ".join(SCOPES),
        "state": state,
        "code_challenge": challenge,
        "code_challenge_method": "S256",
        "prompt": "select_account",
    })
    print(f"tenant {args.tenant}, redirect {redirect_uri}; opening the browser. If it does not open, visit:\n{url}\n")
    webbrowser.open(url)
    deadline = time.time() + args.timeout
    while RedirectHandler.result is None and time.time() < deadline:
        server.handle_request()
    server.server_close()
    result = RedirectHandler.result
    if result is None:
        raise SystemExit("no redirect received before the timeout")
    if "error" in result:
        raise SystemExit(f"authorization refused: {result.get('error')}: {result.get('error_description', '')}")
    if result.get("state") != state:
        raise SystemExit("state mismatch in the redirect; run the script again")
    answer = token_request(args.tenant, {
        "client_id": args.client_id,
        "grant_type": "authorization_code",
        "code": result["code"],
        "redirect_uri": redirect_uri,
        "code_verifier": verifier,
        "scope": " ".join(SCOPES),
    })
    store(args, args.tenant, args.client_id, answer, read_properties(args.out))


def refresh(args):
    props = read_properties(args.out)
    suffix = args.suffix
    refresh_token = props.get(f"OUTLOOK_REFRESH_TOKEN_{suffix}")
    client_id = args.client_id or props.get(f"OUTLOOK_CLIENT_ID_{suffix}")
    tenant = props.get(f"OUTLOOK_TENANT_{suffix}") or args.tenant
    if not refresh_token or not client_id:
        raise SystemExit(f"{args.out} has no refresh token / client id for suffix {suffix}; run the login first")
    answer = token_request(tenant, {
        "client_id": client_id,
        "grant_type": "refresh_token",
        "refresh_token": refresh_token,
        "scope": " ".join(SCOPES),
    })
    store(args, tenant, client_id, answer, props)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--client-id", default=None, help="Application (client) ID of the public client app registration")
    parser.add_argument("--tenant", default="consumers", help="consumers (personal accounts, default), common, or a tenant id")
    parser.add_argument("--out", default=os.path.join(REPO, "build", "outlook-token.properties"), help="properties file for the tokens (git-ignored build/ by default)")
    parser.add_argument("--suffix", default="A", help="key suffix, matching the OUTLOOK_USER_NAME_<suffix> account")
    parser.add_argument("--timeout", type=int, default=300, help="seconds to wait for the browser redirect")
    parser.add_argument("--refresh", action="store_true", help="exchange the stored refresh token for a new access token")
    args = parser.parse_args()
    (refresh if args.refresh else login)(args)


if __name__ == "__main__":
    sys.exit(main())
