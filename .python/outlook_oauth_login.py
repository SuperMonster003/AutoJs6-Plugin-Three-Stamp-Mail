"""Obtains an OAuth 2.0 access token (plus a refresh token) for an Outlook.com / Hotmail or Microsoft 365
mailbox on this PC, for the XOAUTH2 rows of the provider matrix (mail roadmap P6 Outlook.com column;
the in-app flow is roadmap P9).

Usage:
  py .python/outlook_oauth_login.py --client-id <application (client) id> [--tenant consumers|common|<tenant id>]
                                    [--out build/outlook-token.properties] [--suffix HOTMAIL_B] [--timeout 300]
  py .python/outlook_oauth_login.py --refresh [--out build/outlook-token.properties] [--suffix HOTMAIL_B]

`--suffix` names the account of mail-test-accounts.properties the token belongs to, in any of three forms:
the profile (`HOTMAIL_B`, `OUTLOOK_A`), the address key (`HOTMAIL_USER_NAME_B`) or a bare letter (`A` = `OUTLOOK_A`).

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
and `SMTP.Send` plus `offline_access` (and the OpenID `openid email` claims, so the address the account picker
chose is reported at once), catches the redirect on a loopback port, exchanges the code with PKCE,
and writes the tokens to the (git-ignored) output file under the profile's keys, for example
`HOTMAIL_ACCESS_TOKEN_B`, `HOTMAIL_REFRESH_TOKEN_B`, `HOTMAIL_TOKEN_EXPIRES_AT_B` (Unix seconds),
`HOTMAIL_CLIENT_ID_B` and `HOTMAIL_TENANT_B`. The provider matrix probe and the device runners overlay that
file on mail-test-accounts.properties, so a profile with an `<KIND>_ACCESS_TOKEN_<letter>` key runs with
XOAUTH2. `--refresh` reads the refresh token and the client id back from that file and replaces the access
token (Outlook access tokens last about an hour, refresh tokens about 90 days of inactivity). After
storing, the script authenticates once against `outlook.office365.com` IMAP with the profile's address
from mail-test-accounts.properties (`<KIND>_USER_NAME_<letter>`) and logs out again, so a token obtained
for a different account in the browser's account picker is reported at once (`AUTHENTICATE failed`, or
`User is authenticated but not connected` for another Microsoft account). Nothing secret is printed.
"""
import argparse
import base64
import hashlib
import http.server
import imaplib
import io
import json
import os
import re
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
# The browser sign-in also asks for the OpenID identity claims, so the address the account picker chose is
# known from the id_token at once; the refresh grant keeps to the mail scopes (tokens stored before this
# addition have no consent for the identity claims and would be refused).
LOGIN_SCOPES = SCOPES + ["openid", "email"]
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FIELDS = ("ACCESS_TOKEN", "REFRESH_TOKEN", "TOKEN_EXPIRES_AT", "CLIENT_ID", "TENANT")


def profile_of(suffix):
    """HOTMAIL_B / HOTMAIL_USER_NAME_B / A -> (kind, letter); the bare letter means the OUTLOOK_<letter> profile."""
    match = re.fullmatch(r"([A-Z][A-Z0-9]*?)(?:_USER_NAME)?_([A-Z])", suffix.upper())
    if match:
        return match.group(1), match.group(2)
    if re.fullmatch(r"[A-Z]", suffix.upper()):
        return "OUTLOOK", suffix.upper()
    raise SystemExit(f"--suffix {suffix!r}: expected a profile such as HOTMAIL_B, an address key such as HOTMAIL_USER_NAME_B, or a letter")


def key(kind, field, letter):
    return f"{kind}_{field}_{letter}"


def migrate(props):
    """Keys written by the first version of this script with `--suffix <KIND>_USER_NAME_<L>` move to the profile keys."""
    for name in list(props):
        match = re.fullmatch(r"OUTLOOK_(" + "|".join(FIELDS) + r")_([A-Z][A-Z0-9]*)_USER_NAME_([A-Z])", name)
        if match:
            field, kind, letter = match.groups()
            props.setdefault(key(kind, field, letter), props.pop(name))
    return props


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
    kind, letter = profile_of(args.suffix)
    expires_at = int(time.time()) + int(answer.get("expires_in", 0))
    props[key(kind, "ACCESS_TOKEN", letter)] = answer["access_token"]
    if answer.get("refresh_token"):
        props[key(kind, "REFRESH_TOKEN", letter)] = answer["refresh_token"]
    props[key(kind, "TOKEN_EXPIRES_AT", letter)] = str(expires_at)
    props[key(kind, "CLIENT_ID", letter)] = client_id
    props[key(kind, "TENANT", letter)] = tenant
    write_properties(args.out, props)
    granted = answer.get("scope", "").split()
    missing = [s for s in SCOPES if s != "offline_access" and s not in granted]
    print(f"tokens written to {args.out} (profile {kind}_{letter}); access token expires in {answer.get('expires_in')} s "
          f"({time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(expires_at))}); refresh token: "
          f"{'yes' if answer.get('refresh_token') else 'no'}")
    print("granted scopes:", " ".join(granted) or "(none reported)")
    if missing:
        print("WARNING: not granted:", " ".join(missing))
    verify_identity(kind, letter, answer["access_token"], signed_in_address(answer.get("id_token")))


