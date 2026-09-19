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

버전 1.0.0은 개발 중입니다: 저장소 뼈대, 로컬 서버 테스트를 갖춘 메일 코어, AutoJs6 플러그인 센터용 플러그인 식별 정보가 준비되었으며, Binder 계약, 스크립트 API, 설정 페이지는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)의 단계에 따라 진행됩니다. AutoJs6 6.8.0 (빌드 5282) 이상이 필요합니다.

******

### 기능

******

플러그인은 다음 기능을 제공합니다:

- 보내기: 일반 텍스트 또는 HTML, 여러 수신자, 첨부 파일과 인라인 이미지, 사용자 지정 헤더와 우선순위를 지원하며, 제공자가 보낸 편지함에 자동 저장하지 않으면 플러그인이 서버에 저장합니다.
- 받기: 폴더를 페이지 단위로 나열하고, 서버에서 검색하며 (비 ASCII 검색을 거부하는 제공자에서는 클라이언트 필터로 대체), 텍스트와 HTML 본문을 읽고, 첨부 파일을 스크립트 작업 디렉터리로 바로 다운로드합니다.
- 정리: 읽음 또는 플래그 표시, 이동, 복사, 삭제, 완전 삭제, 폴더 만들기, 이름 바꾸기, 삭제를 지원합니다. POP3 계정은 읽기 전용 부분 집합을 사용합니다.
- 감시: 스크립트가 실행되는 동안 새 메일 이벤트를 받으며, 서버가 실제로 푸시하면 IMAP IDLE 을, 그렇지 않으면 폴링 (기본 60 초, 조정 가능) 을 사용합니다: QQ 와 Sina 는 IDLE 을 받아들이지만 알리지 않고, 163 과 126 에는 IDLE 이 없으며, POP3 계정은 항상 폴링합니다; 네트워크 끊김과 플러그인 프로세스 재시작 후에도 감시는 자동으로 복구됩니다.
- 제공자: Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun 프리셋이 호스트, 포트, 암호화를 채워 주며, 다른 서버를 위해 어떤 필드든 재정의할 수 있습니다.
- 인증: 비밀번호와 제공자 인증 코드, 또는 스크립트가 갱신 콜백과 함께 제공하는 XOAUTH2 액세스 토큰을 지원합니다.

******

### 사용 방법

******

1. AutoJs6 빌드 5282 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `Angus Mail`이 인식되는지 확인하고 활성화합니다.
3. 계정을 준비합니다: 메일 제공자 설정에서 IMAP 또는 POP3를 켜고, 인증 코드나 앱 비밀번호 (QQ, 163, 126, Gmail, iCloud) 또는 OAuth 2.0 액세스 토큰 (Outlook.com)을 얻습니다.
4. 스크립트에서 `mail.connect(...)`를 호출하거나, 플러그인 설정 페이지 (플러그인의 런처 아이콘 또는 AutoJs6 개발자 옵션 > 메일 계정 설정) 에 계정을 저장한 뒤 별칭으로 연결합니다.

******

### 빠른 시작

******

보고서를 보내고, 첨부 파일이 있는 읽지 않은 메일을 읽고, 인증 코드를 기다리는 스크립트:

```js
let client = mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' });

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
```

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 진입점은 서명 권한 `org.autojs.permission.PLUGIN`으로 보호되어 AutoJs6만 접근할 수 있으며, 플러그인은 다른 구성 요소를 내보내지 않습니다.
- INTERNET 권한은 스크립트가 지정한 서버로의 IMAP, POP3, SMTP 연결에만 사용되며, 플러그인은 다른 요청을 보내지 않고 어떤 데이터도 수집하지 않습니다.
- 비밀번호와 토큰은 스크립트에서 플러그인으로 Binder 전용 필드를 통해 전달되며, 로그, JSON 문서, 오류 메시지, 충돌 보고서에 나타나지 않고 세션이 유지되는 동안만 메모리에 머뭅니다. 설정 페이지에 저장한 계정은 Android Keystore 키로 암호화되며 백업에서 제외됩니다.
- 연결은 기본적으로 TLS를 사용합니다 (제공자 요구에 따라 SSL 또는 STARTTLS). 평문 연결과 자체 서명 인증서는 계정마다 명시적으로 요청해야 합니다.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 권한은 설정 화면의 안내 버튼만을 위한 것입니다: 버튼은 시스템이 백그라운드에서 플러그인을 중단할 수 있는지 표시하고, 요청 시 시스템 대화 상자를 엽니다; 플러그인이 스스로 요청하는 일은 없으며, 이 제외에 의존하는 기능도 없습니다. P5 감시 매트릭스는 이 제외의 의미를 측정했습니다: 화면이 한동안 꺼지면 (Doze) Android 가 백그라운드 앱의 네트워크를 동결하여 감시가 끊기고, 재연결은 시간 초과되며, 새 메일은 기기가 깨어난 뒤 몇 분 후에 보고됩니다 (Android 9 에서 약 4 분; Doze 가 끝나면 플러그인이 즉시 재연결함); 제외하면 감시가 연결을 유지합니다.

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

