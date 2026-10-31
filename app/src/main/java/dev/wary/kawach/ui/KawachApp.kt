package dev.wary.kawach.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.wary.kawach.R
import dev.wary.kawach.rules.ScamRules
import dev.wary.kawach.rules.Verdict
import java.util.UUID

/** Something another app or the widget asked Kawach to do. */
sealed interface Incoming {
    data class Shared(val text: String) : Incoming
    data class SharedImage(val uri: Uri, val caption: String?) : Incoming
    data object OpenCheck : Incoming
    data class OpenAlert(val checkId: String) : Incoming
}

sealed interface Screen {
    data object Home : Screen
    data object Learn : Screen
    data object Help : Screen
    data object Check : Screen
    data object Family : Screen
    data class Alert(val checkId: String) : Screen
    data class Reading(val uri: Uri, val caption: String? = null) : Screen

    /**
     * [sendId] identifies this check to the family API, so a repeat send doesn't alert twice.
     * [fromPicture] means [text] was read from a screenshot, so words may be missing.
     */
    data class Result(
        val text: String,
        val verdict: Verdict,
        val fromPicture: Boolean = false,
        val sendId: String = UUID.randomUUID().toString(),
    ) : Screen
}

private data class Tab(val screen: Screen, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, R.string.tab_home, Icons.Filled.Home),
    Tab(Screen.Learn, R.string.tab_learn, Icons.Filled.Info),
    Tab(Screen.Help, R.string.tab_help, Icons.Filled.Phone),
)

@Composable
fun KawachApp(incoming: Incoming?, onIncomingHandled: () -> Unit) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = stack.last()

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun reset(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }

    fun checkText(text: String, fromPicture: Boolean = false) =
        push(Screen.Result(text, ScamRules.check(text), fromPicture))

    LaunchedEffect(incoming) {
        when (incoming) {
            is Incoming.Shared -> {
                reset(Screen.Home)
                checkText(incoming.text)
            }
            is Incoming.SharedImage -> {
                reset(Screen.Home)
                push(Screen.Reading(incoming.uri, incoming.caption))
            }
            Incoming.OpenCheck -> {
                reset(Screen.Home)
                push(Screen.Check)
            }
            is Incoming.OpenAlert -> {
                reset(Screen.Home)
                push(Screen.Alert(incoming.checkId))
            }
            null -> return@LaunchedEffect
        }
        onIncomingHandled()
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val onTabRoot = tabs.any { it.screen == current }

    Scaffold(
        bottomBar = {
            if (onTabRoot) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.screen,
                            onClick = { reset(tab.screen) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            when (current) {
                Screen.Home -> HomeScreen(
                    onCheck = { push(Screen.Check) },
                    onCheckText = ::checkText,
                    onHelp = { reset(Screen.Help) },
                    onLearn = { reset(Screen.Learn) },
                    onFamily = { push(Screen.Family) },
                )
                Screen.Learn -> LearnScreen()
                Screen.Help -> HelpScreen()
                Screen.Check -> CheckScreen(
                    onBack = ::pop,
                    onSubmit = { text ->
                        pop()
                        checkText(text)
                    },
                    onPicture = { push(Screen.Reading(it)) },
                )
                is Screen.Result -> ResultScreen(
                    text = current.text,
                    verdict = current.verdict,
                    fromPicture = current.fromPicture,
                    sendId = current.sendId,
                    onBack = ::pop,
                    onHelp = { reset(Screen.Help) },
                )
                Screen.Family -> FamilyScreen(
                    onBack = ::pop,
                    onOpenAlert = { push(Screen.Alert(it)) },
                )
                is Screen.Alert -> AlertScreen(checkId = current.checkId, onBack = ::pop)
                is Screen.Reading -> ReadingScreen(
                    uri = current.uri,
                    caption = current.caption,
                    onBack = ::pop,
                    onRead = { text ->
                        // Back from the result goes to where the picture came from, not to the spinner.
                        pop()
                        if (stack.last() == Screen.Check) pop()
                        checkText(text, fromPicture = true)
                    },
                )
            }
        }
    }
}
