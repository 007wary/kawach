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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import dev.wary.kawach.rules.Risk
import dev.wary.kawach.rules.Verdict
import dev.wary.kawach.ui.theme.LocalStatusColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(text: String, verdict: Verdict, onBack: () -> Unit, onHelp: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusColors.current

    val (container, accent) = when (verdict.risk) {
        Risk.HIGH -> scheme.errorContainer to scheme.error
        Risk.MEDIUM -> status.warnContainer to status.warn
        Risk.LOW -> status.okContainer to status.ok
    }
    val riskLabel = stringResource(verdict.risk.labelRes())
    val title = stringResource(verdict.titleRes())
    val shareText = stringResource(R.string.result_share_text, riskLabel, title, text)

    ScreenColumn {
        ScreenHeader(stringResource(R.string.result_title), onBack)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = container,
            border = BorderStroke(1.5.dp, accent),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(riskLabel, style = MaterialTheme.typography.labelLarge, color = accent)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (verdict.risk == Risk.HIGH) accent else scheme.onSurface,
                )
                Text(stringResource(verdict.adviceRes()), style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (verdict.flags.isNotEmpty()) {
            Text(stringResource(R.string.result_why), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                verdict.flags.forEach { flag ->
                    Surface(shape = RoundedCornerShape(50), color = scheme.surfaceVariant) {
                        Text(
                            text = stringResource(flag.labelRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        Text(stringResource(R.string.result_message), style = MaterialTheme.typography.titleMedium)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = highlighted(text, verdict, status.warnContainer, status.warn),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(14.dp),
            )
        }

        Button(
            onClick = { shareWithFamily(context, shareText) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchHeight),
        ) {
            Text(stringResource(R.string.result_share))
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

private fun highlighted(text: String, verdict: Verdict, background: Color, foreground: Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val style = SpanStyle(background = background, color = foreground, fontWeight = FontWeight.SemiBold)
        verdict.highlights
            .filter { it.first >= 0 && it.last < text.length }
            .forEach { addStyle(style, it.first, it.last + 1) }
    }
