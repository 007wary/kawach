package dev.wary.kawach.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.wary.kawach.R
import dev.wary.kawach.family.Family
import dev.wary.kawach.family.FamilyLink
import dev.wary.kawach.family.GuardianReply
import dev.wary.kawach.family.Session
import dev.wary.kawach.rules.Flag
import dev.wary.kawach.rules.Risk
import dev.wary.kawach.rules.Verdict
import dev.wary.kawach.ui.theme.LocalStatusColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf

@Composable
fun ResultScreen(
    text: String,
    verdict: Verdict,
    fromPicture: Boolean,
    sendId: String,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val session by Family.session.collectAsState()
    val links by Family.links.collectAsState()

    val riskLabel = stringResource(verdict.risk.labelRes())
    val title = stringResource(verdict.titleRes())
    val shareText = stringResource(R.string.result_share_text, riskLabel, title, text)

    ScreenColumn {
        ScreenHeader(stringResource(R.string.result_title), onBack)

        RiskCard(verdict.risk, title, stringResource(verdict.adviceRes()))

        if (verdict.flags.isNotEmpty()) {
            Text(stringResource(R.string.result_why), style = MaterialTheme.typography.titleMedium)
            FlagChips(verdict.flags)
        }

        Text(stringResource(R.string.result_message), style = MaterialTheme.typography.titleMedium)
        MessageCard(text, verdict.highlights)
        if (fromPicture) {
            Text(
                text = stringResource(R.string.result_from_picture),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }

        if (session is Session.Elder && links.isNotEmpty()) {
            FamilySendCard(sendId, text, verdict, fromPicture, links, onShare = { shareWithFamily(context, shareText) })
        } else {
            Button(
                onClick = { shareWithFamily(context, shareText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchHeight),
            ) {
                Text(stringResource(R.string.result_share))
            }
        }
        if (verdict.risk != Risk.LOW) {
            Button(
                onClick = onHelp,
                colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchHeight),
            ) {
                Text(stringResource(R.string.result_paid))
            }
        }
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchHeight),
        ) {
            Text(stringResource(R.string.result_again))
        }
    }
}

/** The elder's view of the family alert: sending, sent, then the guardian's reply as it arrives. */
@Composable
private fun FamilySendCard(
    sendId: String,
    text: String,
    verdict: Verdict,
    fromPicture: Boolean,
    links: List<FamilyLink>,
    onShare: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var checkId by remember(sendId) { mutableStateOf<String?>(null) }
    var failed by remember(sendId) { mutableStateOf(false) }

    LaunchedEffect(sendId, attempt) {
        failed = false
        try {
            checkId = Family.sendCheck(sendId, text, verdict, fromPicture)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        }
    }
    val check by remember(checkId) { checkId?.let(Family::check) ?: flowOf(null) }.collectAsState(initial = null)
    val reply = check?.reply

    if (reply != null) {
        ReplyCard(reply)
        return
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                failed -> {
                    Text(stringResource(R.string.result_family_failed), style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = { attempt++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = TouchHeight),
                    ) {
                        Text(stringResource(R.string.result_family_retry))
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = TouchHeight),
                    ) {
                        Text(stringResource(R.string.result_share))
                    }
                }
                checkId == null -> Text(stringResource(R.string.result_family_sending), style = MaterialTheme.typography.bodyLarge)
                else -> Text(
                    text = stringResource(R.string.result_family_sent, links.joinToString { it.guardianName }),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun RiskCard(risk: Risk, title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    val (container, accent) = when (risk) {
        Risk.HIGH -> scheme.errorContainer to scheme.error
        Risk.MEDIUM -> status.warnContainer to status.warn
        Risk.LOW -> status.okContainer to status.ok
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(1.5.dp, accent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(risk.labelRes()), style = MaterialTheme.typography.labelLarge, color = accent)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (risk == Risk.HIGH) accent else scheme.onSurface,
            )
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** A guardian's "safe" or "scam" answer, in the same colours as the risk card. */
@Composable
fun ReplyCard(reply: GuardianReply) {
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    val (container, accent) = if (reply.safe) status.okContainer to status.ok else scheme.errorContainer to scheme.error
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(1.5.dp, accent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.result_family_reply, reply.guardianName),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
            Text(
                text = stringResource(if (reply.safe) R.string.reply_safe_elder else R.string.reply_scam_elder),
                style = MaterialTheme.typography.headlineSmall,
                color = if (reply.safe) scheme.onSurface else accent,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlagChips(flags: List<Flag>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        flags.forEach { flag ->
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = stringResource(flag.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun MessageCard(text: String, highlights: List<IntRange>) {
    val status = LocalStatusColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = highlighted(text, highlights, status.warnContainer, status.warn),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(14.dp),
        )
    }
}

private fun highlighted(text: String, highlights: List<IntRange>, background: Color, foreground: Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val style = SpanStyle(background = background, color = foreground, fontWeight = FontWeight.SemiBold)
        highlights
            .filter { it.first >= 0 && it.last < text.length }
            .forEach { addStyle(style, it.first, it.last + 1) }
    }
