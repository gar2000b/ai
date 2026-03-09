/**
 * TRACE H.Q. — Create Workflow modal (Workflows section only).
 * Workflow: code, name, description; stages: stage_name + stage_role per row.
 */

const STAGE_ROLES = ['owner', 'dev', 'unit-test', 'integration-test', 'performance-test', 'devops'];

function getModalRoot() {
  let root = document.getElementById('create-workflow-modal-root');
  if (!root) {
    root = document.createElement('div');
    root.id = 'create-workflow-modal-root';
    document.body.appendChild(root);
  }
  return root;
}

function closeModal() {
  const overlay = document.getElementById('create-workflow-overlay');
  if (overlay) overlay.remove();
}

function escapeHtml(s) {
  if (s == null) return '';
  const el = document.createElement('span');
  el.textContent = s;
  return el.innerHTML;
}

function renderStageRow(index, stage = { stage_name: '', stage_role: 'owner' }) {
  const rolesOptions = STAGE_ROLES.map(
    (r) => `<option value="${escapeHtml(r)}" ${r === (stage.stage_role || 'owner') ? 'selected' : ''}>${escapeHtml(r)}</option>`
  ).join('');
  return `
    <div class="workflow-stage-row" data-stage-index="${index}">
      <input type="text" class="workflow-stage-name" placeholder="Stage name" value="${escapeHtml(stage.stage_name || '')}" maxlength="64" />
      <select class="workflow-stage-role" aria-label="Stage role">${rolesOptions}</select>
      <div class="workflow-stage-actions">
        <button type="button" class="workflow-stage-move workflow-stage-move-up" aria-label="Move up" title="Move up">&uarr;</button>
        <button type="button" class="workflow-stage-move workflow-stage-move-down" aria-label="Move down" title="Move down">&darr;</button>
        <button type="button" class="workflow-stage-remove" aria-label="Remove stage" title="Remove">&times;</button>
      </div>
    </div>
  `;
}

function openCreateWorkflowModal(projectId, projectName, onCreated) {
  const root = getModalRoot();
  root.innerHTML = `
    <div id="create-workflow-overlay" class="modal-overlay">
      <div class="modal-content modal-content--create-workflow">
        <div class="modal-header">
          <h2 class="modal-title">Create Workflow</h2>
          <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
        </div>
        <p class="modal-subtitle">Adding to project: <strong>${escapeHtml(projectName)}</strong></p>
        <form id="create-workflow-form" class="modal-form">
          <div class="form-section">
            <div class="form-row">
              <label for="create-workflow-code">Code</label>
              <input type="text" id="create-workflow-code" name="code" required maxlength="32" placeholder="e.g. development, devops" />
              <span class="form-hint">Unique per project (lowercase, no spaces).</span>
            </div>
            <div class="form-row">
              <label for="create-workflow-name">Name</label>
              <input type="text" id="create-workflow-name" name="name" required maxlength="255" placeholder="e.g. Development" />
            </div>
            <div class="form-row">
              <label for="create-workflow-description">Description (optional)</label>
              <textarea id="create-workflow-description" name="description" rows="2" placeholder="e.g. Deliver features from implementation through integration validation."></textarea>
            </div>
          </div>
          <div class="form-section">
            <div class="form-row form-row--head">
              <label>Stages</label>
              <button type="button" id="workflow-add-stage" class="modal-btn modal-btn--secondary">Add stage</button>
            </div>
            <div class="workflow-stages-header">
              <span class="workflow-stages-header__name">Stage Name</span>
              <span class="workflow-stages-header__role">Stage Role</span>
              <span class="workflow-stages-header__actions"></span>
            </div>
            <div id="workflow-stages-list" class="workflow-stages-list">
              ${renderStageRow(0, { stage_name: 'Todo', stage_role: 'owner' })}
              ${renderStageRow(1, { stage_name: 'Done', stage_role: 'owner' })}
            </div>
          </div>
          <div class="modal-actions">
            <button type="submit" class="modal-btn modal-btn--primary">Create</button>
            <button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  `;

  const overlay = document.getElementById('create-workflow-overlay');
  const form = document.getElementById('create-workflow-form');
  const stagesList = document.getElementById('workflow-stages-list');
  const addStageBtn = document.getElementById('workflow-add-stage');

  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && document.getElementById('create-workflow-overlay')) {
      closeModal();
      document.removeEventListener('keydown', onEscape);
    }
  });

  overlay.querySelectorAll('[data-action="cancel"]').forEach((btn) => btn.addEventListener('click', closeModal));

  addStageBtn.addEventListener('click', () => {
    const rows = stagesList.querySelectorAll('.workflow-stage-row');
    const index = rows.length;
    stagesList.insertAdjacentHTML('beforeend', renderStageRow(index, { stage_name: 'In Progress', stage_role: 'dev' }));
  });

  stagesList.addEventListener('click', (e) => {
    const removeBtn = e.target.closest('.workflow-stage-remove');
    if (removeBtn) {
      removeBtn.closest('.workflow-stage-row').remove();
      return;
    }
    const row = e.target.closest('.workflow-stage-row');
    if (!row) return;
    const moveUp = e.target.closest('.workflow-stage-move-up');
    const moveDown = e.target.closest('.workflow-stage-move-down');
    if (moveUp && row.previousElementSibling) {
      stagesList.insertBefore(row, row.previousElementSibling);
    } else if (moveDown && row.nextElementSibling) {
      stagesList.insertBefore(row.nextElementSibling, row);
    }
  });

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const code = (document.getElementById('create-workflow-code').value || '').trim().toLowerCase().replace(/\s+/g, '-');
    const name = (document.getElementById('create-workflow-name').value || '').trim();
    const description = (document.getElementById('create-workflow-description').value || '').trim() || null;
    if (!code || !name) {
      if (typeof window.showToast === 'function') window.showToast('Code and name are required', 'error');
      return;
    }
    const rows = stagesList.querySelectorAll('.workflow-stage-row');
    const stages = [];
    rows.forEach((row) => {
      const stageName = (row.querySelector('.workflow-stage-name').value || '').trim();
      const stageRole = (row.querySelector('.workflow-stage-role').value || 'owner').trim();
      if (stageName) stages.push({ stage_name: stageName, stage_role: stageRole });
    });
    if (stages.length === 0) {
      if (typeof window.showToast === 'function') window.showToast('At least one stage is required', 'error');
      return;
    }
    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating…';
    fetch(`/api/projects/${projectId}/workflows`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, name, description, stages })
    })
      .then((res) => {
        if (!res.ok) return res.json().then((d) => Promise.reject(new Error(d.error || res.statusText)));
        return res.json();
      })
      .then((workflow) => {
        closeModal();
        if (typeof window.showToast === 'function') window.showToast(`Workflow "${workflow.name}" created`, 'success');
        if (typeof onCreated === 'function') onCreated();
      })
      .catch((err) => {
        if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create';
      });
  });

  setTimeout(() => document.getElementById('create-workflow-code').focus(), 50);
}

window.TraceHqWorkflowCreateModal = {
  openCreateWorkflowModal,
  closeCreateWorkflowModal: closeModal
};
