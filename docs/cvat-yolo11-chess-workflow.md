# CVAT + YOLO11 Chess Puzzle Workflow

1. Collect chess puzzle images and PDF-rendered pages.
2. Create a CVAT project with these labels:
   - white_king
   - white_queen
   - white_rook
   - white_bishop
   - white_knight
   - white_pawn
   - black_king
   - black_queen
   - black_rook
   - black_bishop
   - black_knight
   - black_pawn
3. Draw tight bounding boxes around every visible piece.
4. Export from CVAT as `Ultralytics YOLO Detection`.
5. Train:

```bash
pip install -r tools/vision/requirements.txt
yolo detect train model=yolo11n.pt data=data.yaml epochs=100 imgsz=1024
```

6. Copy the trained model to:

```text
models/chess-yolo11/best.pt
```

7. Use the coach upload form. The backend calls `tools/vision/chess_vision_to_fen.py`, converts detections into FEN, and saves the result as a normal puzzle.
