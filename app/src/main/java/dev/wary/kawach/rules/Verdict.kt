package dev.wary.kawach.rules

enum class Risk { HIGH, MEDIUM, LOW }

enum class ScamType {
    DIGITAL_ARREST,
    BANK_KYC,
    UPI_COLLECT,
    INVESTMENT,
    TASK_JOB,
    LOTTERY_PRIZE,
    ELECTRICITY_BILL,
    FAMILY_EMERGENCY,
    SEXTORTION,
    FAKE_DELIVERY,
}

/** A single warning sign found in a message. Shown to the user as a "why" chip. */
enum class Flag {
    OFFICIAL_THREAT,
    ARREST_THREAT,
    VIDEO_CALL,
    SECRECY,
    SAFE_ACCOUNT,
    URGENCY,
    ASKS_OTP_PIN,
    ACCOUNT_BLOCK,
    RECEIVE_BY_PIN,
    ASKS_PAYMENT,
    PRIZE_FEE,
    TOO_GOOD_RETURNS,
    WORK_TASK,
    DISCONNECTION,
    EMERGENCY_MONEY,
    LEAK_THREAT,
    PARCEL_ISSUE,
    SHORT_LINK,
    APK_FILE,
    FOREIGN_NUMBER,
}

data class Verdict(
    val risk: Risk,
    /** Best-matching scam type, or null when the signs don't point to one type. Always null for LOW. */
    val scamType: ScamType?,
    val flags: List<Flag>,
    val score: Int,
    /** Character ranges in the original message to highlight. Sorted and non-overlapping. */
    val highlights: List<IntRange>,
)
