export function renderRoutineEditor(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Gym Buddy routines</title>
  <style>
    :root { color-scheme: dark; }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: ui-sans-serif, system-ui, sans-serif; background: #121212; color: #eee; }
    a { color: #00ffff; }
    header { display: flex; justify-content: space-between; align-items: baseline; gap: 16px; padding: 20px 20px 8px; max-width: 1200px; margin: 0 auto; }
    h1 { color: #f9f72e; font-size: 28px; margin: 0; }
    .layout { display: grid; grid-template-columns: 240px 1fr; gap: 16px; max-width: 1200px; margin: 0 auto; padding: 12px 20px 48px; }
    @media (max-width: 800px) { .layout { grid-template-columns: 1fr; } }
    .card { background: #1e1e1e; border: 1px solid #444; border-radius: 16px; padding: 14px; }
    .versions { display: flex; flex-direction: column; gap: 8px; }
    .versions button.new { background: #5B2C6F; color: #fff; border: 0; border-radius: 10px; padding: 10px; cursor: pointer; font-size: 14px; }
    .ver { text-align: left; background: #2a2a2a; color: #eee; border: 1px solid #444; border-radius: 10px; padding: 10px; cursor: pointer; }
    .ver.active { border-color: #f9f72e; background: #3a2a12; }
    .ver .name { font-weight: 600; }
    .ver .meta { color: #9e9e9e; font-size: 12px; margin-top: 4px; }
    .toolbar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; }
    input, select, textarea, button { font: inherit; }
    input[type=text], input[type=number], textarea, select {
      background: #121212; color: #eee; border: 1px solid #555; border-radius: 8px; padding: 8px 10px;
    }
    .name { flex: 1; min-width: 180px; }
    .toolbar button, .ex-actions button, .modal-actions button {
      background: #2a2a2a; color: #f9f72e; border: 1px solid #555; border-radius: 10px; padding: 8px 12px; cursor: pointer;
    }
    .toolbar button.primary { background: #5B2C6F; color: #fff; border-color: #7D3C98; }
    .toolbar button.danger { color: #ff8a9a; }
    .days { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0 14px; }
    .days button { background: #2a2a2a; color: #ddd; border: 1px solid #444; border-radius: 999px; padding: 6px 12px; cursor: pointer; }
    .days button.active { background: #14331c; border-color: #00aa44; color: #e8ffe8; }
    .rest { color: #9e9e9e; font-style: italic; margin: 8px 0 12px; }
    .ex { border: 1px solid #333; border-radius: 12px; padding: 10px; margin-bottom: 10px; background: #191919; }
    .ex-top { display: grid; grid-template-columns: 1fr auto; gap: 8px; align-items: start; }
    .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 8px; }
    @media (max-width: 700px) { .grid { grid-template-columns: 1fr 1fr; } }
    .field label { display: block; font-size: 11px; text-transform: uppercase; letter-spacing: .04em; color: #bdbdbd; margin-bottom: 4px; }
    textarea { width: 100%; min-height: 64px; margin-top: 8px; resize: vertical; }
    .status { min-height: 1.4em; color: #9e9e9e; margin: 8px 0; }
    .status.err { color: #ff8a9a; }
    .status.ok { color: #00ff88; }
    .gate { position: fixed; inset: 0; background: #121212ee; display: none; align-items: center; justify-content: center; padding: 24px; }
    .gate.show { display: flex; }
    .gate .box { width: min(420px, 100%); }
    .hidden { display: none !important; }
    .modal {
      position: fixed; inset: 0; background: #000a; display: none; align-items: center; justify-content: center; padding: 16px;
    }
    .modal.show { display: flex; }
    .modal .box { width: min(720px, 100%); max-height: 90vh; overflow: auto; }
    .modal textarea { min-height: 420px; font-family: ui-monospace, monospace; font-size: 13px; }
  </style>
</head>
<body>
  <header>
    <h1>Routines</h1>
    <p><a href="/">Gym time calendar</a></p>
  </header>
  <div class="layout">
    <aside class="card versions">
      <button type="button" class="new" id="newBtn">New routine</button>
      <div id="versionList"></div>
    </aside>
    <section class="card">
      <div class="toolbar">
        <input class="name" id="nameInput" type="text" placeholder="Name this version" />
        <button type="button" class="primary" id="saveBtn">Save</button>
        <button type="button" id="saveAsBtn">Save as new</button>
        <button type="button" id="pasteBtn">Edit as JSON</button>
        <button type="button" class="danger" id="deleteBtn">Delete</button>
      </div>
      <div class="status" id="status"></div>
      <div class="days" id="dayTabs"></div>
      <p class="rest hidden" id="restHint">Rest day — add an exercise to train.</p>
      <div id="exercises"></div>
      <button type="button" id="addExBtn">Add exercise</button>
    </section>
  </div>
  <div class="gate" id="gate">
    <div class="box card">
      <h2>Worker token</h2>
      <p>Use the same token as About → Worker token.</p>
      <input id="tokenInput" type="text" placeholder="Token" style="width:100%;margin:12px 0;" />
      <button type="button" class="primary" id="tokenBtn">Continue</button>
      <p class="status err" id="tokenErr"></p>
    </div>
  </div>
  <div class="modal" id="pasteModal">
    <div class="box card">
      <h2>Edit as JSON</h2>
      <p>This is the current week. Edit it, then apply to the form.</p>
      <textarea id="pasteText"></textarea>
      <div class="modal-actions" style="display:flex;gap:8px;margin-top:12px;">
        <button type="button" class="primary" id="pasteApply">Apply</button>
        <button type="button" id="pasteClose">Cancel</button>
      </div>
    </div>
  </div>
  <script>
    const DAYS = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
    const TOKEN_KEY = "gb_token";
    let token = sessionStorage.getItem(TOKEN_KEY) || "";
    let authRequired = false;
    let versions = [];
    let currentId = null;
    let days = emptyDays();
    let activeDay = "Mon";

    function emptyDays() {
      return DAYS.map(function (d) { return { dayOfWeek: d, exercises: [] }; });
    }
    function dayOf(name) {
      return days.find(function (d) { return d.dayOfWeek === name; });
    }
    function setStatus(msg, kind) {
      var el = document.getElementById("status");
      el.textContent = msg || "";
      el.className = "status" + (kind ? " " + kind : "");
    }
    function headers() {
      var h = { "Content-Type": "application/json" };
      if (token) h.Authorization = "Bearer " + token;
      return h;
    }
    function api(path, opts) {
      opts = opts || {};
      return fetch(path, {
        method: opts.method || "GET",
        headers: headers(),
        body: opts.body ? JSON.stringify(opts.body) : undefined
      }).then(function (res) {
        return res.text().then(function (text) {
          var data = null;
          try { data = text ? JSON.parse(text) : null; } catch (e) { data = { error: text }; }
          if (res.status === 401) {
            sessionStorage.removeItem(TOKEN_KEY);
            token = "";
            showGate(true);
            throw new Error("unauthorized");
          }
          if (!res.ok) throw new Error((data && data.error) || ("HTTP " + res.status));
          return data;
        });
      });
    }

    function showGate(on) {
      document.getElementById("gate").classList.toggle("show", on);
    }

    function renderVersions() {
      var root = document.getElementById("versionList");
      root.innerHTML = "";
      if (!versions.length) {
        var empty = document.createElement("p");
        empty.className = "rest";
        empty.textContent = "No named versions yet.";
        root.appendChild(empty);
        return;
      }
      versions.forEach(function (v) {
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "ver" + (v.id === currentId ? " active" : "");
        var name = document.createElement("div");
        name.className = "name";
        name.textContent = v.name;
        var meta = document.createElement("div");
        meta.className = "meta";
        meta.textContent = (v.updatedAt || "").replace("T", " ").slice(0, 16);
        btn.appendChild(name);
        btn.appendChild(meta);
        btn.addEventListener("click", function () { loadVersion(v.id); });
        root.appendChild(btn);
      });
    }

    function renderTabs() {
      var root = document.getElementById("dayTabs");
      root.innerHTML = "";
      DAYS.forEach(function (d) {
        var btn = document.createElement("button");
        btn.type = "button";
        btn.textContent = d + (dayOf(d).exercises.length ? " (" + dayOf(d).exercises.length + ")" : "");
        if (d === activeDay) btn.className = "active";
        btn.addEventListener("click", function () { activeDay = d; renderDay(); renderTabs(); });
        root.appendChild(btn);
      });
    }

    function field(label, el) {
      var wrap = document.createElement("div");
      wrap.className = "field";
      var lab = document.createElement("label");
      lab.textContent = label;
      wrap.appendChild(lab);
      wrap.appendChild(el);
      return wrap;
    }

    function renderDay() {
      var list = dayOf(activeDay).exercises;
      document.getElementById("restHint").classList.toggle("hidden", list.length > 0);
      var root = document.getElementById("exercises");
      root.innerHTML = "";
      list.forEach(function (ex, idx) {
        var card = document.createElement("div");
        card.className = "ex";
        var top = document.createElement("div");
        top.className = "ex-top";
        var title = document.createElement("input");
        title.type = "text";
        title.value = ex.title;
        title.placeholder = "Exercise name";
        title.addEventListener("input", function () { ex.title = title.value; });
        var actions = document.createElement("div");
        actions.className = "ex-actions";
        function act(label, fn) {
          var b = document.createElement("button");
          b.type = "button";
          b.textContent = label;
          b.addEventListener("click", fn);
          actions.appendChild(b);
        }
        act("Up", function () { if (idx === 0) return; list.splice(idx, 1); list.splice(idx - 1, 0, ex); renderDay(); });
        act("Down", function () { if (idx >= list.length - 1) return; list.splice(idx, 1); list.splice(idx + 1, 0, ex); renderDay(); });
        act("Remove", function () { list.splice(idx, 1); renderDay(); renderTabs(); });
        top.appendChild(title);
        top.appendChild(actions);
        card.appendChild(top);

        var grid = document.createElement("div");
        grid.className = "grid";
        function num(label, key) {
          var inp = document.createElement("input");
          inp.type = "number";
          inp.min = "0";
          inp.value = String(ex[key]);
          inp.addEventListener("input", function () { ex[key] = Number(inp.value || 0); });
          grid.appendChild(field(label, inp));
        }
        num("Weight", "weight");
        num("Reps", "reps");
        num("Sets", "sets");
        var rating = document.createElement("select");
        ["easy","good","hard"].forEach(function (r) {
          var o = document.createElement("option");
          o.value = r; o.textContent = r;
          if (ex.easyGoodOrHard === r) o.selected = true;
          rating.appendChild(o);
        });
        rating.addEventListener("change", function () { ex.easyGoodOrHard = rating.value; });
        grid.appendChild(field("Rating", rating));
        card.appendChild(grid);

        var notes = document.createElement("textarea");
        notes.placeholder = "Notes / cues";
        notes.value = ex.notes || "";
        notes.addEventListener("input", function () { ex.notes = notes.value; });
        card.appendChild(notes);
        root.appendChild(card);
      });
    }

    function blankExercise() {
      return { title: "", weight: 0, reps: 10, sets: 3, notes: "", easyGoodOrHard: "good" };
    }

    function applyRecord(rec) {
      currentId = rec.id || null;
      document.getElementById("nameInput").value = rec.name || "";
      days = rec.days;
      renderVersions();
      renderTabs();
      renderDay();
    }

    function newRoutine() {
      currentId = null;
      document.getElementById("nameInput").value = "";
      days = emptyDays();
      activeDay = "Mon";
      renderVersions();
      renderTabs();
      renderDay();
      setStatus("New routine — name it and save.");
    }

    function payload(asNew) {
      return {
        id: asNew ? undefined : (currentId || undefined),
        name: document.getElementById("nameInput").value.trim(),
        days: days,
        overwriteByName: !asNew
      };
    }

    function save(asNew) {
      var body = payload(asNew);
      if (!body.name) { setStatus("Name this version first.", "err"); return; }
      var missing = days.some(function (d) {
        return d.exercises.some(function (ex) { return !String(ex.title).trim(); });
      });
      if (missing) { setStatus("Every exercise needs a title.", "err"); return; }
      setStatus("Saving…");
      var method = (!asNew && currentId) ? "PUT" : "POST";
      var path = (!asNew && currentId) ? "/routines/" + encodeURIComponent(currentId) : "/routines";
      api(path, { method: method, body: body }).then(function (rec) {
        return refreshList().then(function () {
          applyRecord(rec);
          setStatus("Saved “" + rec.name + "”.", "ok");
        });
      }).catch(function (err) { setStatus(err.message, "err"); });
    }

    function loadVersion(id) {
      setStatus("Loading…");
      api("/routines/" + encodeURIComponent(id)).then(function (rec) {
        applyRecord(rec);
        setStatus("Loaded “" + rec.name + "”.");
      }).catch(function (err) { setStatus(err.message, "err"); });
    }

    function refreshList() {
      return api("/routines").then(function (data) {
        versions = (data && data.routines) || [];
        renderVersions();
      });
    }

    document.getElementById("newBtn").onclick = newRoutine;
    document.getElementById("saveBtn").onclick = function () { save(false); };
    document.getElementById("saveAsBtn").onclick = function () { save(true); };
    document.getElementById("deleteBtn").onclick = function () {
      if (!currentId) { setStatus("Nothing to delete.", "err"); return; }
      if (!confirm("Delete this named version?")) return;
      api("/routines/" + encodeURIComponent(currentId), { method: "DELETE" }).then(function () {
        return refreshList().then(function () {
          newRoutine();
          setStatus("Deleted.", "ok");
        });
      }).catch(function (err) { setStatus(err.message, "err"); });
    };
    document.getElementById("addExBtn").onclick = function () {
      dayOf(activeDay).exercises.push(blankExercise());
      renderDay();
      renderTabs();
    };
    document.getElementById("pasteBtn").onclick = function () {
      document.getElementById("pasteText").value = JSON.stringify(days, null, 2);
      document.getElementById("pasteModal").classList.add("show");
    };
    document.getElementById("pasteClose").onclick = function () {
      document.getElementById("pasteModal").classList.remove("show");
    };
    document.getElementById("pasteApply").onclick = function () {
      try {
        var parsed = JSON.parse(document.getElementById("pasteText").value);
        var incoming = Array.isArray(parsed) ? parsed : (parsed && parsed.days);
        if (!Array.isArray(incoming) || incoming.length !== 7) throw new Error("Need an array of 7 days");
        days = DAYS.map(function (name) {
          var found = incoming.find(function (d) {
            var n = String(d.dayOfWeek || "").slice(0, 3);
            return n.toLowerCase() === name.toLowerCase();
          });
          var exercises = ((found && found.exercises) || []).map(function (ex) {
            return {
              title: ex.title || "",
              weight: Number(ex.weight || 0),
              reps: Number(ex.reps || 10),
              sets: Number(ex.sets || 3),
              notes: ex.notes || "",
              easyGoodOrHard: ex.easyGoodOrHard || ex.rating || "good"
            };
          });
          return { dayOfWeek: name, exercises: exercises };
        });
        if (parsed && parsed.name && !document.getElementById("nameInput").value) {
          document.getElementById("nameInput").value = parsed.name;
        }
        document.getElementById("pasteModal").classList.remove("show");
        renderTabs();
        renderDay();
        setStatus("JSON applied. Save the version to keep it on the worker.", "ok");
      } catch (err) {
        setStatus(err.message || "Invalid JSON", "err");
      }
    };
    document.getElementById("tokenBtn").onclick = function () {
      token = document.getElementById("tokenInput").value.trim();
      sessionStorage.setItem(TOKEN_KEY, token);
      document.getElementById("tokenErr").textContent = "";
      boot().catch(function (err) {
        document.getElementById("tokenErr").textContent = err.message;
      });
    };

    function boot() {
      return api("/routines").then(function (data) {
        showGate(false);
        versions = (data && data.routines) || [];
        if (versions.length) return loadVersion(versions[0].id);
        newRoutine();
      });
    }

    fetch("/health").then(function (r) { return r.json(); }).then(function (h) {
      authRequired = !!h.authRequired;
      if (authRequired && !token) { showGate(true); return; }
      boot().catch(function (err) {
        if (authRequired) {
          showGate(true);
          document.getElementById("tokenErr").textContent = err.message;
        } else {
          setStatus(err.message, "err");
        }
      });
    }).catch(function () { boot(); });
  </script>
</body>
</html>`;
}
