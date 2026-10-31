package dev.wary.kawach.rules

import java.util.Locale

/**
 * Offline, rule-based scam checker. Free to run and needs no network.
 *
 * Scoring: each matched [Signal] adds its weight, plus 1 for every extra match of the
 * same sign (up to +2). A critical sign, or a score of 5+, is HIGH; 3–4 is MEDIUM.
 */
object ScamRules {

    private const val HIGH_SCORE = 5
    private const val MEDIUM_SCORE = 3
    private const val MIN_TYPE_VOTES = 3
    private const val MAX_REPEAT_BONUS = 2

    // Real bank OTP messages say "do not share". Mask these so they don't read as a request.
    private val safePhrases = Regex("""\b(do not|don'?t|never|not to)\s+(share|disclose)\b""")

    fun check(message: String): Verdict {
        val text = message.lowercase(Locale.ROOT)
        // Same-length mask keeps character positions aligned with the original message.
        val masked = safePhrases.replace(text) { "#".repeat(it.value.length) }

        val matched = mutableListOf<Signal>()
        val spans = mutableListOf<IntRange>()
        var score = 0

        for (signal in SIGNALS) {
            var hits = 0
            for (regex in signal.regexes) {
                for (match in regex.findAll(masked)) {
                    hits++
                    spans += match.range
                }
            }
            for (phrase in signal.native) {
                var at = masked.indexOf(phrase)
                while (at >= 0) {
                    hits++
                    spans += at until at + phrase.length
                    at = masked.indexOf(phrase, at + phrase.length)
                }
            }
            if (hits > 0) {
                matched += signal
                score += signal.weight + minOf(hits - 1, MAX_REPEAT_BONUS)
            }
        }

        val risk = when {
            matched.any { it.critical } || score >= HIGH_SCORE -> Risk.HIGH
            score >= MEDIUM_SCORE -> Risk.MEDIUM
            else -> Risk.LOW
        }

        val votes = linkedMapOf<ScamType, Int>()
        for (signal in matched) {
            for ((type, vote) in signal.votes) votes[type] = (votes[type] ?: 0) + vote
        }
        val scamType = votes.maxByOrNull { it.value }
            ?.takeIf { it.value >= MIN_TYPE_VOTES && risk != Risk.LOW }
            ?.key

        // Lower-casing can change length for a few scripts; only highlight when positions still line up.
        val highlights = if (text.length == message.length) merge(spans) else emptyList()

        return Verdict(risk, scamType, matched.map { it.flag }, score, highlights)
    }

    private fun merge(spans: List<IntRange>): List<IntRange> {
        if (spans.isEmpty()) return emptyList()
        val sorted = spans.sortedBy { it.first }
        val out = mutableListOf(sorted.first())
        for (span in sorted.drop(1)) {
            val last = out.last()
            if (span.first <= last.last + 1) {
                out[out.lastIndex] = last.first..maxOf(last.last, span.last)
            } else {
                out += span
            }
        }
        return out
    }
}
