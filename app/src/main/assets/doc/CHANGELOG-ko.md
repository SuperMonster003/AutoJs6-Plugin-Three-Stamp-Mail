******

### 릴리스 기록

******

# v1.0.0

###### 2026/09/18

* `힌트` P0 개발 미리보기: 저장소 뼈대, 로컬 서버 테스트를 갖춘 메일 코어, AutoJs6 플러그인 센터용 플러그인 식별 정보. Binder 계약, 스크립트 API, 설정 페이지는 ROADMAP.md의 단계에 따라 진행됩니다.
* `기능` 플러그인 식별자 `angus-mail` (엔진 `mail`), INFO 서비스, Wake Activity, 그리고 `org.autojs.plugin.MAIL` 서비스; 해당 `IMailPlugin` Binder 는 플러그인 정보, 기능, 공급자 및 저장된 계정 목록과 세션 봉투에 응답 (개별 작업은 P2 에서 구현)
* `기능` Eclipse Angus Mail 기반 메일 코어: SSL 또는 STARTTLS를 쓰는 IMAP / POP3 / SMTP 세션 속성, 비밀번호와 XOAUTH2 인증, SMTP 전송과 IMAP 받은 편지함 나열을 로컬 GreenMail 서버에서 검증
* `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 로그
* `의존성` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`)와 Angus Activation 2.0.3, Jakarta Activation API 2.1.4
* `의존성` JVM 메일 코어 테스트용 GreenMail 2.1.13 추가 (테스트 범위만)
* `의존성` 공유 플러그인 계약으로 `common-plugin-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api`, 호스트 빌드 6.8.0 / 5282, MPL 2.0)를 추가하고 `locks/host-api-aars.lock`에 해시를 고정
* `의존성` 메일 Binder 계약으로 `mail-api.aar` (AutoJs6 모듈 `plugin-api/mail-api`, 호스트 빌드 6.8.0 / 5282, MPL 2.0; AIDL 인터페이스 6개, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`)를 추가하고 `locks/host-api-aars.lock`에 해시를 고정
