package dev.wary.kawach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.wary.kawach.R

private const val CYBER_HELPLINE = "1930"
private const val EMERGENCY = "112"
private const val CYBER_PORTAL = "https://cybercrime.gov.in"

private val steps = listOf(
    R.string.help_step_1,
    R.string.help_step_2,
    R.string.help_step_3,
    R.string.help_step_4,
    R.string.help_step_5,
)

@Composable
fun HelpScreen() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var done by rememberSaveable { mutableStateOf(listOf<Int>()) }

    ScreenColumn {
        ScreenHeader(stringResource(R.string.help_title))
        Text(stringResource(R.string.help_golden), style = MaterialTheme.typography.bodyLarge)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = scheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.help_call_label), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = CYBER_HELPLINE,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.error,
                )
                Button(
                    onClick = { dial(context, CYBER_HELPLINE) },
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TouchHeight),
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.help_call))
                }
            }
        }

        steps.forEachIndexed { index, step ->
            val checked = index in done
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchHeight)
                    .clickable { done = if (checked) done - index else done + index },
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(step), style = MaterialTheme.typography.bodyLarge)
            }
        }

        OutlinedButton(
            onClick = { openUrl(context, CYBER_PORTAL) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchHeight),
        ) {
            Text(stringResource(R.string.help_portal))
        }
        Text(
            text = stringResource(R.string.help_112),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dial(context, EMERGENCY) }
                .padding(vertical = 12.dp),
        )
    }
}
