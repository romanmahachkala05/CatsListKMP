package com.example.catslist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.navigation.CatsNavDisplay
import com.example.catslist.presentation.theme.CatsListTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val snackbarNotifier: SnackbarNotifier by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatsListTheme {
                CatsNavDisplay(notifier = snackbarNotifier)
            }
        }
    }
}
