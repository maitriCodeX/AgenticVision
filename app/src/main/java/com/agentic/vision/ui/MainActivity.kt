package com.agentic.vision.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.agentic.vision.camera.PhoneCameraSource
import com.agentic.vision.pipeline.PipelineOrchestrator
import com.agentic.vision.pipeline.PipelineService
import com.agentic.vision.reasoning.FallbackLlmEngine
import com.agentic.vision.reasoning.Phi3LlmEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen {
    MAIN,
    SETTINGS,
    CALIBRATION,
    BENCHMARK,
    REPLAY
}

class MainActivity : ComponentActivity() {

    private lateinit var orchestrator: PipelineOrchestrator
    private var phoneCameraSource: PhoneCameraSource? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            initCameraAndPipeline()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen awake while navigation assistant is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        orchestrator = PipelineOrchestrator(this)

        checkAndRequestPermissions()

        setContent {
            var currentScreen by remember { mutableStateOf(Screen.MAIN) }
            val pipelineState by orchestrator.pipelineState.collectAsState()
            var isHindi by remember { mutableStateOf(false) }
            var isFallbackLlm by remember { mutableStateOf(true) }

            when (currentScreen) {
                Screen.MAIN -> MainScreen(
                    state = pipelineState,
                    onStartStopClick = { togglePipelineRunning() },
                    onGoalSubmit = { orchestrator.setGoal(it) },
                    onSettingsClick = { currentScreen = Screen.SETTINGS },
                    onBenchmarkClick = { currentScreen = Screen.BENCHMARK },
                    onCalibrationClick = { currentScreen = Screen.CALIBRATION }
                )
                Screen.SETTINGS -> SettingsScreen(
                    onBackClick = { currentScreen = Screen.MAIN },
                    isDebugOverlay = pipelineState.isDebugOverlayEnabled,
                    onToggleDebugOverlay = { orchestrator.toggleDebugOverlay() },
                    isHindi = isHindi,
                    onToggleHindi = { isHindi = !isHindi },
                    isFallbackLlm = isFallbackLlm,
                    onToggleFallbackLlm = {
                        isFallbackLlm = !isFallbackLlm
                        orchestrator.llmEngine = if (isFallbackLlm) FallbackLlmEngine() else Phi3LlmEngine(this)
                    }
                )
                Screen.CALIBRATION -> CalibrationScreen(
                    onBackClick = { currentScreen = Screen.MAIN }
                )
                Screen.BENCHMARK -> BenchmarkScreen(
                    metrics = orchestrator.metrics,
                    thermalStateName = orchestrator.thermalMonitor.checkThermalStatus().name,
                    onBackClick = { currentScreen = Screen.MAIN }
                )
                Screen.REPLAY -> ReplayScreen(
                    onBackClick = { currentScreen = Screen.MAIN }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            initCameraAndPipeline()
        }
    }

    private fun initCameraAndPipeline() {
        phoneCameraSource = PhoneCameraSource(this, this)
        phoneCameraSource?.start()

        // Subscribe to frame stream via lifecycleScope
        phoneCameraSource?.let { source ->
            lifecycleScope.launch {
                source.frames.collectLatest { frame ->
                    orchestrator.processFrame(frame)
                }
            }
        }
    }

    private fun togglePipelineRunning() {
        val isRunning = orchestrator.pipelineState.value.isRunning
        if (!isRunning) {
            PipelineService.startService(this)
        } else {
            PipelineService.stopService(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                orchestrator.repeatLastInstruction()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                togglePipelineRunning()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneCameraSource?.stop()
        orchestrator.stop()
    }
}
