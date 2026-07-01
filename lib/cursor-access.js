// Shared Cursor org access (server-side only). Never store Gmail passwords.
// Use CURSOR_API_KEY from Cursor Team → Service accounts (grow24 org).

function getCursorAccessConfig() {
  const teamEmail = (process.env.CURSOR_TEAM_EMAIL || "").trim();
  const apiKey = (process.env.CURSOR_API_KEY || "").trim();
  // Opens Cursor login with team email pre-filled (Person A/B still complete Google sign-in once).
  const teamLoginUrl = teamEmail
    ? `https://authenticator.cursor.sh/?email=${encodeURIComponent(teamEmail)}`
    : null;
  return {
    teamEmail: teamEmail || null,
    teamLoginUrl,
    cloudEnabled: Boolean(apiKey),
    mode: apiKey ? "cloud-api-key" : teamEmail ? "team-email-only" : "not-configured",
    setupNote:
      "Person A and B use PBMP with their own roles. Cursor Cloud uses Account C API key on the server. " +
      "For cursor.com or desktop, use the team sign-in link (Account C email pre-filled).",
  };
}

function cursorAuthHeader() {
  const apiKey = (process.env.CURSOR_API_KEY || "").trim();
  if (!apiKey) return null;
  return `Basic ${Buffer.from(`${apiKey}:`).toString("base64")}`;
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
  return {
    ok: true,
    agent: agentRes.data,
    run,
    status: run?.status || agentRes.data?.status || "UNKNOWN",
    result: run?.result || null,
    git: run?.git || null,
  };
}

async function launchCloudAgent(task, buildContext) {
  const apiKey = (process.env.CURSOR_API_KEY || "").trim();
  if (!apiKey) {
    return { ok: false, error: "CURSOR_API_KEY is not configured on the server." };
  }

  const repoUrl = task.repoUrl || "https://github.com/Grow24/bpmpcursor.git";
  const modelId = (process.env.CURSOR_MODEL || "auto").trim();
  const promptText = [
    buildContext(task),
    "",
    "## Coding task",
    `Work in the project folder: projects/${task.projectFolder}`,
    "Follow the PBMP context above. Implement or update code, then commit.",
  ].join("\n");

  const auth = cursorAuthHeader();
  try {
    const response = await fetch("https://api.cursor.com/v1/agents", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: auth,
      },
      body: JSON.stringify({
        prompt: { text: promptText },
        model: { id: modelId },
        repos: [{ url: repoUrl, startingRef: "main" }],
        autoCreatePR: false,
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
    const webUrl = agentId ? `https://cursor.com/agents/${agentId}` : null;

    return {
      ok: true,
      agentId,
      runId,
      webUrl,
      message:
        "Cursor Cloud Agent is running on Cursor servers via the Grow24 API key. Watch progress below in PBMP — no login needed here.",
    };
  } catch (e) {
    return { ok: false, error: e.message || "Failed to reach Cursor API" };
  }
}

module.exports = { getCursorAccessConfig, launchCloudAgent, getCloudAgentRunStatus };