#### v1.0.0

_2026/09/19_

- `힌트` P0 개발 미리보기: 저장소 뼈대, 로컬 서버 테스트를 갖춘 메일 코어, AutoJs6 플러그인 센터용 플러그인 식별 정보. Binder 계약, 스크립트 API, 설정 페이지는 ROADMAP.md의 단계에 따라 진행됩니다.
- `기능` 플러그인 식별자 `angus-mail` (엔진 `mail`), INFO 서비스, Wake Activity, 그리고 `org.autojs.plugin.MAIL` 서비스; 해당 `IMailPlugin` Binder 는 플러그인 정보, 기능, 공급자 및 저장된 계정 목록과 세션 봉투에 응답 (개별 작업은 P2 에서 구현)
- `기능` Eclipse Angus Mail 기반 메일 코어: SSL 또는 STARTTLS를 쓰는 IMAP / POP3 / SMTP 세션 속성, 비밀번호와 XOAUTH2 인증, SMTP 전송과 IMAP 받은 편지함 나열을 로컬 GreenMail 서버에서 검증
- `기능` 메일 코어 계정 계층 (로드맵 P2.1): Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun 프로바이더 프리셋, 프로토콜별 타임아웃, `tls.trustAll`, IMAP `ID` 명령, 마스킹된 `debug` 트레이스를 갖춘 계정 옵션; 세션은 지연 연결하고 유휴 연결을 끊으며 끊김 후 재연결; `session.test` 는 Binder 를 통해 엔드포인트별 기능과 왕복 시간을 반환
- `기능` 발신 (로드맵 P2.2): `mail.send` 는 to / cc / bcc / replyTo, 텍스트와 HTML 본문 (`multipart/alternative`), 호스트가 전달한 디스크립터에서 읽는 첨부 파일과 인라인 이미지 (`multipart/mixed` / `multipart/related`), 사용자 정의 헤더, 우선순위, `inReplyTo` / `references` 와 날짜를 지원; 수신자, 첨부, 헤더 상한과 헤더 인젝션 거부; `saveToSent` 는 프로바이더가 자동 저장하지 않을 때만 IMAP 으로 사본을 추가; `messages.append` 는 초안을 저장하고 UID 를 반환; 실제 기기에서 QQ 메일과 Gmail 로 검증
- `기능` 수신 (로드맵 P2.3): `folders.list` (트리, special-use 역할은 LIST / XLIST 속성 또는 관례적 이름에서 취득, 개수는 선택), `folders.status` / `create` / `delete` / `rename`; `messages.list` 는 UID 를 커서로 사용하고 (`before` / `after`) `order` 와 `unseenOnly` 를 지원하며 엔벨로프만 가져옴 (`hasAttachments` 는 BODYSTRUCTURE 로 판정); `messages.search` 는 쿼리 JSON (`from` / `to` / `subject` / `body` / `text` / 날짜 / 플래그 / 크기 / `header` / `messageId` / `uid` 와 `and` / `or` / `not`) 을 IMAP SEARCH 로 컴파일하고 서버가 거부하면 클라이언트에서 필터링 (`fallback`); `messages.get` 은 텍스트와 HTML 본문 (HTML 만 있는 메일은 텍스트를 파생), 전체 헤더, `partId` 가 있는 첨부 목록, 인라인 상한 (`bodyTruncated` / `bodyParts`) 과 `includeRaw` 를 반환; `attachments.download` 와 `messages.raw` 는 호스트의 디스크립터로 진행률과 함께 스트리밍; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 문자 집합 복원은 GBK / GB 18030 / ISO-2022-JP, 선언되지 않았거나 알 수 없는 문자 집합, 원시 8비트 헤더와 RFC 2231 / 2047 파일 이름을 다루며 파일 이름을 정제; IMAP `ID` 명령을 모든 연결에서 전송 (163 / 126은 식별되지 않은 연결을 거부), 서버가 `UID EXPUNGE`를 해석하지 못하면 폴더 전체 `EXPUNGE`로 대체; 실제 기기에서 QQ 메일, 163 메일, Gmail로 검증
- `기능` POP3 (로드맵 P2.4): `receive: "pop3"` 계정은 같은 작업으로 유일한 `INBOX` 를 읽고 `uid` 는 UIDL 문자열: `folders.list` 는 `INBOX` 만 반환 (개수는 선택), `messages.list` 는 UIDL 커서로 페이징하며 헤더만 가져옴 (`TOP`), `messages.search` 는 클라이언트에서 헤더를 최신순으로 필터링하고 `limit` 개가 채워지면 중단 (후보 최대 200개, 각각 `TOP` 왕복 1회), `messages.get` / `messages.raw` / `attachments.download` 는 메일 전체를 다운로드, `messages.delete` 는 `DELE` 를 보내고 메일함을 닫을 때 확정; 플래그, 이동, 복사, expunge, 추가, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` 와 본문 검색은 연결 전에 `UNSUPPORTED_OPERATION` 을 반환; 실제 기기에서 QQ 메일로 검증
- `기능` Binder 세션 제어 (로드맵 P2.5): 설치되어 있고 이 플러그인과 같은 키로 서명된 AutoJs6 호스트만 세션을 열거나 저장된 계정을 나열할 수 있음 (그 외에는 `SecurityException`, MCP Server 플러그인과 같은 규칙); 요청과 응답 봉투는 `MAX_ENVELOPE_BYTES` 이내, 오류 메시지는 `MAX_ERROR_MESSAGE_BYTES` 이내; 각 세션은 호출을 순서대로 실행하고 실행 중인 호출 뒤에 최대 `MAX_QUEUED_CALLS` 개를 대기시키며 그 이상은 `LIMIT_EXCEEDED` 로 거부; `cancel` 은 대기 중인 호출에 즉시 응답하고 실행 중인 호출은 소켓을 닫아 중단하므로 응답 없는 서버에서 읽기 시간 초과까지 기다리지 않음; `close` 는 보류 중인 모든 호출에 `SESSION_CLOSED` 로 응답; `getStatus` 는 `queued` 와 `active` 를 보고; 작업 표는 각 작업이 지원하는 수신 프로토콜을 명시하므로 POP3 계정은 인자 해석 전에 거부됨; 기능 목록은 `append` 와 `clientSearchFallback` 을 알림
- `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 로그
- `기능` 저장된 계정 저장소 (로드맵 P4.1): 플러그인에 저장한 계정은 비밀이 없는 계정 문서와 Android Keystore 마스터 키로 AES-256-GCM 암호화한 비밀번호 또는 액세스 토큰을 함께 보관; 인증 데이터가 별칭, 비밀 종류, 문서를 묶으므로 디스크에서 수정되거나 옮겨진 레코드는 더 이상 복호화되지 않음; 레코드는 `noBackupFilesDir` (이미 백업에서 제외) 에 두고 파일 잠금 아래에서 원자적으로 기록하며, 비밀은 사용 후 지워지는 `CharArray` / `ByteArray` 버퍼만 거침; 별칭은 앞뒤 공백을 제거하고 NFC 정규화하며 대소문자를 구분하지 않음
- `기능` 저장된 계정 세션 (로드맵 P4.3): `openSession` 이 별칭 형태 (`accountAlias`) 를 받아 플러그인 프로세스 안에서 비밀을 복호화하므로 `mail.connect('alias')` 는 Binder 로 자격 증명을 전혀 실어 나르지 않음; `listSavedAccounts` 는 저장된 계정마다 별칭, 주소, 사용자 이름, 제공자, 인증 방식, 수신 프로토콜, 엔드포인트, 기본 표시를 비밀 없이 반환; 기능 집합에 `savedAccounts` 추가
- `기능` 설정 화면 (로드맵 P4.2): 런처 아이콘이 계정 목록을 열어 저장된 계정마다 주소, 제공자, 수신 프로토콜, 인증 방식을 표시하고 편집, 연결 테스트, 기본 설정 및 해제, 삭제를 제공; 계정 편집 화면은 제공자 프리셋을 자동 입력하거나 IMAP / POP3 / SMTP 서버를 암호화 방식과 포트까지 직접 받고, 비밀번호 또는 액세스 토큰을 입력란에서 사용 후 지워지는 `CharArray` 로 바로 읽으며, 편집 시 비워 두면 저장된 비밀을 유지하고, 저장 전에 입력한 서버로 `session.test` 를 실행해 프로토콜별 결과와 소요 시간을 디스크에 쓰지 않고 표시; 화면은 AutoJs6 호스트의 테마, 야간 모드, 언어를 따르고 다시 생성된 편집 화면은 비밀을 제외한 모든 항목을 복원
- `기능` 설정 진입점 (로드맵 P4.3): AutoJs6 호스트는 내보낸 `org.autojs.plugin.MAIL_SETTINGS` 액티비티를 통해 계정 목록을 열며, 이 액티비티는 플러그인 권한을 요구하고 매개변수 없는 요청만 받아 즉시 종료함; 기능 집합은 `mailSettingsVersion` 1 을 알림; 런처 진입점 자체는 이 권한을 갖지 않음
- `기능` 릴리스 기록 (로드맵 P4.5): 설정 화면과 정보 화면에서 릴리스 기록 페이지를 열 수 있으며, 내용은 플러그인에 포함된 현재 언어의 변경 기록 (번역이 없으면 영어) 에서 그려지고 버전마다 날짜와 태그가 붙은 항목을 가진 카드로 표시; 플러그인은 자체 업데이트 확인을 하지 않으며 업데이트는 AutoJs6 플러그인 센터를 따름
- `기능` 배터리 최적화 안내 (로드맵 P4.6): 설정 화면은 시스템이 백그라운드에서 이 플러그인을 중단할 수 있는지 표시하고 (`PowerManager.isIgnoringBatteryOptimizations`), 무엇이 바뀌는지 설명한 뒤 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 로 시스템 대화 상자를 엶; 따라서 매니페스트는 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 를 선언; 시작 시 아무것도 요청하지 않으며 이 제외에 의존하는 기능도 없음
- `기능` 새 메일 감시, IMAP IDLE (로드맵 P5): 메일 코어는 전용 연결에서 `IMAPFolder.idle` 로 폴더를 감시하고, IDLE 을 24 분마다 갱신하며, 도착한 메일을 UID 로 (봉투, 또는 요청 시 본문까지) 정확히 한 번 가져오고, 끊김 후에는 지수 백오프 (1 s 에서 5 min 까지, 지터 포함) 로 재연결하며, 폴더의 UIDVALIDITY 가 바뀌면 `resync` 를 보고; IDLE 이 없는 서버나 3 회 연속 실패한 IDLE 은 `mode` 이벤트와 함께 감시를 폴링으로 전환; 세션은 최대 `MAX_WATCHES_PER_SESSION` 개의 감시를 보유하고 세션과 함께 닫음
- `기능` 새 메일 감시, 폴링 (로드맵 P5): `mode: "poll"` 감시는 `pollIntervalMs` 마다 (기본 60 s, 최소 `MIN_POLL_INTERVAL_MS`, 최대 1 시간) 폴링 사이에 유지되는 연결로 폴더를 UID 로 비교; POP3 계정은 항상 폴링되며, 폴링마다 한 번 로그인하여 UIDL 로 비교하므로 메일드롭은 그 사이에 잠기지 않고, 추가만 보고하고 삭제는 보고하지 않음; 첫 폴링은 스냅샷만 찍고 밀린 메일을 보고하지 않음
- `기능` Binder 를 통한 새 메일 감시 (로드맵 P5): `IMailSession.watch` 는 메일 코어의 감시 위에 감시를 열고 즉시 응답 (세션이 닫혔거나, `MAX_WATCHES_PER_SESSION` 에 도달했거나, 옵션을 쓸 수 없거나, 호스트의 콜백이 이미 죽은 경우 null 을 반환하고 이유는 세션 상태에 기록); 이벤트는 전달 스레드에서 호스트의 `generation` 과 1 부터 세는 `seq` 를 담아 호스트의 `oneway` 콜백에 도달하고, 호스트가 소비를 멈추면 `MAX_WATCH_QUEUE` 개 이벤트의 큐는 하나의 `resync` 로 접히며, `MAX_ENVELOPE_BYTES` 를 넘는 이벤트는 본문 없이 또는 `resync` 로 나가고, `stop`, 세션 닫기, 호스트 사망은 모두 단 하나의 `closed` 이벤트로 감시를 끝내며, 기본 네트워크의 변경이나 손실은 실행 중인 감시를 즉시 재연결하고, 기능 목록은 이제 `idle` 을 알림
- `수정` 제공업체 프리셋 (로드맵 P3.2): 163 Mail과 126 Mail은 SMTP로 보낸 모든 메일의 사본을 서버에 보관하므로 두 프리셋의 `autoSavesSent`를 true로 바꾸고, 기본 `saveToSent`가 `已发送`에 두 번째 사본을 추가하지 않도록 했습니다 (실제 163 계정으로 확인: `saveToSent: false`로 보낸 메일이 몇 분 뒤 보낸 편지함에 나타남)
- `수정` 공유 빌드 플러그인 1.8.3을 통해 AGP 9.1의 SDK XML v4 파싱 경고 및 JVM 단위 테스트 조립 작업에서 APK 네이티브 라이브러리 정렬 검사가 잘못 실행되는 문제 해결
- `수정` 계정 편집기 (로드맵 P4.7): 폼 전체를 Android 자동 완성 프레임워크에서 제외하여 비밀번호 관리자가 인증 코드를 저장하려고 하지 않습니다. 이전에는 HyperOS (API 35) 에서 저장 후 편집기를 닫을 때 "계정과 비밀번호 저장" 시트가 나타났습니다.
- `수정` QQ, Sina, 163, 126 의 새 메일 감시 (메일 로드맵 P5 기기 매트릭스): 제공업체 프리셋에 `idlePush` 가 추가되고 (카탈로그 버전 2), 이 네 곳에서는 `mode: auto` 가 IDLE 대신 처음부터 폴링함. QQ 와 Sina 는 IMAP IDLE 을 받아들이지만 클라이언트가 유휴 상태인 동안 한 번도 푸시하지 않고 (실계정, 2026-09-19: 10 분간 태그 없는 응답 없음; Sina 는 60 초 후 연결도 끊음), 163 과 126 에는 IDLE 자체가 없기 때문; 명시적 `mode: 'idle'` 은 여전히 IDLE 에 들어감. 매트릭스 자체 (API 24 에뮬레이터와 Sony 두 대의 QQ, Redmi 의 163: 플러그인 강제 종료, 네트워크 끊김, Wi-Fi 에서 셀룰러로, 강제 Doze) 는 `docs/dev/p5-watch-evidence.md` 에 기록되며 스모크 스크립트 `docs/smoke/watch.js` 와 드라이버 `.python/run_watch_matrix.py` 가 함께함; 같은 매트릭스에서 Doze 가 백그라운드 앱의 네트워크를 동결한다는 점도 드러났으므로 (감시의 재연결이 시간 초과되고, Android 9 에서는 새 메일이 깨어난 뒤 약 4 분 만에 보고됨), 플러그인은 이제 기기가 Doze 를 벗어나는 순간 감시를 다시 연결하고 설정 화면의 배터리 최적화 안내 문구가 이 제외의 의미를 설명함
- `수정` TLS 매트릭스 (메일 로드맵 P6): 암시적 SSL, STARTTLS (GreenMail 앞의 STARTTLS 프록시 경유), 평문, `tls.trustAll` 유무의 자체 서명 인증서, 호스트 이름이 일치하지 않는 신뢰된 인증서, 포트와 모드의 불일치, 업그레이드를 제공하지 않는 포트를 IMAP / POP3 / SMTP 에 대해 검증함 (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`). 검증에서 Angus 의 POP3 스토어가 STLS 업그레이드 누락과 핸드셰이크 전 시간 초과를 인증 실패로 보고하는 것이 드러나, 오류 매퍼는 이를 `AUTH_FAILED` 대신 `TLS_FAILED` 와 `TIMEOUT` 으로 보고함. `TlsDeviceTest` 는 API 24 / 28 / 33 에서 메일 코어가 플랫폼 기본값 그대로 TLS 1.2 전용 서버에 연결되고 API 29 부터 TLS 1.3 을 협상함을 확인함
- `수정` 문자 집합 매트릭스 (메일 로드맵 P6): GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR, UTF-8 을 제목, 표시 이름, 본문, 두 가지 파일 이름 형식에서 선언됨 / 선언되지 않음 / 잘못 선언됨의 세 형태로 검증하고 (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`) API 24 / 28 / 33 에서도 확인함 (`CharsetDeviceTest`). 검증에서 두 가지 디코딩 결함을 수정함: `us-ascii` / ISO-8859-1 로 선언되었지만 UTF-8 이나 GB 18030 바이트를 담은 본문이나 헤더는 Latin-1 깨진 글자로 나왔으나 (Jakarta 는 `us-ascii` 를 결코 실패하지 않는 ISO-8859-1 에 대응시킴) 이제 추측 체인을 거치고, 인코딩 단어가 없는 원시 ISO-2022-JP 제목과 본문은 이스케이프 시퀀스로 인식됨. Big5 와 EUC-KR 은 여전히 선언이 필요함 (바이트 쌍이 GB 18030 으로도 유효함)
- `개선` 오류 매핑: 계정의 POP 액세스가 비활성화되어 로그인 후 메일함을 거부하는 POP3 서버 (Gmail은 STAT에 `[SYS/PERM] Your account is not enabled for POP access`로 응답)에 대해 재시도 가능한 `IO_FAILED` "I/O failed" 대신 원인을 밝히는 메시지가 담긴 `UNSUPPORTED_OPERATION`을 반환합니다
- `개선` 제공자 프리셋: Sina Mail에 보낸편지함 폴더 이름 (`已发送`)을 추가하고, 서버가 보낸 메일 사본을 보관하지 않으며 IMAP CREATE를 거부한다는 점 (폴더는 웹 UI에서만 생성 가능)을 명시했습니다. 126 Mail 서버의 보낸 메일 사본은 실제 계정으로 검증되었습니다
- `개선` 제공자 프리셋: Yahoo Mail 과 Aliyun Mail 의 메모에 이 프리셋이 실제 계정으로 검증되지 않았음 (프로젝트에 테스트 계정이 없음) 을 명시하며, 보낸 편지 사본 동작은 공개 문서를 따릅니다.
- `개선` 비밀 정보 감사 (메일 로드맵 P6): 메일 코어와 앱에서 로그 문, 콘솔 출력, Jakarta 디버그 스위치, 비밀 정보가 실체화되는 모든 위치를 검색함. 그 결과 (`docs/dev/p6-secret-audit.md`) 는 `SecretAuditTest` 가 강제함: 로그나 디버그 문이 있으면 빌드가 실패하고, `reveal()` 은 세 곳의 Jakarta 인증 호출에 고정되며, Jakarta 세션이 결코 디버그하지 않는지, 계정 JSON 안의 비밀 정보가 되돌려 보여지지 않고 거부되는지, 값 객체, 예외 매퍼, 프로토콜 추적이 비밀 정보를 전달되는 모든 형태로 가리는지 검사함
- `개선` 적대적 입력 (메일 로드맵 P6): 메일이 무엇을 담고 있든 메시지 문서는 이제 유계임 (네 주소 목록은 합계 500 항목, 주소와 표시 이름은 320 자, 제목 / 식별자 / 헤더 값은 4096 자, `headers` 맵은 64 KiB, MIME 트리는 깊이 32 와 256 파트, 크기를 알 수 없는 본문은 인라인 예산까지만 읽음) 으로, 적대적 메일 한 통이 `LIMIT_EXCEEDED` 로 목록 페이지 전체를 막을 수 없음. 손상된 base64, 알 수 없는 전송 인코딩, boundary 가 없거나 비어 있는 multipart 는 연결 끊김으로 오인되지 않고 관대하게 디코딩됨. 너무 깊거나 파싱할 수 없는 multipart 는 하나의 리프로 다운로드 가능함. `HostileInputTest` 와 `HostileInputGreenMailTest` (17 건, `docs/dev/p6-hostile-input.md`) 가 깊고 넓은 MIME, 20000 수신자, 헤더 폭탄, Content-Type 누락, 잘못된 base64, 재귀적 `message/rfc822`, 적대적 파일 이름, 크기 불일치, 잘못된 UTF-8 을 다룸
- `의존성` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`)와 Angus Activation 2.0.3, Jakarta Activation API 2.1.4
- `의존성` JVM 메일 코어 테스트용 GreenMail 2.1.13 추가 (테스트 범위만)
- `의존성` 공유 플러그인 계약으로 `common-plugin-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api`, 호스트 빌드 6.8.0 / 5282, MPL 2.0)를 추가하고 `locks/host-api-aars.lock`에 해시를 고정
- `의존성` 메일 Binder 계약으로 `mail-api.aar` (AutoJs6 모듈 `plugin-api/mail-api`, 호스트 빌드 6.8.0 / 5282, MPL 2.0; AIDL 인터페이스 6개, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`)를 추가하고 `locks/host-api-aars.lock`에 해시를 고정

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
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
