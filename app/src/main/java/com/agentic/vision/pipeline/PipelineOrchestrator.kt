package com.agentic.vision.pipeline

import android.content.Context
import android.graphics.Bitmap
import com.agentic.vision.camera.Frame
import com.agentic.vision.fusion.SceneFusion
import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.navigation.FreeSpaceAnalyzer
import com.agentic.vision.navigation.PathAdvice
import com.agentic.vision.perception.MidasDepthEstimator
import com.agentic.vision.perception.YoloDetector
import com.agentic.vision.reasoning.FallbackLlmEngine
import com.agentic.vision.reasoning.GoalGrounder
import com.agentic.vision.reasoning.GroundedGoal
import com.agentic.vision.reasoning.InstructionArbiter
import com.agentic.vision.reasoning.LlmEngine
import com.agentic.vision.reasoning.Phi3LlmEngine
import com.agentic.vision.reasoning.PromptBuilder
import com.agentic.vision.reasoning.SafetyLayer
import com.agentic.vision.speech.SpeechUtterance
import com.agentic.vision.speech.TtsManager
import com.agentic.vision.util.Metrics
import com.agentic.vision.util.TelemetryRecorder
import com.agentic.vision.util.ThermalMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PipelineState(
    val isRunning: Boolean = false,
    val currentGoal: String = "walk forward safely",
    val groundedGoal: GroundedGoal? = null,
    val sceneObjects: List<SceneObject> = emptyList(),
    val pathAdvice: PathAdvice? = null,
    val lastUtterance: SpeechUtterance? = null,
    val fps: Float = 0.0f,
    val lastFrameTimeMs: Long = 0L,
    val isDebugOverlayEnabled: Boolean = false
)

/**
 * Core Pipeline Orchestrator executing the complete Agentic Perception Pipeline:
 * Frame -> YOLOv10 (parallel) MiDaS -> Scene Fusion -> SORT/Kalman -> FreeSpace -> Tier 1 Safety -> Tier 2 LLM -> Arbiter -> TTS.
 */
class PipelineOrchestrator(
    private val context: Context? = null
) {
    private val _pipelineState = MutableStateFlow(PipelineState())
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    private val yoloDetector = YoloDetector(context)
    private val depthEstimator = MidasDepthEstimator(context)
    private val sceneFusion = SceneFusion()
    private val freeSpaceAnalyzer = FreeSpaceAnalyzer()
    private val safetyLayer = SafetyLayer()
    private val goalGrounder = GoalGrounder()
    private val promptBuilder = PromptBuilder()
    private val arbiter = InstructionArbiter()
    private val ttsManager = TtsManager(context)

    var llmEngine: LlmEngine = FallbackLlmEngine()
    val thermalMonitor = ThermalMonitor(context)
    val metrics = Metrics()
    val telemetryRecorder = TelemetryRecorder()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var lastLlmRunTimeMs = 0L
    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    fun setGoal(rawGoalText: String) {
        val grounded = goalGrounder.groundGoal(rawGoalText)
        _pipelineState.value = _pipelineState.value.copy(
            currentGoal = rawGoalText,
            groundedGoal = grounded
        )
        ttsManager.speak(
            SpeechUtterance(
                priority = com.agentic.vision.speech.SpeechPriority.INFO,
                text = grounded.guidanceMessage
            )
        )
    }

    fun processFrame(frame: Frame) {
        val startTime = System.currentTimeMillis()
        val goal = _pipelineState.value.groundedGoal ?: goalGrounder.groundGoal(_pipelineState.value.currentGoal)

        // 1. Parallel Coroutine Execution: YOLOv10 Detection & MiDaS Depth
        val yoloDeferred = scope.async { yoloDetector.detect(frame.bitmap) }
        val depthDeferred = scope.async { depthEstimator.estimateDepth(frame.bitmap) }

        scope.launch {
            val yoloStartTime = System.currentTimeMillis()
            val detections = yoloDeferred.await()
            val yoloMs = System.currentTimeMillis() - yoloStartTime

            val depthStartTime = System.currentTimeMillis()
            val depthMap = depthDeferred.await()
            val depthMs = System.currentTimeMillis() - depthStartTime

            // 2. Scene Fusion (Metric 3D objects, bearings, SORT tracking, Kalman predictions)
            val fusionStartTime = System.currentTimeMillis()
            val sceneObjects = sceneFusion.fuseScene(frame, detections, depthMap)
            val fusionMs = System.currentTimeMillis() - fusionStartTime

            // 3. Free-space clearance & walkable path analysis
            val pathAdvice = freeSpaceAnalyzer.analyze(depthMap, sceneObjects)

            // 4. Tier 1 Rule-Based Safety Check (< 100ms)
            val safetyAlert = safetyLayer.evaluate(sceneObjects, pathAdvice)

            // 5. Tier 2 Agentic LLM Reasoning (Async every 1.5s or on CRITICAL events)
            var llmMs = 0L
            val currentTime = System.currentTimeMillis()
            var llmResponse: com.agentic.vision.reasoning.LlmResponse? = null

            if (currentTime - lastLlmRunTimeMs > 1500L || safetyAlert.priority == com.agentic.vision.reasoning.SafetyPriority.CRITICAL) {
                val llmStart = System.currentTimeMillis()
                val systemPrompt = promptBuilder.buildSystemPrompt()
                val userPrompt = promptBuilder.buildSceneUserPrompt(goal, sceneObjects, pathAdvice, _pipelineState.value.lastUtterance?.text)
                llmResponse = llmEngine.generateInstruction(systemPrompt, userPrompt)
                llmMs = System.currentTimeMillis() - llmStart
                lastLlmRunTimeMs = currentTime
            }

            // 6. Instruction Arbiter & Audio Dispatch
            finalUtteranceDispatch(safetyAlert, llmResponse, pathAdvice)

            // 7. FPS & Latency Metrics Tracking
            val totalMs = System.currentTimeMillis() - startTime
            metrics.recordFrame(yoloMs, depthMs, fusionMs, llmMs, totalMs)

            frameCount++
            if (currentTime - lastFpsTimestamp >= 1000L) {
                val fps = (frameCount * 1000.0f) / (currentTime - lastFpsTimestamp)
                frameCount = 0
                lastFpsTimestamp = currentTime

                _pipelineState.value = _pipelineState.value.copy(
                    fps = fps,
                    sceneObjects = sceneObjects,
                    pathAdvice = pathAdvice,
                    lastFrameTimeMs = currentTime
                )
            }
        }
    }

    private fun finalUtteranceDispatch(
        safetyAlert: com.agentic.vision.reasoning.SafetyAlert,
        llmResponse: com.agentic.vision.reasoning.LlmResponse?,
        pathAdvice: PathAdvice
    ) {
        val utterance = arbiter.arbitrate(safetyAlert, llmResponse, pathAdvice)
        if (utterance.text.isNotEmpty()) {
            ttsManager.speak(utterance)
            _pipelineState.value = _pipelineState.value.copy(lastUtterance = utterance)
        }
    }

    fun repeatLastInstruction() {
        _pipelineState.value.lastUtterance?.let {
            ttsManager.speak(it.copy(priority = com.agentic.vision.speech.SpeechPriority.CRITICAL))
        }
    }

    fun toggleDebugOverlay() {
        _pipelineState.value = _pipelineState.value.copy(
            isDebugOverlayEnabled = !_pipelineState.value.isDebugOverlayEnabled
        )
    }

    fun stop() {
        yoloDetector.close()
        depthEstimator.close()
        llmEngine.close()
        ttsManager.shutdown()
    }
}
