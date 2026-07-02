import shutil
import tempfile
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, File, Form, UploadFile
from pydantic import BaseModel

from predict_puzzle import recognize_puzzle


app = FastAPI(title="EduChess Puzzle Recognition API")


class ImagePathRequest(BaseModel):
    imagePath: str
    sideToMove: str = "w"


@app.post("/recognize-puzzle")
async def recognize_puzzle_endpoint(
    file: Optional[UploadFile] = File(default=None),
    imagePath: Optional[str] = Form(default=None),
    sideToMove: str = Form(default="w")
):
    if file is not None:
        suffix = Path(file.filename or "puzzle.png").suffix or ".png"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            shutil.copyfileobj(file.file, tmp)
            path = tmp.name
        return recognize_puzzle(path, sideToMove)
    if imagePath:
        return recognize_puzzle(imagePath, sideToMove)
    return {
        "success": False,
        "fen": None,
        "confidence": 0,
        "detectedPieces": [],
        "message": "Upload an image file or provide imagePath.",
        "warnings": ["No image was provided."]
    }


@app.post("/recognize-puzzle-path")
async def recognize_puzzle_path(payload: ImagePathRequest):
    return recognize_puzzle(payload.imagePath, payload.sideToMove)
