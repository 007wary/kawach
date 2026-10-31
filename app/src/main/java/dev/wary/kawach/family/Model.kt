package dev.wary.kawach.family

import dev.wary.kawach.rules.Flag
import dev.wary.kawach.rules.Risk
import dev.wary.kawach.rules.ScamType
import java.util.Date

/**
 * Who is using this phone. Elders join with an invite code and get an anonymous Firebase account;
 * guardians sign in with Google. One phone is one or the other.
 */
sealed interface Session {
    data object SignedOut : Session
    data class Elder(val uid: String) : Session
    data class Guardian(val uid: String, val name: String, val email: String) : Session
}

/** A `family_links/{elderId}_{guardianId}` document. Only the Vercel API creates these. */
data class FamilyLink(
    val id: String,
    val elderId: String,
    val guardianId: String,
    val elderName: String,
    val guardianName: String,
)

/** A `checks/{id}` document: one message an elder checked, as their guardians see it. */
data class FamilyCheck(
    val id: String,
    val elderId: String,
    val elderName: String,
    val maskedText: String,
    val risk: Risk,
    val scamType: ScamType?,
    val flags: List<Flag>,
    val createdAt: Date?,
    val reply: GuardianReply?,
)

data class GuardianReply(val guardianName: String, val safe: Boolean)
