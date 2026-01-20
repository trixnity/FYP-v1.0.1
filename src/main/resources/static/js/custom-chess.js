// Clean, chess.js-driven board rendering and move handling
// Piece images expected at img/pieces/: wp.svg, wn.svg, wb.svg, wr.svg, wq.svg, wk.svg, bp.svg, bn.svg, bb.svg, br.svg, bq.svg, bk.svg

const boxes = Array.from(document.querySelectorAll(".box"));
const boardEl = document.getElementById("board");
const arrowLayer = document.getElementById("arrow-layer");
const statusEl = document.getElementById("tog");
const resetBtn = document.getElementById("reset-btn");
const analyzeBtn = document.getElementById("analyze-btn");
const stopBtn = document.getElementById("stop-btn");
const depthInput = document.getElementById("analysis-depth");
const bestMoveEl = document.getElementById("best-move");
const evalEl = document.getElementById("eval-score");
const pvEl = document.getElementById("pv-line");
const engineStatusEl = document.getElementById("engine-status");
const dragCanvas = document.createElement("canvas");
const dragCtx = dragCanvas.getContext("2d");
const boardColors = {
  light: "#e3e9f4",
  dark: "#7f8fb3",
  selected: "#6aa8ff",
  target: "#a8f3c7"
};

let game = new Chess();
let selected = null;
let dragSource = null;
let legalTargets = new Set();
const arrows = new Map();
let arrowStart = null;
let arrowCtx = null;
const stockfishUrl = (boardEl && boardEl.dataset.stockfish) ? boardEl.dataset.stockfish : "lib/stockfish.js";
let engine = null;
let engineReady = false;


function idToSquare(id) {
  // id like b801 -> number 801 -> rank 8, file 1 -> a8
  const num = parseInt(id.slice(1), 10);
  const rank = Math.floor(num / 100);
  const file = num % 100;
  const fileChar = String.fromCharCode("a".charCodeAt(0) + file - 1);
  return `${fileChar}${rank}`;
}

function squareToId(square) {
  const file = square.charCodeAt(0) - "a".charCodeAt(0) + 1;
  const rank = parseInt(square[1], 10);
  return `b${rank * 100 + file}`;
}

function clearHighlights() {
  boxes.forEach((el) => {
    const num = parseInt(el.id.slice(1), 10);
    const rank = Math.floor(num / 100);
    const file = num % 100;
    const light = (rank + file) % 2 === 0;
    el.style.backgroundColor = light ? boardColors.light : boardColors.dark;
    el.classList.remove("selected");
  });
  legalTargets.clear();
}

function setPiece(el, piece) {
  if (!piece) {
    el.dataset.piece = "";
    el.innerHTML = "";
    return;
  }
  el.dataset.piece = piece;
  const isPawn = piece[1] === "p";
  const cls = isPawn ? "all-img all-pawn" : "all-img";
  el.innerHTML = `<img class="${cls}" src="img/pieces/${piece}.svg" alt="${piece}">`;
}

const squareEls = new Map();
boxes.forEach((el) => {
  const square = idToSquare(el.id);
  el.dataset.square = square;
  squareEls.set(square, el);
});

function updateStatus() {
  if (!statusEl) return;
  statusEl.textContent = game.turn() === "w" ? "White's Turn" : "Black's Turn";
}

function setEngineStatus(text) {
  if (!engineStatusEl) return;
  engineStatusEl.textContent = `Engine: ${text}`;
}

function setDraggableForTurn() {
  boxes.forEach((el) => {
    const square = el.dataset.square;
    const piece = game.get(square);
    const img = el.querySelector("img");
    el.draggable = false;
    if (img) {
      img.draggable = !!piece && piece.color === game.turn();
    }
  });
}

function renderAll() {
  clearHighlights();
  boxes.forEach((el) => {
    const square = idToSquare(el.id);
    const piece = game.get(square);
    if (!piece) {
      setPiece(el, null);
      el.dataset.square = square;
      return;
    }
    const code = `${piece.color}${piece.type}`; // e.g., wp, bk
    setPiece(el, code);
    el.dataset.square = square;
  });
  updateStatus();
  setDraggableForTurn();
}

