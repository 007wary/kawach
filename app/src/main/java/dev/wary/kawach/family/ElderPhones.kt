package dev.wary.kawach.family

import android.content.Context

/**
 * The phone number a guardian saved for each elder, for the alert's Call button.
 * Kept only on the guardian's phone, never uploaded.
 */
object ElderPhones {
    private const val PREFS = "elder_phones"

    fun get(context: Context, elderId: String): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(elderId, null).orEmpty()

    fun set(context: Context, elderId: String, phone: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(elderId, phone.trim()).apply()
    }
}
