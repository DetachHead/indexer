package io.github.detachhead.indexer.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application { Window(onCloseRequest = ::exitApplication) {} }
