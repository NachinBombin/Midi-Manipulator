package com.nachinbombin.midimanipulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nachinbombin.midimanipulator.midi.MidiManager
import com.nachinbombin.midimanipulator.navigation.AppNavHost
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.theme.AppTheme
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private lateinit var midiManager: MidiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        midiManager = MidiManager(this)
        midiManager.initialize()

        setContent {
            val themeManager = remember { ThemeManager(this) }
            val currentTheme by themeManager.currentTheme.collectAsState()
            val appViewModel: AppViewModel = viewModel()

            AppTheme(preset = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = currentTheme.bg
                ) {
                    AppNavHost(
                        midiManager = midiManager,
                        themeManager = themeManager,
                        appViewModel = appViewModel
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        midiManager.release()
    }
}
