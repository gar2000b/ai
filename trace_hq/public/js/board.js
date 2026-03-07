/**
 * TRACE H.Q. — Project board: fetch workflows + stories, render stacked workflow sections.
 */

const LAST_PROJECT_KEY = 'tracehq_last_project';

function getLastProjectId() {
  try {
    const id = localStorage.getItem(LAST_PROJECT_KEY);
    return id ? parseInt(id, 10) : null;
  } catch {
    return null;
  }
}

function setLastProjectId(projectId) {
  try {
    localStorage.setItem(LAST_PROJECT_KEY, String(projectId));
  } catch (_) {}
}

async function loadProjects() {
  const res = await fetch('/api/projects');
  if (!res.ok) throw new Error('Failed to load projects');
  return res.json();
}

async function loadWorkflows(projectId) {
  const res = await fetch(`/api/projects/${projectId}/workflows`);
  if (!res.ok) throw new Error('Failed to load workflows');
  return res.json();
}

async function loadStories(projectId) {
  const res = await fetch(`/api/projects/${projectId}/stories`);
  if (!res.ok) throw new Error('Failed to load stories');
  return res.json();
}

function renderBoard(container, projectId, projectName) {
  container.innerHTML = '<p class="board-loading">Loading board…</p>';
  setLastProjectId(projectId);

  Promise.all([loadWorkflows(projectId), loadStories(projectId)])
    .then(([workflows, stories]) => {
      container.innerHTML = '';
      const storiesByStage = {};
      stories.forEach((s) => {
        const k = s.workflow_stage_id || 'none';
        if (!storiesByStage[k]) storiesByStage[k] = [];
        storiesByStage[k].push(s);
      });

      workflows.forEach((wf) => {
        const section = document.createElement('div');
        section.className = 'board-section';
        section.innerHTML = `<h2>${escapeHtml(wf.name)}</h2>`;
        const columns = document.createElement('div');
        columns.className = 'workflow-columns';

        (wf.stages || []).forEach((stage) => {
          const col = document.createElement('div');
          col.className = 'stage-column';
          col.innerHTML = `<div class="stage-column-header">${escapeHtml(stage.stage_name)}</div>`;
          const cardsContainer = document.createElement('div');
          cardsContainer.className = 'stage-cards';

          const stageStories = storiesByStage[stage.id] || [];
          stageStories.forEach((story) => {
            const card = window.TraceHqStoryCard.renderStoryCard(story, wf.stages, () => {
              renderBoard(container, projectId, projectName);
            });
            cardsContainer.appendChild(card);
          });

          col.appendChild(cardsContainer);
          columns.appendChild(col);
        });

        section.appendChild(columns);
        container.appendChild(section);
      });
    })
    .catch((err) => {
      container.innerHTML = `<p class="board-error">${escapeHtml(err.message)}</p>`;
      if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
    });
}

function escapeHtml(s) {
  if (s == null) return '';
  const el = document.createElement('span');
  el.textContent = s;
  return el.innerHTML;
}

window.TraceHqBoard = {
  getLastProjectId,
  setLastProjectId,
  loadProjects,
  renderBoard,
};
