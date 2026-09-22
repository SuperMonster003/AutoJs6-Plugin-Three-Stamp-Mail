Angus Mail은 AutoJs6 스크립트에 전역 객체 `mail`을 제공하여 메일 보내기, 메일함 나열과 검색, 본문 읽기, 첨부 파일 다운로드, 플래그와 폴더 관리, 폴더의 새 메일 감시를 지원합니다. Jakarta Mail의 참조 구현인 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5을 기반으로 하며 TLS 위에서 IMAP, POP3, SMTP를 사용합니다.

버전 1.2.1 은 1.1.0 의 백그라운드 감시 (로드맵 P8) 위에 Google 및 Microsoft 계정의 브라우저 로그인 (로드맵 P9) 을 추가합니다. P0 부터 P8 까지의 모든 항목은 1.0.0 부터 1.1.0 으로 출시되었으며 증거는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) 에 있습니다. AutoJs6 6.8.0 (빌드 5282) 이상이 필요합니다. "메일 도착 시" 작업에는 메일 계약 버전 2 를 가진 호스트 빌드가 필요합니다. 스크립트 API 의 전체 참조는 [AutoJs6 문서](https://docs.autojs6.com/#/mail) 에 있습니다.

### 사용 방법

1. AutoJs6 빌드 5282 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 에서 플러그인 APK 를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `Angus Mail` 이 인식되는지 확인하고 활성화합니다.
3. 계정을 준비합니다: 제공업체의 웹 설정에서 IMAP 또는 POP3 와 SMTP 를 켜고 인증 코드 (QQ, 163, 126, Sina) 또는 앱 비밀번호 (Gmail, iCloud, Yahoo) 를 얻습니다. 로그인 비밀번호 자체는 보통 허용되지 않습니다. Gmail, Outlook.com, Microsoft 365 계정은 대신 플러그인 설정에서 브라우저로 Google 또는 Microsoft 계정에 로그인하거나 (인증 방식에서 "Google / Microsoft 계정으로 로그인 (브라우저)" 선택), 다른 곳에서 얻은 OAuth 2.0 액세스 토큰을 줄 수 있습니다.
4. 스크립트에서 `mail.connect(...)`를 호출하거나, 플러그인 설정 페이지 (플러그인의 런처 아이콘 또는 AutoJs6 개발자 옵션 > 메일 계정 설정) 에 계정을 저장한 뒤 별칭으로 연결합니다.
5. 스크립트를 상주시키지 않고 새 메일에 실행하려면: 플러그인의 감시 페이지 (설정 > 감시: 계정 별칭, 폴더, 모드, 필터) 에서 감시를 추가하고, 요청 시 알림을 허용한 뒤, AutoJs6 에서 작업을 만들고 (스크립트 길게 누르기 > 정시 작업 > 브로드캐스트로 실행 > 메일 도착 시) 감시를 선택합니다. 이 작업에는 메일 계약 버전 2 를 포함한 AutoJs6 빌드가 필요합니다.

연결 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)를 참고하세요.
