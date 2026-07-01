// Shared Cursor org access (server-side only). Never store Gmail passwords.
// Use CURSOR_API_KEY from Cursor Team → Service accounts (grow24 org).

function getCursorAccessConfig() {
  const teamEmail = (process.env.CURSOR_TEAM_EMAIL || "").trim();
  const apiKey = (process.env.CURSOR_API_KEY || "").trim();
  return {
    teamEmail: teamEmail || null,
    cloudEnabled: Boolean(apiKey),
    // Shown in UI — how multi-user access works without per-user Gmail login.
    mode: apiKey ? "cloud-api-key" : teamEmail ? "team-email-only" : "not-configured",
    setupNote:
      "Add CURSOR_API_KEY on Zeabur (from Cursor Team → Service accounts). " +
      "All PBMP users then share cloud agents — no Gmail login on each machine.",
  };
}

async function launchCloudAgent(task, buildContext) {
  const apiKey = (process.env.CURSOR_API_KEY || "").trim();
  if (!apiKey) {
    return { ok: false, error: "CURSOR_API_KEY is not configured on the server." };
  }

  const repoUrl = task.repoUrl || "https://github.com/Grow24/bpmpcursor.git";
  const promptText = [
    buildContext(task),
    "",
    "## Coding task",
    `Work in the project folder: projects/${task.projectFolder}`,
    "Follow the PBMP context above. Implement or update code, then commit.",
  ].join("\n");

  const auth = Buffer.from(`${apiKey}:`).toString("base64");
  try {
    const response = await fetch("https://api.cursor.com/v1/agents", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Basic ${auth}`,
      },
      body: JSON.stringify({
        prompt: { text: promptText },
        model: { id: "composer-2" },
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
    const runId = body?.run?.id || null;
    const webUrl = agentId ? `https://cursor.com/agents/${agentId}` : null;

    return {
      ok: true,
      agentId,
      runId,
      webUrl,
      message:
        "Cursor Cloud Agent started. Open the link to watch progress — no Gmail login needed on this machine.",
    };
  } catch (e) {
    return { ok: false, error: e.message || "Failed to reach Cursor API" };
  }
}

module.exports = { getCursorAccessConfig, launchCloudAgent };
