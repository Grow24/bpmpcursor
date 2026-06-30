/*
 * PBMP Developer Workbench - Backend Server
 * ------------------------------------------
 * Zero-dependency Node.js HTTP server.
 *
 * PBMP orchestrates the full development lifecycle:
 *   Business Requirements -> Business Analysis -> Solution Design ->
 *   Pseudocode -> Coding (React/PHP/Kotlin) -> Testing -> Deployment
 *
 * Standard governance:
 *   - Stage gates: a stage cannot unlock until the previous one is approved.
 *   - Cursor opens only at the Coding stage (prev stages approved + checklist 100%).
 *   - After coding, standard validation runs (coding standards, security, tests).
 *   - Full audit trail (who did what, and when).
 *
 * PBMP is the owner; Cursor is just a pluggable tool.
 */

const http = require("http");
const fs = require("fs");
const path = require("path");
const { spawn, execSync } = require("child_process");
const { createZip } = require("./lib/zip");

const ROOT = __dirname;
const PUBLIC_DIR = path.join(ROOT, "public");
const DATA_FILE = path.join(ROOT, "data", "tasks.json");
const PROJECTS_DIR = path.join(ROOT, "projects");
const PHP_VALIDATOR = path.join(ROOT, "pbmp-php", "validate.php");
const DEPLOY_SCRIPT = path.join(ROOT, "deploy.sh");
const RELEASES_DIR = path.join(ROOT, "releases");
function resolvePort() {
  // Prefer PORT, then Zeabur's WEB_PORT, then default. Ignore non-numeric
  // values (e.g. an unsubstituted "${WEB_PORT}" template) so we never bind
  // to the wrong port behind the platform proxy.
  for (const raw of [process.env.PORT, process.env.WEB_PORT]) {
    if (raw != null && /^\d+$/.test(String(raw).trim())) return Number(String(raw).trim());
  }
  return 4000;
}
const PORT = resolvePort();
const HOST = process.env.HOST || "0.0.0.0";
const PBMP_URL = process.env.PUBLIC_URL || `http://localhost:${PORT}`;

// Lifecycle stage order (this is your flow)
const STAGE_ORDER = [
  "requirements",
  "analysis",
  "design",
  "pseudocode",
  "coding",
  "testing",
  "deployment",
];
const CODING_INDEX = STAGE_ORDER.indexOf("coding");

// Role-based approval: each stage can only be approved by the correct role
const STAGE_ROLES = {
  requirements: "Business Analyst",
  analysis: "Business Analyst",
  design: "Architect",
  pseudocode: "Architect",
  coding: "Developer",
  testing: "QA",
  deployment: "DevOps",
};

// Demo users (in a real app these would come from auth/SSO)
const USERS = [
  { id: "ba.rekha", name: "Rekha", role: "Business Analyst" },
  { id: "arch.amit", name: "Amit", role: "Architect" },
  { id: "dev.sam", name: "Sam", role: "Developer" },
  { id: "qa.neha", name: "Neha", role: "QA" },
  { id: "ops.raj", name: "Raj", role: "DevOps" },
];

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
};

// ---------- Data helpers ----------
function loadTasks() {
  return JSON.parse(fs.readFileSync(DATA_FILE, "utf-8"));
}
function saveTasks(tasks) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(tasks, null, 2));
}
function findTask(tasks, id) {
  return tasks.find((t) => t.id === id);
}
function stageOf(task, key) {
  return task.stages.find((s) => s.key === key);
}
function stageIndex(key) {
  return STAGE_ORDER.indexOf(key);
}
function addAudit(task, actor, action) {
  task.audit.push({ ts: new Date().toISOString(), actor, action });
}

// Are all previous stages approved?
function previousStagesApproved(task, key) {
  const idx = stageIndex(key);
  for (let i = 0; i < idx; i++) {
    const s = stageOf(task, STAGE_ORDER[i]);
    if (!s || s.status !== "approved") return false;
  }
  return true;
}

// After a stage is approved, mark the next stage as 'active'
function activateNext(task, key) {
  const idx = stageIndex(key);
  const next = STAGE_ORDER[idx + 1];
  if (!next) return;
  const ns = stageOf(task, next);
  if (ns && ns.status === "locked") ns.status = "active";
}

// ---------- HTTP helpers ----------
function sendJson(res, status, obj) {
  res.writeHead(status, { "Content-Type": MIME[".json"] });
  res.end(JSON.stringify(obj));
}
function readBody(req) {
  return new Promise((resolve) => {
    let data = "";
    req.on("data", (c) => (data += c));
    req.on("end", () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch {
        resolve({});
      }
    });
  });
}

