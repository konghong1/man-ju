const form = document.getElementById("generate-form");
const statusEl = document.getElementById("status");
const resultEl = document.getElementById("result");
const submitBtn = document.getElementById("submit-btn");

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    submitBtn.disabled = true;
    statusEl.textContent = "生成中，请稍候...";
    resultEl.innerHTML = "";

    try {
        const payload = Object.fromEntries(new FormData(form).entries());
        const response = await fetch("/api/projects", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const project = await response.json();
        statusEl.textContent = `已生成项目：${project.projectId}`;
        renderProject(project);
    } catch (error) {
        statusEl.textContent = "生成失败，请检查输入后重试。";
        resultEl.innerHTML = `<pre class="mono">${escapeHtml(error.message)}</pre>`;
    } finally {
        submitBtn.disabled = false;
    }
});

function renderProject(project) {
    const episodes = project.episodes || [];
    const episodeHtml = episodes.map(renderEpisode).join("");
    resultEl.innerHTML = `
      <p><strong>梗概：</strong>${escapeHtml(project.synopsis)}</p>
      <p class="mono">projectId: ${escapeHtml(project.projectId)}</p>
      ${episodeHtml}
    `;
}

function renderEpisode(episode) {
    const scenesHtml = (episode.scenes || []).map(renderScene).join("");
    return `
      <article class="episode">
        <h3>第 ${episode.index} 集：${escapeHtml(episode.title)}</h3>
        <p>${escapeHtml(episode.summary)}</p>
        ${scenesHtml}
      </article>
    `;
}

function renderScene(scene) {
    const panelsHtml = (scene.panels || []).map(renderPanel).join("");
    return `
      <section class="scene">
        <h4>Scene ${scene.index} · ${escapeHtml(scene.location)}</h4>
        <p><strong>目标：</strong>${escapeHtml(scene.goal)}</p>
        <p><strong>冲突：</strong>${escapeHtml(scene.conflictBeat)}</p>
        ${panelsHtml}
      </section>
    `;
}

function renderPanel(panel) {
    return `
      <div class="panel-item">
        <p><strong>Panel ${panel.index}</strong> · ${escapeHtml(panel.camera)}</p>
        <p>${escapeHtml(panel.narration)}</p>
        <p>对白：${escapeHtml(panel.dialogue)}</p>
        <p>音效：${escapeHtml(panel.sfx)}</p>
        <p class="mono">${escapeHtml(panel.imagePrompt)}</p>
      </div>
    `;
}

function escapeHtml(text) {
    return String(text ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}
