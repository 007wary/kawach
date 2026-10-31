package dev.wary.kawach.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureTextTest {

    @Test
    fun strayLettersAreNotAMessage() {
        assertFalse(PictureText.hasWords(""))
        assertFalse(PictureText.hasWords("12:45 4G 87%"))
        assertFalse(PictureText.hasWords("O K ☺"))
    }

    @Test
    fun messagesInEnglishAndHindiCount() {
        assertTrue(PictureText.hasWords("Your KYC is pending"))
        assertTrue(PictureText.hasWords("आपका खाता बंद होगा"))
    }
}
