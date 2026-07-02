import chess


CLASS_TO_FEN = {
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


def normalize_side_to_move(side_to_move="w"):
    return "b" if str(side_to_move).lower().startswith("b") else "w"


def detections_to_fen(detections, image_width=None, image_height=None, side_to_move="w"):
    if not detections:
        raise ValueError("No chess pieces detected.")

    boxes = [d["bbox"] for d in detections if d.get("className") in CLASS_TO_FEN and len(d.get("bbox", [])) == 4]
    if not boxes:
        raise ValueError("No supported chess piece classes detected.")

    min_x = min(box[0] for box in boxes)
    min_y = min(box[1] for box in boxes)
    max_x = max(box[2] for box in boxes)
    max_y = max(box[3] for box in boxes)

    board_left = 0 if image_width else min_x
    board_top = 0 if image_height else min_y
    board_right = image_width or max_x
    board_bottom = image_height or max_y
    square_w = max((board_right - board_left) / 8.0, 1)
    square_h = max((board_bottom - board_top) / 8.0, 1)

    board = [["" for _ in range(8)] for _ in range(8)]
    warnings = []

    for detection in detections:
        class_name = detection.get("className")
        piece = CLASS_TO_FEN.get(class_name)
        bbox = detection.get("bbox", [])
        if not piece or len(bbox) != 4:
            continue
        center_x = (bbox[0] + bbox[2]) / 2.0
        center_y = (bbox[1] + bbox[3]) / 2.0
        file_idx = int((center_x - board_left) / square_w)
        rank_idx = int((center_y - board_top) / square_h)
        if file_idx < 0 or file_idx > 7 or rank_idx < 0 or rank_idx > 7:
            warnings.append(f"{class_name} outside inferred board.")
            continue
        if board[rank_idx][file_idx]:
            warnings.append(f"Multiple pieces mapped to square {chess.square_name(chess.square(file_idx, 7 - rank_idx))}.")
        board[rank_idx][file_idx] = piece
        detection["square"] = chess.square_name(chess.square(file_idx, 7 - rank_idx))

    ranks = []
    for row in board:
        empty = 0
        fen_rank = ""
        for piece in row:
            if piece:
                if empty:
                    fen_rank += str(empty)
                    empty = 0
                fen_rank += piece
            else:
                empty += 1
        if empty:
            fen_rank += str(empty)
        ranks.append(fen_rank)

    fen = f"{'/'.join(ranks)} {normalize_side_to_move(side_to_move)} - - 0 1"
    try:
        chess.Board(fen)
    except ValueError as exc:
        raise ValueError(f"Generated FEN is invalid: {exc}") from exc
    return fen, warnings
