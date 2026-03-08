/**
 * TRACE H.Q. API — projects, workflows, stories, agents, move story.
 * All responses JSON. Acting user is treated as owner (no login).
 */

const express = require('express');
const router = express.Router();
const { pool } = require('../config/db');

// ----- Health (no DB) -----
router.get('/health', (req, res) => {
  res.json({ ok: true, service: 'trace-hq' });
});

// ----- Helpers -----
function sendError(res, status, message) {
  res.status(status).json({ error: message });
}

async function getOwnerAgentId() {
  const [rows] = await pool.query(
    `SELECT a.id FROM agents a JOIN roles r ON a.role_id = r.id WHERE r.code = 'owner' LIMIT 1`
  );
  return rows[0] ? rows[0].id : null;
}

// ----- GET /api/projects -----
router.get('/projects', async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, name, created_at, updated_at FROM projects ORDER BY id'
    );
    res.json(rows);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch projects');
  }
});

// ----- GET /api/projects/:projectId/workflows -----
router.get('/projects/:projectId/workflows', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  try {
    const [workflows] = await pool.query(
      'SELECT id, project_id, code, name, description, created_at, updated_at FROM workflows WHERE project_id = ? ORDER BY id',
      [projectId]
    );
    for (const w of workflows) {
      const [stages] = await pool.query(
        'SELECT id, workflow_id, stage_order, stage_name, stage_role, created_at FROM workflow_stages WHERE workflow_id = ? ORDER BY stage_order',
        [w.id]
      );
      w.stages = stages;
    }
    res.json(workflows);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch workflows');
  }
});

// ----- GET /api/projects/:projectId/stories -----
router.get('/projects/:projectId/stories', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  try {
    const [workflows] = await pool.query('SELECT id FROM workflows WHERE project_id = ?', [projectId]);
    const workflowIds = workflows.map((w) => w.id);
    if (workflowIds.length === 0) return res.json([]);

    const placeholders = workflowIds.map(() => '?').join(',');
    const [stories] = await pool.query(
      `SELECT s.id, s.title, s.description, s.type, s.priority, s.workflow_id, s.workflow_stage_id,
        s.assignee_id, s.blocked, s.blocked_reason, s.review_status, s.branch, s.review_reference, s.artifact,
        ws.stage_name AS current_stage_name, ws.stage_role AS current_stage_role,
        w.name AS workflow_name,
        a.name AS assignee_name
       FROM stories s
       LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id
       LEFT JOIN workflows w ON s.workflow_id = w.id
       LEFT JOIN agents a ON s.assignee_id = a.id
       WHERE s.workflow_id IN (${placeholders})
       ORDER BY s.id`,
      workflowIds
    );

    const storyIds = stories.map((s) => s.id);
    if (storyIds.length === 0) return res.json([]);

    const [deps] = await pool.query(
      'SELECT story_id, depends_on_story_id FROM story_dependencies WHERE story_id IN (?)',
      [storyIds]
    );
    const [relatedRows] = await pool.query(
      'SELECT story_id, related_story_id FROM story_related WHERE story_id IN (?) OR related_story_id IN (?)',
      [storyIds, storyIds]
    );

    const depMap = {};
    deps.forEach((d) => {
      if (!depMap[d.story_id]) depMap[d.story_id] = [];
      depMap[d.story_id].push(d.depends_on_story_id);
    });
    const relatedMap = {};
    relatedRows.forEach((r) => {
      const sid = r.story_id;
      const rid = r.related_story_id;
      if (!relatedMap[sid]) relatedMap[sid] = [];
      relatedMap[sid].push(rid);
      if (!relatedMap[rid]) relatedMap[rid] = [];
      relatedMap[rid].push(sid);
    });
    const [doneStories] = await pool.query(
      `SELECT s.id FROM stories s JOIN workflow_stages ws ON s.workflow_stage_id = ws.id WHERE ws.stage_name = 'Done' AND s.id IN (?)`,
      [storyIds]
    );
    const doneSet = new Set(doneStories.map((s) => s.id));

    const result = stories.map((s) => {
      const out = { ...s, assignee_name: s.assignee_name || 'Unassigned' };
      out.dependencies = depMap[s.id] || [];
      out.dependencies_resolved = (depMap[s.id] || []).every((id) => doneSet.has(id));
      out.related = [...new Set(relatedMap[s.id] || [])];
      return out;
    });

    res.json(result);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch stories');
  }
});

