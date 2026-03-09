/**
 * TRACE H.Q. — App entry: hash routing, last-viewed project, views, toast.
 */

function showToast(message, type) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = message;
  el.className = 'toast ' + (type || 'info');
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 4000);
}
window.showToast = showToast;

function hideAllViews() {
  document.querySelectorAll('.view').forEach((v) => v.classList.add('hidden'));
}

function showView(viewId) {
  hideAllViews();
  const el = document.getElementById(viewId);
  if (el) el.classList.remove('hidden');
}

function setActiveNav(route) {
  document.querySelectorAll('.nav-item').forEach((a) => {
    a.classList.toggle('active', a.getAttribute('data-route') === route);
  });
}

function closeCreateProjectModal() {
  const overlay = document.getElementById('create-project-overlay');
  if (overlay) overlay.remove();
}

function closeDeleteProjectModal() {
  const overlay = document.getElementById('project-delete-overlay');
  if (overlay) overlay.remove();
}

function closeEditProjectModal() {
  const overlay = document.getElementById('project-edit-overlay');
  if (overlay) overlay.remove();
}

function openEditProjectModal(projectId, currentName, onSaved) {
  const existing = document.getElementById('project-edit-overlay');
  if (existing) existing.remove();
  let root = document.getElementById('project-edit-modal-root');
  if (!root) {
    root = document.createElement('div');
    root.id = 'project-edit-modal-root';
    document.body.appendChild(root);
  }
  root.innerHTML = `
    <div id="project-edit-overlay" class="modal-overlay">
      <div class="modal-content modal-content--edit-project">
        <div class="modal-header">
          <h2 class="modal-title">Edit Project</h2>
          <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
        </div>
        <form id="project-edit-form" class="modal-form">
          <div class="form-section">
            <div class="form-row">
              <label for="project-edit-name">Project name</label>
              <input type="text" id="project-edit-name" name="name" required maxlength="255" placeholder="e.g. my-project" autofocus />
            </div>
          </div>
          <div class="modal-actions">
            <button type="submit" class="modal-btn modal-btn--primary">Save</button>
            <button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  `;
  const overlay = document.getElementById('project-edit-overlay');
  const form = document.getElementById('project-edit-form');
  const nameInput = document.getElementById('project-edit-name');
  nameInput.value = (currentName != null && String(currentName).trim()) ? String(currentName).trim() : '';
  overlay.querySelectorAll('[data-action="cancel"]').forEach((btn) => {
    btn.addEventListener('click', closeEditProjectModal);
  });
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const name = (nameInput.value && nameInput.value.trim()) || '';
    if (!name) return;
    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Saving…';
    fetch(`/api/projects/${projectId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name }),
    })
      .then((res) => {
        if (!res.ok) return res.json().then((d) => Promise.reject(new Error(d.error || res.statusText)));
        return res.json();
      })
      .then(() => {
        closeEditProjectModal();
        if (typeof onSaved === 'function') onSaved();
        if (typeof window.showToast === 'function') window.showToast('Project updated', 'success');
      })
      .catch((err) => {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Save';
        if (typeof window.showToast === 'function') window.showToast(err.message || 'Failed to update project', 'error');
      });
  });
  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && document.getElementById('project-edit-overlay')) {
      closeEditProjectModal();
      document.removeEventListener('keydown', onEscape);
    }
  });
}

function openDeleteProjectModal(projectId, projectName, onDeleted) {
  const existing = document.getElementById('project-delete-overlay');
  if (existing) existing.remove();
  let root = document.getElementById('project-delete-modal-root');
  if (!root) {
    root = document.createElement('div');
    root.id = 'project-delete-modal-root';
    document.body.appendChild(root);
  }
  const name = (projectName && String(projectName).trim()) ? escapeHtml(projectName) : ('Project #' + projectId);
  root.innerHTML = `
    <div id="project-delete-overlay" class="modal-overlay">
      <div class="modal-content modal-content--project-delete">
        <div class="modal-header">
          <h2 class="modal-title">Delete project</h2>
          <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
        </div>
        <div class="modal-body">
          <p>Are you sure you wish to delete the project <strong>${name}</strong>?</p>
          <p class="project-delete-note">The project will be hidden from the board. Its workflows and stories will no longer appear until the project is restored. The data is not removed from the database.</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="modal-btn modal-btn--danger" data-action="delete">Delete</button>
          <button type="button" class="modal-btn modal-btn--secondary" data-action="cancel">Cancel</button>
        </div>
      </div>
    </div>
  `;
  const overlay = document.getElementById('project-delete-overlay');
  overlay.querySelectorAll('[data-action="cancel"]').forEach((btn) => {
    btn.addEventListener('click', closeDeleteProjectModal);
  });
  overlay.querySelector('[data-action="delete"]').addEventListener('click', () => {
    fetch(`/api/projects/${projectId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deleted: true }),
    })
      .then((res) => {
        if (!res.ok) return res.json().then((d) => Promise.reject(new Error(d.error || res.statusText)));
        closeDeleteProjectModal();
        if (typeof onDeleted === 'function') onDeleted();
        if (typeof window.showToast === 'function') window.showToast('Project deleted', 'success');
      })
      .catch((err) => {
        if (typeof window.showToast === 'function') window.showToast(err.message || 'Failed to delete project', 'error');
      });
  });
  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && document.getElementById('project-delete-overlay')) {
      closeDeleteProjectModal();
      document.removeEventListener('keydown', onEscape);
    }
  });
}

