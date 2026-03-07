/**
 * TRACE H.Q. — Story card: render card DOM, move dropdown, call PATCH API.
 */

function renderStoryCard(story, stages, onMoved) {
  const div = document.createElement('div');
  div.className = 'story-card' + (story.blocked ? ' blocked' : '');
  div.dataset.storyId = story.id;

  const depsText = story.dependencies && story.dependencies.length
    ? `Blocked by ${story.dependencies.join(', ')}`
    : '';
  const assignee = story.assignee_name || 'Unassigned';

  div.innerHTML = `
    <div class="story-card-id">${escapeHtml(story.id)}</div>
    <div class="story-card-title">${escapeHtml(story.title)}</div>
    <div class="story-card-meta">${escapeHtml(assignee)}${story.priority ? ' · ' + escapeHtml(story.priority) : ''}</div>
    ${depsText ? `<div class="story-card-meta">${escapeHtml(depsText)}</div>` : ''}
    <div class="story-card-move">
      <select class="story-move-select" data-story-id="${escapeHtml(story.id)}">
        <option value="">Move to…</option>
        ${(stages || [])
          .filter((s) => s.id !== story.workflow_stage_id)
          .map((s) => `<option value="${s.id}">${escapeHtml(s.stage_name)}</option>`)
          .join('')}
      </select>
    </div>
  `;

  const select = div.querySelector('.story-move-select');
  if (select && select.options.length > 1) {
    select.addEventListener('change', async (e) => {
      const stageId = e.target.value;
      if (!stageId) return;
      try {
        const res = await fetch(`/api/stories/${story.id}/stage`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ workflow_stage_id: parseInt(stageId, 10) }),
        });
        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          throw new Error(data.error || res.statusText);
        }
        e.target.value = '';
        if (typeof onMoved === 'function') onMoved();
      } catch (err) {
        if (typeof window.showToast === 'function') window.showToast(err.message, 'error');
      }
    });
  }

  return div;
}

function escapeHtml(s) {
  if (s == null) return '';
  const el = document.createElement('span');
  el.textContent = s;
  return el.innerHTML;
}

window.TraceHqStoryCard = { renderStoryCard };
