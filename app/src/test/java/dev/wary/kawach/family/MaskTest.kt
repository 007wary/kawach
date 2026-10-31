package dev.wary.kawach.family

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskTest {

    @Test
    fun masksPhoneNumbers() {
        assertEquals("Call +•• ••••• •••10 now", Mask.numbers("Call +91 98765 43210 now"))
        assertEquals("Call ••••••••10", Mask.numbers("Call 9876543210"))
    }

    @Test
    fun masksAccountAndCardNumbers() {
        assertEquals("A/c •••••••••••••89 blocked", Mask.numbers("A/c 123456789012389 blocked"))
        assertEquals("•••• •••• •••• ••11", Mask.numbers("4111 1111 1111 1111"))
        assertEquals("OTP ••••56", Mask.numbers("OTP 123456"))
    }

    @Test
    fun keepsAmountsAndShortNumbers() {
        val text = "Pay Rs 1,00,000 or 5000 in 24 hours"
        assertEquals(text, Mask.numbers(text))
    }

    @Test
    fun keepsLength() {
        val text = "Send 9876543210 to KYC 4111-1111-1111"
        assertEquals(text.length, Mask.numbers(text).length)
    }
}
