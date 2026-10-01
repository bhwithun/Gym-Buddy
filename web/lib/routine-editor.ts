import { escapeHtml } from "./http";
import { revisionMark } from "./revision";

export function renderRoutineEditor(options: {
  apiBase: string;
  calendarHref: string;
  slug: string;
  signedIn?: boolean;
  accessToken?: string;
  embedded?: boolean;
}): string {
  const embedded = options.embedded === true;
  const body = `<div class="routine-root">
  <style>
    .routine-root { color: #eee; }
    .routine-root a { color: #00ffff; }
    .routine-root h1 { color: #f9f72e; font-size: 28px; margin: 0; }
    .layout { max-width: 900px; margin: 0 auto; padding: 0 0 48px; }
    .routine-root .card { background: #1e1e1e; border: 1px solid #444; border-radius: 16px; padding: 14px; }
    .versions { display: flex; flex-direction: column; gap: 8px; }
    .versions button.new { background: #5B2C6F; color: #fff; border: 0; border-radius: 10px; padding: 10px; cursor: pointer; font-size: 14px; }
    .ver { text-align: left; background: #2a2a2a; color: #eee; border: 1px solid #444; border-radius: 10px; padding: 10px; cursor: pointer; }
    .ver.active { border-color: #f9f72e; background: #3a2a12; }
    .ver .name { font-weight: 600; }
    .ver .meta { color: #9e9e9e; font-size: 12px; margin-top: 4px; }
    .toolbar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; }
    .routine-root input, .routine-root select, .routine-root textarea, .routine-root button { font: inherit; }
    .routine-root input[type=text], .routine-root input[type=number], .routine-root textarea, .routine-root select {
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
    .ex { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; grid-template-areas: "head head head" "nums notes actions"; gap: 8px 10px; border: 1px solid #333; border-radius: 12px; padding: 10px; margin-bottom: 10px; background: #191919; align-items: stretch; }
    .ex-head { grid-area: head; display: flex; gap: 8px; align-items: center; }
    .ex-head input[type=text] { flex: 1; min-width: 0; }
    .ex-head select { width: 110px; }
    .ex-nums { grid-area: nums; display: flex; flex-direction: column; gap: 6px; width: 112px; }
    .ex-notes { grid-area: notes; display: flex; }
    .ex-notes textarea { width: 100%; height: 100%; min-height: 108px; margin: 0; resize: vertical; }
    .ex-actions { grid-area: actions; display: flex; flex-direction: column; gap: 6px; }
    .ex-actions button { padding: 8px; min-width: 40px; }
    .ex-actions button.trash { color: #ff8a9a; display: inline-flex; align-items: center; justify-content: center; }
    .ex-actions svg { display: block; }
    .field label { display: block; font-size: 11px; text-transform: uppercase; letter-spacing: .04em; color: #bdbdbd; margin-bottom: 4px; }
    .field.weight label, .field.weight input { color: #00FFFF; }
    .field.reps label, .field.reps input { color: #F1C40F; }
    .field.sets label, .field.sets input { color: #00FF00; }
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
  <div class="layout">
    <section class="card">
      <div class="toolbar">
        <button type="button" class="primary" id="saveBtn">Save</button>
        <button type="button" id="pasteBtn">Edit as JSON</button>
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
      <h2>Profile token</h2>
      <p>Paste the token from your connect page. It is the same value saved in the app.</p>
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
    const API_BASE = ${JSON.stringify(options.apiBase)};
    const TOKEN_KEY = ${JSON.stringify(`gb_token_${options.slug}`)};
    const signedIn = ${options.signedIn === true ? "true" : "false"};
    let token = ${JSON.stringify(options.accessToken ?? "")} || sessionStorage.getItem(TOKEN_KEY) || "";
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

    function renderVersions() {}

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
        var head = document.createElement("div");
        head.className = "ex-head";
        var title = document.createElement("input");
        title.type = "text";
        title.value = ex.title;
        title.placeholder = "Exercise name";
        title.addEventListener("input", function () { ex.title = title.value; });
        var rating = document.createElement("select");
        ["easy","good","hard"].forEach(function (r) {
          var o = document.createElement("option");
          o.value = r; o.textContent = r;
          if (ex.easyGoodOrHard === r) o.selected = true;
          rating.appendChild(o);
        });
        rating.addEventListener("change", function () { ex.easyGoodOrHard = rating.value; });
        head.appendChild(title);
        head.appendChild(rating);
        card.appendChild(head);

        var nums = document.createElement("div");
        nums.className = "ex-nums";
        function num(label, key) {
          var inp = document.createElement("input");
          inp.type = "number";
          inp.min = "0";
          inp.value = String(ex[key]);
          inp.addEventListener("input", function () { ex[key] = Number(inp.value || 0); });
          var wrap = field(label, inp);
          wrap.classList.add(key);
          nums.appendChild(wrap);
        }
        num("Weight", "weight");
        num("Reps", "reps");
        num("Sets", "sets");
        card.appendChild(nums);

        var notesWrap = document.createElement("div");
        notesWrap.className = "ex-notes";
        var notes = document.createElement("textarea");
        notes.placeholder = "Notes / cues";
        notes.value = ex.notes || "";
        notes.addEventListener("input", function () { ex.notes = notes.value; });
        notesWrap.appendChild(notes);
        card.appendChild(notesWrap);

        var actions = document.createElement("div");
        actions.className = "ex-actions";
        function act(label, fn, icon) {
          var b = document.createElement("button");
          b.type = "button";
          if (icon) {
            b.className = "trash";
            b.setAttribute("aria-label", label);
            b.title = label;
            b.innerHTML = icon;
          } else {
            b.textContent = label;
          }
          b.addEventListener("click", fn);
          actions.appendChild(b);
        }
        act("Up", function () { if (idx === 0) return; list.splice(idx, 1); list.splice(idx - 1, 0, ex); renderDay(); });
        act("Down", function () { if (idx >= list.length - 1) return; list.splice(idx, 1); list.splice(idx + 1, 0, ex); renderDay(); });
        act("Remove", function () { list.splice(idx, 1); renderDay(); renderTabs(); },
          '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="currentColor" d="M9 3h6l1 2h5v2H3V5h5l1-2zm1 6h2v9h-2V9zm4 0h2v9h-2V9zM7 9h2v9H7V9z"/></svg>');
        card.appendChild(actions);
        root.appendChild(card);
      });
    }

    function blankExercise() {
      return { title: "", weight: 0, reps: 10, sets: 3, notes: "", easyGoodOrHard: "good" };
    }

    function applyRecord(rec) {
      days = rec.days || emptyDays();
      renderTabs();
      renderDay();
    }

    function showEmpty() {
      days = emptyDays();
      activeDay = "Mon";
      renderTabs();
      renderDay();
      setStatus("No routine stored yet.");
    }

    function save() {
      var missing = days.some(function (d) {
        return d.exercises.some(function (ex) { return !String(ex.title).trim(); });
      });
      if (missing) { setStatus("Every exercise needs a title.", "err"); return; }
      setStatus("Saving…");
      api(API_BASE + "/routines", { method: "PUT", body: { days: days } }).then(function (rec) {
        applyRecord(rec);
        setStatus("Saved.", "ok");
      }).catch(function (err) { setStatus(err.message, "err"); });
    }

    document.getElementById("saveBtn").onclick = save;
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
        document.getElementById("pasteModal").classList.remove("show");
        renderTabs();
        renderDay();
        setStatus("JSON applied. Save to keep it on your profile.", "ok");
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
      return api(API_BASE + "/routines").then(function (data) {
        showGate(false);
        if (data && data.routine) applyRecord(data.routine);
        else showEmpty();
      });
    }

    if (signedIn) {
      boot().catch(function (err) { setStatus(err.message, "err"); });
    } else {
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
    }
  </script>
</div>`;
  if (embedded) return body;
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="referrer" content="no-referrer" />
  <title>Gym Buddy routines</title>
</head>
<body style="margin:0;background:#121212;font-family:ui-sans-serif,system-ui,sans-serif">
${revisionMark()}
${body}
</body>
</html>`;
}
