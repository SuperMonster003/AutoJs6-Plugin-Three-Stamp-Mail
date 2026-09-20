"""Runs the real-provider matrix probe (mail roadmap P6) on the JVM with accounts from mail-test-accounts.properties.

Usage: python .python/run_provider_matrix.py QQ_A[,NETEASE_A,...] [--idle-seconds 120] [--poll-ms 30000] [--no-pop3] [--keep] [--ops folders,cleanup] [--diag 'LIST "" "*";STATUS X (MESSAGES)'] [--no-preset]

--no-preset spells the preset's hosts out and sends no `provider`, so the preset's authentication restriction is
bypassed and the server's own refusal is what the mail core reports (Outlook.com with an app password).

Writes build/p6/matrix.properties, runs `:mail-core:test --tests "*ProviderMatrixProbe"`, prints the
result rows with every secret and every account address masked, and checks the Gradle output, the XML
report and the probe logs for the secrets. The command line never carries a secret.
"""
import glob
import os
import re
import subprocess
import sys

profiles = sys.argv[1]
opts = sys.argv[2:]
idle_seconds = opts[opts.index('--idle-seconds') + 1] if '--idle-seconds' in opts else '120'
poll_ms = opts[opts.index('--poll-ms') + 1] if '--poll-ms' in opts else '30000'
pop3 = 'false' if '--no-pop3' in opts else 'true'
keep = 'true' if '--keep' in opts else 'false'
ops = opts[opts.index('--ops') + 1] if '--ops' in opts else ''
diag = opts[opts.index('--diag') + 1] if '--diag' in opts else ''
no_preset = '--no-preset' in opts

props = {}
for line in open('mail-test-accounts.properties', encoding='utf-8'):
    line = line.strip()
    if not line or line[0] in '#!':
        continue
    m = re.match(r'([^=:\s]+)\s*[=:]\s*(.*)', line)
    if m:
        props[m.group(1)] = m.group(2).strip()
secrets = [v for k, v in props.items() if ('AUTH_CODE' in k or 'TOKEN' in k) and v]
addresses = [v for k, v in props.items() if 'USER_NAME' in k and v]


def scrub(text):
    for value in secrets:
        text = text.replace(value, '***')
    for value in addresses:
        text = text.replace(value, '***@' + value.rsplit('@', 1)[-1])
        text = text.replace(value.split('@', 1)[0], '***')
    return text


os.makedirs('build/p6', exist_ok=True)
with open('build/p6/matrix.properties', 'w', encoding='utf-8') as f:
    f.write('profiles=%s\nidleSeconds=%s\npollIntervalMs=%s\npop3=%s\nkeep=%s\n' % (profiles, idle_seconds, poll_ms, pop3, keep))
    if ops:
        f.write('ops=%s\n' % ops)
    if diag:
        f.write('diag=%s\n' % diag.replace('\\', '\\\\'))
    if no_preset:
        f.write('noPreset=true\n')
for f in ['build/p6/matrix-%s.log' % p for p in profiles.split(',')] + ['build/p6/matrix-summary.txt']:
    if os.path.exists(f):
        os.remove(f)

gradlew = os.path.abspath('gradlew.bat') if os.path.exists('gradlew.bat') else os.path.abspath('gradlew')
cmd = ['cmd', '/c', gradlew] if gradlew.endswith('.bat') else ['bash', gradlew]
cmd += [':mail-core:test', '--tests', '*ProviderMatrixProbe', '--console=plain', '-q', '--rerun']
proc = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')
out = scrub(proc.stdout + proc.stderr)
print('gradle exit', proc.returncode)
keep_lines = [l for l in out.splitlines() if l.strip() and not l.startswith(('Classpath', 'Plugin:', '---', '===', 'Gradle version', 'Version', 'SDK', 'JDK', 'Java version', 'JVM target', 'Platform'))]
print('\n'.join(keep_lines[-30:]))
try:
    os.remove('build/p6/matrix.properties')
except OSError:
    pass

leaked = False
for f in glob.glob('mail-core/build/test-results/test/*ProviderMatrixProbe*.xml') + glob.glob('build/p6/matrix-*.log') + glob.glob('build/p6/matrix-summary.txt'):
    text = open(f, encoding='utf-8', errors='replace').read()
    if any(value in text for value in secrets):
        leaked = True
        print('SECRET FOUND IN', f)
if os.path.exists('build/p6/matrix-summary.txt'):
    print('--- summary ---')
    print(scrub(open('build/p6/matrix-summary.txt', encoding='utf-8').read()))
print('leak check:', 'LEAK' if leaked else 'clean')
