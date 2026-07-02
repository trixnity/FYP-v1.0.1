# EduChess YOLO Chess Puzzle Recognition

This folder prepares the Python side of the EduChess puzzle recognition pipeline. Spring Boot uploads a chess puzzle screenshot, calls the FastAPI service, saves the generated FEN as a `PENDING_REVIEW` puzzle, then lets a coach edit and publish it.

## Folder Layout

- `dataset/data.yaml` - replace this placeholder with the YOLO `data.yaml` exported from CVAT.
- `models/best.pt` - replace the placeholder with your trained model.
- `output/` - annotated prediction images are written here.
- `train_yolo.py` - trains YOLO and copies the trained `best.pt` to `ai/models/best.pt`.
- `predict_puzzle.py` - command-line recognizer that returns JSON.
- `fen_generator.py` - maps detected pieces to board squares and validates FEN with `python-chess`.
- `app.py` - FastAPI service used by Spring Boot.

## Install

From the `ai` folder:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

## Export Dataset From CVAT

1. In CVAT, label 12 classes exactly:
   `white_king`, `white_queen`, `white_rook`, `white_bishop`, `white_knight`, `white_pawn`,
   `black_king`, `black_queen`, `black_rook`, `black_bishop`, `black_knight`, `black_pawn`.
2. Export as YOLO format.
3. Place the exported folders under `ai/dataset/`.
4. Replace `ai/dataset/data.yaml` with the exported `data.yaml`, or edit the placeholder paths.

## Train

```bash
python train_yolo.py
```

Optional:

```bash
python train_yolo.py --epochs 100 --imgsz 640 --model yolo11n.pt
```

The final model is copied to:

```text
ai/models/best.pt
```

## Test Prediction

```bash
python predict_puzzle.py path\to\puzzle.png --side-to-move w
```

If the model is missing, the JSON response will contain:

```text
YOLO model not found. Please train model and place best.pt in ai/models.
```

## Run FastAPI

From the `ai` folder:

```bash
uvicorn app:app --reload --port 8000
```

Spring Boot calls:

```text
POST http://localhost:8000/recognize-puzzle
```

Configuration in `application.properties`:

```properties
puzzle.ai.base-url=http://localhost:8000
puzzle.recognition.storage-dir=uploads/puzzles
```

## Upload-to-Playable Flow

1. Start MySQL and Spring Boot.
2. Start the AI service with `uvicorn app:app --reload --port 8000`.
3. Log in as coach.
4. Open Dashboard > Puzzle Upload.
5. Upload a chess puzzle screenshot.
6. Review detected FEN, confidence, side to move, topic, difficulty, and solution moves.
7. Save review.
8. Publish.
9. Log in as student and open Puzzles. Published puzzles are available through the existing puzzle API.
