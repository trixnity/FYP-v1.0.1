import argparse
import json
import os
import sys

try:
    import cv2
    import numpy as np
    from ultralytics import YOLO
except Exception as exc:
    print(json.dumps({"error": f"Missing Python dependency: {exc}"}))
    sys.exit(2)


PIECE_TO_FEN = {
    "white_king": "K",
    "white_queen": "Q",
    "white_rook": "R",
    "white_bishop": "B",
    "white_knight": "N",
    "white_pawn": "P",
    "black_king": "k",
    "black_queen": "q",
    "black_rook": "r",
    "black_bishop": "b",
    "black_knight": "n",
    "black_pawn": "p",
}


def order_points(points):
    rect = np.zeros((4, 2), dtype="float32")
    sums = points.sum(axis=1)
    diffs = np.diff(points, axis=1)
    rect[0] = points[np.argmin(sums)]
    rect[2] = points[np.argmax(sums)]
    rect[1] = points[np.argmin(diffs)]
    rect[3] = points[np.argmax(diffs)]
    return rect


def crop_board(image):
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blurred, 50, 150)
    contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]

    for contour in contours:
        perimeter = cv2.arcLength(contour, True)
        approx = cv2.approxPolyDP(contour, 0.02 * perimeter, True)
        area = cv2.contourArea(approx)
        if len(approx) == 4 and area > image.shape[0] * image.shape[1] * 0.08:
            rect = order_points(approx.reshape(4, 2).astype("float32"))
            width_a = np.linalg.norm(rect[2] - rect[3])
            width_b = np.linalg.norm(rect[1] - rect[0])
            height_a = np.linalg.norm(rect[1] - rect[2])
            height_b = np.linalg.norm(rect[0] - rect[3])
            size = int(max(width_a, width_b, height_a, height_b))
            if size < 120:
                continue
            dst = np.array([[0, 0], [size - 1, 0], [size - 1, size - 1], [0, size - 1]], dtype="float32")
            matrix = cv2.getPerspectiveTransform(rect, dst)
            return cv2.warpPerspective(image, matrix, (size, size)), True

    h, w = image.shape[:2]
    side = min(h, w)
    x = (w - side) // 2
    y = (h - side) // 2
    return image[y:y + side, x:x + side], False


def detections_to_fen(result, names, board_size, side):
    board = [["" for _ in range(8)] for _ in range(8)]
    detections = []

    for box in result.boxes:
        cls_id = int(box.cls[0])
        label = names.get(cls_id, str(cls_id))
        fen_piece = PIECE_TO_FEN.get(label)
        if not fen_piece:
            continue

        x1, y1, x2, y2 = box.xyxy[0].tolist()
        cx = (x1 + x2) / 2.0
        cy = (y1 + y2) / 2.0
        col = max(0, min(7, int(cx / board_size * 8)))
        row = max(0, min(7, int(cy / board_size * 8)))
        conf = float(box.conf[0])

        previous = next((d for d in detections if d["row"] == row and d["col"] == col), None)
        if previous and previous["confidence"] > conf:
            continue
        if previous:
            board[row][col] = ""
            detections.remove(previous)

        board[row][col] = fen_piece
        detections.append({
            "label": label,
            "piece": fen_piece,
            "row": row,
            "col": col,
            "confidence": round(conf, 4),
        })

    rows = []
    for row in board:
        empty = 0
        fen_row = ""
        for piece in row:
            if piece:
                if empty:
                    fen_row += str(empty)
                    empty = 0
                fen_row += piece
            else:
                empty += 1
        if empty:
            fen_row += str(empty)
        rows.append(fen_row)

    return f"{'/'.join(rows)} {side} - - 0 1", detections


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--model", required=True)
    parser.add_argument("--side", default="w", choices=["w", "b"])
    parser.add_argument("--imgsz", type=int, default=1024)
    parser.add_argument("--conf", type=float, default=0.35)
    args = parser.parse_args()

    if not os.path.exists(args.image):
        print(json.dumps({"error": f"Image not found: {args.image}"}))
        return 1
    if not os.path.exists(args.model):
        print(json.dumps({"error": f"YOLO model not found: {args.model}"}))
        return 1

    image = cv2.imread(args.image)
    if image is None:
        print(json.dumps({"error": "Could not read image"}))
        return 1

    board_image, board_detected = crop_board(image)
    board_image = cv2.resize(board_image, (args.imgsz, args.imgsz), interpolation=cv2.INTER_AREA)

    model = YOLO(args.model)
    results = model.predict(board_image, imgsz=args.imgsz, conf=args.conf, verbose=False)
    fen, detections = detections_to_fen(results[0], model.names, args.imgsz, args.side)

    print(json.dumps({
        "fen": fen,
        "boardDetected": board_detected,
        "pieceCount": len(detections),
        "detections": detections,
    }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
