// PBMP Developer Workbench - Frontend logic (vanilla JS)

const api = {
  list: () => fetch("/api/tasks").then((r) => r.json()),
  meta: () => fetch("/api/meta").then((r) => r.json()),
  toggle: (id, key, done) => post(`/api/tasks/${id}/checklist`, { key, done }),
  approve: (id, stageKey) =>
    post(`/api/tasks/${id}/stages/${stageKey}/approve`, {
      actor: currentUser.id,
      role: currentUser.role,
    }),
  launch: (id) => post(`/api/tasks/${id}/launch`, {}),
  validate: (id) => post(`/api/tasks/${id}/validate`, { actor: "ci.bot" }),
  deploy: (id) =>
    post(`/api/tasks/${id}/deploy`, {
      actor: currentUser.id,
      role: currentUser.role,
    }),
};

function post(url, body) {
  return fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  }).then(async (r) => ({ ok: r.ok, data: await r.json() }));
}

let tasks = [];
let activeId = null;
let selectedStage = null;
let stageRoles = {};
let users = [];
let currentUser = { id: "ba.rekha", role: "Business Analyst" };

const $ = (id) => document.getElementById(id);
const CODING = "coding";

function task() {
  return tasks.find((t) => t.id === activeId);
}
function lifecyclePct(t) {
  const done = t.stages.filter((s) => s.status === "approved").length;
  return Math.round((done / t.stages.length) * 100);
}
function checklistPct(t) {
  const done = t.checklist.filter((c) => c.done).length;
  return Math.round((done / t.checklist.length) * 100);
}
function fmtTime(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  return d.toLocaleDateString() + " " + d.toLocaleTimeString().slice(0, 5);
}

function renderList() {
  const el = $("taskList");
  el.innerHTML = "";
  tasks.forEach((t) => {
    const p = lifecyclePct(t);
    const div = document.createElement("div");
    div.className = "task-item" + (t.id === activeId ? " active" : "");
    div.innerHTML = `
      <div class="id">${t.id} · ${t.module}</div>
      <div class="t">${t.title}</div>
      <div class="progress"><i style="width:${p}%"></i></div>
      <div class="pct">${p}% lifecycle complete</div>`;
    div.onclick = () => selectTask(t.id);
    el.appendChild(div);
  });
}

function selectTask(id) {
  activeId = id;
  const t = task();
  const activeStage = t.stages.find((s) => s.status === "active");
  selectedStage = activeStage ? activeStage.key : t.stages[0].key;
  renderList();
  renderDetail();
}

function renderLifecycle(t) {
  const el = $("lifecycle");
  el.innerHTML = "";
  t.stages.forEach((s, i) => {
    const div = document.createElement("div");
    div.className =
      "stage " + s.status + (s.key === selectedStage ? " selected" : "");
    const icon =
      s.status === "approved" ? "✓" : s.status === "active" ? "●" : "🔒";
    div.innerHTML = `
      <div class="st-num"><span>STAGE ${i + 1}</span><span class="dot">${icon}</span></div>
      <div class="st-name">${s.name}</div>
      <span class="st-status">${s.status}</span>`;
    div.onclick = () => {
      selectedStage = s.key;
      renderDetail();
    };
    el.appendChild(div);
  });
}

