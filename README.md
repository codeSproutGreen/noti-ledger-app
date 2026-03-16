# NotiLedger App

금융앱 푸시 알림과 SMS를 자동으로 수집하여 **NotiLedger 서버**로 웹훅 전송하는 Android 앱입니다.

## 주요 기능

### 알림 수집
- `NotificationListenerService`를 사용하여 금융앱 푸시 알림 자동 수집
- 금융앱만 선별 수집 (은행, 페이, 증권, 보험 앱)
- 카카오톡, 시스템 UI 등 노이즈 앱 자동 차단

### SMS 수집
- `BroadcastReceiver`로 문자 메시지 수신
- 금융 키워드 필터링 (승인/입금/출금/결제/이체)
- 인증번호만 수집 옵션

### 웹훅 전송
- 수집된 데이터를 HTTP POST로 NotiLedger 서버에 자동 전송
- HMAC-SHA256 서명 지원
- **기기 이름 설정**으로 멀티 디바이스 구분 (예: 남편폰/아내폰)
- 미전송 건 필터링 및 수동 재전송

### 앱 필터 관리
- 금융앱 프리셋 일괄 등록 (KB국민은행, 신한, 하나, 우리, 토스, 카카오뱅크 등 17개)
- 노이즈 앱 기본 비활성화 (카카오톡, 시스템 UI, 메시지 등)
- 앱별 ON/OFF 토글
- 길게 눌러 필터 삭제

### 데이터 관리
- Room DB 로컬 저장 (기기 내에만 저장)
- **선택 모드**: 길게 눌러 다중 선택 → 수동 전송 / 일괄 삭제
- 전체 선택 지원
- JSON 백업/복원
- 검색 (내용, 제목, 앱 이름)

## 기술 스택

- Kotlin + Jetpack Compose
- Room Database (SQLite)
- DataStore Preferences
- OkHttp (웹훅 HTTP 클라이언트)
- Material 3 + Dynamic Color

## 설치

### APK 직접 설치

[Releases](https://github.com/codeSproutGreen/noti-ledger-app/releases)에서 최신 APK 다운로드 후:

1. 폰에서 **출처를 알 수 없는 앱 설치 허용**
2. APK 파일 실행하여 설치

### 소스에서 빌드

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 초기 설정

### 1. 알림 접근 권한 (필수)
앱 실행 후 **설정 > 권한 설정 > 알림 접근 권한** → `NotiLedger` 허용

### 2. SMS 권한 (필수)
앱 첫 실행 시 SMS 수신 권한 요청 팝업 **허용**

### 3. 배터리 최적화 예외 (권장)
**설정 > 배터리 최적화 예외** → NotiLedger 제외 (백그라운드 안정 동작)

### 4. 웹훅 설정
**설정 > 웹훅 전송** 활성화 후:
- **기기 이름**: 디바이스 식별용 (예: 남편폰, 아내폰)
- **Webhook URL**: NotiLedger 서버 주소
- **Secret**: HMAC 서명용 비밀 키 (선택)

```
https://your-server.com/api/webhook
```

### 5. 앱 필터 프리셋
**필터 탭 > 우측 상단 + 버튼** → 금융앱 프리셋 일괄 등록

## 웹훅 요청 형식

```json
POST /api/webhook
Content-Type: application/json
User-Agent: NotiLedger/1.0
X-Webhook-Signature: <HMAC-SHA256 hex>

{
  "id": 123,
  "type": "NOTIFICATION",
  "source": "com.kbstar.kbbank",
  "sourceName": "KB국민은행",
  "title": "입금 알림",
  "content": "1,000,000원이 입금되었습니다",
  "timestamp": 1710000000000,
  "deviceName": "남편폰"
}
```

## 인식되는 금융앱

### 프리셋 등록 앱

| 분류 | 앱 | 패키지명 |
|---|---|---|
| 은행 | KB국민은행 | com.kbstar.kbbank |
| 은행 | 신한SOL뱅크 | com.shinhan.sbanking |
| 은행 | 우리WON뱅킹 | com.wooribank.smart.banking |
| 은행 | 하나은행 | com.hanabank.ebk.channel.android.hananbank |
| 은행 | NH스마트뱅킹 | com.nh.cashcow |
| 은행 | IBK기업은행 | com.ibk.neobanking |
| 은행 | 우체국금융 | com.epost.psf.sdsi |
| 은행 | 케이뱅크 | com.kbankwith |
| 은행 | 카카오뱅크 | com.kakaobank.channel |
| 핀테크 | 토스 | viva.republica.toss |
| 페이 | 네이버페이 | com.nhn.android.search |
| 페이 | 삼성페이 | com.samsung.android.spay |
| 페이 | SSG페이 | com.ssg.serviceapp.android.egiftcertificate |
| 페이 | L.pay | com.lottemembers.android |
| 페이 | 뱅크페이 | com.kftc.bankpay.android |
| 페이 | ISP/페이북 | kvp.jjy.MispAndroid320 |

### 자동 인식

패키지명에 `bank`, `pay`, `securities`, `stock`, `insurance`가 포함된 앱도 자동으로 금융앱으로 분류됩니다.

## 프로젝트 구조

```
app/src/main/java/com/notiledger/
├── NotiLedgerApp.kt                # Application 클래스
├── data/
│   ├── model/
│   │   ├── NotiRecord.kt           # 알림/SMS 데이터 모델
│   │   └── AppFilter.kt            # 앱 필터 설정 모델
│   ├── db/
│   │   ├── NotiDatabase.kt         # Room 데이터베이스
│   │   ├── NotiRecordDao.kt        # 알림 기록 DAO
│   │   └── AppFilterDao.kt         # 앱 필터 DAO
│   ├── repository/
│   │   ├── NotiRepository.kt       # 데이터 레포지토리
│   │   └── SettingsRepository.kt   # 설정 관리 (DataStore)
│   └── backup/
│       └── BackupManager.kt        # 백업/복원 매니저
├── service/
│   ├── NotiListenerService.kt      # 알림 리스너 서비스
│   ├── SmsReceiver.kt              # SMS 수신 리시버
│   └── BootReceiver.kt             # 부팅 시 재시작
├── webhook/
│   └── WebhookSender.kt            # 웹훅 HTTP 전송
├── util/
│   └── Utils.kt                    # 유틸리티 (날짜, 금융앱 목록)
└── ui/
    ├── MainActivity.kt              # 메인 액티비티 + 네비게이션
    ├── theme/Theme.kt               # Compose 테마
    ├── viewmodel/MainViewModel.kt   # 메인 ViewModel
    └── screens/
        ├── HomeScreen.kt            # 알림 목록 화면
        ├── FilterScreen.kt          # 앱 필터 화면
        └── SettingsScreen.kt        # 설정 화면
```
