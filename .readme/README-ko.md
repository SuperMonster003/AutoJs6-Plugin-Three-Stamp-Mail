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
- 감시: 스크립트가 실행되는 동안 IMAP IDLE로 새 메일 이벤트를 받고, 지원하지 않는 서버나 POP3 계정에서는 폴링으로 대체합니다.
- 제공자: Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun 프리셋이 호스트, 포트, 암호화를 채워 주며, 다른 서버를 위해 어떤 필드든 재정의할 수 있습니다.
- 인증: 비밀번호와 제공자 인증 코드, 또는 스크립트가 갱신 콜백과 함께 제공하는 XOAUTH2 액세스 토큰을 지원합니다.

******

### 사용 방법

******

1. AutoJs6 빌드 5282 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `Angus Mail`이 인식되는지 확인하고 활성화합니다.
3. 계정을 준비합니다: 메일 제공자 설정에서 IMAP 또는 POP3를 켜고, 인증 코드나 앱 비밀번호 (QQ, 163, 126, Gmail, iCloud) 또는 OAuth 2.0 액세스 토큰 (Outlook.com)을 얻습니다.
4. 스크립트에서 `mail.connect(...)`를 호출하거나, 플러그인 설정 페이지에 계정을 저장한 뒤 별칭으로 연결합니다.

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

_2026/09/18_

- `힌트` P0 개발 미리보기: 저장소 뼈대, 로컬 서버 테스트를 갖춘 메일 코어, AutoJs6 플러그인 센터용 플러그인 식별 정보. Binder 계약, 스크립트 API, 설정 페이지는 ROADMAP.md의 단계에 따라 진행됩니다.
- `기능` 플러그인 식별자 `angus-mail` (엔진 `mail`), INFO 서비스, Wake Activity, 그리고 `org.autojs.plugin.MAIL` 서비스; 해당 `IMailPlugin` Binder 는 플러그인 정보, 기능, 공급자 및 저장된 계정 목록과 세션 봉투에 응답 (개별 작업은 P2 에서 구현)
- `기능` Eclipse Angus Mail 기반 메일 코어: SSL 또는 STARTTLS를 쓰는 IMAP / POP3 / SMTP 세션 속성, 비밀번호와 XOAUTH2 인증, SMTP 전송과 IMAP 받은 편지함 나열을 로컬 GreenMail 서버에서 검증
- `기능` 메일 코어 계정 계층 (로드맵 P2.1): Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun 프로바이더 프리셋, 프로토콜별 타임아웃, `tls.trustAll`, IMAP `ID` 명령, 마스킹된 `debug` 트레이스를 갖춘 계정 옵션; 세션은 지연 연결하고 유휴 연결을 끊으며 끊김 후 재연결; `session.test` 는 Binder 를 통해 엔드포인트별 기능과 왕복 시간을 반환
- `기능` 발신 (로드맵 P2.2): `mail.send` 는 to / cc / bcc / replyTo, 텍스트와 HTML 본문 (`multipart/alternative`), 호스트가 전달한 디스크립터에서 읽는 첨부 파일과 인라인 이미지 (`multipart/mixed` / `multipart/related`), 사용자 정의 헤더, 우선순위, `inReplyTo` / `references` 와 날짜를 지원; 수신자, 첨부, 헤더 상한과 헤더 인젝션 거부; `saveToSent` 는 프로바이더가 자동 저장하지 않을 때만 IMAP 으로 사본을 추가; `messages.append` 는 초안을 저장하고 UID 를 반환; 실제 기기에서 QQ 메일과 Gmail 로 검증
- `기능` 수신 (로드맵 P2.3): `folders.list` (트리, special-use 역할은 LIST / XLIST 속성 또는 관례적 이름에서 취득, 개수는 선택), `folders.status` / `create` / `delete` / `rename`; `messages.list` 는 UID 를 커서로 사용하고 (`before` / `after`) `order` 와 `unseenOnly` 를 지원하며 엔벨로프만 가져옴 (`hasAttachments` 는 BODYSTRUCTURE 로 판정); `messages.search` 는 쿼리 JSON (`from` / `to` / `subject` / `body` / `text` / 날짜 / 플래그 / 크기 / `header` / `messageId` / `uid` 와 `and` / `or` / `not`) 을 IMAP SEARCH 로 컴파일하고 서버가 거부하면 클라이언트에서 필터링 (`fallback`); `messages.get` 은 텍스트와 HTML 본문 (HTML 만 있는 메일은 텍스트를 파생), 전체 헤더, `partId` 가 있는 첨부 목록, 인라인 상한 (`bodyTruncated` / `bodyParts`) 과 `includeRaw` 를 반환; `attachments.download` 와 `messages.raw` 는 호스트의 디스크립터로 진행률과 함께 스트리밍; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 문자 집합 복원은 GBK / GB 18030 / ISO-2022-JP, 선언되지 않았거나 알 수 없는 문자 집합, 원시 8비트 헤더와 RFC 2231 / 2047 파일 이름을 다루며 파일 이름을 정제; IMAP `ID` 명령을 모든 연결에서 전송 (163 / 126은 식별되지 않은 연결을 거부), 서버가 `UID EXPUNGE`를 해석하지 못하면 폴더 전체 `EXPUNGE`로 대체; 실제 기기에서 QQ 메일, 163 메일, Gmail로 검증
- `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 로그
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
