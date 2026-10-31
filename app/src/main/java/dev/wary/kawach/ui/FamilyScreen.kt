package dev.wary.kawach.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import dev.wary.kawach.R
import dev.wary.kawach.family.ApiException
import dev.wary.kawach.family.ElderPhones
import dev.wary.kawach.family.Family
import dev.wary.kawach.family.FamilyCheck
import dev.wary.kawach.family.FamilyLink
import dev.wary.kawach.family.Session
import dev.wary.kawach.rules.Verdict
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun FamilyScreen(onBack: () -> Unit, onOpenAlert: (String) -> Unit) {
    val session by Family.session.collectAsState()
    val links by Family.links.collectAsState()

    ScreenColumn {
        ScreenHeader(stringResource(R.string.family_title), onBack)
        val current = session
        when {
            current is Session.Guardian -> {
                NotificationPrompt()
                GuardianPanel(current, links, onOpenAlert)
            }
            links.isEmpty() -> SetupPanel()
            else -> {
                NotificationPrompt()
                ElderPanel(links)
            }
        }
    }
}

/** First visit: the elder types a code, or the family member signs in with Google. */
@Composable
private fun SetupPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }

    Text(stringResource(R.string.family_intro), style = MaterialTheme.typography.bodyLarge)

    Section(stringResource(R.string.family_elder_title), stringResource(R.string.family_elder_sub)) {
        JoinForm(initialName = "")
    }

    Section(stringResource(R.string.family_guardian_title), stringResource(R.string.family_guardian_sub)) {
        OutlinedButton(
            enabled = !busy,
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        Family.signInGuardian(context)
                    } catch (_: GetCredentialCancellationException) {
                        // The person closed the account picker.
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = errorRes(e)
                    }
                    busy = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchHeight),
        ) {
            Text(stringResource(if (busy) R.string.family_working else R.string.family_google))
        }
        error?.let { ErrorText(it) }
    }
}

