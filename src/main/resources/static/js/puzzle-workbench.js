const pieceTheme = (piece) => `img/pieces/${piece.toLowerCase()}.svg`;
const storedRole = (localStorage.getItem("userRole") || "STUDENT").toUpperCase();
const roleHeaders = { "X-User-Role": storedRole };

const solverBoardEl = document.getElementById("solver-board");
const solverIdInput = document.getElementById("solver-id");
const solverLoadBtn = document.getElementById("solver-load");
const solverResetBtn = document.getElementById("solver-reset");
const solverFlipBtn = document.getElementById("solver-flip");
const solverStatusEl = document.getElementById("solver-status");
const solverTitleEl = document.getElementById("solver-title");
const solverInfoEl = document.getElementById("solver-info");
const solverMovesEl = document.getElementById("solver-moves");

const editorBoardEl = document.getElementById("editor-board");
const editIdInput = document.getElementById("edit-id");
const editTitleInput = document.getElementById("edit-title");
const editTopicInput = document.getElementById("edit-topic");
const editLevelInput = document.getElementById("edit-level");
const editFenInput = document.getElementById("edit-fen");
const editSideSelect = document.getElementById("edit-side");
const editSolutionInput = document.getElementById("edit-solution");
const editLoadBtn = document.getElementById("edit-load");
const editApplyFenBtn = document.getElementById("edit-apply-fen");
const editFromBoardBtn = document.getElementById("edit-from-board");
const editSaveBtn = document.getElementById("edit-save");
const editStatusEl = document.getElementById("edit-status");

let solverBoard;
let solverGame = new Chess();
let solverPuzzle = null;
let solverLine = [];
let solverStep = 0;
let solverMovesLog = [];

let editorBoard;

function setSolverStatus(msg) {
  if (solverStatusEl) solverStatusEl.textContent = msg;
}

function setEditorStatus(msg) {
  if (editStatusEl) editStatusEl.textContent = msg;
}

function normalizeFen(fen, side) {
  if (!fen) return "";
  const cleaned = fen.trim();
  if (!cleaned) return "";
  const parts = cleaned.split(/\s+/);
  if (parts.length >= 2) return cleaned;
  const sideChar = side === "b" ? "b" : "w";
  return `${parts[0]} ${sideChar} - - 0 1`;
}

function extractSolutionMoves(puzzle) {
  if (!puzzle) return [];
  if (puzzle.solutionMove && puzzle.solutionMove.trim()) {
    return [puzzle.solutionMove.trim().toLowerCase()];
  }
  if (!Array.isArray(puzzle.moves)) return [];
  return puzzle.moves
    .map((m) => (typeof m === "string" ? m : m && m.moveUci))
    .filter(Boolean)
    .map((m) => String(m).trim().toLowerCase());
}

function renderSolverMeta() {
  if (!solverPuzzle) {
    if (solverTitleEl) solverTitleEl.textContent = "-";
    if (solverInfoEl) solverInfoEl.textContent = "-";
    return;
  }
  if (solverTitleEl) solverTitleEl.textContent = solverPuzzle.title || "Puzzle";
  const topic = solverPuzzle.theme || solverPuzzle.topic || "-";
  const level = solverPuzzle.difficulty ?? solverPuzzle.level ?? "-";
  if (solverInfoEl) solverInfoEl.textContent = `Topic: ${topic} | Level: ${level}`;
}

function renderSolverMoves() {
  if (!solverMovesEl) return;
  solverMovesEl.textContent = solverMovesLog.length ? solverMovesLog.join("\n") : "(moves appear here)";
}

async function fetchPuzzleById(id) {
  const res = await fetch(`/api/puzzles/${id}`, { headers: roleHeaders });
  if (!res.ok) throw new Error("Puzzle not found");
  return res.json();
}

function resetSolverBoard() {
  if (!solverPuzzle) return;
  solverStep = 0;
  solverMovesLog = [];
  const fen = normalizeFen(solverPuzzle.fen, solverPuzzle.side);
  solverGame.load(fen);
  solverBoard.position(fen);
  const side = solverGame.turn() === "w" ? "White" : "Black";
  setSolverStatus(`Ready. ${side} to move.`);
  renderSolverMoves();
}

