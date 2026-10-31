package dev.wary.kawach.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScamRulesTest {

    private fun check(text: String) = ScamRules.check(text)

    @Test
    fun digitalArrest() {
        val v = check(
            "This is CBI officer Sharma. Your Aadhaar is linked to money laundering. Stay on video call. " +
                "Transfer ₹2,40,000 to RBI safe account or you will be arrested.",
        )
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.DIGITAL_ARREST, v.scamType)
        assertTrue(Flag.SAFE_ACCOUNT in v.flags)
        assertTrue(v.highlights.isNotEmpty())
    }

    @Test
    fun genuineBankOtpIsNotFlagged() {
        val v = check("123456 is your OTP for login to SBI YONO. Do not share it with anyone. -SBI")
        assertEquals(Risk.LOW, v.risk)
        assertNull(v.scamType)
    }

    @Test
    fun askingForOtpIsHigh() {
        val v = check("Sir please share the OTP you received to complete your KYC update, otherwise account will be blocked")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.BANK_KYC, v.scamType)
    }

    /** Exact words ML Kit read from an Assamese screenshot on the emulator, misreadings included. */
    @Test
    fun assameseKycScreenshotIsHigh() {
        val v = check("আপোনাৰ বেংক একাউন্ট আজি বন্ধ হৈ যাব।\nএতিয়াই\nKYC আপডেট কবক আৰ oTP কও্ক।")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.BANK_KYC, v.scamType)
    }

    @Test
    fun upiCollectTrap() {
        val v = check("Enter your UPI PIN to receive ₹5,000 cashback from PhonePe")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.UPI_COLLECT, v.scamType)
    }

    @Test
    fun lottery() {
        val v = check("Congratulations! You have won KBC lottery of Rs 25,00,000. Pay processing fee Rs 12,500 to claim.")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.LOTTERY_PRIZE, v.scamType)
    }

    @Test
    fun electricityDisconnection() {
        val v = check(
            "Dear consumer, your electricity power will be disconnected tonight at 9.30 pm because your " +
                "previous month bill was not updated. Please call our officer 9876543210 immediately.",
        )
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.ELECTRICITY_BILL, v.scamType)
    }

    @Test
    fun taskJob() {
        val v = check("Part time job! Earn ₹3000 per day by liking YouTube videos. Contact on Telegram.")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.TASK_JOB, v.scamType)
    }

    @Test
    fun investment() {
        val v = check("Join our VIP trading group for guaranteed returns of 5% daily")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.INVESTMENT, v.scamType)
    }

    @Test
    fun familyEmergency() {
        val v = check("Papa this is my new number. I had an accident, please send 20000 urgently")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.FAMILY_EMERGENCY, v.scamType)
    }

    @Test
    fun apkLinkIsHighEvenWithoutAType() {
        val v = check("Your SBI reward points expire today. Install SBI_Rewards.apk to redeem ₹7,250.")
        assertEquals(Risk.HIGH, v.risk)
        assertTrue(Flag.APK_FILE in v.flags)
    }

    @Test
    fun hindiKycScam() {
        val v = check("आपका खाता बंद हो जाएगा, तुरंत केवाईसी अपडेट करें")
        assertEquals(Risk.HIGH, v.risk)
        assertEquals(ScamType.BANK_KYC, v.scamType)
    }

    @Test
    fun assameseArrestThreat() {
        val v = check("আপোনাক গ্ৰেপ্তাৰ কৰা হ'ব")
        assertNotEquals(Risk.LOW, v.risk)
        assertEquals(ScamType.DIGITAL_ARREST, v.scamType)
    }

    @Test
    fun normalMessagesStayLow() {
        listOf(
            "Hi Maa, are we meeting tomorrow at 5?",
            "Your Amazon order has been delivered. Rate your experience.",
            "Happy Bihu! Wishing you and your family a wonderful year.",
            "Paid ₹250 to Ramesh Kirana via UPI. Ref 412899301172.",
        ).forEach { msg ->
            assertEquals(msg, Risk.LOW, check(msg).risk)
        }
    }

    @Test
    fun indianNumberIsNotForeign() {
        val v = check("Call me on +91 98765 43210")
        assertTrue(Flag.FOREIGN_NUMBER !in v.flags)
        assertTrue(Flag.FOREIGN_NUMBER in check("Call +92 3001234567 now").flags)
    }
}
