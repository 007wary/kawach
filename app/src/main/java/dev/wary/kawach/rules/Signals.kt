package dev.wary.kawach.rules

import dev.wary.kawach.rules.Flag.*
import dev.wary.kawach.rules.ScamType.*

/**
 * One warning sign and how to spot it.
 *
 * [latin] patterns are regexes run on the lower-cased message (English and romanised Hindi).
 * [native] phrases are plain substrings in Devanagari or Assamese script, where `\b` is unreliable.
 * [votes] says which scam types this sign points to, and how strongly.
 * A [critical] sign makes the verdict HIGH on its own.
 */
internal class Signal(
    val flag: Flag,
    val weight: Int,
    val critical: Boolean = false,
    val votes: Map<ScamType, Int> = emptyMap(),
    latin: List<String> = emptyList(),
    val native: List<String> = emptyList(),
) {
    val regexes: List<Regex> = latin.map(::Regex)
}

// Hindi and Assamese phrases are first drafts and need review by native speakers.
internal val SIGNALS: List<Signal> = listOf(
    Signal(
        OFFICIAL_THREAT, weight = 2, votes = mapOf(DIGITAL_ARREST to 3),
        latin = listOf(
            """\b(cbi|ncb|narcotics|enforcement directorate|ed officer|customs (officer|department)|cyber (crime|cell) (officer|department|branch)|trai|(mumbai|delhi) police|income tax officer|rbi officer)\b""",
        ),
        native = listOf("सीबीआई", "पुलिस अधिकारी", "कस्टम विभाग", "চিবিআই", "আৰক্ষী বিষয়া"),
    ),
    Signal(
        ARREST_THREAT, weight = 4, votes = mapOf(DIGITAL_ARREST to 4),
        latin = listOf(
            """\b(digital arrest|arrest warrant|(will|shall) be arrested|non[- ]?bailable|fir (has been|is|was) (registered|filed|lodged)|money laundering|drugs? (parcel|trafficking)|illegal parcel)\b""",
        ),
        native = listOf("डिजिटल अरेस्ट", "गिरफ्तार", "गिरफ़्तार", "গ্ৰেপ্তাৰ", "ডিজিটেল এৰেষ্ট"),
    ),
    Signal(
        VIDEO_CALL, weight = 2, votes = mapOf(DIGITAL_ARREST to 2, SEXTORTION to 1),
        latin = listOf(
            """\b(stay on (the )?(video )?call|skype|video call (for|with) (verification|investigation|statement)|do not (cut|disconnect|end) the call)\b""",
        ),
        native = listOf("वीडियो कॉल पर रहें", "कॉल मत काटना", "ভিডিঅ' কলত থাকক"),
    ),
    Signal(
        SECRECY, weight = 2, votes = mapOf(DIGITAL_ARREST to 2, FAMILY_EMERGENCY to 1),
        latin = listOf(
            """\b(do not (tell|inform) (anyone|anybody|your family|family)|don'?t tell (anyone|anybody|your family)|keep (this|it) (secret|confidential)|confidential investigation)\b""",
        ),
        native = listOf("किसी को मत बताना", "किसी को न बताएं", "किसी को न बताएँ", "কাকো নক'ব"),
    ),
    Signal(
        SAFE_ACCOUNT, weight = 4, critical = true, votes = mapOf(DIGITAL_ARREST to 3),
        latin = listOf(
            """\b(safe account|secure account|verification account|rbi (safe|secure|verification) account|(transfer|deposit).{0,30}for verification)\b""",
        ),
        native = listOf("सुरक्षित खाते", "सुरक्षित खाता", "সুৰক্ষিত একাউণ্ট"),
    ),
    Signal(
        URGENCY, weight = 2,
        latin = listOf(
            """\b(urgent(ly)?|immediately|today itself|tonight|act now|right now|last (chance|warning)|within (\d+|one|two|24) (hours?|hrs?|minutes?|mins?))\b""",
        ),
        native = listOf("तुरंत", "आज रात", "এতিয়াই", "আজি নিশা", "তৎক্ষণাৎ"),
    ),
    Signal(
        ASKS_OTP_PIN, weight = 4, critical = true, votes = mapOf(BANK_KYC to 3, UPI_COLLECT to 1),
        latin = listOf(
            """\b(share|send|tell|give|forward|provide|read out|bata\w*)\W+(\w+\W+){0,4}?(otp|upi pin|atm pin|mpin|pin|cvv|verification code|code)\b""",
            """\b(otp|cvv|upi pin|mpin|atm pin)\W+(\w+\W+){0,4}?(share|send|tell|give|forward|bata\w*)\b""",
        ),
        native = listOf("ओटीपी बताएं", "ओटीपी बताइए", "ओटीपी भेजें", "otp बताएं", "otp बताइए", "otp भेजें", "otp দিয়ক", "otp কওক"),
    ),
    Signal(
        ACCOUNT_BLOCK, weight = 3, votes = mapOf(BANK_KYC to 4),
        latin = listOf(
            """\b(account (will be|has been|is|shall be) (blocked|suspended|closed|frozen|deactivated|on hold)|kyc (update|updation|expired|pending|verification|not (done|updated|completed))|update (your )?(kyc|pan)|pan (card )?(is )?not (linked|updated)|sim (card )?(will be|is being) (blocked|deactivated))\b""",
        ),
        // Both Assamese spellings of "account" are common (ণ্ট and ন্ট), and screenshots read either way.
        native = listOf(
            "खाता बंद", "खाता ब्लॉक", "केवाईसी", "kyc अपडेट",
            "একাউণ্ট বন্ধ", "একাউণ্ট ব্লক", "একাউন্ট বন্ধ", "একাউন্ট ব্লক", "kyc আপডেট",
        ),
    ),
    Signal(
        RECEIVE_BY_PIN, weight = 4, critical = true, votes = mapOf(UPI_COLLECT to 5),
        latin = listOf(
            """\b(enter (your )?(upi )?pin to (receive|get|accept|claim)|scan (this |the )?qr( code)? to (receive|get|claim)|approve (the |this )?(collect )?request to (receive|get)|collect request)\b""",
        ),
        native = listOf("पैसे पाने के लिए पिन", "पैसे पाने के लिए pin"),
    ),
    Signal(
        ASKS_PAYMENT, weight = 1, votes = mapOf(FAMILY_EMERGENCY to 1),
        latin = listOf(
            """\b(pay|transfer|send|deposit)\b.{0,25}(₹|\brs\b|\binr\b|rupees|\d{3,})""",
            """₹\s?\d""",
            """(?<![a-z])rs\.?\s?\d""",
        ),
        native = listOf("पैसे भेजो", "पैसे भेजें", "भुगतान करें", "পইচা পঠিয়াওক", "টকা পঠিয়াওক"),
    ),
    Signal(
        PRIZE_FEE, weight = 4, votes = mapOf(LOTTERY_PRIZE to 4),
        latin = listOf(
            """\b(you (have )?won|lottery|lucky draw|kbc|jackpot|prize money|claim your (prize|reward|gift)|(processing|registration|gst|tax|delivery) (fee|charge)s?)\b""",
        ),
        native = listOf("लॉटरी", "इनाम जीता", "आपने जीता", "লটাৰী", "পুৰস্কাৰ জিকিছে"),
    ),
    Signal(
        TOO_GOOD_RETURNS, weight = 4, votes = mapOf(INVESTMENT to 4),
        latin = listOf(
            """\b(guaranteed (return|profit|income)s?|double (your )?money|\d{1,3}\s?% (daily|per day|weekly|per week|monthly) (return|profit)?|daily profit|stock tips?|trading (group|tips)|vip (group|trading)|ipo allotment|crypto (investment|trading)|risk[- ]free (investment|profit|returns?))""",
        ),
        native = listOf("पैसा डबल", "गारंटीड रिटर्न", "টকা দুগুণ"),
    ),
    Signal(
        WORK_TASK, weight = 4, votes = mapOf(TASK_JOB to 4),
        latin = listOf(
            """\b(part[- ]time (job|work)|work from home|earn (rs\.?|₹|inr)?\s?\d[\d,]* ?(per|a|/) ?(day|daily|task|hour)|lik(e|ing) (youtube )?videos|rat(e|ing) (hotels|products|restaurants)|prepaid tasks?|telegram task|simple tasks?)""",
        ),
        native = listOf("घर बैठे कमाएं", "घर बैठे कमाई", "पार्ट टाइम जॉब", "ঘৰতে বহি উপাৰ্জন"),
    ),
    Signal(
        DISCONNECTION, weight = 4, votes = mapOf(ELECTRICITY_BILL to 4),
        latin = listOf(
            """\b(electricity|power|bijli|apdcl|light connection)\b.{0,40}\b(disconnect(ed|ion)?|cut|will be stopped|suspended)\b""",
            """\bbill (was |is |has )?not (been )?updated\b""",
        ),
        native = listOf("बिजली कनेक्शन", "बिजली काट", "বিদ্যুৎ সংযোগ বিচ্ছিন্ন", "বিদ্যুৎ সংযোগ কাটি"),
    ),
    Signal(
        EMERGENCY_MONEY, weight = 4, votes = mapOf(FAMILY_EMERGENCY to 4),
        latin = listOf(
            """\b(accident|hospital|icu|police station|in jail|arrested)\b.{0,60}\b(money|pay|send|transfer|rs|rupees)\b""",
            """\b(new number|my new no)\b.{0,60}\b(urgent\w*|send money|need money|emergency|accident|hospital)\b""",
        ),
        native = listOf("एक्सीडेंट हो गया", "अस्पताल में हूँ", "पैसे चाहिए", "দুৰ্ঘটনা", "হাস্পতালত"),
    ),
    Signal(
        LEAK_THREAT, weight = 4, critical = true, votes = mapOf(SEXTORTION to 5),
        latin = listOf(
            """\b(your (video|photos?|pics?) (will be |is going )?(viral|leaked|uploaded|shared)|recorded your video|(nude|private|obscene) (video|photo)s?|send (it )?to (all )?your contacts)\b""",
        ),
        native = listOf("वीडियो वायरल", "ভিডিঅ' ভাইৰেল"),
    ),
    Signal(
        PARCEL_ISSUE, weight = 3, votes = mapOf(FAKE_DELIVERY to 4, DIGITAL_ARREST to 1),
        latin = listOf(
            """\b(parcel|package|shipment|courier|india post|delivery)\b.{0,50}\b(on hold|undelivered|could not be delivered|delivery failed|incomplete address|update (your )?address|held (at|by) customs|returned)\b""",
        ),
        native = listOf("पार्सल रुका", "পাৰ্চেল আটক"),
    ),
    Signal(
        SHORT_LINK, weight = 2, votes = mapOf(FAKE_DELIVERY to 1, BANK_KYC to 1),
        latin = listOf(
            """\b(bit\.ly|tinyurl\.com|t\.ly|cutt\.ly|is\.gd|rb\.gy|shorturl\.at|tiny\.cc|goo\.gl|ow\.ly|s\.id)/""",
        ),
    ),
    Signal(
        APK_FILE, weight = 4, critical = true, votes = mapOf(BANK_KYC to 2, FAKE_DELIVERY to 2),
        latin = listOf("""\.apk\b"""),
    ),
    Signal(
        FOREIGN_NUMBER, weight = 2,
        // Country codes most used by scam call centres reaching India: Pakistan, Myanmar,
        // Cambodia, Laos, Vietnam, Philippines, Malaysia, Indonesia, Nigeria.
        latin = listOf("""(?<!\d)(\+|00)(92|95|855|856|84|63|60|62|234)[\s-]?\d{5,}"""),
    ),
)
