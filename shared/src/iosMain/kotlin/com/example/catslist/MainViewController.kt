package com.example.catslist

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** What the iOS app shows: [CatsApp] in a view controller Swift can host. Koin must be started. */
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { CatsApp() }
