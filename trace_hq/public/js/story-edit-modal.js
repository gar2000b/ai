/**
 * TRACE H.Q. — Edit story modal. Open with 'e' when a story card is selected.
 * Fetches full story, shows form with all editable fields, Save/Cancel.
 */

function escapeHtml(s) {
  if (s == null) return '';
  const el = document.createElement('span');
  el.textContent = s;
  return el.innerHTML;
}

const STORY_TYPES = ['dev', 'writing', 'docs', 'research', 'infrastructure', 'performance', 'manual'];
const PRIORITIES = ['low', 'medium', 'high', 'critical'];
const BLOCKED_BY = ['dependency', 'environment', 'owner', 'external', 'unknown'];
const REVIEW_STATUSES = ['not-required', 'pending', 'approved', 'rejected'];

function getModalContainer() {
  let el = document.getElementById('story-edit-modal-root');
  if (!el) {
    el = document.createElement('div');
    el.id = 'story-edit-modal-root';
    document.body.appendChild(el);
  }
  return el;
}

function closeModal() {
  const overlay = document.getElementById('story-edit-overlay');
  if (overlay) overlay.remove();
}

function openEditModal(storyId, onSaved) {
  const existing = document.getElementById('story-edit-overlay');
  if (existing) existing.remove();
  const root = getModalContainer();
  root.innerHTML = '<div id="story-edit-overlay" class="modal-overlay"><div class="modal-content modal-content--story-edit"><p class="modal-loading">Loading story…</p></div></div>';
  const overlay = root.querySelector('#story-edit-overlay');

  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && overlay.parentNode) {
      overlay.remove();
      document.removeEventListener('keydown', onEscape);
    }
  });

  Promise.all([
    fetch('/api/stories/' + encodeURIComponent(storyId)).then((r) => (r.ok ? r.json() : Promise.reject(new Error('Story not found')))),
    fetch('/api/agents').then((r) => (r.ok ? r.json() : []))
  ])
    .then(([story, agents]) => {
      renderModalForm(overlay, story, agents, storyId, onSaved);
    })
    .catch((err) => {
      const content = overlay.querySelector('.modal-content');
      content.innerHTML = '<p class="board-error">' + escapeHtml(err.message) + '</p><button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Close</button>';
      content.querySelector('[data-action="cancel"]').addEventListener('click', () => overlay.remove());
    });
}

