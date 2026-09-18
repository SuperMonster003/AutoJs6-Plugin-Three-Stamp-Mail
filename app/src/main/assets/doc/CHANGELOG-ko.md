******

### 릴리스 기록

******

# v1.0.0

###### 2026/09/18

* `힌트` P0 개발 미리보기: 저장소 뼈대, 로컬 서버 테스트를 갖춘 메일 코어, AutoJs6 플러그인 센터용 플러그인 식별 정보. Binder 계약, 스크립트 API, 설정 페이지는 ROADMAP.md의 단계에 따라 진행됩니다.
* `기능` 플러그인 식별자 `angus-mail` (엔진 `mail`), INFO 서비스, Wake Activity, 그리고 `org.autojs.plugin.MAIL` 서비스; 해당 `IMailPlugin` Binder 는 플러그인 정보, 기능, 공급자 및 저장된 계정 목록과 세션 봉투에 응답 (개별 작업은 P2 에서 구현)
* `기능` Eclipse Angus Mail 기반 메일 코어: SSL 또는 STARTTLS를 쓰는 IMAP / POP3 / SMTP 세션 속성, 비밀번호와 XOAUTH2 인증, SMTP 전송과 IMAP 받은 편지함 나열을 로컬 GreenMail 서버에서 검증
* `기능` 메일 코어 계정 계층 (로드맵 P2.1): Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun 프로바이더 프리셋, 프로토콜별 타임아웃, `tls.trustAll`, IMAP `ID` 명령, 마스킹된 `debug` 트레이스를 갖춘 계정 옵션; 세션은 지연 연결하고 유휴 연결을 끊으며 끊김 후 재연결; `session.test` 는 Binder 를 통해 엔드포인트별 기능과 왕복 시간을 반환
* `기능` 발신 (로드맵 P2.2): `mail.send` 는 to / cc / bcc / replyTo, 텍스트와 HTML 본문 (`multipart/alternative`), 호스트가 전달한 디스크립터에서 읽는 첨부 파일과 인라인 이미지 (`multipart/mixed` / `multipart/related`), 사용자 정의 헤더, 우선순위, `inReplyTo` / `references` 와 날짜를 지원; 수신자, 첨부, 헤더 상한과 헤더 인젝션 거부; `saveToSent` 는 프로바이더가 자동 저장하지 않을 때만 IMAP 으로 사본을 추가; `messages.append` 는 초안을 저장하고 UID 를 반환; 실제 기기에서 QQ 메일과 Gmail 로 검증
* `기능` 수신 (로드맵 P2.3): `folders.list` (트리, special-use 역할은 LIST / XLIST 속성 또는 관례적 이름에서 취득, 개수는 선택), `folders.status` / `create` / `delete` / `rename`; `messages.list` 는 UID 를 커서로 사용하고 (`before` / `after`) `order` 와 `unseenOnly` 를 지원하며 엔벨로프만 가져옴 (`hasAttachments` 는 BODYSTRUCTURE 로 판정); `messages.search` 는 쿼리 JSON (`from` / `to` / `subject` / `body` / `text` / 날짜 / 플래그 / 크기 / `header` / `messageId` / `uid` 와 `and` / `or` / `not`) 을 IMAP SEARCH 로 컴파일하고 서버가 거부하면 클라이언트에서 필터링 (`fallback`); `messages.get` 은 텍스트와 HTML 본문 (HTML 만 있는 메일은 텍스트를 파생), 전체 헤더, `partId` 가 있는 첨부 목록, 인라인 상한 (`bodyTruncated` / `bodyParts`) 과 `includeRaw` 를 반환; `attachments.download` 와 `messages.raw` 는 호스트의 디스크립터로 진행률과 함께 스트리밍; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 문자 집합 복원은 GBK / GB 18030 / ISO-2022-JP, 선언되지 않았거나 알 수 없는 문자 집합, 원시 8비트 헤더와 RFC 2231 / 2047 파일 이름을 다루며 파일 이름을 정제; IMAP `ID` 명령을 모든 연결에서 전송 (163 / 126은 식별되지 않은 연결을 거부), 서버가 `UID EXPUNGE`를 해석하지 못하면 폴더 전체 `EXPUNGE`로 대체; 실제 기기에서 QQ 메일, 163 메일, Gmail로 검증
* `기능` POP3 (로드맵 P2.4): `receive: "pop3"` 계정은 같은 작업으로 유일한 `INBOX` 를 읽고 `uid` 는 UIDL 문자열: `folders.list` 는 `INBOX` 만 반환 (개수는 선택), `messages.list` 는 UIDL 커서로 페이징하며 헤더만 가져옴 (`TOP`), `messages.search` 는 클라이언트에서 헤더를 최신순으로 필터링하고 `limit` 개가 채워지면 중단 (후보 최대 200개, 각각 `TOP` 왕복 1회), `messages.get` / `messages.raw` / `attachments.download` 는 메일 전체를 다운로드, `messages.delete` 는 `DELE` 를 보내고 메일함을 닫을 때 확정; 플래그, 이동, 복사, expunge, 추가, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` 와 본문 검색은 연결 전에 `UNSUPPORTED_OPERATION` 을 반환; 실제 기기에서 QQ 메일로 검증
* `기능` Binder 세션 제어 (로드맵 P2.5): 설치되어 있고 이 플러그인과 같은 키로 서명된 AutoJs6 호스트만 세션을 열거나 저장된 계정을 나열할 수 있음 (그 외에는 `SecurityException`, MCP Server 플러그인과 같은 규칙); 요청과 응답 봉투는 `MAX_ENVELOPE_BYTES` 이내, 오류 메시지는 `MAX_ERROR_MESSAGE_BYTES` 이내; 각 세션은 호출을 순서대로 실행하고 실행 중인 호출 뒤에 최대 `MAX_QUEUED_CALLS` 개를 대기시키며 그 이상은 `LIMIT_EXCEEDED` 로 거부; `cancel` 은 대기 중인 호출에 즉시 응답하고 실행 중인 호출은 소켓을 닫아 중단하므로 응답 없는 서버에서 읽기 시간 초과까지 기다리지 않음; `close` 는 보류 중인 모든 호출에 `SESSION_CLOSED` 로 응답; `getStatus` 는 `queued` 와 `active` 를 보고; 작업 표는 각 작업이 지원하는 수신 프로토콜을 명시하므로 POP3 계정은 인자 해석 전에 거부됨; 기능 목록은 `append` 와 `clientSearchFallback` 을 알림
* `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 로그
* `수정` 제공업체 프리셋 (로드맵 P3.2): 163 Mail과 126 Mail은 SMTP로 보낸 모든 메일의 사본을 서버에 보관하므로 두 프리셋의 `autoSavesSent`를 true로 바꾸고, 기본 `saveToSent`가 `已发送`에 두 번째 사본을 추가하지 않도록 했습니다 (실제 163 계정으로 확인: `saveToSent: false`로 보낸 메일이 몇 분 뒤 보낸 편지함에 나타남)
* `의존성` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`)와 Angus Activation 2.0.3, Jakarta Activation API 2.1.4
* `의존성` JVM 메일 코어 테스트용 GreenMail 2.1.13 추가 (테스트 범위만)
* `의존성` 공유 플러그인 계약으로 `common-plugin-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api`, 호스트 빌드 6.8.0 / 5282, MPL 2.0)를 추가하고 `locks/host-api-aars.lock`에 해시를 고정
* `의존성` 메일 Binder 계약으로 `mail-api.aar` (AutoJs6 모듈 `plugin-api/mail-api`, 호스트 빌드 6.8.0 / 5282, MPL 2.0; AIDL 인터페이스 6개, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`)를 추가하고 `locks/host-api-aars.lock`에 해시를 고정
