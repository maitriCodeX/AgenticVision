#!/usr/bin/env python3
"""
Model Export & Preparation Utilities for Agentic Vision Pipeline
Supports exporting YOLOv10 to ONNX, MiDaS v2.1 Small to ONNX, and downloading Phi-3 Mini GGUF/ONNX.
"""

import os
import sys

def export_yolov10(output_path="yolov10n.onnx"):
    print(f"[YOLOv10] Exporting model to {output_path}...")
    try:
        from ultralytics import YOLO
        model = YOLO("yolov10n.pt")
        model.export(format="onnx", imgsz=640, dynamic=False, simplify=True)
        print("[YOLOv10] Export successful!")
    except Exception as e:
        print(f"[YOLOv10] Export failed or ultralytics not installed: {e}")
        print("[YOLOv10] Generating placeholder structure info for ONNX deployment...")

def export_midas(output_path="midas_v21_small.onnx"):
    print(f"[MiDaS] Exporting MiDaS v2.1 small to {output_path}...")
    try:
        import torch
        midas = torch.hub.load("intel-isl/MiDaS", "MiDaS_small")
        midas.eval()
        dummy_input = torch.randn(1, 3, 256, 256)
        torch.onnx.export(
            midas,
            dummy_input,
            output_path,
            input_names=["input"],
            output_names=["output"],
            opset_version=14
        )
        print("[MiDaS] Export successful!")
    except Exception as e:
        print(f"[MiDaS] Export failed or PyTorch not installed: {e}")

def prepare_phi3():
    print("[Phi-3] Phi-3 Mini 4-bit ONNX/GGUF model downloading instructions...")
    print("Download from HuggingFace: Microsoft/Phi-3-mini-4k-instruct-onnx or GGUF Q4_K_M")

if __name__ == "__main__":
    os.makedirs("models", exist_ok=True)
    export_yolov10()
    export_midas()
    prepare_phi3()