async function loadSolverPuzzle(id) {
  try {
    setSolverStatus("Loading puzzle...");
    const puzzle = await fetchPuzzleById(id);
    const fen = normalizeFen(puzzle.fen, puzzle.side);
    if (!fen) throw new Error("Puzzle has no FEN");
    const testGame = new Chess();
    if (!testGame.load(fen)) throw new Error("Invalid FEN in puzzle");

    solverPuzzle = puzzle;
    solverLine = extractSolutionMoves(puzzle);
    solverStep = 0;
    solverMovesLog = [];
    solverGame.load(fen);
    solverBoard.orientation(puzzle.side === "b" ? "black" : "white");
    solverBoard.position(fen, false);
    renderSolverMeta();
    renderSolverMoves();

    if (!solverLine.length) {
      setSolverStatus("Puzzle loaded, but no solution move found.");
    } else {
      setSolverStatus(`Puzzle loaded. ${solverGame.turn() === "w" ? "White" : "Black"} to move.`);
    }
  } catch (err) {
    setSolverStatus(`Load failed: ${err.message}`);
  }
}

function solverOnDragStart(source, piece) {
  if (!solverPuzzle) return false;
  if (solverGame.game_over()) return false;
  if (solverGame.turn() === "w" && piece.startsWith("b")) return false;
  if (solverGame.turn() === "b" && piece.startsWith("w")) return false;
  return true;
}

function solverOnDrop(source, target) {
  if (!solverPuzzle) return "snapback";
  const move = solverGame.move({ from: source, to: target, promotion: "q" });
  if (!move) return "snapback";

  if (!solverLine.length) {
    setSolverStatus("No solution set for this puzzle.");
    solverGame.undo();
    return "snapback";
  }

  const expected = solverLine[solverStep];
  const attempted = source + target + (move.promotion || "");

  if (attempted !== expected) {
    solverGame.undo();
    setSolverStatus("Wrong move. Try again.");
    return "snapback";
  }

  solverMovesLog.push(`${solverStep + 1}. ${move.san}`);
  solverStep += 1;

  if (solverStep < solverLine.length) {
    const reply = solverLine[solverStep];
    const replyMove = solverGame.move({
      from: reply.slice(0, 2),
      to: reply.slice(2, 4),
      promotion: reply[4]
    });
    if (replyMove) {
      solverMovesLog.push(`... ${replyMove.san}`);
      solverStep += 1;
    }
  }

  solverBoard.position(solverGame.fen());
  renderSolverMoves();

  if (solverStep >= solverLine.length) {
    setSolverStatus(solverGame.isCheckmate() ? "Checkmate. Solved!" : "Solved!");
  } else {
    setSolverStatus("Correct. Continue...");
  }
}

function solverOnSnapEnd() {
  solverBoard.position(solverGame.fen());
}

function positionToFen(position, side) {
  const files = "abcdefgh";
  const ranks = [];
  for (let r = 8; r >= 1; r -= 1) {
    let empty = 0;
    let row = "";
    for (let f = 0; f < 8; f += 1) {
      const square = `${files[f]}${r}`;
      const piece = position[square];
      if (!piece) {
        empty += 1;
        continue;
      }
      if (empty) {
        row += empty;
        empty = 0;
      }
      const code = piece[1];
      row += piece[0] === "w" ? code.toUpperCase() : code.toLowerCase();
    }
    if (empty) row += empty;
    ranks.push(row);
  }
  const sideChar = side === "b" ? "b" : "w";
  return `${ranks.join("/")} ${sideChar} - - 0 1`;
}

function applyEditorFen() {
  if (!editFenInput) return;
  const side = editSideSelect ? editSideSelect.value : "w";
  const fen = normalizeFen(editFenInput.value, side);
  const tmp = new Chess();
  if (!tmp.load(fen)) {
    setEditorStatus("Invalid FEN. Please fix and try again.");
    return;
  }
  editorBoard.position(fen, false);
  setEditorStatus("Board updated from FEN.");
}

