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

    window.TraceHqBoard.loadProjects()
      .then((projects) => {
        if (projects.length === 0) {
          container.innerHTML = '<p>No projects found.</p>';
          return;
        }
        const selectedId = window.TraceHqBoard.getLastProjectId() || projects[0].id;
        select.innerHTML = projects.map((p) => `<option value="${p.id}" ${p.id === selectedId ? 'selected' : ''}>${escapeHtml(p.name)}</option>`).join('');
        const selected = projects.find((p) => p.id === selectedId) || projects[0];
        window.TraceHqBoard.renderBoard(container, selected.id, selected.name);

        if (!select._boardChangeAttached) {
          select._boardChangeAttached = true;
          select.addEventListener('change', () => {
            const opt = select.options[select.selectedIndex];
            const id = parseInt(opt.value, 10);
            window.TraceHqBoard.renderBoard(container, id, opt.text);
          });
        }
      })
      .catch((err) => {
        container.innerHTML = '<p>' + escapeHtml(err.message) + '</p>';
        showToast(err.message, 'error');
      });
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

window.addEventListener('hashchange', handleRoute);
window.addEventListener('load', handleRoute);