def signed_in_address(id_token):
    """The address the browser signed in, from the id_token's claims (None when the answer carries no id_token)."""
    if not id_token:
        return None
    try:
        payload = id_token.split(".")[1]
        claims = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
    except (IndexError, ValueError):
        return None
    address = claims.get("email") or claims.get("preferred_username") or ""
    return address.strip().lower() or None


def profile_named(address):
    """`HOTMAIL_B` when the address is the HOTMAIL_USER_NAME_B of mail-test-accounts.properties, else None."""
    accounts = read_properties(os.path.join(REPO, "mail-test-accounts.properties"))
    for name, value in accounts.items():
        match = re.fullmatch(r"([A-Z][A-Z0-9]*)_USER_NAME_([A-Z])", name)
        if match and value.strip().lower() == address:
            return f"{match.group(1)}_{match.group(2)}"
    return None


def verify_identity(kind, letter, access_token, signed_in=None):
    """One IMAP XOAUTH2 login with the profile's address: the only way to learn whose mailbox an opaque MSA token opens.
    `signed_in` is the address from the sign-in's id_token when the browser flow produced one."""
    accounts = read_properties(os.path.join(REPO, "mail-test-accounts.properties"))
    address = accounts.get(key(kind, "USER_NAME", letter))
    if not address:
        print(f"identity check skipped: no {key(kind, 'USER_NAME', letter)} in mail-test-accounts.properties")
        return
    domain = address.rsplit("@", 1)[-1]
    own_account = signed_in is not None and signed_in == address.strip().lower()
    if signed_in is not None and not own_account:
        other = profile_named(signed_in)
        print(f"WARNING: the browser signed in ***@{signed_in.rsplit('@', 1)[-1]}, which is "
              f"{other + ' of mail-test-accounts.properties' if other else 'none of the profiles of mail-test-accounts.properties'}, "
              f"not {kind}_{letter} (***@{domain}); the token is stored under {kind}_{letter} anyway.")
    elif own_account:
        print(f"signed in as the address of {kind}_{letter} (***@{domain})")
    try:
        imap = imaplib.IMAP4_SSL("outlook.office365.com", 993, timeout=30)
        try:
            imap.authenticate("XOAUTH2", lambda _: f"user={address}\x01auth=Bearer {access_token}\x01\x01".encode("utf-8"))
            print(f"identity check: the token opens the mailbox of {kind}_{letter} (***@{domain})")
        finally:
            try:
                imap.logout()
            except Exception:
                pass
    except imaplib.IMAP4.error as error:
        text = str(error).replace(access_token, "<token>").replace(address, "***@" + domain)
        print(f"WARNING: identity check failed for {kind}_{letter} (***@{domain}): {text}")
        if own_account:
            print("         the sign-in was this very account, so the mailbox itself refuses IMAP for it (an account-side")
            print("         setting or a mailbox not yet opened, not the token); check the account's IMAP / third-party")
            print("         app access in Outlook on the web and run the login again.")
        else:
            print("         the browser's account picker probably signed in another account; run the login again with the")
            print("         --suffix of the account you signed in with, or sign in with this one (prompt=select_account).")
    except OSError as error:
        print(f"identity check skipped: IMAP connection failed ({type(error).__name__})")


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
    profile_of(args.suffix)
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
        "scope": " ".join(LOGIN_SCOPES),
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
        "scope": " ".join(LOGIN_SCOPES),
    })
    store(args, args.tenant, args.client_id, answer, migrate(read_properties(args.out)))


def refresh(args):
    props = migrate(read_properties(args.out))
    kind, letter = profile_of(args.suffix)
    refresh_token = props.get(key(kind, "REFRESH_TOKEN", letter))
    client_id = args.client_id or props.get(key(kind, "CLIENT_ID", letter))
    tenant = props.get(key(kind, "TENANT", letter)) or args.tenant
    if not refresh_token or not client_id:
        raise SystemExit(f"{args.out} has no refresh token / client id for profile {kind}_{letter}; run the login first")
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
    parser.add_argument("--suffix", default="A", help="the account the token belongs to: a profile (HOTMAIL_B), its address key (HOTMAIL_USER_NAME_B) or a letter (A = OUTLOOK_A)")
    parser.add_argument("--timeout", type=int, default=300, help="seconds to wait for the browser redirect")
    parser.add_argument("--refresh", action="store_true", help="exchange the stored refresh token for a new access token")
    args = parser.parse_args()
    (refresh if args.refresh else login)(args)


if __name__ == "__main__":
    sys.exit(main())
