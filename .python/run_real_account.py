"""Runs the real-provider device round trip with an account from mail-test-accounts.properties.

Usage: python .python/run_real_account.py <QQ_A|QQ_B|GMAIL_A|NETEASE_A|NETEASE_B|NETEASE126_A|SINA_A> <adb serial> [--peer QQ_B] [--save-sent true|false] [--debug] [--append Drafts] [--cleanup] [--receive pop3] [--release]

Secrets never reach stdout: every value of a secret key is masked in the Gradle output, the
logcat excerpt and the XML report check. The command line itself is not echoed.
"""
import glob
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

which, serial = sys.argv[1], sys.argv[2]
opts = sys.argv[3:]
peer_name = opts[opts.index('--peer') + 1] if '--peer' in opts else None
save_sent = opts[opts.index('--save-sent') + 1] if '--save-sent' in opts else None
release = '--release' in opts
debug = '--debug' in opts
append_folder = opts[opts.index('--append') + 1] if '--append' in opts else None
cleanup = '--cleanup' in opts
# --receive pop3: the receiving side (the peer when given, else the account itself) reads over POP3 (roadmap P2.4)
receive = opts[opts.index('--receive') + 1] if '--receive' in opts else None
props = {}
for line in open('mail-test-accounts.properties', encoding='utf-8'):
    line = line.strip()
    if not line or line[0] in '#!':
        continue
    m = re.match(r'([^=:\s]+)\s*[=:]\s*(.*)', line)
    if m:
        props[m.group(1)] = m.group(2).strip()

profiles = {
    'QQ_A': dict(address=props.get('QQ_USER_NAME_A'), secret=props.get('QQ_AUTH_CODE_A'), provider='qq', auth='password'),
    'QQ_B': dict(address=props.get('QQ_USER_NAME_B'), secret=props.get('QQ_AUTH_CODE_B'), provider='qq', auth='password'),
    'GMAIL_A': dict(address=props.get('GMAIL_USER_NAME_A'), secret=props.get('GMAIL_ACCESS_TOKEN_A'), provider='gmail', auth='xoauth2'),
    'NETEASE_A': dict(address=props.get('NETEASE_USER_NAME_A'), secret=props.get('NETEASE_AUTH_CODE_A'), provider=None, auth='password'),
    'NETEASE_B': dict(address=props.get('NETEASE_USER_NAME_B'), secret=props.get('NETEASE_AUTH_CODE_B'), provider=None, auth='password'),
    'NETEASE126_A': dict(address=props.get('NETEASE126_USER_NAME_A'), secret=props.get('NETEASE126_AUTH_CODE_A'), provider='126', auth='password'),
    'SINA_A': dict(address=props.get('SINA_USER_NAME_A'), secret=props.get('SINA_AUTH_CODE_A'), provider='sina', auth='password'),
}
# NetEase accounts pick the preset from the address domain (163 / 126); yeah.net keeps the 163 preset
# (ID command, sent folder) but talks to its own hosts.
for name in ('NETEASE_A', 'NETEASE_B'):
    address = profiles[name]['address'] or ''
    domain = address.rsplit('@', 1)[-1]
    profiles[name]['provider'] = '126' if domain == '126.com' else '163'
    if domain == 'yeah.net':
        profiles[name].update(imapHost='imap.yeah.net', pop3Host='pop.yeah.net', smtpHost='smtp.yeah.net')
profile = profiles[which]
assert profile['address'] and profile['secret'], 'profile incomplete'
secret = profile['secret']
safe = re.compile(r'^[A-Za-z0-9._\-/+=]+$')
assert safe.match(secret), 'secret contains characters unsafe for the command line; aborting'

peer = profiles[peer_name] if peer_name else None
if peer:
    assert peer['address'] and peer['secret'], 'peer profile incomplete'
    assert safe.match(peer['secret']), 'peer secret contains characters unsafe for the command line; aborting'
secrets = [secret] + ([peer['secret']] if peer else [])
def scrub(text):
    for value in secrets:
        text = text.replace(value, '***')
    return text

args = {
    'class': 'io.github.supermonster003.autojs6.plugin.angus.mail.MailCoreDeviceTest#realAccountSendsAndListsWhenProvided',
    'mailAddress': profile['address'],
    'mailSecret': secret,
    'mailProvider': profile['provider'],
    'mailAuth': profile['auth'],
}
if profile.get('imapHost'):
    args.update({'mailImapHost': profile['imapHost'], 'mailPop3Host': profile['pop3Host'], 'mailSmtpHost': profile['smtpHost']})
if peer:
    args.update({'mailPeerAddress': peer['address'], 'mailPeerSecret': peer['secret'], 'mailPeerProvider': peer['provider'], 'mailPeerAuth': peer['auth']})
    if peer.get('imapHost'):
        args['mailPeerImapHost'] = peer['imapHost']
        args['mailPeerPop3Host'] = peer['pop3Host']
if receive:
    args['mailPeerReceive' if peer else 'mailReceive'] = receive
if save_sent is not None:
    args['mailSaveToSent'] = save_sent
if debug:
    args['mailDebug'] = 'true'
if append_folder:
    args['mailAppendFolder'] = append_folder
if cleanup:
    args['mailCleanup'] = 'true'
import os
gradlew = os.path.abspath('gradlew.bat') if os.path.exists('gradlew.bat') else os.path.abspath('gradlew')
task = ':app:connectedReleaseAndroidTest' if release else ':app:connectedDebugAndroidTest'
cmd = ['cmd', '/c', gradlew, task, '--console=plain', '-q'] if gradlew.endswith('.bat') else ['bash', gradlew, task, '--console=plain', '-q']
if release:
    cmd.append('-PandroidTestRelease')
cmd += ['-Pandroid.testInstrumentationRunnerArguments.%s=%s' % (k, v) for k, v in args.items()]
env = dict(__import__('os').environ, ANDROID_SERIAL=serial)
subprocess.run(['adb', '-s', serial, 'logcat', '-c'], capture_output=True)
proc = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace', env=env)
out = scrub(proc.stdout + proc.stderr)
keep = [l for l in out.splitlines() if l.strip() and not l.startswith(('Classpath', 'Plugin:', '---', '===', 'Gradle version', 'Version', 'SDK', 'JDK', 'Java version', 'JVM target', 'Platform'))]
print('gradle exit', proc.returncode)
print('\n'.join(keep[-25:]))

log = subprocess.run(['adb', '-s', serial, 'logcat', '-d', '-s', 'MailCoreDeviceTest', 'TestRunner'], capture_output=True, text=True, encoding='utf-8', errors='replace').stdout
print('--- logcat ---')
for l in scrub(log).splitlines():
    if 'MailCoreDeviceTest' in l or 'failed' in l.lower():
        print(l[l.index('I ') + 2:] if 'I ' in l else l)

leaked = False
for f in glob.glob('app/build/outputs/androidTest-results/connected/**/*.xml', recursive=True):
    text = open(f, encoding='utf-8', errors='replace').read()
    if any(value in text for value in secrets):
        leaked = True
        print('SECRET FOUND IN REPORT', f)
    t = ET.parse(f).getroot()
    print(f.replace(chr(92), '/').split('/')[-1], 'tests=%s failures=%s errors=%s skipped=%s' % (t.get('tests'), t.get('failures'), t.get('errors'), t.get('skipped')))
    for tc in t.iter('testcase'):
        for fail in list(tc.findall('failure')) + list(tc.findall('error')):
            print('  FAIL', tc.get('name'))
            print('   ', scrub(fail.text or '')[:2000])
print('report leak check:', 'LEAK' if leaked else 'clean')
