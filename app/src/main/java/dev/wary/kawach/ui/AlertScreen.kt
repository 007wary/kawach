package dev.wary.kawach.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.wary.kawach.R
import dev.wary.kawach.family.ElderPhones
import dev.wary.kawach.family.Family
import dev.wary.kawach.family.FamilyCheck
import dev.wary.kawach.family.Session
import dev.wary.kawach.rules.ScamRules
import dev.wary.kawach.rules.Verdict
import dev.wary.kawach.ui.theme.LocalStatusColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Wraps the check so "still loading" (null) differs from "deleted or not ours" (check = null). */
private data class Loaded(val check: FamilyCheck?)

/**
 * One check, as a family alert. The guardian sees the message and replies "safe" or "scam";
 * the elder sees the same check with the reply when it comes.
 */
@Composable
fun AlertScreen(checkId: String, onBack: () -> Unit) {
    val session by Family.session.collectAsState()
    val loaded by remember(checkId) { Family.check(checkId).map(::Loaded) }.collectAsState(initial = null)

    ScreenColumn {
        ScreenHeader(stringResource(R.string.alert_title), onBack)
        val state = loaded ?: run {
            Text(stringResource(R.string.family_working), style = MaterialTheme.typography.bodyLarge)
            return@ScreenColumn
        }
        val check = state.check ?: run {
            Text(stringResource(R.string.alert_missing), style = MaterialTheme.typography.bodyLarge)
            return@ScreenColumn
        }
        val verdict = Verdict(check.risk, check.scamType, check.flags, 0, emptyList())

        Text(
            text = stringResource(R.string.alert_checked, check.elderName),
            style = MaterialTheme.typography.titleMedium,
        )
        check.createdAt?.let {
            Text(
                text = DateUtils.getRelativeTimeSpanString(it.time).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RiskCard(check.risk, stringResource(verdict.titleRes()), stringResource(verdict.adviceRes()))
        if (check.flags.isNotEmpty()) FlagChips(check.flags)

        Text(stringResource(R.string.result_message), style = MaterialTheme.typography.titleMedium)
        val highlights = remember(check.maskedText) { ScamRules.check(check.maskedText).highlights }
        MessageCard(check.maskedText, highlights)
        Text(
            text = stringResource(R.string.alert_masked),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        check.reply?.let { ReplyCard(it) }
        if (session is Session.Guardian) {
            GuardianActions(check)
        } else if (check.reply == null) {
            Text(stringResource(R.string.alert_no_reply), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun GuardianActions(check: FamilyCheck) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }

    fun send(safe: Boolean) = scope.launch {
        busy = true
        error = null
        try {
            Family.reply(check.id, safe)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = errorRes(e)
        }
        busy = false
    }

    OutlinedButton(
        onClick = { dial(context, ElderPhones.get(context, check.elderId)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchHeight),
    ) {
        Icon(Icons.Filled.Phone, contentDescription = null)
        Text("  " + stringResource(R.string.alert_call, check.elderName))
    }

    Text(
        text = stringResource(R.string.alert_ask, check.elderName),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Button(
        enabled = !busy,
        onClick = { send(safe = false) },
        colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchHeight),
    ) {
        Text(stringResource(R.string.alert_reply_scam))
    }
    Button(
        enabled = !busy,
        onClick = { send(safe = true) },
        colors = ButtonDefaults.buttonColors(containerColor = status.ok, contentColor = scheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchHeight),
    ) {
        Text(stringResource(R.string.alert_reply_safe))
    }
    error?.let { ErrorText(it) }
}
