<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 스크립트에서 IMAP, POP3, SMTP로 메일을 보내고 받고 검색하고 감시합니다</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어

******

현재 README.md는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### 소개

******

Angus Mail은 AutoJs6 스크립트에 전역 객체 `mail`을 제공하여 메일 보내기, 메일함 나열과 검색, 본문 읽기, 첨부 파일 다운로드, 플래그와 폴더 관리, 폴더의 새 메일 감시를 지원합니다. Jakarta Mail의 참조 구현인 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5을 기반으로 하며 TLS 위에서 IMAP, POP3, SMTP를 사용합니다.

모든 메일 트래픽은 플러그인 프로세스 안에서 처리됩니다. AutoJs6는 Binder 서비스를 통해 플러그인을 발견하고, 스크립트가 제공한 계정 (또는 플러그인 설정 페이지에 저장한 별칭)을 넘기며, JSON 결과와 첨부 파일 스트림을 받습니다. 호스트 자체에는 메일 코드가 없습니다. 플러그인에 계정을 저장하지 않는 한 자격 증명은 세션이 유지되는 동안만 메모리에 머뭅니다.

******

### 현재 상태

******

버전 1.2.0 은 1.1.0 의 백그라운드 감시 (로드맵 P8) 위에 Google 및 Microsoft 계정의 브라우저 로그인 (로드맵 P9) 을 추가합니다. P0 부터 P8 까지의 모든 항목은 1.0.0 부터 1.1.0 으로 출시되었으며 증거는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) 에 있습니다. AutoJs6 6.8.0 (빌드 5282) 이상이 필요합니다. "메일 도착 시" 작업에는 메일 계약 버전 2 를 가진 호스트 빌드가 필요합니다. 스크립트 API 의 전체 참조는 [AutoJs6 문서](https://docs.autojs6.com/#/mail) 에 있습니다.

******

### 기능

******

플러그인은 다음 기능을 제공합니다:

- 보내기: 일반 텍스트 또는 HTML, 여러 수신자, 첨부 파일과 인라인 이미지, 사용자 지정 헤더와 우선순위를 지원하며, 제공자가 보낸 편지함에 자동 저장하지 않으면 플러그인이 서버에 저장합니다.
- 받기: 폴더를 페이지 단위로 나열하고, 서버에서 검색하며 (비 ASCII 검색을 거부하는 제공자에서는 클라이언트 필터로 대체), 텍스트와 HTML 본문을 읽고, 첨부 파일을 스크립트 작업 디렉터리로 바로 다운로드합니다.
- 정리: 읽음 또는 플래그 표시, 이동, 복사, 삭제, 완전 삭제, 폴더 만들기, 이름 바꾸기, 삭제를 지원합니다. POP3 계정은 읽기 전용 부분 집합을 사용합니다.
- 감시: 스크립트가 실행되는 동안 새 메일 이벤트를 받으며, 서버가 실제로 푸시하면 IMAP IDLE 을, 그렇지 않으면 폴링 (기본 60 초, 조정 가능) 을 사용합니다: QQ 와 Sina 는 IDLE 을 받아들이지만 알리지 않고, 163 과 126 에는 IDLE 이 없으며, POP3 계정은 항상 폴링합니다; 네트워크 끊김과 플러그인 프로세스 재시작 후에도 감시는 자동으로 복구됩니다.
- 백그라운드 감시: 설정의 감시 페이지는 스크립트가 실행되지 않는 동안에도 포그라운드 서비스로 저장된 계정에 대한 IMAP IDLE 또는 폴링 연결을 유지하고, 새 메일을 모두 기록하며, 선택한 감시에 대해 AutoJs6 의 "메일 도착 시" 작업을 깨웁니다 (보낸 사람과 제목으로 필터 가능). 메일은 `engines.myEngine().execArgv.mail` 로 스크립트에 전달됩니다.
- 제공자: Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun 프리셋이 호스트, 포트, 암호화를 채워 주며, 다른 서버를 위해 어떤 필드든 재정의할 수 있습니다.
- 인증: 비밀번호와 제공자 인증 코드, 또는 스크립트가 갱신 콜백과 함께 제공하는 XOAUTH2 액세스 토큰을 지원합니다.
- 브라우저 로그인: Gmail, Outlook.com 또는 Microsoft 365 계정은 플러그인 설정에서 시스템 브라우저로 Google 또는 Microsoft 계정에 로그인하여 추가할 수 있습니다 (PKCE 를 사용하는 OAuth 2.0 인증 코드 흐름). 플러그인은 새로 고침 토큰을 기기 안에 암호화하여 보관하고 세션마다 액세스 토큰을 갱신하며 계정 페이지에 로그인 상태와 "다시 로그인" / "로그인 취소" 를 표시합니다. 스크립트는 계속 별칭으로 연결하며 토큰을 보지 않습니다.

******

### 사용 방법

******

1. AutoJs6 빌드 5282 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 에서 플러그인 APK 를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `Angus Mail` 이 인식되는지 확인하고 활성화합니다.
3. 계정을 준비합니다: 제공업체의 웹 설정에서 IMAP 또는 POP3 와 SMTP 를 켜고 인증 코드 (QQ, 163, 126, Sina) 또는 앱 비밀번호 (Gmail, iCloud, Yahoo) 를 얻습니다. 로그인 비밀번호 자체는 보통 허용되지 않습니다. Gmail, Outlook.com, Microsoft 365 계정은 대신 플러그인 설정에서 브라우저로 Google 또는 Microsoft 계정에 로그인하거나 (인증 방식에서 "Google / Microsoft 계정으로 로그인 (브라우저)" 선택), 다른 곳에서 얻은 OAuth 2.0 액세스 토큰을 줄 수 있습니다.
4. 스크립트에서 `mail.connect(...)`를 호출하거나, 플러그인 설정 페이지 (플러그인의 런처 아이콘 또는 AutoJs6 개발자 옵션 > 메일 계정 설정) 에 계정을 저장한 뒤 별칭으로 연결합니다.
5. 스크립트를 상주시키지 않고 새 메일에 실행하려면: 플러그인의 감시 페이지 (설정 > 감시: 계정 별칭, 폴더, 모드, 필터) 에서 감시를 추가하고, 요청 시 알림을 허용한 뒤, AutoJs6 에서 작업을 만들고 (스크립트 길게 누르기 > 정시 작업 > 브로드캐스트로 실행 > 메일 도착 시) 감시를 선택합니다. 이 작업에는 메일 계약 버전 2 를 포함한 AutoJs6 빌드가 필요합니다.

******

### 제공자 준비

******

모든 제공자는 먼저 웹 설정에서 IMAP (또는 POP3) 과 SMTP 를 켜야 하며, 로그인 비밀번호 대신 인증 코드, 앱 비밀번호 또는 액세스 토큰을 사용합니다; 각 프리셋 (`provider` 의 값) 의 요점:

- QQ 메일 (`qq`): 웹 계정 설정에서 IMAP/SMTP 서비스를 켜고 인증 코드를 생성하여 `password` 로 사용합니다.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net 은 `163` 프리셋에 호스트를 덮어써서 사용): 웹 설정의 POP3/SMTP/IMAP 페이지에서 서비스를 켜고 인증 코드를 생성합니다; POP3 는 별도로 켜야 하며, 그렇지 않으면 IMAP 과 SMTP 가 받아들이는 코드를 POP3 가 거부합니다. 서버는 모든 IMAP 연결이 먼저 `ID` 명령을 보내도록 요구하며 (아니면 `Unsafe Login` 으로 응답), 플러그인이 이를 자동으로 수행합니다.
- Sina 메일 (`sina`): 웹 클라이언트 설정에서 IMAP/SMTP 서비스를 켜고 인증 코드를 사용합니다. 서버는 보낸 메일 사본을 보관하지 않고 (플러그인이 보낸 편지함에 추가), IMAP 을 통한 폴더 생성을 거부하며, 텍스트 검색은 플러그인이 클라이언트에서 수행합니다.
- 가장 간단한 경로는 플러그인 설정 페이지의 브라우저 로그인 ("Google 계정으로 로그인 (브라우저)") 으로, 앱 비밀번호가 필요 없고 토큰이 자동으로 갱신됩니다. 그 밖의 방법: Gmail (`gmail`): 2 단계 인증을 켠 뒤 Google 계정에서 앱 비밀번호를 생성하여 `password` 로 쓰거나, `https://mail.google.com/` 범위의 OAuth 2.0 액세스 토큰 (`accessToken` 과 `tokenProvider`) 을 제공합니다; 폴더는 `[Gmail]` 네임스페이스에 있고 새 메일은 IDLE 로 푸시됩니다. 프로젝트는 토큰으로 실제 계정을 검증했습니다.
- 가장 간단한 경로는 플러그인 설정 페이지의 브라우저 로그인 ("Microsoft 계정으로 로그인 (브라우저)") 으로, 토큰의 획득과 갱신을 플러그인이 맡습니다. 그 밖의 방법: Outlook.com / Hotmail (`outlook`) 과 Microsoft 365 (`office365`): Microsoft 가 개인 계정의 기본 인증을 비활성화했으므로 앱 비밀번호는 IMAP, POP3, SMTP 모두에서 거부되며, `outlook` 프리셋은 OAuth 2.0 액세스 토큰 (`accessToken` 과 `tokenProvider`) 만 받아들입니다; 직장 또는 학교 계정 (`office365`) 은 비밀번호나 토큰을 쓸 수 있지만 테넌트 정책이 IMAP, POP3 또는 SMTP AUTH 를 비활성화할 수 있습니다. 토큰에는 `https://outlook.office.com/` 의 위임 범위 `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All`, `SMTP.Send` 가 필요합니다 (프로젝트는 그런 토큰으로 개인 계정을 검증했습니다: IMAP, POP3, SMTP 와 IDLE 푸시); 일부 최근 개인 사서함은 Microsoft 가 SMTP AUTH 를 비활성화했으며 (`535 5.7.139`) 사용자 설정에 켜는 스위치가 없습니다.
- iCloud (`icloud`): Apple 계정에서 앱 전용 비밀번호를 생성합니다; POP3 서비스가 없습니다.
- Yahoo (`yahoo`) 와 Aliyun 개인 메일 (`aliyun`): 앱 비밀번호 또는 인증 코드를 생성합니다; 이 두 프리셋은 공개 문서를 따르며 프로젝트에 테스트 계정이 없어 검증되지 않았습니다.

