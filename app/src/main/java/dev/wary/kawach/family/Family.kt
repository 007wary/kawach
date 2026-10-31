package dev.wary.kawach.family

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dev.wary.kawach.R
import dev.wary.kawach.rules.Flag
import dev.wary.kawach.rules.Risk
import dev.wary.kawach.rules.ScamType
import dev.wary.kawach.rules.Verdict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** The family link: who is signed in, who they are linked with, and the alerts between them. */
@OptIn(ExperimentalCoroutinesApi::class)
object Family {
    /** Most guardians one elder can have (blueprint: family_links). */
    const val MAX_GUARDIANS = 3

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val session: StateFlow<Session> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toSession()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser.toSession())

    /** Guardians for an elder, or the elders a guardian protects. */
    val links: StateFlow<List<FamilyLink>> = session.flatMapLatest { s ->
        when (s) {
            Session.SignedOut -> flowOf(emptyList())
            is Session.Elder -> linksWhere("elder_id", s.uid)
            is Session.Guardian -> linksWhere("guardian_id", s.uid)
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** A guardian's most recent alerts, newest first. Empty for elders. */
    val alerts: StateFlow<List<FamilyCheck>> = session.flatMapLatest { s ->
        if (s !is Session.Guardian) {
            flowOf(emptyList())
        } else {
            db.collection("checks")
                .whereArrayContains("guardian_ids", s.uid)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(30)
                .listen { snap -> snap.documents.mapNotNull { it.toCheck() } }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun check(id: String): Flow<FamilyCheck?> = callbackFlow {
        val registration = db.collection("checks").document(id).addSnapshotListener { snap, _ ->
            trySend(snap?.toCheck())
        }
        awaitClose { registration.remove() }
    }

    /** Shows Google's account picker, then signs this phone in as a guardian. */
    suspend fun signInGuardian(context: Context) {
        val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(context).getCredential(context, request)
        val google = GoogleIdTokenCredential.createFrom(result.credential.data)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await()
        saveUser("guardian")
    }

    /** Elder side: joins the guardian who made [code]. Returns the guardian's name. */
    suspend fun join(code: String, elderName: String): String {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        saveUser("elder")
        val response = Api.post("/api/pair", JSONObject().put("code", code).put("name", elderName))
        return response.optString("guardian_name")
    }

    /** Guardian side: a fresh 6-digit code for the elder to type in. */
    suspend fun createInvite(): String = Api.post("/api/invite", JSONObject()).getString("code")

    /**
     * Sends a check to every linked guardian and returns the check's ID.
     * [clientId] makes it safe to call again for the same check: the API sends the alert only once.
     */
    suspend fun sendCheck(clientId: String, text: String, verdict: Verdict, fromPicture: Boolean): String {
        val body = JSONObject()
            .put("type", "check")
            .put("client_id", clientId)
            .put("input_type", if (fromPicture) "image" else "text")
            .put("masked_text", Mask.numbers(text).take(MAX_TEXT))
            .put("verdict", verdict.risk.name)
            .put("scam_type", verdict.scamType?.name ?: JSONObject.NULL)
            .put("red_flags", JSONArray(verdict.flags.map { it.name }))
        return Api.post("/api/notify", body).getString("check_id")
    }

    /** Guardian side: tells the elder whether the message is safe. */
    suspend fun reply(checkId: String, safe: Boolean) {
        val body = JSONObject()
            .put("type", "reply")
            .put("check_id", checkId)
            .put("reply", if (safe) "safe" else "scam")
        Api.post("/api/notify", body)
    }

    suspend fun unlink(link: FamilyLink) {
        db.collection("family_links").document(link.id).delete().await()
    }

    suspend fun signOutGuardian(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            db.collection("users").document(uid).update("fcm_tokens", FieldValue.arrayRemove(token)).await()
        }
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    /** Saves this phone's FCM token again on every start, in case the save at sign-in failed offline. */
    fun syncToken() {
        val uid = auth.currentUser?.uid ?: return
        scope.launch { runCatching { saveToken(uid, FirebaseMessaging.getInstance().token.await()) } }
    }

    /** Called when FCM rotates this phone's token. */
    fun onNewToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        scope.launch { runCatching { saveToken(uid, token) } }
    }

    private suspend fun saveUser(role: String) {
        val uid = auth.currentUser?.uid ?: return
        val user = mapOf(
            "role" to role,
            "language" to Locale.getDefault().language,
            "updated_at" to FieldValue.serverTimestamp(),
        )
        db.collection("users").document(uid).set(user, SetOptions.merge()).await()
        runCatching { saveToken(uid, FirebaseMessaging.getInstance().token.await()) }
    }

    private suspend fun saveToken(uid: String, token: String) {
        db.collection("users").document(uid)
            .set(mapOf("fcm_tokens" to FieldValue.arrayUnion(token)), SetOptions.merge())
            .await()
    }

    private fun linksWhere(field: String, uid: String): Flow<List<FamilyLink>> =
        db.collection("family_links").whereEqualTo(field, uid).listen { snap ->
            snap.documents.map { doc ->
                FamilyLink(
                    id = doc.id,
                    elderId = doc.getString("elder_id").orEmpty(),
                    guardianId = doc.getString("guardian_id").orEmpty(),
                    elderName = doc.getString("elder_name").orEmpty(),
                    guardianName = doc.getString("guardian_name").orEmpty(),
                )
            }
        }

    /** Snapshot listener as a flow. Errors (offline, signed out mid-listen) show as an empty list. */
    private fun <T> Query.listen(map: (com.google.firebase.firestore.QuerySnapshot) -> List<T>): Flow<List<T>> =
        callbackFlow {
            val registration = addSnapshotListener { snap, _ -> trySend(snap?.let(map).orEmpty()) }
            awaitClose { registration.remove() }
        }

    private fun DocumentSnapshot.toCheck(): FamilyCheck? {
        if (!exists()) return null
        val reply = get("guardian_reply") as? Map<*, *>
        return FamilyCheck(
            id = id,
            elderId = getString("elder_id").orEmpty(),
            elderName = getString("elder_name").orEmpty(),
            maskedText = getString("masked_text").orEmpty(),
            risk = enumOrNull<Risk>(getString("verdict")) ?: Risk.MEDIUM,
            scamType = enumOrNull<ScamType>(getString("scam_type")),
            flags = (get("red_flags") as? List<*>).orEmpty().mapNotNull { enumOrNull<Flag>(it as? String) },
            createdAt = getTimestamp("created_at")?.toDate(),
            reply = reply?.let {
                GuardianReply(
                    guardianName = it["guardian_name"] as? String ?: "",
                    safe = it["reply"] == "safe",
                )
            },
        )
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private fun FirebaseUser?.toSession(): Session = when {
        this == null -> Session.SignedOut
        isAnonymous -> Session.Elder(uid)
        else -> Session.Guardian(uid, displayName.orEmpty(), email.orEmpty())
    }

    /** Same cap as the API; long forwards are cut, the warning signs are near the start. */
    private const val MAX_TEXT = 2000
}