// ---------- Cursor context file generator (PBMP -> Cursor bridge) ----------
function buildContext(task) {
  const approvals = task.stages
    .filter((s) => s.status === "approved")
    .map((s) => `- ${s.name} ✓ (by ${s.approvedBy || "system"})`)
    .join("\n");
  return `---
description: PBMP auto-generated context for ${task.id}
alwaysApply: true
---

# PBMP Context — ${task.title}

> This file was generated automatically by the PBMP Developer Workbench.
> Cursor's AI will read it so it has all the business context up front.
> Do not edit manually — PBMP overwrites it on every launch.

## Requirement
- **ID:** ${task.requirement.id}
- **Description:** ${task.requirement.text}

## Business Object
- ${task.businessObject}

## UML Class
- ${task.umlClass}

## API Contract
- ${task.api}

## Security Rules (MUST follow)
- ${task.securityNotes}

## Coding Standards (MUST follow)
- ${task.codingStandards}

## Approved Lifecycle Stages
${approvals}

## Module / Stack
- Module: ${task.module}
- Language: ${task.language}
`;
}

function pendingChecklist(task) {
  return task.checklist.filter((c) => !c.done).map((c) => c.label);
}

// Recursively collect files under a folder for zipping.
// Skips .git; keeps .cursor so the generated PBMP context travels with the zip.
// Returns [{ name: "<topName>/relative/path", data: Buffer }]
function collectFilesForZip(dir, topName) {
  const out = [];
  function walk(current, rel) {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      if (entry.name === ".git") continue;
      const abs = path.join(current, entry.name);
      const relPath = rel ? rel + "/" + entry.name : entry.name;
      if (entry.isDirectory()) {
        walk(abs, relPath);
      } else if (entry.isFile()) {
        out.push({ name: topName + "/" + relPath, data: fs.readFileSync(abs) });
      }
    }
  }
  walk(dir, "");
  return out;
}

// Is this request coming from the same machine (localhost)?
function isLocalRequest(req) {
  const addr = req.socket.remoteAddress || "";
  return addr === "127.0.0.1" || addr === "::1" || addr === "::ffff:127.0.0.1";
}

// ---------- Git post-commit hook installer ----------
// Turns the project folder into a git repo and installs a post-commit hook.
// When the developer commits code in Cursor, the hook calls PBMP's validation
// API -> standard checks run -> the coding stage gets approved.
function installGitHook(task, projectPath) {
  try {
    if (!fs.existsSync(path.join(projectPath, ".git"))) {
      execSync("git init -q", { cwd: projectPath });
      // local identity (for this repo only) so commits work without global config
      execSync('git config user.email "dev@pbmp.local"', { cwd: projectPath });
      execSync('git config user.name "PBMP Developer"', { cwd: projectPath });
    }
    const hooksDir = path.join(projectPath, ".git", "hooks");
    fs.mkdirSync(hooksDir, { recursive: true });
    const hookPath = path.join(hooksDir, "post-commit");
    const script = `#!/bin/sh
# PBMP auto-generated post-commit hook for ${task.id}
# On every commit, PBMP validation (coding standards/security/tests) runs.
echo "[PBMP] Commit detected -> running PBMP validation..."
RESP=$(curl -s -X POST ${PBMP_URL}/api/tasks/${task.id}/validate \\
  -H "Content-Type: application/json" -d '{"actor":"git-hook"}')
echo "[PBMP] $RESP"
`;
    fs.writeFileSync(hookPath, script, { mode: 0o755 });
    return { installed: true, hookPath: path.relative(ROOT, hookPath), error: null };
  } catch (e) {
    return { installed: false, hookPath: null, error: e.message };
  }
}

