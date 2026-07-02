document.addEventListener("DOMContentLoaded", () => {
  window.addEventListener("message", (e) => {
                      if (!e.data || e.data.id !== "14342945") return;
                      const el = document.getElementById(e.data.id);
                      if (el && e.data.frameHeight) {
                        el.style.height = `${e.data.frameHeight + 37}px`;
                      }
                    });

  
    const endpoints = { users: "/api/users", register: "/api/users/register", students: "/api/students" };
    const storedRole = (localStorage.getItem("userRole") || "STUDENT").toUpperCase();
    const roleHeaders = { "X-User-Role": storedRole };
    const pieceTheme = (piece) => `img/pieces/${piece.toLowerCase()}.svg`;
    const views = document.querySelectorAll(".views > section");
      const navLinks = document.querySelectorAll(".nav-link[data-target]");
      const logoutLink = document.getElementById("logoutLink");
    const statusBox = document.getElementById("status");
    const usersList = document.getElementById("usersList");
    const usersListAdmin = document.getElementById("usersListAdmin");
    const studentsList = document.getElementById("studentsList");
    const studentsListCoach = document.getElementById("studentsListCoach");
    const userForm = document.getElementById("userForm");
    const studentForm = document.getElementById("studentForm");
    const statPlayers = document.getElementById("statPlayers");
    const statStudents = document.getElementById("statStudents");
    const statRating = document.getElementById("statRating");
    const appShell = document.getElementById("appShell");
    const headerNav = document.getElementById("headerNav");
    const puzzleTabs = document.getElementById("puzzleTabs");
    const puzzleGrid = document.getElementById("puzzleGrid");
    const puzzleTitle = document.getElementById("puzzleTitle");
    const puzzleMeta = document.getElementById("puzzleMeta");
    const puzzleLevels = document.getElementById("puzzleLevels");
    const puzzleBoardEl = document.getElementById("puzzleBoard");
    const puzzleStatus = document.getElementById("puzzleStatus");
    const puzzleNextBtn = document.getElementById("puzzleNext");
    const puzzleResetBtn = document.getElementById("puzzleReset");
    const puzzleMarkSolvedBtn = document.getElementById("puzzleMarkSolved");
    const puzzleStarsEl = document.getElementById("puzzleStars");
    const greetingUser = document.getElementById("greetingUser");
    const analysisBoardEl = document.getElementById("analysisBoard");
    const analysisStatus = document.getElementById("analysisStatus");
    const analysisEngineBtn = document.getElementById("analysisEngine");
    const analysisResetBtn = document.getElementById("analysisReset");
    const analysisFenForm = document.getElementById("analysisFenForm");
    const analysisFenInput = document.getElementById("analysisFen");
    const analysisFromPuzzleBtn = document.getElementById("analysisFromPuzzle");
    const analysisFlipBtn = document.getElementById("analysisFlip");
    let isAuthenticated = true;
    let activeCategory = "tactic";
    let activeThemeId = null;
    let puzzleGame = null;
    let puzzleBoard = null;
    let puzzleIndex = 0;
    let analysisGame = null;
    let analysisBoard = null;
    const storedEmailRaw = localStorage.getItem("userEmail") || "";
    const storedEmail = storedEmailRaw.toLowerCase();
  
    const puzzleCatalog = {
      tactic: [
        { id: "mate1", title: "Mate in one", icon: "?", points: "0 / 1746", levels: ["Queen mating", "Rook mating", "Bishop mating", "Knight mating", "Pawn mating"] },
        { id: "double-attack", title: "Double attack & forks", icon: "??", points: "0 / 513", levels: ["Level 1 (51)", "Level 2 (60)", "Level 3 (80)"] },
        { id: "pin", title: "A pin", icon: "??", points: "0 / 774", levels: ["Basic pins", "Advanced pins"] },
        { id: "discovered", title: "Discovered attack", icon: "???", points: "0 / 687", levels: ["Level 1", "Level 2"] },
        { id: "deflection", title: "Deflection", icon: "??", points: "0 / 600", levels: ["Level 1", "Level 2", "Level 3"] },
        { id: "mate2", title: "Mate in two", icon: "?", points: "0 / 1230", levels: ["Level 1", "Level 2", "Exam"] },
        { id: "remove-defender", title: "Remove the defender", icon: "??", points: "0 / 624", levels: ["Level 1", "Level 2"] },
        { id: "overload", title: "Overload", icon: "??", points: "0 / 291", levels: ["Level 1"] },
        { id: "clearance", title: "Clearance", icon: "??", points: "0 / 651", levels: ["Level 1"] }
      ],
      openings: [
        { id: "sicilian", title: "Sicilian Tactics", icon: "??", points: "0 / 200", levels: ["Najdorf tricks", "Dragon shots"] },
        { id: "queen-gambit", title: "Queen’s Gambit ideas", icon: "??", points: "0 / 160", levels: ["Trap motifs", "Isolated pawn tactics"] }
      ],
      endings: [
        { id: "rook-endings", title: "Rook endings", icon: "?", points: "0 / 180", levels: ["Lucena", "Philidor", "Checks & bridges"] },
        { id: "minor-endings", title: "Minor piece endings", icon: "?", points: "0 / 140", levels: ["Bishop vs knight", "Opposite bishops"] }
      ],
      beginners: [
        { id: "basics", title: "For beginners", icon: "?", points: "0 / 120", levels: ["Mate in one", "Simple forks"] }
      ],
      coach: [
        { id: "coach-pack", title: "Coach tools", icon: "??", points: "0 / 80", levels: ["Assign set", "Review attempts"] }
      ]
    };
  
    const puzzlesPlayable = [
      { id: "mate1-1", theme: "mate1", fen: "6k1/5ppp/8/8/8/3Q4/5PPP/6K1 w - - 0 1", side: "w", solution: ["Qd8#"], title: "Mate in 1: back rank" },
      { id: "mate1-2", theme: "mate1", fen: "5rk1/5ppp/8/8/8/8/5PPP/5RK1 w - - 0 1", side: "w", solution: ["Re1#"], title: "Mate in 1: file mate" },
      { id: "pin-1", theme: "pin", fen: "rnbqkbnr/pppp1ppp/8/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq - 1 2", side: "b", solution: ["Bb4+"], title: "Pin tactic" }
    ];
  
    const mapJsonPuzzle = (raw) => {
      const fen = raw.fen || "";
      const side = raw.side || (fen.split(" ")[1] === "b" ? "b" : "w");
      const solution = raw.solution || raw.moves || raw.movesUci || [];
      return {
        id: raw.id || raw.puzzleId || `p-${Math.random().toString(36).slice(2)}`,
        theme: raw.theme || (Array.isArray(raw.themes) ? raw.themes[0] : (raw.themes || "json")),
        fen,
        side,
        solution,
        title: raw.title || `${raw.id || "Puzzle"} (${raw.rating || ""})`.trim(),
        rating: raw.rating || raw.Rating
      };
    };
  
    const convertFirstMoveToSan = (p) => {
      if (!p || !p.fen || !p.solution || !p.solution.length || !window.Chess) return null;
      const first = p.solution[0];
      if (!/^[a-h][1-8][a-h][1-8]/.test(first)) return first; // already SAN
      try {
        const tmp = new Chess(p.fen);
        const move = tmp.move({ from: first.slice(0, 2), to: first.slice(2, 4), promotion: first[4] });
        if (move) {
          p.solutionSan = [move.san];
          return move.san;
        }
      } catch (_) { /* ignore */ }
      return first;
    };
  
    const setStatus = (message, tone = "info") => {
      if (!statusBox) return;
      statusBox.textContent = message;
      const colors = { info: "var(--muted)", success: "var(--accent-2)", warn: "var(--accent)" };
      statusBox.style.color = colors[tone] || colors.info;
    };

    const setGreeting = (user) => {
      if (!greetingUser) return;
      const label = (user && user.name) || storedEmailRaw || (user && user.email) || "player";
      greetingUser.textContent = `Hi, ${label}`;
    };
  
    const switchView = (targetId) => {
      views.forEach((v) => v.classList.toggle("active", v.id === `section-${targetId}`));
      navLinks.forEach((link) => link.classList.toggle("active", link.dataset.target === targetId));
    };
  
    navLinks.forEach((link) => {
      link.addEventListener("click", (e) => { e.preventDefault(); switchView(link.dataset.target); });
    });
  
      const enforceRoleVisibility = (role) => {
        const coachItems = document.querySelectorAll(".coach-only");
        const adminItems = document.querySelectorAll(".admin-only");
        coachItems.forEach((el) => el.style.display = (role === "COACH" || role === "ADMIN") ? "" : "none");
        adminItems.forEach((el) => el.style.display = role === "ADMIN" ? "" : "none");
        const activeLink = document.querySelector(".nav-link.active");
        if (activeLink && activeLink.style.display === "none") switchView("dashboard");
      };
  
    const renderUsers = (users) => {
      const render = (container) => {
        if (!container) return;
        if (!users || !users.length) { container.innerHTML = '<div class="empty">No players yet. Add one to start tracking.</div>'; return; }
        container.innerHTML = users.map((u) => `
          <div class="card">
            <h3>${u.name || "Unnamed player"}</h3>
            <div class="meta">${u.email || "No email on record"}</div>
            <div class="meta">
              <span class="badge">${u.role || "STUDENT"}</span>
              ${u.rating ? `<span class="badge" style="background: rgba(245, 184, 65, 0.16); color: ${u.rating >= 2000 ? '#ffcc67' : 'var(--accent)'};">${u.rating} Elo</span>` : ""}
            </div>
            ${u.goals ? `<div class="meta">Goals: ${u.goals}</div>` : ""}
          </div>
        `).join("");
      };
      render(usersList); render(usersListAdmin);
      if (statPlayers) statPlayers.textContent = users ? users.length : 0;
      const ratings = (users || []).map((u) => u.rating).filter((r) => typeof r === "number");
      if (ratings.length && statRating) { const min = Math.min(...ratings); const max = Math.max(...ratings); statRating.textContent = `${min} - ${max}`; } else if (statRating) { statRating.textContent = "—"; }
    };
  
    const renderStudents = (students) => {
      const render = (container) => {
        if (!container) return;
        if (!students || !students.length) { container.innerHTML = '<div class="empty">No students yet. Log one to track progress.</div>'; return; }
        container.innerHTML = students.map((s) => `
          <div class="card">
            <h3>${s.name || "Unnamed student"}</h3>
            <div class="meta">${s.email || "No email on record"}</div>
            <div class="meta">${s.id ? `ID: ${s.id}` : ""} ${typeof s.puzzlesSolved === "number" ? ` | Puzzles: ${s.puzzlesSolved}` : ""}</div>
            <div class="meta">${s.coachId ? `Coach ID: ${s.coachId}` : ""}</div>
            ${s.id ? `<button class="small-action" data-del-student="${s.id}" data-name="${s.name || ""}">Delete</button>` : ""}
          </div>
        `).join("");
        container.querySelectorAll("[data-del-student]").forEach((btn) => {
          btn.addEventListener("click", () => {
            const id = btn.getAttribute("data-del-student");
            const name = btn.getAttribute("data-name") || "";
            deleteStudent(id, name);
          });
        });
      };
      render(studentsList); render(studentsListCoach);
      if (statStudents) statStudents.textContent = students ? students.length : 0;
    };
  
    const deleteStudent = (id, name = "") => {
      if (!id) return;
      const ok = window.confirm(`Delete student ${name || id}?`);
      if (!ok) return;
      fetch(`${endpoints.students}/${id}`, { method: "DELETE" })
        .then((res) => {
          if (!res.ok) throw new Error("Failed");
          setStatus(`Deleted student ${id}`, "success");
          fetchStudents();
        })
        .catch(() => setStatus("Could not delete student. Check the API.", "warn"));
    };
  
    const fetchUsers = () => {
      fetch(endpoints.users)
        .then((res) => res.json())
        .then((data) => {
          renderUsers(data);
          const matched = (data || []).find((u) => (u.email || "").toLowerCase() === storedEmail);
          setGreeting(matched);
        })
        .catch(() => setStatus("Could not load users. Is the backend running?", "warn"));
    };
  
    const fetchStudents = () => {
      fetch(endpoints.students)
        .then((res) => res.json())
        .then((data) => renderStudents(data))
        .catch(() => setStatus("Could not load students. Is the backend running?", "warn"));
    };
  
    if (userForm) {
      userForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const formData = new FormData(userForm);
        const rating = formData.get("rating");
        const payload = { name: formData.get("name"), email: formData.get("email"), password: formData.get("password"), role: formData.get("role") || "STUDENT", goals: formData.get("goals") || null, rating: rating ? Number(rating) : null };
        fetch(endpoints.register, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) })
          .then((res) => res.json())
          .then(() => { setStatus("Player registered via /api/users/register", "success"); userForm.reset(); fetchUsers(); })
          .catch(() => setStatus("Could not register player. Check the API logs.", "warn"));
      });
    }
  
    if (studentForm) {
      studentForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const formData = new FormData(studentForm);
        const payload = { name: formData.get("name"), email: formData.get("email") };
        fetch(endpoints.students, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) })
          .then(async (res) => {
            if (!res.ok) {
              if (res.status === 409) throw new Error("duplicate");
              throw new Error("create-failed");
            }
            return res.json();
          })
          .then(() => { setStatus("Student created via /api/students", "success"); studentForm.reset(); fetchStudents(); })
          .catch((err) => {
            if (err.message === "duplicate") {
              alert("Email already exists for another student.");
              setStatus("Email already exists for another student.", "warn");
            } else {
              alert("Could not create student. Please check the API or logs.");
              setStatus("Could not create student. Check the API logs.", "warn");
            }
          });
      });
    }
  
    const ensureBoard = () => {
      if (!puzzleBoardEl || !window.Chessboard || !window.Chess) return;
      if (!puzzleBoard) {
        puzzleGame = new Chess("1k6/8/QK6/8/8/8/8/8 w - - 0 1"); // white to move
        puzzleBoard = Chessboard("puzzleBoard", {
          position: puzzleGame.fen(),
          orientation: "white",
          draggable: true,
          pieceTheme
        });
      }
    };
  
    const initPuzzleBoard = () => {
      ensureBoard();
      if (puzzlesPlayable.length) {
        loadPuzzle(puzzleIndex);
      } else {
        updatePuzzleUI({ title: "Puzzle" }, "No puzzle loaded", "warn");
      }
    };
  
    const loadPuzzle = (idx) => {
      if (!puzzlesPlayable.length) return;
      puzzleIndex = (idx + puzzlesPlayable.length) % puzzlesPlayable.length;
      const p = puzzlesPlayable[puzzleIndex];
      if (!p) return;
      puzzleGame = new Chess(p.fen);
      if (!puzzleBoard) {
        puzzleBoard = Chessboard("puzzleBoard", {
          position: p.fen,
          orientation: p.side === "b" ? "black" : "white",
          draggable: true,
          pieceTheme,
          onDragStart: (source, piece) => {
            if (p.side === "w" && piece.startsWith("b")) return false;
            if (p.side === "b" && piece.startsWith("w")) return false;
            if (puzzleGame.game_over()) return false;
          },
          onDrop: (source, target) => handlePuzzleDrop(p, source, target),
          onSnapEnd: () => puzzleBoard.position(puzzleGame.fen())
        });
      } else {
        puzzleBoard.orientation(p.side === "b" ? "black" : "white");
        puzzleBoard.position(p.fen, false);
        puzzleBoard.config({
          onDrop: (source, target) => handlePuzzleDrop(p, source, target),
          pieceTheme
        });
      }
      updatePuzzleUI(p, "Your move.");
    };
  
    const handlePuzzleDrop = (p, source, target) => {
      const move = puzzleGame.move({ from: source, to: target, promotion: "q" });
      if (!move) return "snapback";
      const expectedSan = p.solutionSan?.[0] || convertFirstMoveToSan(p) || p.solution?.[0];
      const expectedUci = p.solution?.[0];
      const moveUci = (move.from + move.to + (move.promotion || "")).toLowerCase();
      const matchSan = expectedSan && move.san === expectedSan;
      const matchUci = expectedUci && /^[a-h][1-8][a-h][1-8]/.test(expectedUci) && moveUci === expectedUci.toLowerCase();
      if (matchSan || matchUci) {
        updatePuzzleUI(p, "Correct! Load next when ready.", "success");
      } else {
        p.tryCount = (p.tryCount || 0) + 1;
        puzzleGame.undo();
        puzzleBoard.position(puzzleGame.fen(), false);
        updatePuzzleUI(p, "Try again.", "warn");
        return "snapback";
      }
    };
  
    const updatePuzzleUI = (p, message, tone = "info") => {
      if (puzzleStatus) puzzleStatus.textContent = message;
      if (puzzleTitle) puzzleTitle.textContent = p.title || "Puzzle";
      if (puzzleMeta) puzzleMeta.textContent = `Side to move: ${p.side === "b" ? "Black" : "White"}`;
      const embed = document.getElementById("queenEmbedPanel");
      if (embed) {
        const showEmbed = (p.title || "").toLowerCase().includes("queen mating") || (p.theme || "").toLowerCase().includes("queen");
        embed.style.display = showEmbed ? "" : "none";
      }
      if (puzzleStarsEl) puzzleStarsEl.textContent = "Stars: -";
    };
  
    if (puzzleNextBtn) puzzleNextBtn.addEventListener("click", () => loadPuzzle(puzzleIndex + 1));
    if (puzzleResetBtn) puzzleResetBtn.addEventListener("click", () => loadPuzzle(puzzleIndex));
  
    const postAttempt = async (puzzleId, theme, solved, attempts, stars) => {
      try {
        const res = await fetch("/api/attempts", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ puzzleId, userId: 1, userEmail: "demo@user.com", solved, attempts, stars, theme })
        });
        if (!res.ok) throw new Error("fail");
      } catch (e) {
        console.warn("Attempt save failed", e);
      }
    };
  
    if (puzzleMarkSolvedBtn) {
      puzzleMarkSolvedBtn.addEventListener("click", () => {
        if (!puzzlesPlayable.length) return;
        const p = puzzlesPlayable[puzzleIndex];
        const stars = (p.tryCount === 1) ? 3 : (p.tryCount === 2 ? 2 : 1);
        if (puzzleStarsEl) puzzleStarsEl.textContent = `Stars: ${stars}`;
        postAttempt(p.id, p.theme, true, p.tryCount || 1, stars);
        updatePuzzleUI(p, "Marked solved", "success");
      });
    }
  
    // Fallback: if nothing loaded after 500ms, show a simple board so UI is never empty
    setTimeout(() => {
      if (!puzzleBoard) {
        ensureBoard();
        updatePuzzleUI({ title: "Puzzle" }, "Fallback board loaded", "warn");
      }
    }, 500);
  
    // login removed on dashboard page
    // no hamburger: nav stays visible
  
    const renderTabs = () => {
      if (!puzzleTabs) return;
      const themes = Object.entries(puzzleThemes).map(([key, val]) => ({ id: key, label: val.title }));
      if (!activeThemeKey) activeThemeKey = themes[0]?.id;
      puzzleTabs.innerHTML = themes.map((c) => `
        <button class="tab ${c.id === activeThemeKey ? "active" : ""}" data-cat="${c.id}">${c.label}</button>
      `).join("");
      puzzleTabs.querySelectorAll(".tab").forEach((btn) => {
        btn.addEventListener("click", () => {
          activeThemeKey = btn.dataset.cat;
          const theme = puzzleThemes[activeThemeKey];
          activeLevelId = theme?.levels?.[0]?.id || null;
          renderLevels();
          renderPuzzleGridLarge();
        });
      });
    };
  
    const renderPuzzleGrid = () => {
      if (!puzzleGrid) return;
      const themes = puzzleCatalog[activeCategory] || [];
      if (!themes.length) {
        puzzleGrid.innerHTML = '<div class="empty">No themes yet.</div>';
        return;
      }
      if (!activeThemeId) activeThemeId = themes[0]?.id;
      puzzleGrid.innerHTML = themes.map((t) => `
        <div class="puzzle-card" data-theme="${t.id}">
          <div class="puzzle-icon">${t.icon || "?"}</div>
          <h4>${t.title}</h4>
          <div class="puzzle-points">Points ${t.points}</div>
        </div>
      `).join("");
      puzzleGrid.querySelectorAll(".puzzle-card").forEach((card) => {
        card.addEventListener("click", () => {
          activeThemeId = card.dataset.theme;
          renderPuzzleDetail();
          loadPuzzleForTheme();
        });
      });
    };
  
    const renderPuzzleDetail = () => {
      if (!puzzleTitle || !puzzleLevels) return;
      const themes = puzzleCatalog[activeCategory] || [];
      const theme = themes.find((t) => t.id === activeThemeId) || themes[0];
      if (!theme) {
        puzzleTitle.textContent = "Select a theme";
        puzzleMeta.textContent = "Pick a category to see levels.";
        puzzleLevels.innerHTML = '<li><span>No theme selected</span><span class="badge tiny">0/0</span></li>';
        return;
      }
      puzzleTitle.textContent = theme.title;
      puzzleMeta.textContent = `Points ${theme.points}`;
      puzzleLevels.innerHTML = (theme.levels || []).map((lvl) => `
        <li data-theme="${theme.id}" data-level="${lvl}"><span>${lvl}</span><span class="badge tiny">0 passed</span></li>
      `).join("");
      puzzleLevels.querySelectorAll("li").forEach((li) => {
        li.addEventListener("click", () => {
          activeThemeId = li.dataset.theme;
          // If queen mating levels, show embed and load corresponding puzzle/theme
          if ((li.dataset.level || "").toLowerCase().includes("queen")) {
            const embed = document.getElementById("queenEmbedPanel");
            if (embed) embed.style.display = "";
          }
          renderPuzzleDetail();
          loadPuzzleForTheme();
        });
      });
    };
  
    const loadPuzzleForTheme = () => {
      if (!activeThemeId || !puzzlesPlayable.length) return;
      const idx = puzzlesPlayable.findIndex((p) => p.theme === activeThemeId);
      if (idx >= 0) {
        loadPuzzle(idx);
      }
    };
  
    const loadPuzzlesFromJson = async () => {
      try {
        const res = await fetch("/puzzles.json");
        if (!res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data) || !data.length) return;
        const mapped = data.map(mapJsonPuzzle);
        puzzlesPlayable.splice(0, puzzlesPlayable.length, ...mapped);
        loadPuzzle(0);
      } catch (_) {
        /* ignore fetch errors, keep fallback puzzles */
      }
    };

      const storedRole = localStorage.getItem("userRole") || "STUDENT";
      enforceRoleVisibility(storedRole);
    setGreeting({ email: storedEmailRaw });
    fetchUsers();
    fetchStudents();
    renderTabs();
    renderPuzzleGrid();
    renderPuzzleDetail();
    loadPuzzlesFromJson()
      .then(() => loadPuzzleForTheme());
    // Delay init to ensure scripts are loaded
    setTimeout(initPuzzleBoard, 50);
  
    // --- Analysis board with engine ---
    const updateAnalysisStatus = (msg) => { if (analysisStatus) analysisStatus.textContent = msg; };
  
    const initAnalysisBoard = () => {
      if (!analysisBoardEl || !window.Chessboard || !window.Chess) return;
      analysisGame = new Chess();
      analysisBoard = Chessboard("analysisBoard", {
        position: "start",
        draggable: true,
        pieceTheme,
        onDragStart: (_, piece) => {
          if (analysisGame.game_over()) return false;
          if (analysisGame.turn() === "w" && piece.startsWith("b")) return false;
          if (analysisGame.turn() === "b" && piece.startsWith("w")) return false;
        },
        onDrop: (source, target) => {
          const move = analysisGame.move({ from: source, to: target, promotion: "q" });
          if (!move) return "snapback";
          updateAnalysisStatus("Your move: " + move.san);
        },
        onSnapEnd: () => analysisBoard.position(analysisGame.fen())
      });
    };
  
    const loadAnalysisFen = (fen) => {
      if (!analysisGame || !analysisBoard) return;
      const ok = analysisGame.load(fen);
      if (!ok) { updateAnalysisStatus("Invalid FEN"); return; }
      analysisBoard.position(fen);
      updateAnalysisStatus("Position loaded.");
    };
  
    const callEngine = async () => {
      if (!analysisGame || !analysisBoard) return;
      const fen = analysisGame.fen();
      updateAnalysisStatus("Engine thinking...");
      try {
        const res = await fetch("https://chess-api.com/v1", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ fen })
        });
        const data = await res.json();
        const mv = data.move;
        if (!mv || mv.length < 4) { updateAnalysisStatus("Engine move missing"); return; }
        const move = analysisGame.move({ from: mv.slice(0,2), to: mv.slice(2,4), promotion: mv[4] });
        if (!move) { updateAnalysisStatus("Engine move invalid"); return; }
        analysisBoard.position(analysisGame.fen());
        updateAnalysisStatus(`Engine: ${move.san} ${data.score ? "(score: " + data.score + ")" : ""}`);
      } catch (e) {
        updateAnalysisStatus("Engine error. Check network/API.");
      }
    };
  
      if (logoutLink) {
        logoutLink.addEventListener("click", (e) => {
          e.preventDefault();
          localStorage.removeItem("userRole");
          localStorage.removeItem("userEmail");
          window.location.href = "index.html";
        });
      }
  
      if (analysisEngineBtn) analysisEngineBtn.addEventListener("click", callEngine);
    if (analysisResetBtn) analysisResetBtn.addEventListener("click", () => {
      if (!analysisGame || !analysisBoard) return;
      analysisGame.reset();
      analysisBoard.start();
      updateAnalysisStatus("Reset to start.");
    });
    if (analysisFenForm) analysisFenForm.addEventListener("submit", (e) => {
      e.preventDefault();
      const fen = analysisFenInput?.value?.trim();
      if (fen) loadAnalysisFen(fen);
    });
    if (analysisFromPuzzleBtn) analysisFromPuzzleBtn.addEventListener("click", () => {
      // Prefer current puzzleGame; fallback to first puzzle FEN
      if (puzzleGame) {
        loadAnalysisFen(puzzleGame.fen());
      } else if (puzzlesPlayable.length) {
        const p = puzzlesPlayable[0];
        loadAnalysisFen(p.fen);
      } else {
        updateAnalysisStatus("No puzzle loaded to import.");
      }
    });
    if (analysisFlipBtn) analysisFlipBtn.addEventListener("click", () => {
      if (analysisBoard) analysisBoard.flip();
    });
  
    setTimeout(initAnalysisBoard, 80);
});