function highlightMoves(fromSquare) {
  clearHighlights();
  const fromEl = document.getElementById(squareToId(fromSquare));
  fromEl.classList.add("selected");
  fromEl.style.backgroundColor = boardColors.selected;
  const moves = game.moves({ square: fromSquare, verbose: true });
  moves.forEach((mv) => {
    const id = squareToId(mv.to);
    const el = document.getElementById(id);
    el.style.backgroundColor = boardColors.target;
    legalTargets.add(mv.to);
  });
}

function updateSquare(square, pieceCode) {
  const el = squareEls.get(square);
  if (!el) return;
  setPiece(el, pieceCode);
}

function applyMove(move) {
  if (!move) return;
  const toPiece = `${move.color}${move.promotion || move.piece}`;
  updateSquare(move.from, null);
  updateSquare(move.to, toPiece);

  if (move.flags.includes("e")) {
    const capturedSquare = move.to[0] + move.from[1];
    updateSquare(capturedSquare, null);
  }

  if (move.flags.includes("k")) {
    const rookFrom = move.color === "w" ? "h1" : "h8";
    const rookTo = move.color === "w" ? "f1" : "f8";
    updateSquare(rookFrom, null);
    updateSquare(rookTo, `${move.color}r`);
  } else if (move.flags.includes("q")) {
    const rookFrom = move.color === "w" ? "a1" : "a8";
    const rookTo = move.color === "w" ? "d1" : "d8";
    updateSquare(rookFrom, null);
    updateSquare(rookTo, `${move.color}r`);
  }

  updateStatus();
  setDraggableForTurn();
  resetAnalysisUI();
}

function resetAnalysisUI() {
  if (bestMoveEl) bestMoveEl.textContent = "-";
  if (evalEl) evalEl.textContent = "-";
  if (pvEl) pvEl.textContent = "-";
  setEngineStatus("idle");
}

function initEngine() {
  if (engine || !stockfishUrl) return;
  engineReady = false;
  setEngineStatus("loading");
  engine = new Worker(stockfishUrl);
  engine.onmessage = (e) => {
    const line = typeof e.data === "string" ? e.data : (e.data && e.data.data) ? e.data.data : "";
    if (!line) return;
    handleEngineLine(String(line).trim());
  };
  engine.onerror = () => {
    setEngineStatus("error");
  };
  engine.postMessage("uci");
  engine.postMessage("isready");
}

function formatScore(type, value) {
  if (type === "cp") {
    const score = (value / 100).toFixed(2);
    return `${value >= 0 ? "+" : ""}${score}`;
  }
  if (type === "mate") {
    return `M${value}`;
  }
  return "-";
}

function handleEngineLine(line) {
  if (line === "uciok") {
    setEngineStatus("ready");
    return;
  }
  if (line === "readyok") {
    engineReady = true;
    setEngineStatus("ready");
    return;
  }
  if (line.startsWith("bestmove ")) {
    const move = line.split(" ")[1];
    if (bestMoveEl) bestMoveEl.textContent = move || "-";
    setEngineStatus("done");
    return;
  }
  if (!line.startsWith("info ")) return;

  const tokens = line.split(" ");
  const depthIdx = tokens.indexOf("depth");
  if (depthIdx !== -1 && tokens[depthIdx + 1]) {
    setEngineStatus(`searching (d${tokens[depthIdx + 1]})`);
  }

  const scoreIdx = tokens.indexOf("score");
  if (scoreIdx !== -1) {
    const type = tokens[scoreIdx + 1];
    const value = parseInt(tokens[scoreIdx + 2], 10);
    if (evalEl && Number.isFinite(value)) {
      evalEl.textContent = formatScore(type, value);
    }
  }

  const pvIdx = tokens.indexOf("pv");
  if (pvIdx !== -1 && pvEl) {
    pvEl.textContent = tokens.slice(pvIdx + 1).join(" ");
  }
}

