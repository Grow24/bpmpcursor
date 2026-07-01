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
  cloudAgent: (id) => post(`/api/tasks/${id}/cloud-agent`, {}),
  cloudStatus: (agentId, runId) =>
    fetch(`/api/cursor/agents/${agentId}/runs/${runId}`).then(async (r) => ({
      ok: r.ok,
      data: await r.json(),
    })),
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

let pendingLaunchData = null;
const LOCAL_ROOT_KEY = "pbmpProjectsRoot";

function getLocalProjectsRoot() {
  return (localStorage.getItem(LOCAL_ROOT_KEY) || "").trim().replace(/\/+$/, "");
}

function saveLocalProjectsRoot(path) {
  const cleaned = (path || "").trim().replace(/\/+$/, "");
  if (cleaned) localStorage.setItem(LOCAL_ROOT_KEY, cleaned);
  else localStorage.removeItem(LOCAL_ROOT_KEY);
  const input = $("localProjectsRoot");
  if (input) input.value = cleaned;
  return cleaned;
}

function localProjectPath(folder) {
  const root = getLocalProjectsRoot();
  if (!root) return "";
  return `${root}/${folder}`;
}

function openFolderInCursor(absPath) {
  const normalized = absPath.replace(/\\/g, "/");
  // Cursor registers its own cursor:// protocol handler. Using vscode://
  // would open Visual Studio Code instead.
  const uri = normalized.startsWith("/")
    ? `cursor://file${normalized}`
    : `cursor://file/${normalized}`;
  window.location.href = uri;
}

function autoDownloadZip(url, folder) {
  const a = document.createElement("a");
  a.href = url;
  a.download = `${folder}.zip`;
  document.body.appendChild(a);
  a.click();
  a.remove();
}

function setupLocalPathControls() {
  const input = $("localProjectsRoot");
  const saveBtn = $("saveLocalPath");
  if (input) input.value = getLocalProjectsRoot();

  if (saveBtn) {
    saveBtn.onclick = () => {
      const saved = saveLocalProjectsRoot(input.value);
      if (saved) alert("Saved. Next time you click Open in Cursor, that folder will open in Cursor.");
      else alert("Enter a folder path first.");
    };
  }

  const modal = $("pathModal");
  const modalInput = $("pathModalInput");
  const modalSave = $("pathModalSave");
  const modalCancel = $("pathModalCancel");

  if (modalCancel) {
    modalCancel.onclick = () => {
      modal.classList.add("hidden");
      pendingLaunchData = null;
    };
  }

  if (modalSave) {
    modalSave.onclick = () => {
      const root = saveLocalProjectsRoot(modalInput.value);
      if (!root) return alert("Enter your local projects folder path.");
      modal.classList.add("hidden");
      if (pendingLaunchData) {
        finishCloudLaunch(pendingLaunchData);
        pendingLaunchData = null;
      }
    };
  }
}

function promptLocalPathIfNeeded(data) {
  const root = getLocalProjectsRoot();
  if (root) return false;
  pendingLaunchData = data;
  const modal = $("pathModal");
  const modalInput = $("pathModalInput");
  if (modalInput) modalInput.value = "";
  modal.classList.remove("hidden");
  modalInput?.focus();
  return true;
}