function renderStagePanel(t) {
  const s = t.stages.find((x) => x.key === selectedStage);
  const el = $("stagePanel");
  if (!s) {
    el.innerHTML = "";
    return;
  }
  const reqRole = stageRoles[s.key];
  const roleHint = reqRole
    ? `<div class="req-role">🔑 Approver role: ${reqRole}</div>`
    : "";
  let actionHtml = "";
  if (s.status === "approved") {
    actionHtml = `<div class="meta">✓ Approved by ${s.approvedBy} · ${fmtTime(
      s.approvedAt
    )}</div>`;
  } else if (s.key === CODING) {
    actionHtml = `<div class="meta">The Coding stage is completed below via "Open in Cursor" + commit/"Validate & Complete".</div>`;
  } else if (s.status === "active") {
    const canApprove = currentUser.role === reqRole;
    actionHtml = canApprove
      ? `<button class="approve-btn" id="approveBtn">Approve "${s.name}" →</button>`
      : `<button class="approve-btn" disabled>Only ${reqRole} can approve</button>`;
  } else {
    actionHtml = `<button class="approve-btn" disabled>Locked (approve the previous stage first)</button>`;
  }
  el.innerHTML = `
    <h4>${s.name} <span class="hint">— artifact</span></h4>
    <p>${s.artifact}</p>
    ${roleHint}
    ${actionHtml}`;

  const btn = $("approveBtn");
  if (btn)
    btn.onclick = async () => {
      const { ok, data } = await api.approve(t.id, s.key);
      if (!ok) return alert(data.error || "Approval failed");
      Object.assign(t, data);
      const next = t.stages.find((x) => x.status === "active");
      selectedStage = next ? next.key : s.key;
      renderList();
      renderDetail();
    };
}

function renderChecklist(t) {
  const ul = $("checklist");
  ul.innerHTML = "";
  t.checklist.forEach((c) => {
    const li = document.createElement("li");
    li.className = c.done ? "done" : "";
    li.innerHTML = `<span class="box">${c.done ? "✓" : ""}</span> ${c.label}`;
    li.onclick = async () => {
      const { data } = await api.toggle(t.id, c.key, !c.done);
      Object.assign(t, data);
      renderDetail();
    };
    ul.appendChild(li);
  });
}

function renderAudit(t) {
  const ul = $("audit");
  ul.innerHTML = "";
  [...t.audit].reverse().forEach((a) => {
    const li = document.createElement("li");
    li.innerHTML = `<span class="who">${a.actor}</span> ${a.action}
      <span class="when">${fmtTime(a.ts)}</span>`;
    ul.appendChild(li);
  });
}

function renderDetail() {
  const t = task();
  $("detailEmpty").classList.toggle("hidden", !!t);
  $("detailCard").classList.toggle("hidden", !t);
  if (!t) return;

  $("dModule").textContent = t.module;
  $("dLang").textContent = t.language;
  $("dTitle").textContent = t.title;
  $("dReq").textContent = `${t.requirement.id} — ${t.requirement.text}`;
  $("dObj").textContent = t.businessObject;
  $("dUml").textContent = t.umlClass;
  $("dApi").textContent = t.api;
  $("dSec").textContent = t.securityNotes;

  renderLifecycle(t);
  renderStagePanel(t);
  renderChecklist(t);
  renderAudit(t);

  // Coding gate: requirements..pseudocode approved?
  const order = ["requirements", "analysis", "design", "pseudocode"];
  const preApproved = order.every(
    (k) => t.stages.find((s) => s.key === k).status === "approved"
  );
  const ready = preApproved && checklistPct(t) === 100;
  const coding = t.stages.find((s) => s.key === CODING);
  const codingDone = coding.status === "approved";
  const codingStarted = coding.status !== "locked";

  const lb = $("launchBtn");
  lb.disabled = !ready || codingDone;
  lb.textContent = codingDone ? "Coding completed ✓" : "Open in Cursor";
  lb.onclick = () => doLaunch(t.id);

  const vb = $("validateBtn");
  vb.disabled = !codingStarted || codingDone;
  vb.textContent = codingDone ? "Validated ✓" : "Validate & Complete";
  vb.onclick = () => doValidate(t.id);

  // Deploy button: deployment stage active + role DevOps
  const deployment = t.stages.find((s) => s.key === "deployment");
  const deployDone = deployment.status === "approved";
  const deployReady = deployment.status === "active";
  const isDevOps = currentUser.role === stageRoles.deployment;
  const db = $("deployBtn");
  db.disabled = !deployReady || !isDevOps || deployDone;
  db.textContent = deployDone ? "Deployed ✓" : "🚀 Deploy to Production";
  db.onclick = () => doDeploy(t.id);

  $("launchHint").textContent = deployDone
    ? "🎉 Deployed to production! Lifecycle complete."
    : deployReady
    ? isDevOps
      ? "Deployment ready. DevOps can deploy."
      : "Only DevOps (Raj) can deploy — switch roles."
    : codingDone
    ? "Coding approved. Now Testing (QA) -> Deployment (DevOps)."
    : !preApproved
    ? "Approve the stages up to Design/Pseudocode first."
    : checklistPct(t) !== 100
    ? "Complete the pre-coding checklist."
    : "All ready! You can launch Cursor.";
}

