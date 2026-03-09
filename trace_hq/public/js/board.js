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
  const isRefresh = container.querySelector('.board-section') != null;

  if (container._dragendCleanup) {
    document.removeEventListener('dragend', container._dragendCleanup);
    container._dragendCleanup = null;
  }
  if (!isRefresh) {
    container.innerHTML = '<p class="board-loading">Loading board…</p>';
  }
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

      workflows.forEach((wf, index) => {
        const section = document.createElement('div');
        section.className = 'board-section';
        section.innerHTML = `
          <div class="board-section-header">
            <h2 class="board-section-title">${escapeHtml(wf.name)}</h2>
            <div class="board-section-actions">
              <button type="button" class="board-section-move board-section-move-up" aria-label="Move workflow up" title="Move up">&uarr;</button>
              <button type="button" class="board-section-move board-section-move-down" aria-label="Move workflow down" title="Move down">&darr;</button>
            </div>
            <a href="#" class="board-section-edit" data-workflow-id="${escapeHtml(String(wf.id))}" aria-label="Edit workflow">Edit</a>
            <a href="#" class="board-section-delete" data-workflow-id="${escapeHtml(String(wf.id))}" data-workflow-name="${escapeHtml(wf.name)}" aria-label="Delete workflow">Delete</a>
          </div>
        `;
        const editLink = section.querySelector('.board-section-edit');
        editLink.addEventListener('click', (e) => {
          e.preventDefault();
          if (window.TraceHqWorkflowCreateModal && typeof window.TraceHqWorkflowCreateModal.openEditWorkflowModal === 'function') {
            window.TraceHqWorkflowCreateModal.openEditWorkflowModal(projectId, projectName, wf, refreshBoard);
          }
        });
        const deleteLink = section.querySelector('.board-section-delete');
        deleteLink.addEventListener('click', (e) => {
          e.preventDefault();
          openDeleteWorkflowModal(projectId, projectName, wf.id, wf.name, refreshBoard);
        });
        const moveUp = section.querySelector('.board-section-move-up');
        const moveDown = section.querySelector('.board-section-move-down');
        moveUp.disabled = index === 0;
        moveDown.disabled = index === workflows.length - 1;
        const reorder = (newOrder) => {
          fetch(`/api/projects/${projectId}/workflows/order`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ orderedWorkflowIds: newOrder })
          })
            .then((res) => {
              if (!res.ok) return res.json().then((d) => Promise.reject(new Error(d.error || res.statusText)));
              refreshBoard();
            })
            .catch((err) => {
              if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
            });
        };
        moveUp.addEventListener('click', () => {
          if (index === 0) return;
          const newOrder = workflows.slice().map((w) => w.id);
          [newOrder[index - 1], newOrder[index]] = [newOrder[index], newOrder[index - 1]];
          reorder(newOrder);
        });
        moveDown.addEventListener('click', () => {
          if (index === workflows.length - 1) return;
          const newOrder = workflows.slice().map((w) => w.id);
          [newOrder[index], newOrder[index + 1]] = [newOrder[index + 1], newOrder[index]];
          reorder(newOrder);
        });
        const columns = document.createElement('div');
        columns.className = 'workflow-columns';

        const refreshBoard = () => renderBoard(container, projectId, projectName);

        (wf.stages || []).forEach((stage) => {
          const col = document.createElement('div');
          col.className = 'stage-column';
          col.dataset.stageId = String(stage.id);
          col.dataset.workflowId = String(wf.id);
          col.innerHTML = `<div class="stage-column-header">${escapeHtml(stage.stage_name)}</div>`;
          const cardsContainer = document.createElement('div');
          cardsContainer.className = 'stage-cards';

          const stageStories = storiesByStage[stage.id] || [];
          stageStories.forEach((story) => {
            const card = window.TraceHqStoryCard.renderStoryCard(story, wf.stages, refreshBoard);
            cardsContainer.appendChild(card);
          });

          col.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            col.classList.add('drop-target');
          });
          col.addEventListener('dragleave', (e) => {
            if (!col.contains(e.relatedTarget)) col.classList.remove('drop-target');
          });
          col.addEventListener('drop', async (e) => {
            e.preventDefault();
            col.classList.remove('drop-target');
            const stageId = parseInt(col.dataset.stageId, 10);
            if (Number.isNaN(stageId)) return;
            let payload;
            try {
              payload = JSON.parse(e.dataTransfer.getData('application/json') || '{}');
            } catch {
              return;
            }
            const { storyId, currentStageId } = payload;
            if (!storyId || stageId === currentStageId) return;
            try {
              const res = await fetch(`/api/stories/${storyId}/stage`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ workflow_stage_id: stageId }),
              });
              if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.error || res.statusText);
              }
              refreshBoard();
            } catch (err) {
              if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
            }
          });

          col.appendChild(cardsContainer);
          columns.appendChild(col);
        });

        section.appendChild(columns);
        container.appendChild(section);
      });

      container._dragendCleanup = () => {
        container.querySelectorAll('.stage-column.drop-target').forEach((el) => el.classList.remove('drop-target'));
      };
      document.addEventListener('dragend', container._dragendCleanup);
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