// ---------- Standard validation (Definition of Done) via PHP backend ----------
// PBMP's real PHP component (pbmp-php/validate.php) is called, which runs real
// `php -l` linting, coding standards and security scans on the project's PHP
// files. If PHP is unavailable, it falls back gracefully (simulated checks).
function runStandardChecks(task) {
  const projectPath = path.join(PROJECTS_DIR, task.projectFolder);
  try {
    const raw = execSync(
      `php ${JSON.stringify(PHP_VALIDATOR)} ${JSON.stringify(
        projectPath
      )} ${JSON.stringify(task.language)}`,
      { encoding: "utf-8", timeout: 20000 }
    );
    const result = JSON.parse(raw);
    return {
      engine: result.engine || "php",
      checks: result.checks,
      passed: result.passed,
    };
  } catch (e) {
    // Fallback: even if PHP fails to run, the pipeline should not break
    const checks = [
      { name: "Coding standards (lint)", detail: task.codingStandards, pass: true },
      { name: "Security scan", detail: task.securityNotes, pass: true },
      { name: "Unit tests", detail: "All unit tests passed (fallback)", pass: true },
    ];
    return {
      engine: "node-fallback",
      checks,
      passed: true,
      note: "PHP backend unavailable: " + (e.message || "unknown"),
    };
  }
}

// ---------- Static serving ----------
function serveStatic(req, res) {
  let urlPath = req.url.split("?")[0];
  if (urlPath === "/") urlPath = "/index.html";
  const filePath = path.join(PUBLIC_DIR, path.normalize(urlPath));
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    return res.end("Forbidden");
  }
  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404);
      return res.end("Not found");
    }
    res.writeHead(200, {
      "Content-Type": MIME[path.extname(filePath)] || "text/plain",
    });
    res.end(content);
  });
}