async function doLaunch(id) {
  const t = task();
  const lb = $("launchBtn");
  lb.disabled = true;
  lb.textContent = "Launching...";
  const { ok, data } = await api.launch(id);
  const box = $("result");
  box.classList.remove("hidden");
  $("checksBox").innerHTML = "";
  $("gitHookBox").innerHTML = "";
  $("openOptions").innerHTML = "";
  $("previewWrap").style.display = "block";

  if (!ok) {
    box.style.borderColor = "var(--danger)";
    $("resultTitle").textContent = "⛔ Launch blocked";
    $("resultMsg").textContent =
      (data.error || "Error") +
      " " +
      [...(data.pendingStages || []), ...(data.pending || [])].join(", ");
    $("resultCmd").textContent = "";
    $("resultPreview").textContent = "";
  } else {
    box.style.borderColor = "var(--accent-2)";
    $("resultTitle").textContent = "✅ Ready to open in Cursor";
    $("resultMsg").textContent = data.message;
    $("resultCmd").textContent = "";
    renderOpenOptions(data);
    if (data.gitHook && data.gitHook.installed) {
      $("gitHookBox").innerHTML = `<b>🪝 Git post-commit hook installed:</b> ${data.gitHook.hookPath}<br>A <b>git commit</b> in the project runs PBMP validation automatically.`;
    } else if (data.gitHook) {
      $("gitHookBox").innerHTML = `<b>🪝 Git hook not installed:</b> ${data.gitHook.error || "unknown"}`;
    }
    $("resultPreview").textContent = data.contextPreview;
    // On the same machine, auto-open via the official Cursor deep link
    if (data.isLocal && data.deepLink) {
      try {
        window.location.href = data.deepLink;
      } catch (e) {}
    }
    tasks = await api.list();
  }
  renderDetail();
}

// Build the three ways to open ONLY this project in Cursor on the user's machine
function renderOpenOptions(data) {
  const el = $("openOptions");
  const localNote = data.isLocal
    ? `<small>You are on the same machine as the server — Cursor should open automatically. If not, click the button.</small>`
    : `<small>This deep link opens the folder only if it already exists on your machine (use Git or ZIP below first).</small>`;

  el.innerHTML = `
    <div class="open-opt">
      <div class="ttl">A) One-click <span>(opens only this folder; no login needed)</span></div>
      <a class="btn-link" href="${data.deepLink}">🖱️ Open in Cursor</a>
      ${localNote}
    </div>
    <div class="open-opt">
      <div class="ttl">B) Git <span>(clone just this project, then open it)</span></div>
      <div class="cmd-row">
        <code class="cmd" id="cloneCmd">${data.cloneCommand}</code>
        <button class="copy-btn" id="copyClone">Copy</button>
      </div>
      <small>Run this in your terminal. It clones only this repo and opens that one folder in Cursor.</small>
    </div>
    <div class="open-opt">
      <div class="ttl">C) ZIP <span>(download just this project)</span></div>
      <a class="dl-btn" href="${data.downloadUrl}" download>⬇ Download ${data.folder}.zip</a>
      <small>Extract it, then in that folder run <code>cursor .</code> (or drag the folder into Cursor) — only that folder opens.</small>
    </div>`;

  const copyBtn = $("copyClone");
  if (copyBtn) {
    copyBtn.onclick = () => {
      navigator.clipboard?.writeText(data.cloneCommand);
      copyBtn.textContent = "Copied!";
      setTimeout(() => (copyBtn.textContent = "Copy"), 1500);
    };
  }
}

