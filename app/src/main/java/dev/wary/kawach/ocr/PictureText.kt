package dev.wary.kawach.ocr

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Reads the words in a screenshot or photo, on the phone (ML Kit). The picture is never uploaded.
 * The Devanagari model reads Hindi and English, and on the emulator it also read Assamese script,
 * with a few letters wrong. The result screen asks people to paste the message if words look wrong.
 */
object PictureText {

    class NoWords : Exception()

    /** Google Play services is still downloading the reading model (first use on a sideloaded app). */
    class NotReady : Exception()

    private val recognizer by lazy { TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()) }

    suspend fun read(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val text = recognize(context, image)
        if (!hasWords(text)) throw NoWords()
        return text
    }

    private suspend fun recognize(context: Context, image: InputImage): String {
        repeat(READY_TRIES) { attempt ->
            try {
                return recognizer.process(image).await().textBlocks.joinToString("\n") { it.text }
            } catch (e: MlKitException) {
                if (e.errorCode != MlKitException.UNAVAILABLE) throw e
                if (attempt == 0) {
                    val request = ModuleInstallRequest.newBuilder().addApi(recognizer).build()
                    runCatching { ModuleInstall.getClient(context).installModules(request).await() }
                }
                delay(READY_WAIT_MS)
            }
        }
        throw NotReady()
    }

    /** A blank photo or a picture of a face reads as a few stray letters; that isn't a message. */
    fun hasWords(text: String): Boolean = text.count { it.isLetter() } >= MIN_LETTERS

    private const val MIN_LETTERS = 8
    private const val READY_TRIES = 20
    private const val READY_WAIT_MS = 2_000L
}