// ----- GET /api/projects/:projectId/backlog -----
router.get('/projects/:projectId/backlog', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  try {
    const [stories] = await pool.query(
      `SELECT s.id, s.title, s.description, s.type, s.priority, s.project_id, s.assignee_id, s.blocked, s.blocked_reason,
        s.created_at, s.last_updated_at, s.backlog_order,
        a.name AS assignee_name
       FROM stories s
       LEFT JOIN agents a ON s.assignee_id = a.id
       WHERE s.project_id = ? AND s.workflow_id IS NULL
       ORDER BY COALESCE(s.backlog_order, 999999), s.id`,
      [projectId]
    );
    const storyIds = stories.map((s) => s.id);
    if (storyIds.length === 0) return res.json([]);

    const [deps] = await pool.query(
      'SELECT story_id, depends_on_story_id FROM story_dependencies WHERE story_id IN (?)',
      [storyIds]
    );
    const [relatedRows] = await pool.query(
      'SELECT story_id, related_story_id FROM story_related WHERE story_id IN (?) OR related_story_id IN (?)',
      [storyIds, storyIds]
    );
    const depMap = {};
    deps.forEach((d) => {
      if (!depMap[d.story_id]) depMap[d.story_id] = [];
      depMap[d.story_id].push(d.depends_on_story_id);
    });
    const relatedMap = {};
    relatedRows.forEach((r) => {
      const sid = r.story_id;
      const rid = r.related_story_id;
      if (!relatedMap[sid]) relatedMap[sid] = [];
      relatedMap[sid].push(rid);
      if (!relatedMap[rid]) relatedMap[rid] = [];
      relatedMap[rid].push(sid);
    });

    const result = stories.map((s) => {
      const out = { ...s, assignee_name: s.assignee_name || 'Unassigned' };
      out.dependencies = depMap[s.id] || [];
      out.related = [...new Set(relatedMap[s.id] || [])];
      return out;
    });
    res.json(result);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch backlog');
  }
});

// ----- PUT /api/projects/:projectId/backlog/order (reorder backlog) -----
router.put('/projects/:projectId/backlog/order', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  const orderedStoryIds = req.body && Array.isArray(req.body.orderedStoryIds) ? req.body.orderedStoryIds : null;
  if (Number.isNaN(projectId) || !orderedStoryIds || orderedStoryIds.length === 0) {
    return sendError(res, 400, 'Invalid project id or missing orderedStoryIds array');
  }
  const conn = await pool.getConnection();
  try {
    const placeholders = orderedStoryIds.map(() => '?').join(',');
    const [rows] = await conn.query(
      `SELECT id FROM stories WHERE project_id = ? AND workflow_id IS NULL AND id IN (${placeholders})`,
      [projectId, ...orderedStoryIds]
    );
    const validIds = new Set(rows.map((r) => r.id));
    const filteredOrder = orderedStoryIds.filter((id) => validIds.has(id));
    if (filteredOrder.length !== orderedStoryIds.length) {
      conn.release();
      return sendError(res, 400, 'Some story ids are not in this project\'s backlog');
    }
    for (let i = 0; i < filteredOrder.length; i++) {
      await conn.query(
        'UPDATE stories SET backlog_order = ?, last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ? AND project_id = ?',
        [i * 10, filteredOrder[i], projectId]
      );
    }
    res.json({ ok: true, orderedStoryIds: filteredOrder });
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to reorder backlog');
  } finally {
    conn.release();
  }
});
// ----- GET /api/agents -----
router.get('/agents', async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT a.id, a.name, r.code AS role_code FROM agents a JOIN roles r ON a.role_id = r.id ORDER BY a.id'
    );
    res.json(rows);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch agents');
  }
});

