/**
 * TRACE H.Q. — Backlog: global list of stories not in any workflow.
 * Grid layout; "Add to workflow" dropdown grouped by project (project → workflows).
 */

async function loadBacklog() {
  const res = await fetch('/api/backlog');
  if (!res.ok) throw new Error('Failed to load backlog');
  return res.json();
}

async function loadProjectsWithWorkflows() {
  const res = await fetch('/api/projects-with-workflows');
  if (!res.ok) throw new Error('Failed to load projects and workflows');
  return res.json();
}

function escapeHtml(s) {
  if (s == null) return '';
  const el = document.createElement('span');
  el.textContent = s;
  return el.innerHTML;
}

function buildAddToWorkflowOptions(projectsWithWorkflows) {
  if (!projectsWithWorkflows || projectsWithWorkflows.length === 0) return '';
  const parts = [];
  for (const project of projectsWithWorkflows) {
    if (!project.workflows || project.workflows.length === 0) continue;
    const opts = project.workflows.map(
      (w) => `<option value="${w.id}:${w.firstStageId}" data-workflow-id="${w.id}" data-stage-id="${w.firstStageId}">${escapeHtml(w.name)}</option>`
    ).join('');
    parts.push(`<optgroup label="${escapeHtml(project.name)}">${opts}</optgroup>`);
  }
  return parts.join('');
}

function renderBacklogCard(story, projectsWithWorkflows, onAddedToWorkflow) {
  const div = document.createElement('div');
  div.className = 'backlog-card story-card' + (story.blocked ? ' blocked' : '');
  div.dataset.storyId = story.id;
  div.draggable = true;
  div.setAttribute('draggable', 'true');

  const depsText = story.dependencies && story.dependencies.length
    ? `Blocked by ${story.dependencies.join(', ')}`
    : '';
  const assignee = story.assignee_name || 'Unassigned';

  const optgroupsHtml = buildAddToWorkflowOptions(projectsWithWorkflows);
  div.innerHTML = `
    <div class="story-card-id">${escapeHtml(story.id)}</div>
    <div class="story-card-title">${escapeHtml(story.title)}</div>
    <div class="story-card-meta">${escapeHtml(assignee)}${story.priority ? ' · ' + escapeHtml(story.priority) : ''}${story.type ? ' · ' + escapeHtml(story.type) : ''}</div>
    ${depsText ? `<div class="story-card-meta">${escapeHtml(depsText)}</div>` : ''}
    <div class="story-card-move">
      <select class="backlog-add-to-workflow" data-story-id="${escapeHtml(story.id)}">
        <option value="">Add to workflow…</option>
        ${optgroupsHtml}
      </select>
    </div>
  `;

  const select = div.querySelector('.backlog-add-to-workflow');
  if (select && select.options.length > 1) {
    select.addEventListener('change', async (e) => {
      const opt = e.target.options[e.target.selectedIndex];
      const value = (opt && opt.value) || '';
      if (!value) return;
      const colon = value.indexOf(':');
      if (colon === -1) return;
      const workflowId = parseInt(value.slice(0, colon), 10);
      const stageId = parseInt(value.slice(colon + 1), 10);
      if (Number.isNaN(workflowId) || Number.isNaN(stageId)) return;
      try {
        const res = await fetch(`/api/stories/${story.id}/workflow`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ workflow_id: workflowId, workflow_stage_id: stageId }),
        });
        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          throw new Error(data.error || res.statusText);
        }
        e.target.value = '';
        if (typeof window.showToast === 'function') window.showToast(`Added to ${opt.text}`, 'success');
        if (typeof onAddedToWorkflow === 'function') onAddedToWorkflow();
      } catch (err) {
        if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
      }
    });
  }

  return div;
}

function renderBacklog(container) {
  container.innerHTML = '<p class="board-loading">Loading backlog…</p>';

  Promise.all([loadBacklog(), loadProjectsWithWorkflows()])
    .then(([stories, projectsWithWorkflows]) => {
      container.innerHTML = '';
      if (stories.length === 0) {
        container.innerHTML = '<p class="backlog-empty">No stories in backlog.</p>';
        return;
      }
      const grid = document.createElement('div');
      grid.className = 'backlog-grid backlog-grid--sortable';
      stories.forEach((story) => {
        const card = renderBacklogCard(story, projectsWithWorkflows, () => renderBacklog(container));
        grid.appendChild(card);
      });
      container.appendChild(grid);
      attachBacklogDragAndDrop(container, grid);
    })
    .catch((err) => {
      container.innerHTML = `<p class="board-error">${escapeHtml(err.message)}</p>`;
      if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
    });
}

function getBacklogOrderFromGrid(grid) {
  const cards = grid.querySelectorAll('.backlog-card[data-story-id]');
  return Array.from(cards).map((el) => el.dataset.storyId);
}

function attachBacklogDragAndDrop(container, grid) {
  let draggedCard = null;

  grid.querySelectorAll('.backlog-card').forEach((card) => {
    card.addEventListener('dragstart', (e) => {
      draggedCard = card;
      e.dataTransfer.setData('text/plain', card.dataset.storyId);
      e.dataTransfer.effectAllowed = 'move';
      card.classList.add('backlog-card--dragging');
    });
    card.addEventListener('dragend', () => {
      card.classList.remove('backlog-card--dragging');
      grid.querySelectorAll('.backlog-card').forEach((c) => c.classList.remove('backlog-card--drop-target'));
      draggedCard = null;
    });
    card.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      if (draggedCard && card !== draggedCard) {
        grid.querySelectorAll('.backlog-card').forEach((c) => c.classList.remove('backlog-card--drop-target'));
        card.classList.add('backlog-card--drop-target');
      }
    });
    card.addEventListener('dragleave', () => {
      card.classList.remove('backlog-card--drop-target');
    });
    card.addEventListener('drop', async (e) => {
      e.preventDefault();
      card.classList.remove('backlog-card--drop-target');
      if (!draggedCard || draggedCard === card) return;
      const orderedIds = getBacklogOrderFromGrid(grid);
      const draggedId = draggedCard.dataset.storyId;
      const fromIdx = orderedIds.indexOf(draggedId);
      const toIdx = orderedIds.indexOf(card.dataset.storyId);
      if (fromIdx === -1 || toIdx === -1) return;
      orderedIds.splice(fromIdx, 1);
      orderedIds.splice(toIdx, 0, draggedId);
      try {
        const res = await fetch('/api/backlog/order', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ orderedStoryIds: orderedIds }),
        });
        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          throw new Error(data.error || res.statusText);
        }
        if (typeof window.showToast === 'function') window.showToast('Backlog order saved', 'success');
        renderBacklog(container);
      } catch (err) {
        if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
      }
    });
  });
}

window.TraceHqBacklog = {
  loadBacklog,
  loadProjectsWithWorkflows,
  renderBacklog,
};
