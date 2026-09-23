package com.example.catslist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication

/**
 * `MainActivity`, minus the parts that belong to `:app`: it sets `CatsApp()` as its content in
 * `onCreate`, so recreating it — what a rotation does — rebuilds the UI the way the app does.
 * Its Koin is the test's, handed over through [koin] because an Activity cannot be constructed
 * with arguments.
 */
class CatsAppTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KoinIsolatedContext(context = checkNotNull(koin)) { CatsApp() } }
    }

    companion object {
        var koin: KoinApplication? = null
    }
}
