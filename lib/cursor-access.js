// Shared Cursor org access (server-side only). Never store Gmail passwords.
// Account C (CURSOR_TEAM_EMAIL) owns the API key; Person A/B trigger runs via PBMP only.

function envFlag(name, defaultValue = false) {
  const raw = (process.env[name] || "").trim().toLowerCase();
  if (!raw) return defaultValue;
  return raw === "1" || raw === "true" || raw === "yes";
}

function resolveRepoUrl(task) {
  const fromTask = (task.repoUrl || "").trim();
  const fromEnv = (process.env.CURSOR_TARGET_REPOSITORY || "").trim();
  const repoUrl = fromEnv || fromTask || "https://github.com/Grow24/bpmpcursor.git";

  const allowed = (process.env.PBMP_ALLOWED_REPOS || "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  if (allowed.length > 0 && !allowed.includes(repoUrl)) {
    return { ok: false, error: `Repository not allowed: ${repoUrl}` };
  }
  return { ok: true, repoUrl };
}

function getCursorAccessConfig() {
  const teamEmail = (process.env.CURSOR_TEAM_EMAIL || "").trim();
  const apiKey = (process.env.CURSOR_API_KEY || "").trim();
  const teamLoginUrl = teamEmail
    ? `https://authenticator.cursor.sh/?email=${encodeURIComponent(teamEmail)}`
    : null;
  return {
    teamEmail: teamEmail || null,
    teamLoginUrl,
    cloudEnabled: Boolean(apiKey),
    targetRepository:
      (process.env.CURSOR_TARGET_REPOSITORY || "").trim() ||
      "https://github.com/Grow24/bpmpcursor",
    defaultRef: (process.env.CURSOR_DEFAULT_REF || "main").trim(),
    autoCreatePR: envFlag("CURSOR_AUTO_CREATE_PR", true),
    mode: apiKey ? "cloud-api-key" : teamEmail ? "team-email-only" : "not-configured",
    setupNote:
      "Person A/B use PBMP roles only. PBMP calls Cursor Cloud Agents with Account C API key. " +
      "Do not use cursor.com browser login as the main workflow.",
  };
}

function cursorAuthHeader() {
  const apiKey = (process.env.CURSOR_API_KEY || "").trim();
  if (!apiKey) return null;
  return `Basic ${Buffer.from(`${apiKey}:`).toString("base64")}`;
}

function buildCloudPrompt(task, buildContext) {
  const prefix = (process.env.PBMP_AGENT_BRANCH_PREFIX || "pbmp").trim();
  return [
    buildContext(task),
    "",
    "## Coding task (PBMP-controlled scope)",
    `- Repository folder: projects/${task.projectFolder}`,
    `- Task ID: ${task.id}`,
    `- Module: ${task.module}`,
    `- Stack: ${task.language}`,
    "",
    "## Rules",
    "- Change only files required for this task.",
    "- Follow security and coding standards from the PBMP context.",
    "- Do not modify unrelated projects or global config unless required.",
    `- Prefer branch prefix: ${prefix}/${task.id.toLowerCase()}-`,
    "- Run relevant tests if present before finishing.",
    "",
    "Implement or update code, then commit. Open a PR if autoCreatePR is enabled.",
  ].join("\n");
}

async function cursorApiGet(path) {
  const auth = cursorAuthHeader();
  if (!auth) return { ok: false, error: "CURSOR_API_KEY is not configured on the server." };
  try {
    const response = await fetch(`https://api.cursor.com${path}`, {
      headers: { Authorization: auth },
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      const msg =
        body?.error?.message || body?.message || `Cursor API error (${response.status})`;
      return { ok: false, error: msg, status: response.status };
    }
    return { ok: true, data: body };
  } catch (e) {
    return { ok: false, error: e.message || "Failed to reach Cursor API" };
  }
}

async function getCloudAgentRunStatus(agentId, runId) {
  let run = null;
  if (runId) {
    const runRes = await cursorApiGet(`/v1/agents/${agentId}/runs/${runId}`);
    if (runRes.ok) run = runRes.data;
  }
  const agentRes = await cursorApiGet(`/v1/agents/${agentId}`);
  if (!agentRes.ok) return agentRes;
  const latestRunId = run?.id || agentRes.data?.latestRunId;
  if (!run && latestRunId) {
    const runRes = await cursorApiGet(`/v1/agents/${agentId}/runs/${latestRunId}`);
    if (runRes.ok) run = runRes.data;
  }
  const branch = run?.git?.branches?.[0];
  return {
    ok: true,
    agent: agentRes.data,
    run,
    status: run?.status || agentRes.data?.status || "UNKNOWN",
    result: run?.result || null,
    git: run?.git || null,
    branch: branch?.branch || null,
    prUrl: branch?.prUrl || null,
  };
}

async function launchCloudAgent(task, buildContext) {
  if (!cursorAuthHeader()) {
    return { ok: false, error: "CURSOR_API_KEY is not configured on the server." };
  }

  const repo = resolveRepoUrl(task);
  if (!repo.ok) return repo;

  const modelId = (process.env.CURSOR_MODEL || "auto").trim();
  const startingRef = (process.env.CURSOR_DEFAULT_REF || "main").trim();
  const autoCreatePR = envFlag("CURSOR_AUTO_CREATE_PR", true);
  const promptText = buildCloudPrompt(task, buildContext);

  try {
    const response = await fetch("https://api.cursor.com/v1/agents", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: cursorAuthHeader(),
      },
      body: JSON.stringify({
        prompt: { text: promptText },
        model: { id: modelId },
        repos: [{ url: repo.repoUrl, startingRef }],
        autoCreatePR,
      }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      const msg =
        body?.error?.message || body?.message || `Cursor API error (${response.status})`;
      return { ok: false, error: msg, status: response.status };
    }

    const agentId = body?.agent?.id || body?.id || null;
    const runId = body?.run?.id || body?.latestRunId || null;

    return {
      ok: true,
      agentId,
      runId,
      repository: repo.repoUrl,
      startingRef,
      autoCreatePR,
      message:
        "Cursor Cloud Agent started via PBMP (Account C API key). Watch status below — no cursor.com login required.",
    };
  } catch (e) {
    return { ok: false, error: e.message || "Failed to reach Cursor API" };
  }
}

module.exports = {
  getCursorAccessConfig,
  launchCloudAgent,
  getCloudAgentRunStatus,
};
