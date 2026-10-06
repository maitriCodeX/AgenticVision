# System Architecture: Agentic Vision Perception Pipeline

## High-Level Architecture

```
CameraSource (Phone / Multi-cam)
       │
       ▼
   Frame Stream (Flow<Frame>)
       │
       ├───► YoloDetector (YOLOv10 ONNX) ──────┐
       │                                         │
       └───► DepthEstimator (MiDaS v2.1 ONNX) ──┴─► Scene Fusion
                                                        │
                                                        ▼
                                              Multi-Object Tracker
                                              + 2D Kalman Filters
                                                        │
                                                        ▼
                                              FreeSpaceAnalyzer
                                                        │
                      ┌─────────────────────────────────┴─────────────────────────────────┐
                      │                                                                   │
                      ▼                                                                   ▼
           Tier 1: Safety Layer                                                  Tier 2: Agentic LLM
         (Rule-based, <100ms, deterministic)                                    (Phi-3 Mini, async 1-2s)
                      │                                                                   │
                      └─────────────────────────────────┬─────────────────────────────────┘
                                                        │
                                                        ▼
                                                Instruction Arbiter
                                                        │
                                                        ▼
                                              TTS Queue & Speech Engine
```

## Threading & Concurrency
- **Camera Capture**: CameraX execution thread feeding a `StateFlow` / `Flow` of `Frame` objects using `STRATEGY_KEEP_ONLY_LATEST`.
- **Perception Stage**: Parallel coroutine execution using `Dispatchers.Default` for YOLO detection and MiDaS depth estimation.
- **Fusion & Tracking**: Single-threaded pipeline update loop processing output synchronously per frame.
- **Reasoning Tier 1**: Immediate safety checks executed on frame completion (< 100ms).
- **Reasoning Tier 2**: Async background coroutine loop polling scene summary every 1.5 seconds without blocking frame stream.
- **Audio Output**: Dedicated speech priority queue dispatching to Android TextToSpeech engine.
