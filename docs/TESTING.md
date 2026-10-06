# Testing & Evaluation Guide

## 1. Unit Tests
Run unit tests via Gradle:
```bash
./gradlew test
```
Covered components:
- **Kalman Filter**: Constant velocity trajectory tracking, synthetic motion prediction (approaching, crossing, stationary).
- **SORT / ByteTrack Data Association**: Hungarian/Greedy matching on synthetic object boxes.
- **TTC / CPA Calculation**: Time-To-Collision and Closest Point of Approach calculations against 0.8m safety corridor.
- **Free-Space Analysis**: 5-sector clearance, hazard detection, path recommendation.
- **Instruction Arbiter**: Preemption of unsafe LLM actions by Safety Layer and Sector clearances.
- **Prompt & JSON Validation**: LLM response JSON parsing, fallback logic, goal grounding.
- **Multi-Camera Aggregation Stub**: Simulation of 4-camera source stream fusion into unified body coordinates.

## 2. Distance Evaluation Tool
The app includes a dedicated Calibration & Evaluation Screen (`CalibrationScreen.kt`):
- Place an object at known distances (e.g., 1.0m, 2.0m, 3.0m).
- Log estimated metric depth vs. ground truth.
- Computes Mean Absolute Error (MAE) and Mean Absolute Percentage Error (MAPE).

## 3. LLM Evaluation Harness
Run the offline LLM harness test (`LlmEvalHarnessTest.kt`):
- Evaluates 20+ scripted scene JSON scenarios with expected safety/navigation output sets.
- Reports pass rate and fallback invocation frequency.

## 4. Replay & Telemetry Mode
- Select recorded MP4 files in Replay Mode (`ReplayScreen.kt`).
- Export CSV/JSON frame telemetry logs containing bounding boxes, metric depth estimates, tracking vectors, safety alerts, and stage latencies.