// ----- PATCH /api/stories/:storyId/stage -----
router.patch('/stories/:storyId/stage', async (req, res) => {
  const storyId = (req.params.storyId || '').trim();
  const targetStageId = req.body && req.body.workflow_stage_id != null ? parseInt(req.body.workflow_stage_id, 10) : null;
  if (!storyId || Number.isNaN(targetStageId)) return sendError(res, 400, 'Missing or invalid story id or workflow_stage_id');

  const conn = await pool.getConnection();
  try {
    const [storyRows] = await conn.query(
      `SELECT s.id, s.workflow_id, s.workflow_stage_id, ws.stage_name AS from_stage_name, ws.stage_order AS from_order, ws.stage_role AS from_stage_role
       FROM stories s
       LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id
       WHERE s.id = ?`,
      [storyId]
    );
    const story = storyRows[0];
    if (!story) {
      sendError(res, 404, 'Story not found');
      return;
    }

    const [targetRows] = await conn.query(
      'SELECT id, workflow_id, stage_name, stage_order FROM workflow_stages WHERE id = ?',
      [targetStageId]
    );
    const target = targetRows[0];
    if (!target) {
      sendError(res, 400, 'Target stage not found');
      return;
    }
    if (story.workflow_id !== target.workflow_id) {
      sendError(res, 400, 'Target stage is in a different workflow');
      return;
    }

    const ownerId = await getOwnerAgentId();
    if (!ownerId) {
      sendError(res, 500, 'Owner agent not found');
      return;
    }
    // Current user is always owner — allow any transition.

    await conn.query(
      'UPDATE stories SET workflow_stage_id = ?, last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
      [targetStageId, storyId]
    );

    await conn.query(
      `INSERT INTO story_stage_history (story_id, from_stage_name, to_stage_name, from_workflow_stage_id, to_workflow_stage_id, assignee_id, changed_by_agent_id)
       VALUES (?, ?, ?, ?, ?, (SELECT assignee_id FROM stories WHERE id = ?), ?)`,
      [storyId, story.from_stage_name || null, target.stage_name, story.workflow_stage_id, targetStageId, storyId, ownerId]
    );

    await conn.query(
      'INSERT INTO story_audit_log (story_id, event_type, note) VALUES (?, ?, ?)',
      [storyId, 'stage_transition', `${story.from_stage_name || 'none'} → ${target.stage_name}`]
    );

    const [updated] = await conn.query(
      `SELECT s.id, s.title, s.workflow_id, s.workflow_stage_id, ws.stage_name AS current_stage_name
       FROM stories s LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id WHERE s.id = ?`,
      [storyId]
    );

    res.json(updated[0]);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to move story');
  } finally {
    conn.release();
  }
});

// ----- PATCH /api/stories/:storyId/workflow (add to workflow / move from backlog) -----
router.patch('/stories/:storyId/workflow', async (req, res) => {
  const storyId = (req.params.storyId || '').trim();
  const workflowId = req.body && req.body.workflow_id != null ? parseInt(req.body.workflow_id, 10) : null;
  const stageId = req.body && req.body.workflow_stage_id != null ? parseInt(req.body.workflow_stage_id, 10) : null;
  if (!storyId || Number.isNaN(workflowId) || Number.isNaN(stageId)) return sendError(res, 400, 'Missing or invalid story id, workflow_id, or workflow_stage_id');

  const conn = await pool.getConnection();
  try {
    const [storyRows] = await conn.query(
      'SELECT id, project_id, workflow_id, workflow_stage_id FROM stories WHERE id = ?',
      [storyId]
    );
    const story = storyRows[0];
    if (!story) {
      sendError(res, 404, 'Story not found');
      return;
    }

    const [wfRows] = await conn.query(
      'SELECT id, project_id, name FROM workflows WHERE id = ?',
      [workflowId]
    );
    const workflow = wfRows[0];
    if (!workflow) {
      sendError(res, 400, 'Workflow not found');
      return;
    }
    if (story.project_id !== workflow.project_id) {
      sendError(res, 400, 'Workflow does not belong to the story\'s project');
      return;
    }

    const [stageRows] = await conn.query(
      'SELECT id, workflow_id, stage_name FROM workflow_stages WHERE id = ?',
      [stageId]
    );
    const stage = stageRows[0];
    if (!stage || stage.workflow_id !== workflowId) {
      sendError(res, 400, 'Stage not found or does not belong to the workflow');
      return;
    }

    const ownerId = await getOwnerAgentId();
    if (!ownerId) {
      sendError(res, 500, 'Owner agent not found');
      return;
    }

    const fromStageName = story.workflow_id ? null : 'Backlog';

    await conn.query(
      'UPDATE stories SET workflow_id = ?, workflow_stage_id = ?, backlog_order = NULL, last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
      [workflowId, stageId, storyId]
    );

    await conn.query(
      `INSERT INTO story_stage_history (story_id, from_stage_name, to_stage_name, from_workflow_stage_id, to_workflow_stage_id, assignee_id, changed_by_agent_id)
       VALUES (?, ?, ?, ?, ?, (SELECT assignee_id FROM stories WHERE id = ?), ?)`,
      [storyId, fromStageName, stage.stage_name, story.workflow_stage_id, stageId, storyId, ownerId]
    );

    await conn.query(
      'INSERT INTO story_audit_log (story_id, event_type, note) VALUES (?, ?, ?)',
      [storyId, 'add_to_workflow', `Added to ${workflow.name} → ${stage.stage_name}`]
    );

    const [updated] = await conn.query(
      `SELECT s.id, s.title, s.workflow_id, s.workflow_stage_id, ws.stage_name AS current_stage_name
       FROM stories s LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id WHERE s.id = ?`,
      [storyId]
    );

    res.json(updated[0]);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to add story to workflow');
  } finally {
    conn.release();
  }
});

module.exports = router;
