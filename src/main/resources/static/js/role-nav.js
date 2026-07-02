(function () {
  const role = (localStorage.getItem("userRole") || "STUDENT").toUpperCase();
  const current = document.body.dataset.activeNav || "";
  const nav = document.querySelector(".sidebar-nav");
  if (!nav) return;

  const items = role === "ADMIN"
    ? [
        ["Overview", "dashboard.html?view=admin-dashboard", "overview"],
        ["Users", "dashboard.html?view=admin-dashboard", "users"],
        ["Class Applications", "admin-class-applications.html", "class-applications"],
        ["Class Plans", "classes.html", "class-plans"],
        ["Coach Profiles", "know-our-coaches.html", "coaches"],
        ["Achievements", "achievements.html", "achievements"],
        ["Messages", "dashboard.html?view=messages", "messages"],
        ["Reports", "dashboard.html?view=reports", "reports"]
      ]
    : role === "COACH"
    ? [
        ["Overview", "dashboard.html?view=coach-dashboard", "overview"],
        ["My Students", "dashboard.html?view=students", "students"],
        ["Assigned Applications", "coach-class-applications.html", "assigned-applications"],
        ["Sessions", "dashboard.html?view=classes", "sessions"],
        ["Class Plans", "classes.html", "class-plans"],
        ["Lessons", "dashboard.html?view=lesson-management", "lessons"],
        ["Puzzle Upload", "dashboard.html?view=puzzle-upload", "puzzle-upload"],
        ["Puzzle Editor", "puzzle-editor.html", "puzzle-editor"],
        ["Messages", "dashboard.html?view=messages", "messages"],
        ["Reports", "dashboard.html?view=reports", "reports"]
      ]
    : [
        ["Home", "dashboard.html?view=dashboard", "home"],
        ["My Progress", "dashboard.html?view=progress", "progress"],
        ["My Payments", "dashboard.html?view=payments", "payments"],
        ["My Classes", "dashboard.html?view=dashboard&focus=studentSessionPlansList", "classes"],
        ["Class Plans", "classes.html", "class-plans"],
        ["Messages", "dashboard.html?view=messages", "messages"],
        ["Puzzle Library", "puzzle.html", "puzzle-library"],
        ["Game Analysis", "analysis.html", "analysis"],
        ["My Games", "my-games.html", "my-games"],
        ["Class Applications", "my-class-applications.html", "my-class-applications"],
        ["Know Our Coaches", "know-our-coaches.html", "coaches"],
        ["Achievements", "achievements.html", "achievements"],
        ["Lessons", "dashboard.html?view=lessons", "lessons"]
      ];

  nav.innerHTML = `
    <div class="nav-section-title">${role === "ADMIN" ? "Admin" : role === "COACH" ? "Coach" : "Student"}</div>
    ${items.map(([label, href, key]) => `<a class="nav-link ${key === current ? "active" : ""}" href="${href}">${label}</a>`).join("")}
  `;

  const footer = document.querySelector(".sidebar-footer");
  if (footer) {
    footer.innerHTML = `
      <a class="nav-link ${current === "profile" ? "active" : ""}" href="dashboard.html?view=profile">Profile</a>
      <button class="sidebar-logout" type="button">Logout</button>
    `;
    footer.querySelector(".sidebar-logout")?.addEventListener("click", () => {
      localStorage.removeItem("authToken");
      window.location.href = "index.html";
    });
  }
})();