function openCreateProjectModal(onCreated) {
  let root = document.getElementById('create-project-modal-root');
  if (!root) {
    root = document.createElement('div');
    root.id = 'create-project-modal-root';
    document.body.appendChild(root);
  }
  root.innerHTML = `
    <div id="create-project-overlay" class="modal-overlay">
      <div class="modal-content modal-content--create-project">
        <div class="modal-header">
          <h2 class="modal-title">Create Project</h2>
          <button type="button" class="modal-close" aria-label="Close" data-action="cancel">&times;</button>
        </div>
        <form id="create-project-form" class="modal-form">
          <div class="form-section">
            <div class="form-row">
              <label for="create-project-name">Project name</label>
              <input type="text" id="create-project-name" name="name" required maxlength="255" placeholder="e.g. my-new-project" autofocus />
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
  const overlay = document.getElementById('create-project-overlay');
  const form = document.getElementById('create-project-form');
  const nameInput = document.getElementById('create-project-name');

  document.addEventListener('keydown', function onEscape(e) {
    if (e.key === 'Escape' && document.getElementById('create-project-overlay')) {
      closeCreateProjectModal();
      document.removeEventListener('keydown', onEscape);
    }
  });

  overlay.querySelectorAll('[data-action="cancel"]').forEach((btn) => btn.addEventListener('click', closeCreateProjectModal));

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const name = (nameInput.value && nameInput.value.trim()) || '';
    if (!name) return;
    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating…';
    fetch('/api/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name })
    })
      .then((res) => {
        if (!res.ok) return res.json().then((d) => Promise.reject(new Error(d.error || res.statusText)));
        return res.json();
      })
      .then((project) => {
        closeCreateProjectModal();
        showToast('Project created', 'success');
        window.TraceHqBoard.setLastProjectId(project.id);
        if (typeof onCreated === 'function') onCreated();
      })
      .catch((err) => {
        showToast(err.message, 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create';
      });
  });

  setTimeout(() => nameInput && nameInput.focus(), 50);
}

function handleRoute() {
  const hash = (window.location.hash || '#/').slice(1) || '/';
  const path = hash.startsWith('/') ? hash : '/' + hash;
  const route = path === '/' ? 'home' : path.slice(1).split('/')[0];

  setActiveNav(route);

  if (route === 'home') {
    showView('view-home');
    return;
  }

  if (route === 'settings') {
    showView('view-settings');
    const container = document.getElementById('settings-container');
    if (container && window.TraceHqSettings) window.TraceHqSettings.renderSettingsPanel(container);
    return;
  }

  if (route === 'backlog') {
    showView('view-backlog');
    const container = document.getElementById('backlog-container');
    window.TraceHqBacklog.renderBacklog(container);
    return;
  }

  if (route === 'workflows') {
    showView('view-workflows');
    const container = document.getElementById('board-container');
    const select = document.getElementById('project-select');
    const createBtn = document.getElementById('create-project-btn');

    function refreshWorkflowsView() {
      window.TraceHqBoard.loadProjects()
        .then((projects) => {
          const editLink = document.getElementById('project-edit-link');
          const deleteLink = document.getElementById('project-delete-link');
          const selectedId = window.TraceHqBoard.getLastProjectId();
          if (projects.length === 0) {
            select.innerHTML = '<option value="">No projects</option>';
            container.innerHTML = '<p class="board-empty">No projects yet. Create one above.</p>';
            if (editLink) editLink.classList.add('hidden');
            if (deleteLink) deleteLink.classList.add('hidden');
            return;
          }
          select.innerHTML = projects.map((p) => `<option value="${p.id}" ${p.id === selectedId ? 'selected' : ''}>${escapeHtml(p.name)}</option>`).join('');
          const selected = projects.find((p) => p.id === selectedId) || projects[0];
          window.TraceHqBoard.setLastProjectId(selected.id);
          window.TraceHqBoard.renderBoard(container, selected.id, selected.name);
          if (editLink) {
            editLink.classList.remove('hidden');
            editLink.dataset.projectId = selected.id;
            editLink.dataset.projectName = selected.name;
          }
          if (deleteLink) {
            deleteLink.classList.remove('hidden');
            deleteLink.dataset.projectId = selected.id;
            deleteLink.dataset.projectName = selected.name;
          }
        })
        .catch((err) => {
          container.innerHTML = '<p class="board-error">' + escapeHtml(err.message) + '</p>';
          showToast(err.message, 'error');
          const editLink = document.getElementById('project-edit-link');
          const deleteLink = document.getElementById('project-delete-link');
          if (editLink) editLink.classList.add('hidden');
          if (deleteLink) deleteLink.classList.add('hidden');
        });
    }
    window.__traceHqRefreshWorkflows = refreshWorkflowsView;

    if (!select._boardChangeAttached) {
      select._boardChangeAttached = true;
      select.addEventListener('change', () => {
        const opt = select.options[select.selectedIndex];
        const val = opt.value;
        if (!val) return;
        const id = parseInt(val, 10);
        window.TraceHqBoard.renderBoard(container, id, opt.text);
        const editLink = document.getElementById('project-edit-link');
        const deleteLink = document.getElementById('project-delete-link');
        if (editLink) {
          editLink.classList.remove('hidden');
          editLink.dataset.projectId = id;
          editLink.dataset.projectName = opt.text;
        }
        if (deleteLink) {
          deleteLink.classList.remove('hidden');
          deleteLink.dataset.projectId = id;
          deleteLink.dataset.projectName = opt.text;
        }
      });
    }

    const projectEditLink = document.getElementById('project-edit-link');
    if (projectEditLink && !projectEditLink._editAttached) {
      projectEditLink._editAttached = true;
      projectEditLink.addEventListener('click', (e) => {
        e.preventDefault();
        const projectId = projectEditLink.dataset.projectId ? parseInt(projectEditLink.dataset.projectId, 10) : null;
        const projectName = projectEditLink.dataset.projectName || '';
        if (!projectId || Number.isNaN(projectId)) return;
        openEditProjectModal(projectId, projectName, refreshWorkflowsView);
      });
    }

    const projectDeleteLink = document.getElementById('project-delete-link');
    if (projectDeleteLink && !projectDeleteLink._deleteAttached) {
      projectDeleteLink._deleteAttached = true;
      projectDeleteLink.addEventListener('click', (e) => {
        e.preventDefault();
        const projectId = projectDeleteLink.dataset.projectId ? parseInt(projectDeleteLink.dataset.projectId, 10) : null;
        const projectName = projectDeleteLink.dataset.projectName || '';
        if (!projectId || Number.isNaN(projectId)) return;
        openDeleteProjectModal(projectId, projectName, refreshWorkflowsView);
      });
    }

    if (createBtn && !createBtn._createProjectAttached) {
      createBtn._createProjectAttached = true;
      createBtn.addEventListener('click', () => openCreateProjectModal(refreshWorkflowsView));
    }

    const createWorkflowBtn = document.getElementById('create-workflow-btn');
    if (createWorkflowBtn && !createWorkflowBtn._createWorkflowAttached) {
      createWorkflowBtn._createWorkflowAttached = true;
      createWorkflowBtn.addEventListener('click', () => {
        const select = document.getElementById('project-select');
        const opt = select && select.options[select.selectedIndex];
        const projectId = opt && opt.value ? parseInt(opt.value, 10) : null;
        const projectName = opt ? opt.text : '';
        if (!projectId || Number.isNaN(projectId)) {
          showToast('Select a project first', 'error');
          return;
        }
        if (window.TraceHqWorkflowCreateModal && typeof window.TraceHqWorkflowCreateModal.openCreateWorkflowModal === 'function') {
          window.TraceHqWorkflowCreateModal.openCreateWorkflowModal(projectId, projectName, refreshWorkflowsView);
        }
      });
    }

    const createStoryBtn = document.getElementById('create-story-btn');
    if (createStoryBtn && !createStoryBtn._createStoryAttached) {
      createStoryBtn._createStoryAttached = true;
      createStoryBtn.addEventListener('click', () => {
        if (window.TraceHqStoryCreateModal && typeof window.TraceHqStoryCreateModal.openCreateModal === 'function') {
          window.TraceHqStoryCreateModal.openCreateModal(function onCreated() {
            if (typeof window.__traceHqRefreshWorkflows === 'function') window.__traceHqRefreshWorkflows();
          });
        }
      });
    }

    refreshWorkflowsView();
    return;
  }

  showView('view-home');
}

function escapeHtml(s) {
  if (s == null) return '';
  const el = document.createElement('span');
  el.textContent = s;
  return el.innerHTML;
}

function isAnyModalOpen() {
  return document.getElementById('create-project-overlay') ||
    document.getElementById('create-workflow-overlay') ||
    document.getElementById('story-create-overlay') ||
    document.getElementById('story-edit-overlay') ||
    document.getElementById('story-delete-overlay') ||
    document.getElementById('workflow-delete-overlay') ||
    document.getElementById('project-delete-overlay') ||
    document.getElementById('project-edit-overlay');
}

function isTargetInsideModal(target) {
  return target && (
    target.closest('#create-project-overlay') ||
    target.closest('#create-workflow-overlay') ||
    target.closest('#story-create-overlay') ||
    target.closest('#story-edit-overlay') ||
    target.closest('#story-delete-overlay') ||
    target.closest('#workflow-delete-overlay') ||
    target.closest('#project-delete-overlay') ||
    target.closest('#project-edit-overlay')
  );
}

window.addEventListener('hashchange', handleRoute);
window.addEventListener('load', handleRoute);

document.addEventListener('keydown', function handleCreateStoryKey(e) {
  if (e.key !== 'c' && e.key !== 'C') return;
  if (e.ctrlKey || e.metaKey) return;
  if (isAnyModalOpen() || isTargetInsideModal(e.target)) return;
  const hash = (window.location.hash || '#/').slice(1) || '/';
  const path = hash.startsWith('/') ? hash : '/' + hash;
  const route = path === '/' ? 'home' : path.slice(1).split('/')[0];
  if (route !== 'workflows' && route !== 'backlog') return;
  e.preventDefault();
  if (window.TraceHqStoryCreateModal && typeof window.TraceHqStoryCreateModal.openCreateModal === 'function') {
    window.TraceHqStoryCreateModal.openCreateModal(function onCreated() {
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
    });
  }
}, true);

document.addEventListener('keydown', function handleCreateProjectKey(e) {
  if (e.key !== 'p' && e.key !== 'P') return;
  if (e.ctrlKey || e.metaKey) return;
  if (isAnyModalOpen() || isTargetInsideModal(e.target)) return;
  const hash = (window.location.hash || '#/').slice(1) || '/';
  const path = hash.startsWith('/') ? hash : '/' + hash;
  const route = path === '/' ? 'home' : path.slice(1).split('/')[0];
  if (route !== 'workflows') return;
  e.preventDefault();
  if (typeof window.__traceHqRefreshWorkflows === 'function') {
    openCreateProjectModal(window.__traceHqRefreshWorkflows);
  }
}, true);

document.addEventListener('keydown', function handleCreateWorkflowKey(e) {
  if (e.key !== 'w' && e.key !== 'W') return;
  if (e.ctrlKey || e.metaKey) return;
  if (isAnyModalOpen() || isTargetInsideModal(e.target)) return;
  const hash = (window.location.hash || '#/').slice(1) || '/';
  const path = hash.startsWith('/') ? hash : '/' + hash;
  const route = path === '/' ? 'home' : path.slice(1).split('/')[0];
  if (route !== 'workflows') return;
  e.preventDefault();
  const select = document.getElementById('project-select');
  const opt = select && select.options[select.selectedIndex];
  const projectId = opt && opt.value ? parseInt(opt.value, 10) : null;
  const projectName = opt ? opt.text : '';
  if (!projectId || Number.isNaN(projectId)) {
    showToast('Select a project first', 'error');
    return;
  }
  if (window.TraceHqWorkflowCreateModal && typeof window.TraceHqWorkflowCreateModal.openCreateWorkflowModal === 'function') {
    window.TraceHqWorkflowCreateModal.openCreateWorkflowModal(projectId, projectName, window.__traceHqRefreshWorkflows);
  }
}, true);
