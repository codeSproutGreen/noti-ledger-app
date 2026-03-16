# NotiLedger App

금융앱 푸시 알림과 SMS를 자동으로 수집하여 **NotiLedger 서버**로 웹훅 전송하는 Android 앱입니다.

## 주요 기능

- **알림 수집**: `NotificationListenerService`를 사용하여 앱 푸시 알림 수집
- **SMS 수집**: `BroadcastReceiver`로 문자 메시지 수신 (인증번호만 필터링 가능)
- **앱별 필터링**: 알림을 수집할 앱을 개별적으로 ON/OFF
- **웹훅 전송**: 수집된 데이터를 HTTP POST로 NotiLedger 서버에 전송 (HMAC-SHA256 서명 지원)
- **로컬 저장**: 모든 데이터는 Room DB에 기기 내에만 저장
- **백업/복원**: JSON 파일로 내보내기/가져오기
- **검색**: 알림 내용, 제목, 앱 이름으로 검색
- **금융앱 자동 인식**: 한국 주요 은행/페이 앱 자동 분류

## 기술 스택

- Kotlin + Jetpack Compose
- Room Database (SQLite)
- DataStore Preferences
- OkHttp (웹훅 HTTP 클라이언트)
- Material 3 + Dynamic Color

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

## 빌드 및 설치

### Android Studio에서 열기

1. Android Studio (Hedgehog 이상) 실행
2. `File > Open` → 이 프로젝트 폴더 선택
3. Gradle Sync 완료 대기

### APK 빌드

```bash
./gradlew assembleDebug
```

### 직접 설치

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 초기 설정

### 필수: 알림 접근 권한

앱 실행 후 **설정 > 권한 설정 > 알림 접근 권한** → `NotiLedger` 허용

### 필수: SMS 권한

앱 첫 실행 시 SMS 수신 권한 요청 팝업 **허용**

### 권장: 배터리 최적화 예외

**설정 > 배터리 최적화 예외** → NotiLedger 제외

### 웹훅 설정

**설정 > 웹훅 전송** 활성화 후 NotiLedger 서버 URL 입력:
```
https://your-server.com/api/webhook
```

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
  "timestamp": 1710000000000
}
```

## 인식되는 금융앱

| 앱 | 패키지명 |
|---|---|
| KB국민은행 | com.kbstar.kbbank |
| 신한SOL뱅크 | com.shinhan.sbanking |
| 우리WON뱅킹 | com.wooribank.smart.banking |
| 하나은행 | com.hanabank.ebk.channel.android.hananbank |
| NH스마트뱅킹 | com.nh.cashcow |
| IBK기업은행 | com.ibk.neobanking |
| 토스 | viva.republica.toss |
| 카카오뱅크 | com.kakaobank.channel |
| 케이뱅크 | com.kbankwith |

패키지명에 `bank`, `pay`, `securities`, `stock`, `insurance`가 포함된 앱도 자동으로 금융앱으로 분류됩니다.