function closeDeleteWorkflowModal() {
  const overlay = document.getElementById('workflow-delete-overlay');
  if (overlay) overlay.remove();
}

function openDeleteWorkflowModal(projectId, projectName, workflowId, workflowName, onDeleted) {
  const existing = document.getElementById('workflow-delete-overlay');
  if (existing) existing.remove();
  let root = document.getElementById('workflow-delete-modal-root');
  if (!root) {
    root = document.createElement('div');
    root.id = 'workflow-delete-modal-root';
    document.body.appendChild(root);
  }
  const name = (workflowName && String(workflowName).trim()) ? escapeHtml(workflowName) : ('Workflow #' + workflowId);
  root.innerHTML = `
    <div id="workflow-delete-overlay" class="modal-overlay">
      <div class="modal-content modal-content--workflow-delete">
        <div class="modal-header">
          <h2 class="modal-title">Delete workflow</h2>
          <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
        </div>
        <div class="modal-body">
          <p>Are you sure you wish to delete the workflow <strong>${name}</strong>?</p>
          <p class="workflow-delete-note">The workflow will be hidden from the board. Stories in this workflow will no longer appear until the workflow is restored. The data is not removed from the database.</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="modal-btn modal-btn--danger" data-action="delete">Delete</button>
          <button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Cancel</button>
        </div>
      </div>
    </div>
  `;
  const overlay = document.getElementById('workflow-delete-overlay');
  overlay.querySelectorAll('[data-action="cancel"]').forEach((btn) => {
    btn.addEventListener('click', closeDeleteWorkflowModal);
  });
  overlay.querySelector('[data-action="delete"]').addEventListener('click', () => {
    fetch(`/api/projects/${projectId}/workflows/${workflowId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deleted: true }),
    })
      .then((res) => {
        if (!res.ok) return res.json().then((d) => Promise.reject(new Error(d.error || res.statusText)));
        closeDeleteWorkflowModal();
        if (typeof onDeleted === 'function') onDeleted();
        if (typeof window.showToast === 'function') window.showToast('Workflow deleted', 'success');
      })
      .catch((err) => {
        if (typeof window.showToast === 'function') window.showToast(err.message || 'Failed to delete workflow', 'error');
      });
  });
  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && document.getElementById('workflow-delete-overlay')) {
      closeDeleteWorkflowModal();
      document.removeEventListener('keydown', onEscape);
    }
  });
}

window.TraceHqBoard = {
  getLastProjectId,
  setLastProjectId,
  loadProjects,
  renderBoard,
  openDeleteWorkflowModal,
  closeDeleteWorkflowModal,
};