function useBoardFen() {
  if (!editorBoard || !editFenInput) return;
  const side = editSideSelect ? editSideSelect.value : "w";
  const fen = positionToFen(editorBoard.position(), side);
  editFenInput.value = fen;
  setEditorStatus("FEN updated from board.");
}

async function loadEditorPuzzle(id) {
  try {
    setEditorStatus("Loading puzzle...");
    const puzzle = await fetchPuzzleById(id);
    editTitleInput.value = puzzle.title || "";
    editTopicInput.value = puzzle.theme || puzzle.topic || "";
    editLevelInput.value = puzzle.difficulty ?? puzzle.level ?? "";
    editFenInput.value = normalizeFen(puzzle.fen, puzzle.side);
    editSideSelect.value = puzzle.side === "b" ? "b" : "w";
    editSolutionInput.value = puzzle.solutionMove || "";
    applyEditorFen();
    setEditorStatus("Puzzle loaded.");
  } catch (err) {
    setEditorStatus(`Load failed: ${err.message}`);
  }
}

async function saveEditorPuzzle() {
  const id = editIdInput.value.trim();
  if (!id) {
    setEditorStatus("Enter a puzzle ID.");
    return;
  }
  const payload = {};
  const title = editTitleInput.value.trim();
  const topic = editTopicInput.value.trim();
  const level = parseInt(editLevelInput.value, 10);
  const side = editSideSelect.value;
  const fen = normalizeFen(editFenInput.value, side);
  const solution = editSolutionInput.value.trim();

  if (title) payload.title = title;
  if (topic) payload.theme = topic;
  if (Number.isFinite(level)) payload.difficulty = level;
  if (fen) payload.fen = fen;
  if (side) payload.side = side;
  if (solution) payload.solutionMove = solution;

  const checkGame = new Chess();
  if (fen && !checkGame.load(fen)) {
    setEditorStatus("Invalid FEN. Fix it before saving.");
    return;
  }

  if (solution) {
    const test = new Chess();
    if (fen && test.load(fen)) {
      const move = test.move({
        from: solution.slice(0, 2),
        to: solution.slice(2, 4),
        promotion: solution[4]
      });
      if (!move) {
        setEditorStatus("Solution move is illegal for this FEN.");
        return;
      }
    }
  }

  try {
    setEditorStatus("Saving...");
    const res = await fetch(`/api/puzzles/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", ...roleHeaders },
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error("Save failed");
    setEditorStatus("Saved.");
  } catch (err) {
    setEditorStatus(`Save failed: ${err.message}`);
  }
}

solverBoard = Chessboard("solver-board", {
  draggable: true,
  position: "start",
  pieceTheme,
  onDragStart: solverOnDragStart,
  onDrop: solverOnDrop,
  onSnapEnd: solverOnSnapEnd
});

editorBoard = Chessboard("editor-board", {
  draggable: true,
  position: "start",
  sparePieces: true,
  dropOffBoard: "trash",
  pieceTheme
});

if (solverLoadBtn) {
  solverLoadBtn.addEventListener("click", () => {
    const id = solverIdInput.value.trim() || "1";
    loadSolverPuzzle(id);
  });
}

if (solverResetBtn) {
  solverResetBtn.addEventListener("click", resetSolverBoard);
}

if (solverFlipBtn) {
  solverFlipBtn.addEventListener("click", () => solverBoard.flip());
}

if (editLoadBtn) {
  editLoadBtn.addEventListener("click", () => {
    const id = editIdInput.value.trim();
    if (id) loadEditorPuzzle(id);
  });
}

if (editApplyFenBtn) {
  editApplyFenBtn.addEventListener("click", applyEditorFen);
}

if (editFromBoardBtn) {
  editFromBoardBtn.addEventListener("click", useBoardFen);
}

if (editSaveBtn) {
  editSaveBtn.addEventListener("click", saveEditorPuzzle);
}

setSolverStatus("Ready. Load a puzzle to begin.");
setEditorStatus("Load a puzzle ID to edit.");