그 밖의 서버는 `provider` 를 비우고 `imap` (또는 `pop3`) 과 `smtp` 의 `host`, `port`, `tls` (`ssl`, `starttls` 또는 `none`) 를 지정합니다; 프리셋의 어떤 필드도 덮어쓸 수 있습니다. 모든 옵션은 [MailAccountOptions](https://docs.autojs6.com/#/mailAccountOptionsType) 에 설명되어 있으며, 내장 프리셋은 `mail.providers.list()` 로 볼 수 있습니다.

******

### 빠른 시작

******

별칭으로 연결하고, 보고서를 보내고, 첨부 파일이 있는 읽지 않은 메일을 읽고, 인증 코드를 감시하며, 비동기로 검색하는 스크립트:

```js
// A saved alias keeps the credential inside the plugin; an inline account works as well:
// mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
watch.on('error', e => console.warn(e.code, e.message));

// Every network method also has an Async form; every failure is a MailError with a code.
mail.setDefault(client);
mail.searchAsync({ subject: 'invoice', since: '2026-09-01' }).then(list => console.log(list.length, list.fallback));
```

******

### 저장된 계정과 별칭

******

별칭은 플러그인 안에 저장된 계정입니다. 플러그인 설정 페이지 (런처 아이콘 또는 AutoJs6 개발자 옵션 > 메일 계정 설정) 에서 계정을 입력하고 연결을 테스트한 뒤 저장하면, 비밀번호나 토큰은 Android Keystore 키로 암호화되어 플러그인의 비공개 디렉터리에 저장되고 백업에서 제외됩니다; 이후 스크립트는 `mail.connect('별칭')` 으로 연결하며, 자격 증명은 스크립트도 Binder 도 거치지 않습니다.

설정 페이지에서 계정 하나를 기본으로 표시할 수 있습니다; `mail.accounts.list()` 가 반환하는 항목에는 해당 계정에 `default: true` 가 붙고, `mail.accounts.has(alias)` 는 별칭의 존재를 확인합니다. 여러 계정을 동시에 쓸 때는 별칭마다 클라이언트를 하나씩 만듭니다; `mail.setDefault(client)` 이후에는 `mail.fetch(...)` 같은 전달 메서드가 기본 클라이언트에 작용합니다.

******

### 호환성

******

아래 결론은 로드맵 P6 의 실제 계정 매트릭스 (2026-09-19 와 09-20: 각 제공자가 중국어 제목, 본문, 표시 이름, 파일 이름을 가진 메일을 자기 자신에게 보낸 뒤 모든 작업을 검증) 와 TLS, 문자 집합, 수명 주기, 성능 매트릭스에서 나온 것입니다:

- QQ 메일: 보내기, 목록, 본문, 첨부, 플래그, 이동 (`MOVE`), POP3 모두 통과; 중국어 검색에 서버가 오류 대신 0 건으로 응답하므로 `fallback: 'always'` 가 필요; 보낸 메일의 Message-ID 가 서버에서 다시 쓰임; 폴더 생성 거부; 감시는 폴링 (IDLE 은 푸시하지 않음) 이며 새 메일은 배달 후 15-40 초 뒤에 서버에서 보입니다.
- 163 / 126 / yeah.net: 모두 통과; 서버가 보낸 사본을 보관; IDLE 이 없어 감시는 폴링; 163 은 최근 메일에 대한 텍스트 검색에 0 건으로 응답 (126 과 yeah.net 은 정상); 보낸 사람 표시 이름의 공백이 밑줄로 돌아옴; yeah.net 의 POP3 는 웹 설정에서 별도로 켜야 합니다.
- Sina 메일: 통과; 서버는 ALL, SINCE, 플래그 조건만 받아들이므로 텍스트 검색은 자동으로 클라이언트 필터링으로 대체; IDLE 없음; 폴더 생성 거부; 보낸 사본은 플러그인이 추가합니다.
- Gmail: OAuth 2.0 토큰으로 모든 행 통과, 새 메일은 IDLE 로 푸시 (약 30 초, Gmail 자체의 알림 주기); 중국어를 포함한 서버 검색 모두 적중 (플러그인은 `UTF8=ACCEPT` 를 켜지 않음); 사용자 지정 IMAP 키워드가 저장됨 (여섯 곳 중 유일); POP3 보기에는 계정이 자기 자신에게 보낸 메일이 없습니다.
- Outlook.com / Hotmail: OAuth 2.0 토큰으로 개인 계정 하나에서 모든 행을 통과 (2026-09-21): 폴더 역할은 관례적 이름에서, 서버가 보낸 편지 사본을 보관하고 Message-ID 를 다시 씀, `MOVE` 와 폴더 생성 정상, POP3 는 두 줄 형식의 `AUTH XOAUTH2` 로 로그인, IDLE 은 약 10 초 안에 푸시 (일곱 제공자 중 가장 빠름); 중국어 제목의 서버 검색은 적중하지만 수 분이 걸릴 수 있음; 앱 비밀번호는 IMAP, POP3, SMTP 에서 여전히 거부되며 (`AUTH_MECHANISM_UNSUPPORTED`), 일부 최근 개인 사서함은 Microsoft 가 SMTP AUTH 를 비활성화함 (`535 5.7.139`); iCloud, Yahoo, Aliyun 은 테스트 계정이 없어 프리셋이 검증되지 않았습니다.
- TLS 와 문자 집합: 암시적 SSL, STARTTLS, 평문, 자체 서명 인증서 (`tls.trustAll` 유무), 호스트 이름 불일치, 포트 모드 불일치를 IMAP, POP3, SMTP 에서 하나씩 테스트했고, 실패는 `TLS_FAILED`, `TIMEOUT` 같은 구별 가능한 코드로 매핑됩니다; GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR, UTF-8 의 제목, 표시 이름, 본문, 파일 이름을 선언됨, 선언되지 않음, 잘못 선언됨 세 경우로 검증했습니다.
- 기기와 수명 주기: Android 7.0 (API 24) 에뮬레이터와 Sony (Android 9), Redmi (Android 13) 실기; 스크립트 정상 종료, `exit()`, `engines.stopAll()`, 호스트 또는 플러그인 강제 중지, 제자리 업그레이드, 플러그인 비활성화와 제거의 여덟 가지 종료 방식에서 연결, 바인딩, 스레드가 해제됩니다; 화면이 꺼져 Doze 에 들어가면 감시는 연결을 잃고 기기가 깨어난 뒤 복구되며, 끊김 없는 감시가 필요하면 설정 페이지에서 배터리 최적화 제외를 요청할 수 있습니다.
- 성능 기준선: 로컬 10000 통 받은 편지함과 50 MiB 첨부 파일에 대한 목록, 검색, 다운로드, 보내기, 1 시간 IDLE 대기를 JVM, Redmi, Sony 에서 측정하여 `docs/dev/p6-performance-baseline.md` 에 기록했습니다; 메일 문서에는 상한이 있어 (주소, 헤더, MIME 트리, 인라인 본문) 적대적 입력이 세션을 무너뜨리지 못합니다.

******

### 자주 묻는 질문

******

- **`AUTH_FAILED` 는 어떻게 해결하나요?** 로그인 비밀번호가 아니라 인증 코드나 앱 비밀번호를 쓰고 있는지, 웹 설정에서 해당 프로토콜이 켜져 있는지 (IMAP 과 POP3 는 따로 켬) 확인하고, `client.test()` 로 수신 엔드포인트와 SMTP 의 결과와 오류 코드를 따로 확인합니다. 토큰 계정의 `AUTH_FAILED` 는 보통 토큰 만료이며, `tokenProvider` 가 있으면 플러그인이 갱신하고 한 번 재시도합니다. 오류의 `code`, `details`, `retryable` 이 재시도할 가치가 있는지 알려 줍니다.
- **163 / 126 이 `Unsafe Login` 으로 응답하나요?** NetEase 의 IMAP 서버는 `ID` 명령을 보내지 않은 연결을 거부합니다; 플러그인은 모든 IMAP 연결에서 로그인 직후 `ID` 를 보내므로 보통은 발생하지 않습니다. 그래도 발생하면 웹 설정에서 IMAP 서비스를 다시 켜고 인증 코드를 새로 생성하세요.
- **중국어 검색에 결과가 없나요?** 서버마다 비 ASCII 검색을 다르게 처리합니다: Sina 는 거부하고 (플러그인이 스스로 클라이언트 필터링으로 대체), QQ 와 163 은 오류 없이 0 건을 반환합니다 (기본 `fallback: 'client'` 는 발동하지 않음). 이런 계정에서는 `fallback: 'always'` 를 쓰고 `since` 나 `limit` 으로 범위를 좁히세요; 클라이언트 본문 필터링은 후보를 한 통씩 가져오므로 큰 사서함에서는 느릴 수 있습니다.
- **`mail.connect` 는 성공했는데 첫 `fetch` 에서 실패하나요?** `connect` 는 플러그인 세션만 열고 메일 서버에 접속하지 않습니다; 첫 네트워크 메서드가 로그인합니다 (SMTP 는 첫 보내기 때). 미리 확인하려면 `client.test()` 를 호출하세요.
- **화면이 꺼지면 감시가 멈추나요?** Android 의 Doze 는 백그라운드 앱의 네트워크를 동결하므로 감시는 연결을 잃고, 새 메일은 기기가 깨어난 뒤 몇 분 안에 보고됩니다 (Doze 가 끝나면 플러그인이 즉시 다시 연결). 끊김 없는 감시가 필요하면 설정 페이지의 안내 버튼으로 플러그인의 배터리 최적화 제외를 요청하세요; 감시는 스크립트가 실행되는 동안만 유효하며 스크립트가 종료되면 닫힙니다.
- **Outlook.com / Hotmail 은 어떻게 연결하나요?** Microsoft 가 개인 계정의 기본 인증을 비활성화했으므로 프리셋은 비밀번호를 받지 않습니다. 플러그인 설정 페이지에서 계정을 저장하고 인증 방식으로 "Microsoft 계정으로 로그인 (브라우저)" 를 선택하세요: 브라우저에 Microsoft 로그인 페이지가 열리고, 플러그인으로 돌아오는 것은 인증 코드뿐이며, 액세스 토큰은 플러그인이 스스로 갱신합니다. 그 뒤 스크립트는 별칭으로 연결합니다. 스크립트는 다른 곳에서 얻은 액세스 토큰을 `accessToken` 으로 넘기고 `tokenProvider` 로 갱신할 수도 있습니다. 계정 페이지에 "다시 로그인 필요" 가 보이면 갱신이 거부된 것입니다 (로그인이 취소되었거나 만료됨): 계정의 메뉴에서 "다시 로그인" 을 선택하세요.
- **POP3 계정으로 무엇을 할 수 있나요?** `INBOX` 만 있고 `uid` 는 UIDL 문자열입니다; 목록, 읽기, 다운로드, 삭제, 폴링 감시가 가능하고, 플래그, 이동, 복사, 추가, 비우기, 폴더 관리는 `UNSUPPORTED_OPERATION` 을 반환합니다; 검색은 클라이언트에서 봉투 조건으로만 수행됩니다.

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 진입점은 서명 권한 `org.autojs.permission.PLUGIN`으로 보호되어 AutoJs6만 접근할 수 있으며, 플러그인은 다른 구성 요소를 내보내지 않습니다.
- INTERNET 권한은 스크립트가 지정한 서버로의 IMAP, POP3, SMTP 연결과, 브라우저로 로그인한 계정의 경우 Google (`oauth2.googleapis.com`) 및 Microsoft (`login.microsoftonline.com`) 의 토큰 엔드포인트로의 HTTPS 요청에만 쓰입니다: 인증 코드 교환, 만료가 임박한 액세스 토큰 갱신, 요청 시 Google 토큰 취소입니다. 플러그인은 다른 요청을 하지 않으며 데이터를 수집하지 않습니다.
- 비밀번호와 토큰은 스크립트에서 플러그인으로 Binder 전용 필드를 통해 전달되며, 로그, JSON 문서, 오류 메시지, 충돌 보고서에 나타나지 않고 세션이 유지되는 동안만 메모리에 머뭅니다. 설정 페이지에 저장한 계정은 Android Keystore 키로 암호화되며 백업에서 제외됩니다. 브라우저 로그인의 토큰도 같은 방식으로 저장됩니다: 새로 고침 토큰은 기기를 떠나지 않고, 메일 세션에는 액세스 토큰만 전달되며, AutoJs6 도 스크립트도 읽을 수 없습니다. "로그인 취소" 는 즉시 폐기합니다.
- 연결은 기본적으로 TLS를 사용합니다 (제공자 요구에 따라 SSL 또는 STARTTLS). 평문 연결과 자체 서명 인증서는 계정마다 명시적으로 요청해야 합니다.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 권한은 설정 화면의 안내 버튼만을 위한 것입니다: 버튼은 시스템이 백그라운드에서 플러그인을 중단할 수 있는지 표시하고, 요청 시 시스템 대화 상자를 엽니다; 플러그인이 스스로 요청하는 일은 없으며, 이 제외에 의존하는 기능도 없습니다. P5 감시 매트릭스는 이 제외의 의미를 측정했습니다: 화면이 한동안 꺼지면 (Doze) Android 가 백그라운드 앱의 네트워크를 동결하여 감시가 끊기고, 재연결은 시간 초과되며, 새 메일은 기기가 깨어난 뒤 몇 분 후에 보고됩니다 (Android 9 에서 약 4 분; Doze 가 끝나면 플러그인이 즉시 재연결함); 제외하면 감시가 연결을 유지합니다.
- 네 가지 권한은 1.1.0 의 백그라운드 감시에만 쓰입니다. FOREGROUND_SERVICE 와 FOREGROUND_SERVICE_SPECIAL_USE 는 감시 서비스 (유형 `specialUse`, 하위 유형 `mail_background_watch`: 메일 감시는 범위가 정해진 데이터 동기화가 아니라 서버의 푸시를 기다리는 열린 연결이기 때문) 를 낮은 우선순위 알림 하나와 함께 실행합니다. POST_NOTIFICATIONS 는 Android 13 이상에서 그 알림을 표시할 수 있도록 감시 페이지에서 감시를 켤 때만 요청됩니다. RECEIVE_BOOT_COMPLETED 는 그 페이지의 부팅 시 시작 스위치를 뒷받침하며 기본은 꺼짐이고 켤 때만 리시버를 활성화합니다. 서비스는 감시 페이지나 호스트 구독에서만 시작되고, 저장된 계정에만 연결하며, 비밀은 플러그인 프로세스 안에 남습니다. AutoJs6 로 가는 깨우기 브로드캐스트는 메일 봉투만 담고 (본문은 절대 없음) PLUGIN 서명 권한 뒤의 리시버에만 도달합니다.

플러그인은 공식 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 페이지 또는 AutoJs6 플러그인 센터에서만 받으세요. 출처를 알 수 없는 패키지는 버전 번호가 같아 보여도 호스트 검증에 실패하거나 위험을 동반할 수 있습니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것입니다. 호스트는 이 식별자로 플러그인을 발견하고 호환성을 협상합니다:

```text
application id: io.github.supermonster003.autojs6.plugin.angus.mail
plugin id: angus-mail
engine: mail
variant: default
service action: org.autojs.plugin.MAIL
service category: mail
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.mail.api.IMailPlugin
minimum host build: 5282 (6.8.0)
```

`AngusMailPluginService`는 호스트 mail-api 계약 `org.autojs.plugin.mail.api.IMailPlugin`를 구현하고 `org.autojs.plugin.MAIL` (category `mail`)에 응답합니다. `AngusMailPluginInfoService`는 `org.autojs.plugin.INFO`에 PluginInfo로 응답합니다. `WakeActivity`를 통해 호스트가 플러그인을 활성화할 수 있습니다.

******

### 로드맵

******

플러그인의 계획과 진행 상황은 ROADMAP.md에 체크 가능한 목록으로 관리되며, 단계별로 수락 기준과 증거 수준이 함께 기록됩니다. 체크되지 않은 항목은 현재 기능이 아니라 의도를 나타냅니다. Issues를 통한 논의를 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.2.0

_2026/09/21_

- `힌트` 브라우저 로그인은 해당 제공업체의 OAuth 2.0 클라이언트 id 를 담은 빌드에서만 제공됩니다 (관리자의 등록 정보는 빌드 시 Git 이 무시하는 `oauth-clients.properties` 에서 읽습니다). 이것이 없는 빌드는 토큰과 앱 비밀번호 경로를 유지하며 인증 방식 대화 상자에서 그렇게 말합니다. Google 클라이언트는 Google Cloud 프로젝트의 민감한 scope 검증을 통과해야 임의의 계정으로 로그인할 수 있으며, 그 전까지 Google 은 프로젝트의 테스트 사용자로 제한합니다.
- `기능` Google 및 Microsoft 계정의 브라우저 로그인 (메일 로드맵 P9): 계정 편집기는 Gmail 프리셋에 "Google 계정으로 로그인 (브라우저)", Outlook.com 및 Microsoft 365 프리셋에 "Microsoft 계정으로 로그인 (브라우저)" 를 제공합니다. 로그인은 Custom Tab (대체는 아무 브라우저) 에서 제공업체 페이지를 열고 PKCE (`S256`) 와 무작위 `state` 를 담은 OAuth 2.0 인증 코드 요청을 보내며, 리디렉션 (`<applicationId>://oauth2/microsoft`, 또는 Google 의 역순 클라이언트 id 스킴) 은 `OAuthRedirectActivity` 에 도착하여 대기 중인 로그인 화면으로 전달됩니다. 화면은 `state` 가 일치하지 않는 리디렉션을 거부하고, HTTPS 로 토큰 엔드포인트에서 코드를 교환하며 (`HttpsFormPoster`, 플러그인의 유일한 HTTP 클라이언트), id 토큰에서 주소를 미리 채웁니다
- `기능` 토큰 저장과 갱신: 브라우저 로그인의 토큰은 계정 저장소의 새 종류 `OAUTH2` 인 암호화 레코드 하나입니다 (Binder 필드에도 `mail.accounts.list()` 에도 절대 나타나지 않음). 계정 문서는 `oauth` 객체 (`provider`, `authorizedAt`, `expiresAt`, `needsReauth`) 를 가지며 `mail.accounts.list()` 가 이를 보고합니다. 그 별칭의 모든 세션 (스크립트, 연결 테스트, 백그라운드 감시) 은 `AccountSecrets` 에서 액세스 토큰을 받으며, 5 분 미만 남으면 새로 고침 토큰으로 갱신합니다 (계정별 직렬화). 갱신이 거부되면 (`invalid_grant`) 레코드에 `needsReauth` 가 표시되고 세션은 `AUTH_FAILED` ("sign in again") 로 실패하며 계정 페이지는 해당 계정 옆에 "다시 로그인 필요" 를 표시합니다
- `기능` 계정 페이지 작업 "다시 로그인" (새 브라우저 로그인을 같은 레코드에 저장) 과 "로그인 취소" (레코드의 토큰을 즉시 취소 표시로 바꾸고 Google 에 새로 고침 토큰 취소를 요청하며 다시 로그인할 때까지 계정이 작동하지 않음). `gmail`, `outlook`, `office365` 프리셋의 `authHint` 는 브라우저 로그인을 먼저 언급합니다 (`providers.json` 버전 4). 11 개 언어에 35 개의 새 문자열. JVM 테스트: PKCE, 인증 요청과 리디렉션 파싱, 스크립트화된 전송에 대한 토큰 클라이언트, 토큰 문서, 제공업체 표, 빌드의 클라이언트와 리디렉션 URI, `AccountSecrets` (갱신, 거부 표시, 취소된 레코드, 지우기), 계정 옵션 / 폼 / 계정 문서의 `oauth` 객체

#### v1.1.0

_2026/09/21_

- `힌트` 1.1.0 에 백그라운드 감시가 추가되었습니다 (메일 로드맵 P8): 설정의 감시 페이지는 스크립트가 실행되지 않는 동안에도 포그라운드 서비스로 저장된 계정의 감시를 유지하고 AutoJs6 의 "메일 도착 시" 작업을 깨웁니다. 이 작업과 감시 선택기는 메일 계약 버전 2 를 포함한 호스트 빌드 (AutoJs6 6.8.0 빌드 5282 이후) 가 필요합니다. 이전 호스트에서는 페이지에 AutoJs6 를 깨울 수 없다고 표시되며 새 메일은 감시의 기록 목록에만 남습니다. 이 기능은 네 가지 권한을 추가하며 각 이유는 README 보안 절에 있습니다: FOREGROUND_SERVICE 와 FOREGROUND_SERVICE_SPECIAL_USE (감시 서비스), POST_NOTIFICATIONS (상주 알림, 감시를 켤 때만 요청), RECEIVE_BOOT_COMPLETED (감시 페이지의 부팅 시 시작 스위치, 기본 꺼짐).
- `기능` 백그라운드 감시 (메일 로드맵 P8): 설정에 감시 페이지가 추가되어 저장된 계정에 최대 16 개의 감시 (`MAX_TRIGGERS`) 를 설정할 수 있습니다. 각 감시는 이름, 계정 별칭, 폴더, 모드 (자동, IDLE 또는 간격이 있는 폴링), 선택적 보낸 사람 / 제목 필터를 가지며 no-backup 디렉터리의 `mail-triggers/triggers.json` 에 저장됩니다. `specialUse` 포그라운드 서비스 `MailWatchService` 는 스크립트가 실행되지 않는 동안 P5 워처로 활성 감시를 실행하고 (서버가 푸시하면 IDLE, 아니면 폴링, 백오프 재연결, 네트워크 변경 시 즉시 재연결) 낮은 우선순위 알림 하나를 표시합니다. 새 메일은 감시의 기록 목록 (최근 100 개의 봉투 요약, `MAX_TRIGGER_RECORDS`, 본문 없음) 에 추가되고 페이지는 연결 상태, 마지막 오류, 기록, 재연결 동작을 보여 줍니다. 부팅 시 시작 스위치 (기본 꺼짐) 는 재부팅 후 서비스를 다시 시작하는 `BOOT_COMPLETED` 리시버를 활성화합니다
- `기능` 메일 계약 버전 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, 기능 플래그 `backgroundWatch`): 호스트는 generation 과 선택적 필터로 설정된 감시를 구독하고 즉시 현재 상태를 받은 뒤 `onStatus` (stopped, connecting, connected 또는 failed 와 이유 및 마지막 오류) 와 일치하는 메일마다 `onMail(generation, seq, event)` 를 받습니다. `mail` 이벤트는 감시 id, 별칭, 주소, 폴더, 메일 봉투, `receivedAt` 를 담습니다. 감시당 구독자는 최대 4 (`MAX_TRIGGER_SUBSCRIBERS`), 비활성 또는 존재하지 않는 감시와 사용할 수 없는 옵션은 이유가 `refused` 인 `stopped` 상태로 거부되고, `update` 는 구독자의 필터를 교체하며 `stop` 은 구독만 끝냅니다. 살아 있는 구독자가 없으면 각 이벤트는 `PLUGIN` 서명 권한 뒤의 AutoJs6 로 명시적 브로드캐스트 `org.autojs.autojs6.action.MAIL_TRIGGER` 로 전달되어 스크립트가 실행 중이 아니어도 호스트의 "메일 도착 시" 작업을 시작합니다 (API 37 AVD 의 `MailTriggerBinderTest`; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `기능` 메일 코어에 페이지, 서비스, Binder 가 공유하는 트리거 문서와 규칙 (`TriggerConfig`, 보낸 사람과 제목 부분 문자열을 대소문자 구분 없이 비교하는 `TriggerFilter`, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) 과 상한 `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS`, `MIN_TRIGGER_INTERVAL_MS` (3 초, 호스트의 작업별 스로틀) 가 추가되었고, 워처는 `onConnected` 를 보고하여 백그라운드 감시가 폴더를 연 즉시 `connected` 를 표시합니다

#### v1.0.1

_2026/09/21_

- `수정` OAuth 2.0 토큰을 쓰는 Outlook.com 의 POP3 (메일 로드맵 P6 제공자 매트릭스, 2026-09-21): 서버가 Angus Mail 이 기본으로 보내는 한 줄 형식의 `AUTH XOAUTH2 <base64>` 에 `-ERR Protocol error. Connection is closed.` 로 답하고 연결을 끊어 `outlook` 프리셋의 POP3 계정이 `AUTH_FAILED` 로 실패했습니다; 제공자 프리셋에 `pop3Xoauth2TwoLine` (카탈로그 버전 3, `outlook` 과 `office365` 에서 true) 을 추가하고, 메일 코어는 이 프리셋들과 프리셋 없이 입력된 Microsoft POP3 호스트에 대해 명령만 먼저 보낸 뒤 서버의 `+` 계속 응답 후에 base64 응답을 보냅니다 (`Pop3OAuthScriptedTest` 5 건; 실제 계정으로 JVM 과 API 33 실기기에서 검증)
- `개선` 제공자 매트릭스의 Outlook.com 열 (메일 로드맵 P6) 과 P5 기기 행을 관리자의 Entra 공용 클라이언트 등록으로 얻은 토큰으로 개인 계정 하나에서 실행: 세션 테스트, 폴더 (역할은 관례적 이름에서, SPECIAL-USE 없음), 보내기 (`sentCopy = server`, Message-ID 는 서버가 다시 씀), 목록 (약 7 초 후 표시), 서버 검색 (모든 키 적중; 중국어 제목도 정확히 적중하지만 2300 통의 받은 편지함에서 서버가 수 분 소요), 본문, 첨부, 플래그 (사용자 키워드는 저장되지 않음), 폴더 생성, `MOVE`, 감시 (IDLE 이 제출 후 약 10 초 안에 푸시, 일곱 제공자 중 가장 빠름), POP3 (5 자 UIDL, 방금 보낸 메일도 표시) 와 정리 (보낸 사본을 UID 로 지정 가능) 모두 통과; Redmi (API 33) 에서 baseline / 플러그인 강제 종료 / Wi-Fi 끄기 세 시나리오가 9 에서 14 초에 도착하고 이벤트 순서는 Gmail 과 같음; 프리셋 notes, README, 증거 파일에 차이를 기록 (Microsoft 가 최근 개인 사서함에 주는 `535 5.7.139 SmtpClientAuthentication is disabled for the Mailbox` 거부 포함)

##### 더 많은 릴리스 기록

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드와 검증

******

이 섹션은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다. 일반 사용자는 Releases 페이지의 미리 빌드된 APK를 설치하면 됩니다.

디버그 APK 빌드:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM 단위 테스트 실행 및 계측 테스트 APK 빌드:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

릴리스 APK 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

릴리스 산출물을 수집하고 파일 이름에 버전과 CRC32 다이제스트를 추가:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

다국어 문서 소스와 생성된 산출물이 동기화되어 있는지 검증 (CI에서도 적용):

```powershell
py .python\generate_markdown.py --check
```

빌드에는 JDK 21 이상과 Android SDK 37이 필요합니다. Gradle과 플러그인 버전은 `version.properties`와 `io.github.supermonster003.autojs6-platform-versions`로 중앙에서 관리됩니다.

******

### 현지화와 문서 생성

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/`와 `.changelog/`의 언어 JSON 파일이 README, 플러그인 센터 안내, 변경 기록의 유일한 소스입니다. 항상 이 JSON 소스를 편집하고 `py .python/generate_markdown.py`를 다시 실행하세요. 생성된 README, `plugin_instruction.md`, 변경 기록 산출물은 절대 손으로 편집하지 않습니다. `py .python/generate_markdown.py --check`를 실행하면 모든 생성 산출물을 검증할 수 있습니다.

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE)에 따라 제공됩니다. 서드파티 구성 요소와 라이선스는 [서드파티 고지](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md)에 나열되어 있습니다.

******

### 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: https://docs.autojs6.com
- 메일 모듈 문서: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
