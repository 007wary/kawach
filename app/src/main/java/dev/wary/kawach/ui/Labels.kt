package dev.wary.kawach.ui

import androidx.annotation.StringRes
import dev.wary.kawach.R
import dev.wary.kawach.rules.Flag
import dev.wary.kawach.rules.Risk
import dev.wary.kawach.rules.ScamType
import dev.wary.kawach.rules.Verdict

@StringRes
fun ScamType.titleRes(): Int = when (this) {
    ScamType.DIGITAL_ARREST -> R.string.scam_digital_arrest_title
    ScamType.BANK_KYC -> R.string.scam_bank_kyc_title
    ScamType.UPI_COLLECT -> R.string.scam_upi_collect_title
    ScamType.INVESTMENT -> R.string.scam_investment_title
    ScamType.TASK_JOB -> R.string.scam_task_job_title
    ScamType.LOTTERY_PRIZE -> R.string.scam_lottery_prize_title
    ScamType.ELECTRICITY_BILL -> R.string.scam_electricity_bill_title
    ScamType.FAMILY_EMERGENCY -> R.string.scam_family_emergency_title
    ScamType.SEXTORTION -> R.string.scam_sextortion_title
    ScamType.FAKE_DELIVERY -> R.string.scam_fake_delivery_title
}

@StringRes
fun ScamType.adviceRes(): Int = when (this) {
    ScamType.DIGITAL_ARREST -> R.string.scam_digital_arrest_advice
    ScamType.BANK_KYC -> R.string.scam_bank_kyc_advice
    ScamType.UPI_COLLECT -> R.string.scam_upi_collect_advice
    ScamType.INVESTMENT -> R.string.scam_investment_advice
    ScamType.TASK_JOB -> R.string.scam_task_job_advice
    ScamType.LOTTERY_PRIZE -> R.string.scam_lottery_prize_advice
    ScamType.ELECTRICITY_BILL -> R.string.scam_electricity_bill_advice
    ScamType.FAMILY_EMERGENCY -> R.string.scam_family_emergency_advice
    ScamType.SEXTORTION -> R.string.scam_sextortion_advice
    ScamType.FAKE_DELIVERY -> R.string.scam_fake_delivery_advice
}

@StringRes
fun Flag.labelRes(): Int = when (this) {
    Flag.OFFICIAL_THREAT -> R.string.flag_official_threat
    Flag.ARREST_THREAT -> R.string.flag_arrest_threat
    Flag.VIDEO_CALL -> R.string.flag_video_call
    Flag.SECRECY -> R.string.flag_secrecy
    Flag.SAFE_ACCOUNT -> R.string.flag_safe_account
    Flag.URGENCY -> R.string.flag_urgency
    Flag.ASKS_OTP_PIN -> R.string.flag_asks_otp_pin
    Flag.ACCOUNT_BLOCK -> R.string.flag_account_block
    Flag.RECEIVE_BY_PIN -> R.string.flag_receive_by_pin
    Flag.ASKS_PAYMENT -> R.string.flag_asks_payment
    Flag.PRIZE_FEE -> R.string.flag_prize_fee
    Flag.TOO_GOOD_RETURNS -> R.string.flag_too_good_returns
    Flag.WORK_TASK -> R.string.flag_work_task
    Flag.DISCONNECTION -> R.string.flag_disconnection
    Flag.EMERGENCY_MONEY -> R.string.flag_emergency_money
    Flag.LEAK_THREAT -> R.string.flag_leak_threat
    Flag.PARCEL_ISSUE -> R.string.flag_parcel_issue
    Flag.SHORT_LINK -> R.string.flag_short_link
    Flag.APK_FILE -> R.string.flag_apk_file
    Flag.FOREIGN_NUMBER -> R.string.flag_foreign_number
}

@StringRes
fun Risk.labelRes(): Int = when (this) {
    Risk.HIGH -> R.string.risk_high
    Risk.MEDIUM -> R.string.risk_medium
    Risk.LOW -> R.string.risk_low
}

@StringRes
fun Verdict.titleRes(): Int = when {
    risk == Risk.LOW -> R.string.low_title
    scamType != null -> scamType.titleRes()
    risk == Risk.HIGH -> R.string.generic_high_title
    else -> R.string.generic_medium_title
}

@StringRes
fun Verdict.adviceRes(): Int = when {
    risk == Risk.LOW -> R.string.low_advice
    scamType != null -> scamType.adviceRes()
    risk == Risk.HIGH -> R.string.generic_high_advice
    else -> R.string.generic_medium_advice
}
