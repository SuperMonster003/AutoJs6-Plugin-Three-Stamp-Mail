Angus Mail은 AutoJs6 스크립트에 전역 객체 `mail`을 제공하여 메일 보내기, 메일함 나열과 검색, 본문 읽기, 첨부 파일 다운로드, 플래그와 폴더 관리, 폴더의 새 메일 감시를 지원합니다. Jakarta Mail의 참조 구현인 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5을 기반으로 하며 TLS 위에서 IMAP, POP3, SMTP를 사용합니다.

버전 1.0.0은 첫 정식 릴리스입니다: 로드맵 P0 부터 P6 까지의 모든 항목 (메일 코어, Binder 계약, 스크립트 API, 설정 페이지와 저장된 계정, 새 메일 감시, 그리고 TLS, 문자 집합, 제공자, 수명 주기, 적대적 입력, 비밀 감사, 성능 매트릭스) 이 증거와 함께 완료되었으며 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) 에서 확인할 수 있습니다. AutoJs6 6.8.0 (빌드 5282) 이상이 필요합니다; 스크립트 API 의 전체 참조는 [AutoJs6 문서](https://docs.autojs6.com/#/mail) 에 있습니다.

### 사용 방법

1. AutoJs6 빌드 5282 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 에서 플러그인 APK 를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `Angus Mail` 이 인식되는지 확인하고 활성화합니다.
3. 계정을 준비합니다: 메일 제공자의 웹 설정에서 IMAP 또는 POP3 와 SMTP 를 켜고, 인증 코드 (QQ, 163, 126, Sina), 앱 비밀번호 (Gmail, iCloud, Yahoo) 또는 OAuth 2.0 액세스 토큰 (Outlook.com) 을 얻습니다; 로그인 비밀번호 자체는 보통 받아들여지지 않습니다.
4. 스크립트에서 `mail.connect(...)`를 호출하거나, 플러그인 설정 페이지 (플러그인의 런처 아이콘 또는 AutoJs6 개발자 옵션 > 메일 계정 설정) 에 계정을 저장한 뒤 별칭으로 연결합니다.

연결 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)를 참고하세요.
