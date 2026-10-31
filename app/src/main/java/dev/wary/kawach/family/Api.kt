package dev.wary.kawach.family

import com.google.firebase.auth.FirebaseAuth
import dev.wary.kawach.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** The error code the API returned, such as "bad_code" or "not_linked". "network" means it couldn't be reached. */
class ApiException(val error: String) : Exception(error)

/** Calls the Vercel functions in web/api with the user's Firebase ID token. */
internal object Api {

    suspend fun post(path: String, body: JSONObject): JSONObject {
        val user = FirebaseAuth.getInstance().currentUser ?: throw ApiException("signed_out")
        val token = runCatching { user.getIdToken(false).await().token }.getOrNull()
            ?: throw ApiException("network")
        return withContext(Dispatchers.IO) {
            val conn = URL(BuildConfig.API_BASE + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                if (status !in 200..299) throw ApiException(json.optString("error", "http_$status"))
                json
            } catch (e: java.io.IOException) {
                throw ApiException("network")
            } finally {
                conn.disconnect()
            }
        }
    }
}
