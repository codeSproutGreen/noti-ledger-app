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
        "kr.co.bccard.vp" to "BC카드"
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
}
