package dev.wary.kawach.family

/**
 * Hides phone, account and card numbers before a message leaves the phone (see the privacy policy).
 * Any run of 6 or more digits, allowing single spaces or dashes between them, keeps only its last 2 digits.
 * The text keeps its length, so the rule engine's highlight ranges still line up.
 */
object Mask {
    private val longNumber = Regex("""\d(?:[ -]?\d){5,}""")

    fun numbers(text: String): String = longNumber.replace(text) { match ->
        val digits = match.value.count(Char::isDigit)
        var seen = 0
        buildString {
            match.value.forEach { c ->
                if (c.isDigit()) {
                    seen++
                    append(if (seen > digits - 2) c else '•')
                } else {
                    append(c)
                }
            }
        }
    }
}