function analyzePosition() {
  initEngine();
  if (!engine) return;
  const depth = parseInt(depthInput?.value || "12", 10);
  setEngineStatus(engineReady ? "searching" : "loading");
  if (bestMoveEl) bestMoveEl.textContent = "-";
  if (pvEl) pvEl.textContent = "-";
  if (evalEl) evalEl.textContent = "-";
  engine.postMessage("stop");
  engine.postMessage("ucinewgame");
  engine.postMessage(`position fen ${game.fen()}`);
  engine.postMessage(`go depth ${Number.isFinite(depth) ? depth : 12}`);
}

function stopAnalysis() {
  if (!engine) return;
  engine.postMessage("stop");
  setEngineStatus("stopped");
}

function clearArrows() {
  if (!arrows.size) return;
  arrows.clear();
  drawArrows();
}

function onBoxClick(e) {
  clearArrows();
  const el = e.currentTarget;
  const square = el.dataset.square || idToSquare(el.id);
  const piece = game.get(square);

  if (selected && legalTargets.has(square)) {
    const move = game.move({ from: selected, to: square, promotion: "q" });
    selected = null;
    clearHighlights();
    applyMove(move);
    return;
  }

  if (piece && piece.color === game.turn()) {
    selected = square;
    highlightMoves(square);
    return;
  }

  selected = null;
  clearHighlights();
}

function onDragStart(e) {
  const box = e.currentTarget;
  const square = box.dataset.square || idToSquare(box.id);
  const piece = game.get(square);
  if (!piece || piece.color !== game.turn()) {
    e.preventDefault();
    return;
  }
  dragSource = square;
  highlightMoves(square);
  if (e.dataTransfer) {
    const img = box.querySelector("img");
    if (img && dragCtx) {
      const rect = img.getBoundingClientRect();
      const width = Math.max(1, rect.width);
      const height = Math.max(1, rect.height);
      const scale = window.devicePixelRatio || 1;
      dragCanvas.width = Math.max(1, Math.floor(width * scale));
      dragCanvas.height = Math.max(1, Math.floor(height * scale));
      dragCtx.setTransform(scale, 0, 0, scale, 0, 0);
      dragCtx.clearRect(0, 0, width, height);
      dragCtx.drawImage(img, 0, 0, width, height);
      e.dataTransfer.setDragImage(dragCanvas, width / 2, height / 2);
    }
    e.dataTransfer.setData("text/plain", square);
    e.dataTransfer.effectAllowed = "move";
  }
}

function onDragOver(e) {
  if (!dragSource) return;
  const box = e.currentTarget;
  const square = box.dataset.square || idToSquare(box.id);
  e.preventDefault();
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = legalTargets.has(square) ? "move" : "none";
  }
}

function onDrop(e) {
  if (!dragSource) return;
  const box = e.currentTarget;
  const square = box.dataset.square || idToSquare(box.id);
  e.preventDefault();
  const isLegal = legalTargets.has(square);
  clearHighlights();
  if (isLegal) {
    const move = game.move({ from: dragSource, to: square, promotion: "q" });
    applyMove(move);
  }
  dragSource = null;
}

function onDragEnd() {
  clearHighlights();
  dragSource = null;
}

function getSquareCenter(square) {
  if (!boardEl) return null;
  const squareEl = squareEls.get(square);
  if (!squareEl) return null;
  const boardRect = boardEl.getBoundingClientRect();
  const rect = squareEl.getBoundingClientRect();
  return {
    x: rect.left - boardRect.left + rect.width / 2,
    y: rect.top - boardRect.top + rect.height / 2
  };
}

