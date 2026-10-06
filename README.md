# Agentic Navigation Assistant for the Visually Impaired (Android Prototype)

An edge-AI spatial perception, reasoning, and speech-guidance system built for Android. This prototype runs real-time object detection (YOLOv10), neural depth fusion (MiDaS), 2D Kalman motion tracking, 5-sector free-space clearance analysis, a deterministic Safety Layer, and an agentic LLM navigation reasoner (Phi-3 Mini / Fallback Engine)—all completely offline on-device.

## Project Structure
```
app/
 ├── camera/       # CameraSource interface, PhoneCameraSource, Frame, FrameConverter
 ├── perception/   # YoloDetector (ONNX), DepthEstimator (MiDaS ONNX), MetricDepthCalibrator
 ├── fusion/       # SceneFusion, SceneObject, MultiCameraFusion stub
 ├── tracking/     # Tracker, KalmanFilter, TrajectoryPredictor, CollisionRisk
 ├── navigation/   # FreeSpaceAnalyzer, PathAdvice
 ├── reasoning/    # SafetyLayer, LlmEngine, Phi3LlmEngine, PromptBuilder, GoalGrounder, InstructionArbiter
 ├── speech/       # TtsManager, SpeechPrioritiser, GoalRecognizer
 ├── pipeline/     # PipelineOrchestrator, FrameScheduler, PipelineService
 ├── ui/           # Jetpack Compose UI (Main, Settings, Calibration, Benchmark, Replay)
 └── util/         # Logging, Metrics, ThermalMonitor
tools/             # Python export and validation scripts
docs/              # System architecture, architectural decisions, testing docs
```

## Quick Start
1. Ensure Android SDK 29+ and Java 21 are configured.
2. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run unit tests:
   ```bash
   ./gradlew test
   ```
4. Install on an Android device and grant Camera & Audio permissions.

## Offline Models Setup
Run model preparation scripts in `tools/` to export YOLOv10 and MiDaS ONNX models:
```bash
python tools/export_models.py
```
Models can also be downloaded on first launch via the in-app Model Manager.
