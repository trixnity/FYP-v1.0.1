import argparse
import json
from pathlib import Path

import cv2

from fen_generator import detections_to_fen


BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "models" / "best.pt"
OUTPUT_DIR = BASE_DIR / "output"


def model_missing():
    if not MODEL_PATH.exists():
        return True
    try:
        return MODEL_PATH.read_text(errors="ignore").startswith("YOLO model placeholder")
    except UnicodeDecodeError:
        return False


def recognize_puzzle(image_path, side_to_move="w"):
    image_path = Path(image_path)
    if model_missing():
        return {
            "success": False,
            "fen": None,
            "confidence": 0,
            "detectedPieces": [],
            "boardImagePath": None,
            "warnings": ["YOLO model not found. Please train model and place best.pt in ai/models."],
            "message": "YOLO model not found. Please train model and place best.pt in ai/models."
        }
    if not image_path.exists():
        return {
            "success": False,
            "fen": None,
            "confidence": 0,
            "detectedPieces": [],
            "boardImagePath": None,
            "warnings": [f"Image not found: {image_path}"],
            "message": "Image not found."
        }

    from ultralytics import YOLO

    model = YOLO(str(MODEL_PATH))
    results = model.predict(str(image_path), imgsz=640, conf=0.25, verbose=False)
    image = cv2.imread(str(image_path))
    height, width = image.shape[:2] if image is not None else (None, None)
    detections = []
    confidences = []

    for result in results:
        names = result.names
        for box in result.boxes:
            class_id = int(box.cls[0])
            confidence = float(box.conf[0])
            xyxy = [float(v) for v in box.xyxy[0].tolist()]
            class_name = names.get(class_id, str(class_id))
            detections.append({
                "className": class_name,
                "confidence": round(confidence, 4),
                "bbox": [round(v, 2) for v in xyxy]
            })
            confidences.append(confidence)

    warnings = []
    try:
        fen, fen_warnings = detections_to_fen(detections, width, height, side_to_move)
        warnings.extend(fen_warnings)
    except ValueError as exc:
        fen = None
        warnings.append(str(exc))

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    board_image_path = OUTPUT_DIR / f"{image_path.stem}_detected.jpg"
    if results:
        annotated = results[0].plot()
        cv2.imwrite(str(board_image_path), annotated)

    confidence = sum(confidences) / len(confidences) if confidences else 0
    success = fen is not None
    return {
        "success": success,
        "fen": fen,
        "confidence": round(confidence, 4),
        "detectedPieces": detections,
        "boardImagePath": str(board_image_path) if board_image_path.exists() else None,
        "warnings": warnings,
        "message": "Puzzle recognized successfully" if success else "Puzzle recognition needs manual review."
    }


def main():
    parser = argparse.ArgumentParser(description="Recognize a chess puzzle image with YOLO and output JSON.")
    parser.add_argument("image_path")
    parser.add_argument("--side-to-move", default="w", choices=["w", "b"])
    args = parser.parse_args()
    print(json.dumps(recognize_puzzle(args.image_path, args.side_to_move), indent=2))


if __name__ == "__main__":
    main()
