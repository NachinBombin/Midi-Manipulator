package com.nachinbombin.midimanipulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nachinbombin.midimanipulator.gamepad.GamepadDispatcher
import com.nachinbombin.midimanipulator.midi.MidiManager
import com.nachinbombin.midimanipulator.theme.AppTheme
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.ui.navigation.AppNavHost
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private lateinit var midiManager: MidiManager
    private lateinit var gamepadDispatcher: GamepadDispatcher
    private lateinit var appViewModel: AppViewModel

    private val bleLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled; MidiManager.refreshDevices() is called after init */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        midiManager = MidiManager(this)
        midiManager.initialize()

        requestBlePermissions()

        setContent {
            val themeManager = remember { ThemeManager(this) }
            val currentTheme by themeManager.currentTheme.collectAsState()
            val vm: AppViewModel = viewModel()
            appViewModel = vm

            // FIX: inject engine reference so ViewModel can call unlock/hardlock directly
            LaunchedEffect(Unit) { vm.engineRef = midiManager.engine }

            gamepadDispatcher = remember { GamepadDispatcher(vm, midiManager.engine) }

            AppTheme(preset = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = currentTheme.bg
                ) {
                    AppNavHost(
                        viewModel    = vm,
                        midiManager  = midiManager,
                        themeManager = themeManager
                    )
                }
            }
        }
    }

    private fun requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val needed = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) bleLauncher.launch(needed.toTypedArray())
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::gamepadDispatcher.isInitialized && gamepadDispatcher.onKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (::gamepadDispatcher.isInitialized && gamepadDispatcher.onMotionEvent(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        midiManager.release()
    }
}