function finishCloudLaunch(data) {
  const projectPath = localProjectPath(data.folder);
  if (!projectPath) return;

  openFolderInCursor(projectPath);
  autoDownloadZip(data.downloadUrl, data.folder);

  const el = $("openOptions");
  if (el) {
    el.innerHTML = `
      <div class="open-opt">
        <div class="ttl">Opening in Cursor…</div>
        <small>Cursor should open: <code>${projectPath}</code></small>
        <div class="cmd-row" style="margin-top:8px">
          <code class="cmd" id="openCmd">cursor "${projectPath}"</code>
          <button class="copy-btn" id="copyOpen">Copy</button>
        </div>
        <button type="button" class="btn-link" id="retryOpen" style="margin-top:10px">🖱️ Open in Cursor again</button>
        <small style="display:block;margin-top:8px">A ZIP was also downloaded with the latest PBMP context. If a browser dialog appears, choose <b>Cursor</b>. If Cursor did not open, run the command above in terminal.</small>
      </div>`;
    wireCopy("copyOpen", "openCmd");
    $("retryOpen").onclick = () => openFolderInCursor(projectPath);
  }
}
let tasks = [];
let activeId = null;
let selectedStage = null;
let stageRoles = {};
let users = [];
let cursorAccess = {};
let cloudPollTimer = null;
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
  lb.textContent = codingDone ? "Coding completed ✓" : "Open in Cursor (local)";
  lb.onclick = () => doLaunch(t.id);

  const cb = $("cloudBtn");
  if (cb) {
    cb.classList.toggle("hidden", !cursorAccess.cloudEnabled);
    cb.disabled = !ready || codingDone || !cursorAccess.cloudEnabled;
    cb.textContent = codingDone ? "Cloud run done ✓" : "☁ Run in Cursor Cloud";
    cb.onclick = () => doCloudAgent(t.id);
  }

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
    if (!data.isLocal) {
      if (!promptLocalPathIfNeeded(data)) finishCloudLaunch(data);
    }
    tasks = await api.list();
  }
  renderDetail();
}

async function doCloudAgent(id) {
  const cb = $("cloudBtn");
  cb.disabled = true;
  cb.textContent = "Starting cloud agent...";
  if (cloudPollTimer) clearInterval(cloudPollTimer);
  const { ok, data } = await api.cloudAgent(id);
  const box = $("result");
  box.classList.remove("hidden");
  $("checksBox").innerHTML = "";
  $("gitHookBox").innerHTML = "";
  $("openOptions").innerHTML = "";
  $("previewWrap").style.display = "block";

  if (!ok) {
    box.style.borderColor = "var(--danger)";
    $("resultTitle").textContent = "⛔ Cursor Cloud not available";
    $("resultMsg").textContent =
      data.error ||
      "Set CURSOR_API_KEY on Zeabur (Cursor Dashboard → API Keys). Gmail password cannot be stored here.";
    $("resultPreview").textContent = "";
  } else {
    box.style.borderColor = "var(--accent-2)";
    $("resultTitle").textContent = "☁ Cursor Cloud Agent started";
    $("resultMsg").textContent = data.message;
    renderCloudStatusPanel(data);
    if (data.agentId && data.runId) startCloudStatusPolling(data.agentId, data.runId);
    $("resultPreview").textContent = "";
    tasks = await api.list();
  }
  renderDetail();
}

function renderCloudStatusPanel(data, statusData) {
  const el = $("openOptions");
  const status = statusData?.status || "STARTING";
  const branch = statusData?.git?.branches?.[0]?.branch;
  const prUrl = statusData?.git?.branches?.[0]?.prUrl;
  const resultText = statusData?.result;
  const terminal = ["FINISHED", "ERROR", "CANCELLED", "EXPIRED"].includes(status);

  el.innerHTML = `
    <div class="open-opt">
      <div class="ttl">Live status <span>(no Cursor login needed in PBMP)</span></div>
      <div class="cloud-status-line"><b>Status:</b> <span id="cloudStatusText">${status}</span></div>
      ${branch ? `<div class="cloud-status-line"><b>Branch:</b> ${branch}</div>` : ""}
      ${prUrl ? `<div class="cloud-status-line"><b>PR:</b> <a href="${prUrl}" target="_blank" rel="noopener">${prUrl}</a></div>` : ""}
      ${resultText ? `<pre class="cloud-result">${resultText}</pre>` : ""}
      <small id="cloudStatusHint">${
        terminal
          ? "Run finished. Use Validate & Complete when code is on GitHub, or pull the branch locally."
          : "Agent is working on Cursor servers using the Grow24 API key. This page updates automatically."
      }</small>
      ${
        data.webUrl
          ? `<details style="margin-top:10px"><summary>Optional: open on cursor.com (requires Grow24 team login)</summary><a href="${data.webUrl}" target="_blank" rel="noopener">${data.webUrl}</a></details>`
          : ""
      }
    </div>`;
}

