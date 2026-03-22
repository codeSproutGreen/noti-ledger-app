package com.notiledger.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val fullFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
    private val shortFormat = SimpleDateFormat("MM.dd HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault())

    fun formatFull(timestamp: Long): String = fullFormat.format(Date(timestamp))

    fun formatSmart(timestamp: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            now.get(Calendar.DATE) == target.get(Calendar.DATE) &&
            now.get(Calendar.MONTH) == target.get(Calendar.MONTH) &&
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) -> {
                "오늘 ${timeFormat.format(Date(timestamp))}"
            }
            now.get(Calendar.DATE) - target.get(Calendar.DATE) == 1 &&
            now.get(Calendar.MONTH) == target.get(Calendar.MONTH) -> {
                "어제 ${timeFormat.format(Date(timestamp))}"
            }
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) -> {
                shortFormat.format(Date(timestamp))
            }
            else -> fullFormat.format(Date(timestamp))
        }
    }

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
}

/** 금융앱 패키지명 (한국 주요 은행/증권) */
object FinanceApps {
    val KNOWN_APPS = mapOf(
        "com.kbstar.kbbank" to "KB국민은행",
        "com.shinhan.sbanking" to "신한SOL뱅크",
        "com.wooribank.smart.banking" to "우리WON뱅킹",
        "com.hanabank.ebk.channel.android.hananbank" to "하나은행",
        "com.nh.cashcow" to "NH스마트뱅킹",
        "com.ibk.neobanking" to "IBK기업은행",
        "com.epost.psf.sdsi" to "우체국금융",
        "com.kbankwith" to "케이뱅크",
        "viva.republica.toss" to "토스",
        "com.kakaobank.channel" to "카카오뱅크",
        "com.nhn.android.search" to "네이버페이",
        "com.samsung.android.spay" to "삼성페이",
        "com.kakao.talk" to "카카오톡",
        "com.ssg.serviceapp.android.egiftcertificate" to "SSG페이",
        "com.lottemembers.android" to "L.pay",
        "com.kftc.bankpay.android" to "뱅크페이",
        "kvp.jjy.MispAndroid320" to "ISP/페이북",
        "com.hyundaicard.appcard" to "현대카드",
        "com.shinhancard.smartshinhan" to "신한카드",
        "com.kbcard.cxh.appcard" to "KB국민카드",
        "com.lottecard.lottesmartpay" to "롯데카드",
        "com.samsungcard.mpocket" to "삼성카드",
        "kr.co.bccard.vp" to "BC카드",
        // 메시지 앱 (RCS 등 SMS 브로드캐스트로 수신되지 않는 금융 문자 캡처용)
        "com.samsung.android.messaging" to "메시지",
        "com.google.android.apps.messaging" to "Google 메시지"
    )

    private val MESSAGING_APPS = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging"
    )

    /** 메시지 내용에서 금융사를 추출하기 위한 매핑 (접두어 → 표시명) */
    private val FINANCE_PREFIXES = listOf(
        "삼성" to "삼성카드",
        "신한" to "신한카드",
        "현대" to "현대카드",
        "KB" to "KB국민카드",
        "국민" to "KB국민카드",
        "롯데" to "롯데카드",
        "BC" to "BC카드",
        "하나" to "하나카드",
        "우리" to "우리카드",
        "NH" to "NH카드",
        "IBK" to "IBK기업은행",
        "카카오뱅크" to "카카오뱅크",
        "케이뱅크" to "케이뱅크",
        "토스" to "토스"
    )

    fun isFinanceApp(packageName: String): Boolean {
        return KNOWN_APPS.containsKey(packageName) ||
            packageName.contains("bank", ignoreCase = true) ||
            packageName.contains("pay", ignoreCase = true) ||
            packageName.contains("securities", ignoreCase = true) ||
            packageName.contains("stock", ignoreCase = true) ||
            packageName.contains("insurance", ignoreCase = true) ||
            packageName.contains("card", ignoreCase = true)
    }

    fun isMessagingApp(packageName: String): Boolean = packageName in MESSAGING_APPS

    /** 메시지 앱 알림의 내용에서 실제 금융사 이름을 추출 */
    fun extractFinanceName(content: String): String? {
        val trimmed = content.trimStart()
        for ((prefix, name) in FINANCE_PREFIXES) {
            if (trimmed.startsWith(prefix, ignoreCase = true)) return name
        }
        return null
    }
}