/** The elder's name and the guardian's 6-digit code. */
@Composable
private fun JoinForm(initialName: String) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var joined by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(40) },
        label = { Text(stringResource(R.string.family_name_label)) },
        placeholder = { Text(stringResource(R.string.family_name_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.filter(Char::isDigit).take(6) },
        label = { Text(stringResource(R.string.family_code_label)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 6.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        enabled = !busy,
        onClick = {
            error = when {
                name.isBlank() -> R.string.error_name
                code.length != 6 -> R.string.error_code_length
                else -> null
            }
            if (error != null) return@Button
            scope.launch {
                busy = true
                try {
                    joined = Family.join(code, name.trim())
                    code = ""
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = errorRes(e)
                }
                busy = false
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchHeight),
    ) {
        Text(stringResource(if (busy) R.string.family_working else R.string.family_join))
    }
    error?.let { ErrorText(it) }
    joined?.let {
        Text(stringResource(R.string.family_joined, it), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ElderPanel(links: List<FamilyLink>) {
    var adding by rememberSaveable { mutableStateOf(false) }

    Text(stringResource(R.string.family_my_guardians), style = MaterialTheme.typography.titleMedium)
    links.forEach { link ->
        LinkRow(name = link.guardianName, link = link)
    }
    if (links.size < Family.MAX_GUARDIANS) {
        if (adding) {
            Section(stringResource(R.string.family_add_guardian), stringResource(R.string.family_elder_sub)) {
                JoinForm(initialName = links.first().elderName)
            }
        } else {
            OutlinedButton(
                onClick = { adding = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchHeight),
            ) {
                Text(stringResource(R.string.family_add_guardian))
            }
        }
    }
}

@Composable
private fun GuardianPanel(session: Session.Guardian, links: List<FamilyLink>, onOpenAlert: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alerts by Family.alerts.collectAsState()

    Text(
        text = stringResource(R.string.family_signed_in, session.name.ifBlank { session.email }),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Text(stringResource(R.string.family_alerts), style = MaterialTheme.typography.titleMedium)
    if (alerts.isEmpty()) {
        Text(stringResource(R.string.family_no_alerts), style = MaterialTheme.typography.bodyMedium)
    }
    alerts.forEach { AlertRow(it, onClick = { onOpenAlert(it.id) }) }

    Text(stringResource(R.string.family_protecting), style = MaterialTheme.typography.titleMedium)
    if (links.isEmpty()) {
        Text(stringResource(R.string.family_none_yet), style = MaterialTheme.typography.bodyMedium)
    }
    links.forEach { link ->
        LinkRow(name = link.elderName, link = link) {
            var phone by remember(link.elderId) { mutableStateOf(ElderPhones.get(context, link.elderId)) }
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16)
                    ElderPhones.set(context, link.elderId, phone)
                },
                label = { Text(stringResource(R.string.family_phone_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    InviteCard()

    TextButton(
        onClick = { scope.launch { Family.signOutGuardian(context) } },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchHeight),
    ) {
        Text(stringResource(R.string.family_sign_out))
    }
}

/** Gets a fresh code from the API and shows it large, ready to read out or send. */
@Composable
private fun InviteCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val shareText = code?.let { stringResource(R.string.family_code_share_text, it) }

    code?.let {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.family_code_title), textAlign = TextAlign.Center)
                Text(
                    text = it.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
                Text(
                    text = stringResource(R.string.family_code_expiry),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    onClick = { shareText?.let { text -> shareWithFamily(context, text) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TouchHeight),
                ) {
                    Text(stringResource(R.string.family_code_share))
                }
            }
        }
    }
    Button(
        enabled = !busy,
        onClick = {
            scope.launch {
                busy = true
                error = null
                try {
                    code = Family.createInvite()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = errorRes(e)
                }
                busy = false
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchHeight),
    ) {
        Text(stringResource(if (busy) R.string.family_working else R.string.family_get_code))
    }
    error?.let { ErrorText(it) }
}

@Composable
private fun AlertRow(check: FamilyCheck, onClick: () -> Unit) {
    val verdict = Verdict(check.risk, check.scamType, check.flags, 0, emptyList())
    val status = when {
        check.reply == null -> stringResource(R.string.family_alert_waiting)
        check.reply.safe -> stringResource(R.string.family_alert_safe)
        else -> stringResource(R.string.family_alert_scam)
    }
    val time = check.createdAt?.let { DateUtils.getRelativeTimeSpanString(it.time).toString() }.orEmpty()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (check.reply == null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = check.elderName + " · " + stringResource(check.risk.labelRes()),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(stringResource(verdict.titleRes()), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$status · $time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A linked person, with Remove (after confirming) and optional extra fields below. */
@Composable
private fun LinkRow(name: String, link: FamilyLink, extra: @Composable () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { confirm = true }) {
                    Text(stringResource(R.string.family_remove))
                }
            }
            extra()
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.family_remove_title, name)) },
            text = { Text(stringResource(R.string.family_remove_body, name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch { runCatching { Family.unlink(link) } }
                }) {
                    Text(stringResource(R.string.family_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Asks for notification permission once linked; alerts are useless without it. */
@Composable
private fun NotificationPrompt() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { enabled = it }

    LaunchedEffect(Unit) {
        if (!enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if (enabled) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.family_notify_off), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchHeight),
            ) {
                Text(stringResource(R.string.family_notify_on))
            }
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
fun ErrorText(@StringRes message: Int) {
    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@StringRes
fun errorRes(e: Throwable): Int = when ((e as? ApiException)?.error) {
    "bad_code" -> R.string.error_bad_code
    "expired" -> R.string.error_expired
    "too_many_guardians" -> R.string.error_too_many_guardians
    "already_linked" -> R.string.error_already_linked
    "too_many_tries" -> R.string.error_too_many_tries
    "network" -> R.string.error_network
    else -> if (e is java.io.IOException || e is com.google.firebase.FirebaseNetworkException) {
        R.string.error_network
    } else {
        R.string.error_generic
    }
}