function renderModalForm(overlay, story, agents, storyId, onSaved) {
  const dependenciesStr = Array.isArray(story.dependencies) ? story.dependencies.join(', ') : '';
  const relatedStr = Array.isArray(story.related) ? story.related.join(', ') : '';

  const content = overlay.querySelector('.modal-content');
  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">Edit story ${escapeHtml(story.id)}</h2>
      <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
    </div>
    <form id="story-edit-form" class="modal-form">
      <input type="hidden" name="id" value="${escapeHtml(story.id)}" />

      <div class="form-section">
        <h3 class="form-section-title">Identity</h3>
        <div class="form-row">
          <label for="story-edit-id">ID</label>
          <input type="text" id="story-edit-id" value="${escapeHtml(story.id)}" readonly class="readonly" />
        </div>
        <div class="form-row">
          <label for="story-edit-title">Title <span class="required">*</span></label>
          <input type="text" id="story-edit-title" name="title" value="${escapeHtml(story.title || '')}" required maxlength="512" />
        </div>
        <div class="form-row">
          <label for="story-edit-description">Description</label>
          <textarea id="story-edit-description" name="description" rows="3">${escapeHtml(story.description || '')}</textarea>
        </div>
        <div class="form-row form-row--inline">
          <div>
            <label for="story-edit-type">Type</label>
            <select id="story-edit-type" name="type">${STORY_TYPES.map((t) => '<option value="' + t + '"' + (story.type === t ? ' selected' : '') + '>' + escapeHtml(t) + '</option>').join('')}</select>
          </div>
          <div>
            <label for="story-edit-priority">Priority</label>
            <select id="story-edit-priority" name="priority">${PRIORITIES.map((p) => '<option value="' + p + '"' + (story.priority === p ? ' selected' : '') + '>' + escapeHtml(p) + '</option>').join('')}</select>
          </div>
        </div>
      </div>

      <div class="form-section">
        <h3 class="form-section-title">Assignment &amp; blocking</h3>
        <div class="form-row">
          <label for="story-edit-assignee">Assignee</label>
          <select id="story-edit-assignee" name="assignee_id">
            <option value="">Unassigned</option>
            ${(agents || []).map((a) => '<option value="' + a.id + '"' + (story.assignee_id === a.id ? ' selected' : '') + '>' + escapeHtml(a.name) + ' (' + escapeHtml(a.role_code) + ')</option>').join('')}
          </select>
        </div>
        <div class="form-row form-row--inline">
          <div class="form-row-checkbox">
            <input type="checkbox" id="story-edit-blocked" name="blocked" ${story.blocked ? ' checked' : ''} />
            <label for="story-edit-blocked">Blocked</label>
          </div>
        </div>
        <div class="form-row">
          <label for="story-edit-blocked-reason">Blocked reason</label>
          <input type="text" id="story-edit-blocked-reason" name="blocked_reason" value="${escapeHtml(story.blocked_reason || '')}" maxlength="512" />
        </div>
        <div class="form-row">
          <label for="story-edit-blocked-by">Blocked by</label>
          <select id="story-edit-blocked-by" name="blocked_by">
            <option value="">—</option>
            ${BLOCKED_BY.map((b) => '<option value="' + b + '"' + (story.blocked_by === b ? ' selected' : '') + '>' + escapeHtml(b) + '</option>').join('')}
          </select>
        </div>
      </div>

      <div class="form-section">
        <h3 class="form-section-title">Dependencies &amp; related</h3>
        <div class="form-row">
          <label for="story-edit-dependencies">Depends on (story IDs, comma-separated)</label>
          <input type="text" id="story-edit-dependencies" name="dependencies" value="${escapeHtml(dependenciesStr)}" placeholder="e.g. S001, S003" />
        </div>
        <div class="form-row">
          <label for="story-edit-related">Related stories (IDs, comma-separated)</label>
          <input type="text" id="story-edit-related" name="related" value="${escapeHtml(relatedStr)}" placeholder="e.g. S002, S005" />
        </div>
      </div>

      <div class="form-section">
        <h3 class="form-section-title">Execution &amp; review</h3>
        <div class="form-row">
          <label for="story-edit-branch">Branch</label>
          <input type="text" id="story-edit-branch" name="branch" value="${escapeHtml(story.branch || '')}" maxlength="255" />
        </div>
        <div class="form-row">
          <label for="story-edit-review-reference">Review reference (PR link)</label>
          <input type="text" id="story-edit-review-reference" name="review_reference" value="${escapeHtml(story.review_reference || '')}" maxlength="512" />
        </div>
        <div class="form-row">
          <label for="story-edit-artifact">Artifact (path or deliverable)</label>
          <input type="text" id="story-edit-artifact" name="artifact" value="${escapeHtml(story.artifact || '')}" maxlength="512" />
        </div>
        <div class="form-row">
          <label for="story-edit-review-status">Review status</label>
          <select id="story-edit-review-status" name="review_status">${REVIEW_STATUSES.map((s) => '<option value="' + s + '"' + (story.review_status === s ? ' selected' : '') + '>' + escapeHtml(s) + '</option>').join('')}</select>
        </div>
        <div class="form-row">
          <label for="story-edit-review-notes">Review notes</label>
          <textarea id="story-edit-review-notes" name="review_notes" rows="2">${escapeHtml(story.review_notes || '')}</textarea>
        </div>
        <div class="form-row">
          <label for="story-edit-rejection-count">Rejection count</label>
          <input type="number" id="story-edit-rejection-count" name="rejection_count" value="${story.rejection_count != null ? story.rejection_count : 0}" min="0" />
        </div>
      </div>

      <div class="form-section">
        <h3 class="form-section-title">Notes</h3>
        <div class="form-row">
          <label for="story-edit-acceptance-criteria">Acceptance criteria</label>
          <textarea id="story-edit-acceptance-criteria" name="acceptance_criteria" rows="3">${escapeHtml(story.acceptance_criteria || '')}</textarea>
        </div>
        <div class="form-row">
          <label for="story-edit-implementation-notes">Implementation notes</label>
          <textarea id="story-edit-implementation-notes" name="implementation_notes" rows="3">${escapeHtml(story.implementation_notes || '')}</textarea>
        </div>
      </div>

      <div class="modal-actions">
        <button type="submit" class="modal-btn modal-btn--primary">Save</button>
        <button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Cancel</button>
      </div>
    </form>
  `;

  content.querySelectorAll('[data-action="cancel"]').forEach((btn) => btn.addEventListener('click', () => overlay.remove()));

  content.querySelector('#story-edit-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const dependenciesInput = form.querySelector('[name="dependencies"]');
    const relatedInput = form.querySelector('[name="related"]');
    const dependencies = (dependenciesInput.value || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    const related = (relatedInput.value || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    const payload = {
      title: form.querySelector('[name="title"]').value.trim(),
      description: form.querySelector('[name="description"]').value.trim() || null,
      type: form.querySelector('[name="type"]').value,
      priority: form.querySelector('[name="priority"]').value,
      assignee_id: form.querySelector('[name="assignee_id"]').value || null,
      blocked: form.querySelector('[name="blocked"]').checked,
      blocked_reason: form.querySelector('[name="blocked_reason"]').value.trim() || null,
      blocked_by: form.querySelector('[name="blocked_by"]').value || null,
      acceptance_criteria: form.querySelector('[name="acceptance_criteria"]').value.trim() || null,
      implementation_notes: form.querySelector('[name="implementation_notes"]').value.trim() || null,
      branch: form.querySelector('[name="branch"]').value.trim() || null,
      review_reference: form.querySelector('[name="review_reference"]').value.trim() || null,
      artifact: form.querySelector('[name="artifact"]').value.trim() || null,
      review_status: form.querySelector('[name="review_status"]').value,
      review_notes: form.querySelector('[name="review_notes"]').value.trim() || null,
      rejection_count: parseInt(form.querySelector('[name="rejection_count"]').value, 10) || 0,
      dependencies,
      related
    };

    const saveBtn = form.querySelector('button[type="submit"]');
    saveBtn.disabled = true;
    saveBtn.textContent = 'Saving…';
    try {
      const res = await fetch('/api/stories/' + encodeURIComponent(storyId), {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.error || res.statusText);
      }
      if (typeof window.showToast === 'function') window.showToast('Story saved', 'success');
      overlay.remove();
      if (typeof onSaved === 'function') onSaved();
    } catch (err) {
      if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
      saveBtn.disabled = false;
      saveBtn.textContent = 'Save';
    }
  });
}

function getSelectedStoryId() {
  const card = document.querySelector('.story-card.selected, .backlog-card.selected');
  return card ? card.dataset.storyId || null : null;
}

function getSelectedStoryTitle() {
  const card = document.querySelector('.story-card.selected, .backlog-card.selected');
  if (!card) return null;
  const titleEl = card.querySelector('.story-card-title');
  return titleEl ? titleEl.textContent.trim() : null;
}

function closeDeleteConfirmModal() {
  const overlay = document.getElementById('story-delete-overlay');
  if (overlay) overlay.remove();
}

function openDeleteConfirmModal(storyId, storyTitle, onDeleted) {
  const existing = document.getElementById('story-delete-overlay');
  if (existing) existing.remove();
  const root = getModalContainer();
  const rawTitle = (storyTitle && storyTitle.trim()) ? storyTitle.trim() : '';
  const displayTitle = rawTitle ? escapeHtml(rawTitle) : ('Story ' + escapeHtml(storyId));
  root.innerHTML = `
    <div id="story-delete-overlay" class="modal-overlay">
      <div class="modal-content modal-content--story-delete">
        <div class="modal-header">
          <h2 class="modal-title">Delete story</h2>
          <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
        </div>
        <div class="modal-body">
          <p>Are you sure you wish to delete the story <strong id="story-delete-id">${escapeHtml(storyId)}</strong>?</p>
          <p class="story-delete-title"><em>${displayTitle}</em></p>
          <p class="story-delete-note">The story will be hidden from TRACE H.Q. It is not removed from the database and can be restored later if needed.</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="modal-btn modal-btn--danger" data-action="delete">Delete</button>
          <button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Cancel</button>
        </div>
      </div>
    </div>
  `;
  const overlay = document.getElementById('story-delete-overlay');
  overlay.querySelectorAll('[data-action="cancel"]').forEach((btn) => {
    btn.addEventListener('click', closeDeleteConfirmModal);
  });
  overlay.querySelector('[data-action="delete"]').addEventListener('click', () => {
    fetch('/api/stories/' + encodeURIComponent(storyId), {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deleted: true }),
    })
      .then((res) => {
        if (!res.ok) return res.json().then((d) => Promise.reject(new Error(d.error || res.statusText)));
        closeDeleteConfirmModal();
        if (typeof onDeleted === 'function') onDeleted();
        if (typeof window.showToast === 'function') window.showToast('Story deleted', 'success');
      })
      .catch((err) => {
        if (typeof window.showToast === 'function') window.showToast(err.message || 'Failed to delete story', 'error');
      });
  });
  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && document.getElementById('story-delete-overlay')) {
      closeDeleteConfirmModal();
      document.removeEventListener('keydown', onEscape);
    }
  });
}

function initEditStoryOnKeyE() {
  document.addEventListener('keydown', (e) => {
    if (e.key !== 'e' && e.key !== 'E') return;
    if (document.getElementById('create-project-overlay') || document.getElementById('create-workflow-overlay') || document.getElementById('story-create-overlay') || document.getElementById('story-edit-overlay') || document.getElementById('story-delete-overlay')) return;
    if (e.target && (e.target.closest('#create-project-overlay') || e.target.closest('#create-workflow-overlay') || e.target.closest('#story-create-overlay') || e.target.closest('#story-edit-overlay') || e.target.closest('#story-delete-overlay'))) return;
    const storyId = getSelectedStoryId();
    if (!storyId) return;
    e.preventDefault();
    const onSaved = () => {
      const boardContainer = document.getElementById('board-container');
      const backlogContainer = document.getElementById('backlog-container');
      if (boardContainer && boardContainer.querySelector('.board-section')) {
        const select = document.getElementById('project-select');
        if (select && select.selectedIndex >= 0) {
          const opt = select.options[select.selectedIndex];
          if (opt && window.TraceHqBoard) window.TraceHqBoard.renderBoard(boardContainer, parseInt(opt.value, 10), opt.text);
        }
      }
      if (backlogContainer && window.TraceHqBacklog) window.TraceHqBacklog.renderBacklog(backlogContainer);
    };
    openEditModal(storyId, onSaved);
  });
}

function initDeleteStoryOnKeyD() {
  document.addEventListener('keydown', (e) => {
    if (e.key !== 'd' && e.key !== 'D') return;
    if (document.getElementById('create-project-overlay') || document.getElementById('create-workflow-overlay') || document.getElementById('story-create-overlay') || document.getElementById('story-edit-overlay') || document.getElementById('story-delete-overlay')) return;
    if (e.target && (e.target.closest('#create-project-overlay') || e.target.closest('#create-workflow-overlay') || e.target.closest('#story-create-overlay') || e.target.closest('#story-edit-overlay') || e.target.closest('#story-delete-overlay'))) return;
    const storyId = getSelectedStoryId();
    if (!storyId) return;
    e.preventDefault();
    const storyTitle = getSelectedStoryTitle();
    const onDeleted = () => {
      const boardContainer = document.getElementById('board-container');
      const backlogContainer = document.getElementById('backlog-container');
      if (boardContainer && boardContainer.querySelector('.board-section')) {
        const select = document.getElementById('project-select');
        if (select && select.selectedIndex >= 0) {
          const opt = select.options[select.selectedIndex];
          if (opt && window.TraceHqBoard) window.TraceHqBoard.renderBoard(boardContainer, parseInt(opt.value, 10), opt.text);
        }
      }
      if (backlogContainer && window.TraceHqBacklog) window.TraceHqBacklog.renderBacklog(backlogContainer);
    };
    openDeleteConfirmModal(storyId, storyTitle, onDeleted);
  });
}

if (typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initEditStoryOnKeyE();
      initDeleteStoryOnKeyD();
    });
  } else {
    initEditStoryOnKeyE();
    initDeleteStoryOnKeyD();
  }
}

window.TraceHqStoryEditModal = {
  openEditModal,
  closeModal,
  openDeleteConfirmModal,
  closeDeleteConfirmModal,
  getSelectedStoryId,
  getSelectedStoryTitle
};

// ----- Create story modal (C key from Workflows/Backlog) -----
function getCreateModalRoot() {
  let el = document.getElementById('story-create-modal-root');
  if (!el) {
    el = document.createElement('div');
    el.id = 'story-create-modal-root';
    document.body.appendChild(el);
  }
  return el;
}

function closeCreateModal() {
  const overlay = document.getElementById('story-create-overlay');
  if (overlay) overlay.remove();
}

function buildPlacementOptions(projectsWithWorkflows) {
  let html = '<option value="backlog">Backlog</option>';
  if (!projectsWithWorkflows || !projectsWithWorkflows.length) return html;
  for (const project of projectsWithWorkflows) {
    if (!project.workflows || !project.workflows.length) continue;
    const opts = project.workflows
      .map((w) => `<option value="${w.id}:${w.firstStageId}">${escapeHtml(w.name)}</option>`)
      .join('');
    html += `<optgroup label="${escapeHtml(project.name)}">${opts}</optgroup>`;
  }
  return html;
}

function openCreateModal(onCreated) {
  const root = getCreateModalRoot();
  root.innerHTML = '<div id="story-create-overlay" class="modal-overlay"><div class="modal-content modal-content--story-edit"><p class="modal-loading">Loading…</p></div></div>';
  const overlay = document.getElementById('story-create-overlay');
  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && document.getElementById('story-create-overlay')) {
      closeCreateModal();
      document.removeEventListener('keydown', onEscape);
    }
  });
  Promise.all([
    fetch('/api/stories/next-id').then((r) => (r.ok ? r.json() : { nextId: 'S001' })),
    fetch('/api/agents').then((r) => (r.ok ? r.json() : [])),
    fetch('/api/projects-with-workflows').then((r) => (r.ok ? r.json() : []))
  ])
    .then(([idRes, agents, projectsWithWorkflows]) => {
      const nextId = (idRes && idRes.nextId) || 'S001';
      renderCreateForm(overlay, nextId, agents, projectsWithWorkflows, onCreated);
    })
    .catch((err) => {
      const content = overlay.querySelector('.modal-content');
      content.innerHTML = '<p class="board-error">' + escapeHtml(err.message) + '</p><button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Close</button>';
      content.querySelectorAll('[data-action="cancel"]').forEach((btn) => btn.addEventListener('click', closeCreateModal));
    });
}

function renderCreateForm(overlay, nextId, agents, projectsWithWorkflows, onCreated) {
  const placementOptions = buildPlacementOptions(projectsWithWorkflows);
  const content = overlay.querySelector('.modal-content');
  content.innerHTML = `
    <div class="modal-header">
      <h2 class="modal-title">Create story</h2>
      <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
    </div>
    <form id="story-create-form" class="modal-form">
      <div class="form-section">
        <h3 class="form-section-title">Placement</h3>
        <div class="form-row">
          <label for="story-create-placement">Place new story in</label>
          <select id="story-create-placement" name="placement" required>${placementOptions}</select>
        </div>
      </div>
      <div class="form-section">
        <h3 class="form-section-title">Identity</h3>
        <div class="form-row">
          <label for="story-create-id">ID</label>
          <input type="text" id="story-create-id" value="${escapeHtml(nextId)}" readonly class="readonly" title="Auto-generated" />
        </div>
        <div class="form-row">
          <label for="story-create-title">Title <span class="required">*</span></label>
          <input type="text" id="story-create-title" name="title" required maxlength="512" placeholder="Short summary of the story" />
        </div>
        <div class="form-row">
          <label for="story-create-description">Description</label>
          <textarea id="story-create-description" name="description" rows="3" placeholder="Optional longer explanation"></textarea>
        </div>
        <div class="form-row form-row--inline">
          <div>
            <label for="story-create-type">Type</label>
            <select id="story-create-type" name="type">${STORY_TYPES.map((t) => '<option value="' + t + '"' + (t === 'dev' ? ' selected' : '') + '>' + escapeHtml(t) + '</option>').join('')}</select>
          </div>
          <div>
            <label for="story-create-priority">Priority</label>
            <select id="story-create-priority" name="priority">${PRIORITIES.map((p) => '<option value="' + p + '"' + (p === 'medium' ? ' selected' : '') + '>' + escapeHtml(p) + '</option>').join('')}</select>
          </div>
        </div>
      </div>
      <div class="form-section">
        <h3 class="form-section-title">Assignment &amp; blocking</h3>
        <div class="form-row">
          <label for="story-create-assignee">Assignee</label>
          <select id="story-create-assignee" name="assignee_id">
            <option value="">Unassigned</option>
            ${(agents || []).map((a) => '<option value="' + a.id + '">' + escapeHtml(a.name) + ' (' + escapeHtml(a.role_code) + ')</option>').join('')}
          </select>
        </div>
        <div class="form-row form-row--inline">
          <div class="form-row-checkbox">
            <input type="checkbox" id="story-create-blocked" name="blocked" />
            <label for="story-create-blocked">Blocked</label>
          </div>
        </div>
        <div class="form-row">
          <label for="story-create-blocked-reason">Blocked reason</label>
          <input type="text" id="story-create-blocked-reason" name="blocked_reason" maxlength="512" />
        </div>
        <div class="form-row">
          <label for="story-create-blocked-by">Blocked by</label>
          <select id="story-create-blocked-by" name="blocked_by">
            <option value="">—</option>
            ${BLOCKED_BY.map((b) => '<option value="' + b + '">' + escapeHtml(b) + '</option>').join('')}
          </select>
        </div>
      </div>
      <div class="form-section">
        <h3 class="form-section-title">Dependencies &amp; related</h3>
        <div class="form-row">
          <label for="story-create-dependencies">Depends on (story IDs, comma-separated)</label>
          <input type="text" id="story-create-dependencies" name="dependencies" placeholder="e.g. S001, S003" />
        </div>
        <div class="form-row">
          <label for="story-create-related">Related stories (IDs, comma-separated)</label>
          <input type="text" id="story-create-related" name="related" placeholder="e.g. S002, S005" />
        </div>
      </div>
      <div class="form-section">
        <h3 class="form-section-title">Execution &amp; review</h3>
        <div class="form-row">
          <label for="story-create-branch">Branch</label>
          <input type="text" id="story-create-branch" name="branch" maxlength="255" />
        </div>
        <div class="form-row">
          <label for="story-create-review-reference">Review reference (PR link)</label>
          <input type="text" id="story-create-review-reference" name="review_reference" maxlength="512" />
        </div>
        <div class="form-row">
          <label for="story-create-artifact">Artifact (path or deliverable)</label>
          <input type="text" id="story-create-artifact" name="artifact" maxlength="512" />
        </div>
        <div class="form-row">
          <label for="story-create-review-status">Review status</label>
          <select id="story-create-review-status" name="review_status">${REVIEW_STATUSES.map((s) => '<option value="' + s + '"' + (s === 'not-required' ? ' selected' : '') + '>' + escapeHtml(s) + '</option>').join('')}</select>
        </div>
        <div class="form-row">
          <label for="story-create-review-notes">Review notes</label>
          <textarea id="story-create-review-notes" name="review_notes" rows="2"></textarea>
        </div>
      </div>
      <div class="form-section">
        <h3 class="form-section-title">Notes</h3>
        <div class="form-row">
          <label for="story-create-acceptance-criteria">Acceptance criteria</label>
          <textarea id="story-create-acceptance-criteria" name="acceptance_criteria" rows="3"></textarea>
        </div>
        <div class="form-row">
          <label for="story-create-implementation-notes">Implementation notes</label>
          <textarea id="story-create-implementation-notes" name="implementation_notes" rows="3"></textarea>
        </div>
      </div>
      <div class="modal-actions">
        <button type="submit" class="modal-btn modal-btn--primary">Create</button>
        <button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Cancel</button>
      </div>
    </form>
  `;
  content.querySelectorAll('[data-action="cancel"]').forEach((btn) => btn.addEventListener('click', closeCreateModal));
  content.querySelector('#story-create-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const placementValue = form.querySelector('[name="placement"]').value;
    let placement;
    if (placementValue === 'backlog') {
      placement = 'backlog';
    } else {
      const colon = placementValue.indexOf(':');
      if (colon === -1) return;
      placement = { workflow_id: parseInt(placementValue.slice(0, colon), 10), workflow_stage_id: parseInt(placementValue.slice(colon + 1), 10) };
    }
    const dependencies = (form.querySelector('[name="dependencies"]').value || '').split(',').map((s) => s.trim()).filter(Boolean);
    const related = (form.querySelector('[name="related"]').value || '').split(',').map((s) => s.trim()).filter(Boolean);
    const payload = {
      title: form.querySelector('[name="title"]').value.trim(),
      description: form.querySelector('[name="description"]').value.trim() || null,
      type: form.querySelector('[name="type"]').value,
      priority: form.querySelector('[name="priority"]').value,
      assignee_id: form.querySelector('[name="assignee_id"]').value || null,
      blocked: form.querySelector('[name="blocked"]').checked,
      blocked_reason: form.querySelector('[name="blocked_reason"]').value.trim() || null,
      blocked_by: form.querySelector('[name="blocked_by"]').value || null,
      acceptance_criteria: form.querySelector('[name="acceptance_criteria"]').value.trim() || null,
      implementation_notes: form.querySelector('[name="implementation_notes"]').value.trim() || null,
      branch: form.querySelector('[name="branch"]').value.trim() || null,
      review_reference: form.querySelector('[name="review_reference"]').value.trim() || null,
      artifact: form.querySelector('[name="artifact"]').value.trim() || null,
      review_status: form.querySelector('[name="review_status"]').value,
      review_notes: form.querySelector('[name="review_notes"]').value.trim() || null,
      placement,
      dependencies,
      related
    };
    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating…';
    try {
      const res = await fetch('/api/stories', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.error || res.statusText);
      }
      if (typeof window.showToast === 'function') window.showToast('Story created', 'success');
      closeCreateModal();
      if (typeof onCreated === 'function') onCreated();
    } catch (err) {
      if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
      submitBtn.disabled = false;
      submitBtn.textContent = 'Create';
    }
  });
}

window.TraceHqStoryCreateModal = {
  openCreateModal,
  closeCreateModal
};