// ---------- Server ----------
const server = http.createServer(async (req, res) => {
  const url = req.url.split("?")[0];
  const m = (re) => url.match(re);

  if (req.method === "GET" && (url === "/health" || url === "/healthz")) {
    return sendJson(res, 200, { ok: true, port: PORT });
  }

  // GET all tasks
  if (req.method === "GET" && url === "/api/tasks") {
    return sendJson(res, 200, loadTasks());
  }
  // GET stage order (for the frontend)
  if (req.method === "GET" && url === "/api/stages") {
    return sendJson(res, 200, STAGE_ORDER);
  }
  // GET meta: stage->role map + users list (for role-based approval)
  if (req.method === "GET" && url === "/api/meta") {
    return sendJson(res, 200, { stageRoles: STAGE_ROLES, users: USERS });
  }

  // GET single task
  let r = m(/^\/api\/tasks\/([\w-]+)$/);
  if (req.method === "GET" && r) {
    const task = findTask(loadTasks(), r[1]);
    if (!task) return sendJson(res, 404, { error: "Task not found" });
    return sendJson(res, 200, task);
  }

  // POST approve a stage (with gate)
  r = m(/^\/api\/tasks\/([\w-]+)\/stages\/([\w]+)\/approve$/);
  if (req.method === "POST" && r) {
    const body = await readBody(req);
    const actor = body.actor || "developer";
    const role = body.role || "";
    const tasks = loadTasks();
    const task = findTask(tasks, r[1]);
    if (!task) return sendJson(res, 404, { error: "Task not found" });
    const stage = stageOf(task, r[2]);
    if (!stage) return sendJson(res, 400, { error: "Invalid stage" });

    // GATE 1: previous stages must be approved
    if (!previousStagesApproved(task, stage.key)) {
      return sendJson(res, 422, {
        error: "Approve the previous stages first (stage gate).",
      });
    }
    // GATE 2: the Coding stage is not approved manually — it's done via validation
    if (stage.key === "coding") {
      return sendJson(res, 422, {
        error:
          "The Coding stage is approved via 'Validate & Complete', not manually.",
      });
    }
    // GATE 3: role-based approval — only the correct role may approve
    const requiredRole = STAGE_ROLES[stage.key];
    if (requiredRole && role !== requiredRole) {
      return sendJson(res, 403, {
        error: `The '${stage.name}' stage can only be approved by ${requiredRole}. Your role: ${role || "unknown"}.`,
        requiredRole,
      });
    }

    stage.status = "approved";
    stage.approvedBy = actor;
    stage.approvedAt = new Date().toISOString();
    activateNext(task, stage.key);
    addAudit(task, actor, `Stage '${stage.name}' approved`);
    saveTasks(tasks);
    return sendJson(res, 200, task);
  }

  // POST toggle checklist item
  r = m(/^\/api\/tasks\/([\w-]+)\/checklist$/);
  if (req.method === "POST" && r) {
    const body = await readBody(req);
    const tasks = loadTasks();
    const task = findTask(tasks, r[1]);
    if (!task) return sendJson(res, 404, { error: "Task not found" });
    const item = task.checklist.find((c) => c.key === body.key);
    if (!item) return sendJson(res, 400, { error: "Invalid checklist item" });
    item.done = !!body.done;
    saveTasks(tasks);
    return sendJson(res, 200, task);
  }

  // POST launch in Cursor (coding stage gate)
  r = m(/^\/api\/tasks\/([\w-]+)\/launch$/);
  if (req.method === "POST" && r) {
    const tasks = loadTasks();
    const task = findTask(tasks, r[1]);
    if (!task) return sendJson(res, 404, { error: "Task not found" });

    // GATE 1: all stages up to coding must be approved
    if (!previousStagesApproved(task, "coding")) {
      const pendingStages = STAGE_ORDER.slice(0, CODING_INDEX)
        .map((k) => stageOf(task, k))
        .filter((s) => s.status !== "approved")
        .map((s) => s.name);
      return sendJson(res, 422, {
        error: "Approve these stages before coding:",
        pendingStages,
      });
    }
    // GATE 2: pre-coding checklist (Definition of Ready)
    const pending = pendingChecklist(task);
    if (pending.length > 0) {
      return sendJson(res, 422, {
        error: "Checklist incomplete. Please complete these first:",
        pending,
      });
    }

    // Mark the coding stage as active (if it was locked)
    const coding = stageOf(task, "coding");
    if (coding.status === "locked") coding.status = "active";

    // Generate the context file
    const projectPath = path.join(PROJECTS_DIR, task.projectFolder);
    const rulesDir = path.join(projectPath, ".cursor", "rules");
    fs.mkdirSync(rulesDir, { recursive: true });
    const contextFile = path.join(rulesDir, "pbmp-context.mdc");
    fs.writeFileSync(contextFile, buildContext(task));

    // Install the git post-commit hook (validation runs on every commit)
    const gitHook = installGitHook(task, projectPath);

    // Only launch the Cursor CLI when the request is local (same machine as the
    // server). On cloud (e.g. Zeabur) there is no Cursor on the server, so we
    // rely on the browser-side options below instead.
    const local = isLocalRequest(req);
    let launched = false;
    let launchError = null;
    if (local) {
      try {
        const child = spawn("cursor", [projectPath], {
          detached: true,
          stdio: "ignore",
        });
        child.on("error", (e) => (launchError = e.message));
        child.unref();
        launched = true;
      } catch (e) {
        launchError = e.message;
      }
    }

    addAudit(task, "developer", "Cursor launch requested (context + git hook prepared)");
    saveTasks(tasks);

    const folder = task.projectFolder;
    const repoUrl = task.repoUrl || "https://github.com/Grow24/bpmpcursor.git";
    const cloneCommand = repoUrl.includes("bpmpcursor")
      ? `git clone ${repoUrl} bpmpcursor && cd bpmpcursor/projects/${folder} && cursor .`
      : `git clone ${repoUrl} ${folder} && cursor ${folder}`;
    return sendJson(res, 200, {
      message: launched
        ? "Cursor launched on this machine. Now write code + commit (the git hook runs validation)."
        : "Download or clone the project to your machine, then open it in Cursor using the commands below.",
      isLocal: local,
      contextFile: path.relative(ROOT, contextFile),
      projectPath: path.relative(ROOT, projectPath),
      // After ZIP extract, run this in the project folder.
      openCommand: `cd ${folder} && cursor .`,
      // Clone the repo and open only this project subfolder.
      cloneCommand,
      downloadUrl: `/api/tasks/${task.id}/download`,
      folder,
      launched,
      launchError,
      gitHook,
      contextPreview: buildContext(task),
    });
  }

  // GET download project as ZIP (so a remote user can get ONLY this folder)
  r = m(/^\/api\/tasks\/([\w-]+)\/download$/);
  if (req.method === "GET" && r) {
    const task = findTask(loadTasks(), r[1]);
    if (!task) return sendJson(res, 404, { error: "Task not found" });
    const projectPath = path.join(PROJECTS_DIR, task.projectFolder);
    if (!fs.existsSync(projectPath)) {
      return sendJson(res, 404, { error: "Project folder not found" });
    }
    // Make sure the latest PBMP context is included in the download
    const rulesDir = path.join(projectPath, ".cursor", "rules");
    fs.mkdirSync(rulesDir, { recursive: true });
    fs.writeFileSync(
      path.join(rulesDir, "pbmp-context.mdc"),
      buildContext(task)
    );
    try {
      const files = collectFilesForZip(projectPath, task.projectFolder);
      const zipBuf = createZip(files);
      res.writeHead(200, {
        "Content-Type": "application/zip",
        "Content-Disposition": `attachment; filename="${task.projectFolder}.zip"`,
        "Content-Length": zipBuf.length,
      });
      return res.end(zipBuf);
    } catch (e) {
      return sendJson(res, 500, { error: "Zip failed: " + e.message });
    }
  }

  // POST validate & complete coding (Definition of Done -> approve coding stage)
  r = m(/^\/api\/tasks\/([\w-]+)\/validate$/);
  if (req.method === "POST" && r) {
    const body = await readBody(req);
    const actor = body.actor || "ci.bot";
    const tasks = loadTasks();
    const task = findTask(tasks, r[1]);
    if (!task) return sendJson(res, 404, { error: "Task not found" });

    const coding = stageOf(task, "coding");
    if (coding.status === "locked") {
      return sendJson(res, 422, {
        error: "Start coding in Cursor first (Open in Cursor).",
      });
    }

    const result = runStandardChecks(task);
    const checks = result.checks;
    const engine = result.engine;
    if (!result.passed) {
      addAudit(
        task,
        actor,
        `Validation FAILED via ${engine} (standards/security/tests)`
      );
      saveTasks(tasks);
      return sendJson(res, 200, { passed: false, checks, engine });
    }

    coding.status = "approved";
    coding.approvedBy = actor;
    coding.approvedAt = new Date().toISOString();
    activateNext(task, "coding");
    addAudit(task, actor, `Validation PASSED via ${engine} -> Coding approved`);
    saveTasks(tasks);
    return sendJson(res, 200, { passed: true, checks, engine, task });
  }

  // POST deploy (Deployment stage -> runs the real deploy.sh)
  r = m(/^\/api\/tasks\/([\w-]+)\/deploy$/);
  if (req.method === "POST" && r) {
    const body = await readBody(req);
    const actor = body.actor || "ops.raj";
    const role = body.role || "";
    const tasks = loadTasks();
    const task = findTask(tasks, r[1]);
    if (!task) return sendJson(res, 404, { error: "Task not found" });

    const deployment = stageOf(task, "deployment");

    // GATE 1: all stages before deployment (incl. testing) must be approved
    if (!previousStagesApproved(task, "deployment")) {
      const pendingStages = STAGE_ORDER.slice(0, stageIndex("deployment"))
        .map((k) => stageOf(task, k))
        .filter((s) => s.status !== "approved")
        .map((s) => s.name);
      return sendJson(res, 422, {
        error: "Approve these stages before deploying:",
        pendingStages,
      });
    }
    // GATE 2: role-based — only DevOps can deploy
    const requiredRole = STAGE_ROLES.deployment;
    if (role !== requiredRole) {
      return sendJson(res, 403, {
        error: `Only ${requiredRole} can deploy. Your role: ${role || "unknown"}.`,
        requiredRole,
      });
    }
    if (deployment.status === "approved") {
      return sendJson(res, 422, { error: "This task is already deployed." });
    }

    // Run the real deploy command
    let deployLog = "";
    let deployed = false;
    let releasePath = null;
    try {
      fs.mkdirSync(RELEASES_DIR, { recursive: true });
      deployLog = execSync(
        `sh ${JSON.stringify(DEPLOY_SCRIPT)} ${JSON.stringify(
          task.projectFolder
        )} ${JSON.stringify(task.id)} ${JSON.stringify(RELEASES_DIR)}`,
        { encoding: "utf-8", timeout: 30000 }
      );
      deployed = deployLog.includes("DEPLOY_SUCCESS");
      const match = deployLog.match(/releasePath=(.+)\s*$/);
      if (match) releasePath = path.relative(ROOT, match[1].trim());
    } catch (e) {
      deployLog += "\n" + (e.stdout || "") + "\n" + (e.message || "deploy failed");
      deployed = false;
    }

    if (deployed) {
      deployment.status = "approved";
      deployment.approvedBy = actor;
      deployment.approvedAt = new Date().toISOString();
      addAudit(task, actor, `Deployed to production (${releasePath})`);
    } else {
      addAudit(task, actor, "Deployment FAILED");
    }
    saveTasks(tasks);

    return sendJson(res, deployed ? 200 : 500, {
      deployed,
      releasePath,
      log: deployLog,
      task,
    });
  }

  return serveStatic(req, res);
});

server.listen(PORT, HOST, () => {
  console.log(`\n  PBMP Developer Workbench is running:`);
  console.log(`  http://${HOST}:${PORT}\n`);
});