async function doValidate(id) {
  const vb = $("validateBtn");
  vb.disabled = true;
  vb.textContent = "Validating...";
  const { data } = await api.validate(id);
  const box = $("result");
  box.classList.remove("hidden");
  $("previewWrap").style.display = "none";
  $("resultCmd").textContent = "";
  $("gitHookBox").innerHTML = "";
  $("openOptions").innerHTML = "";
  $("deployLog").classList.add("hidden");
  $("engineBox").innerHTML = data.engine
    ? `<b>⚙️ Validation engine:</b> ${
        data.engine === "php" ? "PBMP PHP backend (php -l + scans)" : data.engine
      }`
    : "";

  const checksHtml = (data.checks || [])
    .map(
      (c) =>
        `<div class="chk ${c.pass ? "pass" : "fail"}">${
          c.pass ? "✓" : "✗"
        } ${c.name} — <span style="opacity:.8">${c.detail}</span></div>`
    )
    .join("");
  $("checksBox").innerHTML = checksHtml;

  if (data.passed) {
    box.style.borderColor = "var(--accent-2)";
    $("resultTitle").textContent = "✅ Validation Passed — Coding stage approved";
    $("resultMsg").textContent =
      "Standard checks pass. Coding stage approved -> Testing stage unlock.";
  } else {
    box.style.borderColor = "var(--danger)";
    $("resultTitle").textContent = "⛔ Validation Failed";
    $("resultMsg").textContent = "Some checks failed. Fix them and validate again.";
  }
  tasks = await api.list();
  renderDetail();
}

async function doDeploy(id) {
  const db = $("deployBtn");
  db.disabled = true;
  db.textContent = "Deploying...";
  const { ok, data } = await api.deploy(id);
  const box = $("result");
  box.classList.remove("hidden");
  $("previewWrap").style.display = "none";
  $("resultCmd").textContent = "";
  $("gitHookBox").innerHTML = "";
  $("openOptions").innerHTML = "";
  $("engineBox").innerHTML = "";
  $("checksBox").innerHTML = "";

  const logEl = $("deployLog");
  logEl.classList.remove("hidden");
  logEl.textContent = data.log || data.error || "(no output)";

  if (ok && data.deployed) {
    box.style.borderColor = "var(--accent-2)";
    $("resultTitle").textContent = "🚀 Deployed to Production";
    $("resultMsg").textContent = "Release: " + (data.releasePath || "-");
  } else {
    box.style.borderColor = "var(--danger)";
    $("resultTitle").textContent = "⛔ Deploy blocked / failed";
    $("resultMsg").textContent =
      (data.error || "Deploy failed") +
      " " +
      (data.pendingStages ? data.pendingStages.join(", ") : "");
  }
  tasks = await api.list();
  renderDetail();
}

function setupUserSelector() {
  const sel = $("userSelect");
  sel.innerHTML = "";
  users.forEach((u) => {
    const opt = document.createElement("option");
    opt.value = u.id;
    opt.textContent = `${u.name} (${u.role})`;
    sel.appendChild(opt);
  });
  const applyUser = () => {
    currentUser = users.find((u) => u.id === sel.value) || users[0];
    $("userRole").textContent = currentUser.role;
    if (activeId) renderDetail();
  };
  sel.onchange = applyUser;
  sel.value = currentUser.id;
  applyUser();
}

async function init() {
  const meta = await api.meta();
  stageRoles = meta.stageRoles || {};
  users = meta.users || [];
  if (users.length) currentUser = users[0];
  setupUserSelector();
  tasks = await api.list();
  renderList();
}

init();
