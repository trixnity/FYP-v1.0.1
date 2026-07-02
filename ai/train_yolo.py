import argparse
from pathlib import Path

from ultralytics import YOLO


BASE_DIR = Path(__file__).resolve().parent
DATA_YAML = BASE_DIR / "dataset" / "data.yaml"
MODELS_DIR = BASE_DIR / "models"


def main():
    parser = argparse.ArgumentParser(description="Train YOLO for EduChess chess piece recognition.")
    parser.add_argument("--epochs", type=int, default=50)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--model", default="yolo11n.pt")
    args = parser.parse_args()

    if not DATA_YAML.exists():
        raise FileNotFoundError(f"Dataset config not found: {DATA_YAML}")

    model = YOLO(args.model)
    results = model.train(
        data=str(DATA_YAML),
        epochs=args.epochs,
        imgsz=args.imgsz,
        project=str(BASE_DIR / "runs"),
        name="chess_pieces",
        exist_ok=True
    )

    trained_best = Path(results.save_dir) / "weights" / "best.pt"
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    target = MODELS_DIR / "best.pt"
    target.write_bytes(trained_best.read_bytes())
    print(f"Saved trained model to {target}")


if __name__ == "__main__":
    main()
