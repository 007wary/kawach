package dev.wary.kawach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.wary.kawach.R
import dev.wary.kawach.rules.ScamType

@Composable
fun LearnScreen() {
    ScreenColumn {
        ScreenHeader(stringResource(R.string.learn_title))
        ScamType.entries.forEach { type ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(type.titleRes()), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(type.adviceRes()), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
