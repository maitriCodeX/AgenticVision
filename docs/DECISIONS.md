# Architectural & Implementation Decisions

## 1. TensorRT vs ONNX Runtime + NNAPI / XNNPACK
- **Context**: The abstract mentions TensorRT for edge deployment.
- **Constraint**: TensorRT is exclusive to NVIDIA hardware (Jetson / Desktop GPUs) and is unavailable on Android devices.
- **Decision**: Android deployment utilizes ONNX Runtime Mobile with NNAPI and XNNPACK execution providers, fallback to CPU FP16/INT8. Export scripts in `tools/` maintain standard ONNX format compatible with TensorRT for Jetson edge nodes.

## 2. Pinhole Anchor Calibration for Relative Neural Depth
- **Context**: MiDaS models output relative inverse depth, not absolute meters.
- **Decision**: Hybrid metric scaling combining known physical object priors (person 1.65m, car 1.5m, door 2.0m, chair 0.9m) with pinhole geometric projections ($Z = f \cdot H / h$). A Huber regression fits frame-level scale/shift, smoothed via an exponential moving average. Fallbacks include manual user calibration and approximate scale memory.

## 3. Two-Tiered Reasoning Architecture
- **Context**: On-device LLMs (Phi-3 Mini 4-bit) require 1-2 seconds per prompt step on mobile processors, which is unacceptable for immediate collision avoidance.
- **Decision**: Tier 1 Safety Layer runs deterministic rule-based checks (<100ms) to trigger immediate warnings ("Stop", "Obstacle ahead"). Tier 2 LLM runs asynchronously to provide goal-aware navigation advice. The Instruction Arbiter prioritizes Tier 1 over Tier 2 and suppresses spatially unsafe LLM suggestions.

## 4. Ground Plane 2D Kalman Filter & Motion Tracking
- **Context**: 3D motion tracking on mobile requires stable coordinate framing.
- **Decision**: Objects are projected onto a 2D ground plane $(x, z)$. The Kalman Filter maintains state $[x, z, v_x, v_z]^T$ with measurement covariance scaling quadratically with estimated distance to model depth noise.

## 5. Camera Source Abstraction for Neckband Scalability
- **Context**: Prototype runs on phone camera, but ultimate target is a 4-camera neckband array (120° FOV each).
- **Decision**: `CameraSource` interface provides yaw offsets (`0°`, `90°`, `180°`, `270°`). `PhoneCameraSource` wraps the rear phone camera (yaw `0°`), while `MultiCameraFusion` models four-camera spatial aggregation.