function startCloudStatusPolling(agentId, runId) {
  const poll = async () => {
    const { ok, data } = await api.cloudStatus(agentId, runId);
    if (!ok) return;
    renderCloudStatusPanel({ agentId, runId, webUrl: `https://cursor.com/agents/${agentId}` }, data);
    if (["FINISHED", "ERROR", "CANCELLED", "EXPIRED"].includes(data.status)) {
      clearInterval(cloudPollTimer);
      cloudPollTimer = null;
    }
  };
  poll();
  cloudPollTimer = setInterval(poll, 4000);
}

// Build the ways to open ONLY this project in Cursor on the user's machine.
// Cursor does not support a folder/open deeplink, so cloud users must download
// or clone the project first, then run `cursor .` locally.
function renderOpenOptions(data) {
  const el = $("openOptions");

  if (data.isLocal && data.launched) {
    el.innerHTML = `
      <div class="open-opt">
        <div class="ttl">Cursor opened on this machine</div>
        <small>The project folder was launched via the Cursor CLI. If the window did not appear, run the command below in your terminal.</small>
        <div class="cmd-row" style="margin-top:8px">
          <code class="cmd" id="openCmd">cursor ${data.folder}</code>
          <button class="copy-btn" id="copyOpen">Copy</button>
        </div>
      </div>`;
    wireCopy("copyOpen", "openCmd");
    return;
  }

  el.innerHTML = `
    <div class="open-opt">
      <div class="ttl">A) ZIP <span>(if Cursor did not open automatically)</span></div>
      <a class="dl-btn" href="${data.downloadUrl}" download>⬇ Download ${data.folder}.zip</a>
      <small>Extract the ZIP, open a terminal in that folder, then run:</small>
      <div class="cmd-row" style="margin-top:8px">
        <code class="cmd" id="openCmd">${data.openCommand || `cd ${data.folder} && cursor .`}</code>
        <button class="copy-btn" id="copyOpen">Copy</button>
      </div>
    </div>
    <div class="open-opt">
      <div class="ttl">B) Git <span>(clone repo, open this project folder)</span></div>
      <div class="cmd-row">
        <code class="cmd" id="cloneCmd">${data.cloneCommand}</code>
        <button class="copy-btn" id="copyClone">Copy</button>
      </div>
      <small>Run this in your terminal. It clones the repo and opens only this project in Cursor.</small>
    </div>
    <div class="open-opt note-box">
      <small><b>Tip:</b> Set your <b>Local projects folder</b> in the top bar (e.g. <code>/home/bappu/bpmpcursor/projects</code>). Then <b>Open in Cursor</b> will open the task folder directly in Cursor.</small>
    </div>`;

  wireCopy("copyClone", "cloneCmd");
  wireCopy("copyOpen", "openCmd");
}

function wireCopy(btnId, codeId) {
  const btn = $(btnId);
  const code = $(codeId);
  if (!btn || !code) return;
  btn.onclick = () => {
    navigator.clipboard?.writeText(code.textContent);
    btn.textContent = "Copied!";
    setTimeout(() => (btn.textContent = "Copy"), 1500);
  };
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

function renderCursorAccessBox() {
  const el = $("cursorAccessBox");
  if (!el) return;
  if (!cursorAccess.teamEmail && !cursorAccess.cloudEnabled) {
    el.classList.add("hidden");
    return;
  }
  el.classList.remove("hidden");
  const status = cursorAccess.cloudEnabled
    ? "☁ Cloud enabled — all users share team API key (no per-machine Gmail login)"
    : "⚠ Add CURSOR_API_KEY on Zeabur for multi-user cloud access";
  el.innerHTML = `<span class="cursor-team-label">Cursor team:</span> <b>${cursorAccess.teamEmail || "not set"}</b> · ${status}`;
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
  cursorAccess = meta.cursorAccess || {};
  if (users.length) currentUser = users[0];
  setupUserSelector();
  setupLocalPathControls();
  renderCursorAccessBox();
  tasks = await api.list();
  renderList();
}

init();