function drawArrow(fromSquare, toSquare) {
  if (!arrowCtx) return;
  const from = getSquareCenter(fromSquare);
  const to = getSquareCenter(toSquare);
  if (!from || !to) return;
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const len = Math.hypot(dx, dy);
  if (len < 1) return;

  const headLength = Math.min(18, len * 0.3);
  const headWidth = headLength * 0.6;
  const angle = Math.atan2(dy, dx);
  const endX = to.x - Math.cos(angle) * headLength;
  const endY = to.y - Math.sin(angle) * headLength;

  arrowCtx.beginPath();
  arrowCtx.moveTo(from.x, from.y);
  arrowCtx.lineTo(endX, endY);
  arrowCtx.stroke();

  arrowCtx.beginPath();
  arrowCtx.moveTo(to.x, to.y);
  arrowCtx.lineTo(endX + Math.cos(angle + Math.PI / 2) * headWidth, endY + Math.sin(angle + Math.PI / 2) * headWidth);
  arrowCtx.lineTo(endX + Math.cos(angle - Math.PI / 2) * headWidth, endY + Math.sin(angle - Math.PI / 2) * headWidth);
  arrowCtx.closePath();
  arrowCtx.fill();
}

function drawArrows() {
  if (!arrowCtx || !boardEl || !arrowLayer) return;
  const rect = boardEl.getBoundingClientRect();
  arrowCtx.clearRect(0, 0, rect.width, rect.height);
  arrowCtx.lineWidth = Math.max(2, rect.width / 70);
  arrowCtx.lineCap = "round";
  arrowCtx.lineJoin = "round";
  arrowCtx.strokeStyle = "rgba(220, 38, 38, 0.85)";
  arrowCtx.fillStyle = "rgba(220, 38, 38, 0.85)";
  arrows.forEach((arrow) => drawArrow(arrow.from, arrow.to));
}

function resizeArrowLayer() {
  if (!boardEl || !arrowLayer) return;
  const rect = boardEl.getBoundingClientRect();
  const scale = window.devicePixelRatio || 1;
  arrowLayer.style.width = `${rect.width}px`;
  arrowLayer.style.height = `${rect.height}px`;
  arrowLayer.width = Math.max(1, Math.floor(rect.width * scale));
  arrowLayer.height = Math.max(1, Math.floor(rect.height * scale));
  arrowCtx = arrowLayer.getContext("2d");
  if (!arrowCtx) return;
  arrowCtx.setTransform(scale, 0, 0, scale, 0, 0);
  drawArrows();
}

function onRightDown(e) {
  if (e.button !== 2) return;
  e.preventDefault();
  const square = e.currentTarget.dataset.square || idToSquare(e.currentTarget.id);
  arrowStart = square;
}

function onRightUp(e) {
  if (e.button !== 2) return;
  e.preventDefault();
  const square = e.currentTarget.dataset.square || idToSquare(e.currentTarget.id);
  if (arrowStart && arrowStart !== square) {
    const key = `${arrowStart}-${square}`;
    if (arrows.has(key)) {
      arrows.delete(key);
    } else {
      arrows.set(key, { from: arrowStart, to: square });
    }
    drawArrows();
  }
  arrowStart = null;
}

boxes.forEach((el) => {
  el.addEventListener("click", onBoxClick);
  el.addEventListener("dragstart", onDragStart);
  el.addEventListener("dragover", onDragOver);
  el.addEventListener("drop", onDrop);
  el.addEventListener("dragend", onDragEnd);
  el.addEventListener("mousedown", onRightDown);
  el.addEventListener("mouseup", onRightUp);
});

if (boardEl) {
  boardEl.addEventListener("contextmenu", (e) => e.preventDefault());
}

if (resetBtn) {
  resetBtn.addEventListener("click", () => {
    game = new Chess();
    selected = null;
    dragSource = null;
    renderAll();
    resetAnalysisUI();
  });
}

if (analyzeBtn) {
  analyzeBtn.addEventListener("click", analyzePosition);
}

if (stopBtn) {
  stopBtn.addEventListener("click", stopAnalysis);
}

renderAll();
resizeArrowLayer();
window.addEventListener("resize", resizeArrowLayer);
