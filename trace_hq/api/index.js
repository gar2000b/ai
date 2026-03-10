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
    let rows;
    try {
      [rows] = await pool.query(
        'SELECT id, name, created_at, updated_at FROM projects WHERE (deleted_at IS NULL) ORDER BY id'
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [rows] = await pool.query(
          'SELECT id, name, created_at, updated_at FROM projects ORDER BY id'
        );
      } else {
        throw colErr;
      }
    }
    res.json(rows);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch projects');
  }
});

// ----- PATCH /api/projects/:projectId (update name, or logical delete via deleted: true) -----
router.patch('/projects/:projectId', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  const body = req.body || {};
  try {
    let existing;
    try {
      [existing] = await pool.query(
        'SELECT id, name, deleted_at FROM projects WHERE id = ?',
        [projectId]
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [existing] = await pool.query('SELECT id, name FROM projects WHERE id = ?', [projectId]);
      } else {
        throw colErr;
      }
    }
    if (!existing.length) return sendError(res, 404, 'Project not found');
    if (existing[0].deleted_at) return sendError(res, 404, 'Project not found');

    if (body.deleted === true) {
      try {
        await pool.query(
          'UPDATE projects SET deleted_at = CURRENT_TIMESTAMP(3), updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
          [projectId]
        );
      } catch (updErr) {
        if (updErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(updErr.message)) {
          return sendError(res, 501, 'Project logical delete not available; run migration 09_projects_deleted_at.sql');
        }
        throw updErr;
      }
      return res.json({ ok: true, id: projectId });
    }

    const name = (body.name != null) ? String(body.name).trim() : null;
    if (name !== null) {
      if (!name) return sendError(res, 400, 'Project name cannot be empty');
      await pool.query(
        'UPDATE projects SET name = ?, updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
        [name, projectId]
      );
      const [rows] = await pool.query(
        'SELECT id, name, created_at, updated_at FROM projects WHERE id = ?',
        [projectId]
      );
      return res.json(rows[0]);
    }

    return sendError(res, 400, 'Provide name (to update) or deleted: true (to delete)');
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to update project');
  }
});

// ----- POST /api/projects (create empty project) -----
router.post('/projects', async (req, res) => {
  const name = (req.body && req.body.name && String(req.body.name).trim()) || null;
  if (!name) return sendError(res, 400, 'Project name is required');
  try {
    const [result] = await pool.query(
      'INSERT INTO projects (name) VALUES (?)',
      [name]
    );
    const [rows] = await pool.query(
      'SELECT id, name, created_at, updated_at FROM projects WHERE id = ?',
      [result.insertId]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to create project');
  }
});

// ----- GET /api/projects/:projectId/workflows -----
router.get('/projects/:projectId/workflows', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  try {
    let projectExists;
    try {
      const [projRows] = await pool.query(
        'SELECT id FROM projects WHERE id = ? AND (deleted_at IS NULL)',
        [projectId]
      );
      projectExists = projRows.length > 0;
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        const [projRows] = await pool.query('SELECT id FROM projects WHERE id = ?', [projectId]);
        projectExists = projRows.length > 0;
      } else {
        throw colErr;
      }
    }
    if (!projectExists) return sendError(res, 404, 'Project not found');

    let workflows;
    try {
      [workflows] = await pool.query(
        'SELECT id, project_id, code, name, description, display_order, created_at, updated_at FROM workflows WHERE project_id = ? AND (deleted_at IS NULL) ORDER BY display_order ASC, id ASC',
        [projectId]
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /display_order/.test(colErr.message)) {
        [workflows] = await pool.query(
          'SELECT id, project_id, code, name, description, created_at, updated_at FROM workflows WHERE project_id = ? AND (deleted_at IS NULL) ORDER BY id',
          [projectId]
        );
      } else if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [workflows] = await pool.query(
          'SELECT id, project_id, code, name, description, display_order, created_at, updated_at FROM workflows WHERE project_id = ? ORDER BY display_order ASC, id ASC',
          [projectId]
        );
      } else {
        throw colErr;
      }
    }
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

// ----- POST /api/projects/:projectId/workflows (create workflow + stages) -----
router.post('/projects/:projectId/workflows', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  const code = (req.body && req.body.code && String(req.body.code).trim()) || null;
  const name = (req.body && req.body.name && String(req.body.name).trim()) || null;
  const description = (req.body && req.body.description != null) ? String(req.body.description).trim() : null;
  const stages = Array.isArray(req.body.stages) ? req.body.stages : [];
  if (!code || !name) return sendError(res, 400, 'Workflow code and name are required');
  if (stages.length === 0) return sendError(res, 400, 'At least one stage is required');
  const validRoles = ['owner', 'dev', 'unit-test', 'integration-test', 'performance-test', 'devops'];
  for (let i = 0; i < stages.length; i++) {
    const s = stages[i];
    const sn = (s && s.stage_name != null) ? String(s.stage_name).trim() : '';
    const sr = (s && s.stage_role != null) ? String(s.stage_role) : '';
    if (!sn) return sendError(res, 400, `Stage ${i + 1}: stage name is required`);
    if (!validRoles.includes(sr)) return sendError(res, 400, `Stage ${i + 1}: invalid stage_role (use one of ${validRoles.join(', ')})`);
  }
  try {
    let projectRows;
    try {
      [projectRows] = await pool.query(
        'SELECT id FROM projects WHERE id = ? AND (deleted_at IS NULL)',
        [projectId]
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [projectRows] = await pool.query('SELECT id FROM projects WHERE id = ?', [projectId]);
      } else {
        throw colErr;
      }
    }
    if (!projectRows.length) return sendError(res, 404, 'Project not found');
    const [existing] = await pool.query('SELECT id FROM workflows WHERE project_id = ? AND code = ?', [projectId, code]);
    if (existing.length) return sendError(res, 400, `A workflow with code "${code}" already exists in this project`);
    let workflowId;
    try {
      const [[{ nextOrder }]] = await pool.query(
        'SELECT COALESCE(MAX(display_order), 0) + 1 AS nextOrder FROM workflows WHERE project_id = ?',
        [projectId]
      );
      const [insertResult] = await pool.query(
        'INSERT INTO workflows (project_id, code, name, description, display_order) VALUES (?, ?, ?, ?, ?)',
        [projectId, code, name, description || null, nextOrder]
      );
      workflowId = insertResult.insertId;
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /display_order/.test(colErr.message)) {
        const [insertResult] = await pool.query(
          'INSERT INTO workflows (project_id, code, name, description) VALUES (?, ?, ?, ?)',
          [projectId, code, name, description || null]
        );
        workflowId = insertResult.insertId;
      } else {
        throw colErr;
      }
    }
    for (let i = 0; i < stages.length; i++) {
      const s = stages[i];
      const stageName = String(s.stage_name).trim();
      const stageRole = String(s.stage_role);
      await pool.query(
        'INSERT INTO workflow_stages (workflow_id, stage_order, stage_name, stage_role) VALUES (?, ?, ?, ?)',
        [workflowId, i + 1, stageName, stageRole]
      );
    }
    let workflows;
    try {
      [workflows] = await pool.query(
        'SELECT id, project_id, code, name, description, display_order, created_at, updated_at FROM workflows WHERE id = ?',
        [workflowId]
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /display_order/.test(colErr.message)) {
        [workflows] = await pool.query(
          'SELECT id, project_id, code, name, description, created_at, updated_at FROM workflows WHERE id = ?',
          [workflowId]
        );
      } else {
        throw colErr;
      }
    }
    const [stageRows] = await pool.query(
      'SELECT id, workflow_id, stage_order, stage_name, stage_role, created_at FROM workflow_stages WHERE workflow_id = ? ORDER BY stage_order',
      [workflowId]
    );
    const created = workflows[0];
    created.stages = stageRows;
    res.status(201).json(created);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to create workflow');
  }
});

// ----- PATCH /api/projects/:projectId/workflows/:workflowId (update workflow + stages; logical delete via deleted: true) -----
router.patch('/projects/:projectId/workflows/:workflowId', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  const workflowId = parseInt(req.params.workflowId, 10);
  if (Number.isNaN(projectId) || Number.isNaN(workflowId)) return sendError(res, 400, 'Invalid project or workflow id');
  const body = req.body || {};
  const name = (body.name != null) ? String(body.name).trim() : null;
  const description = (body.description != null) ? String(body.description).trim() : null;
  const stages = Array.isArray(body.stages) ? body.stages : null;

  try {
    let wfRows;
    try {
      [wfRows] = await pool.query(
        'SELECT id, project_id, code, name, description, deleted_at FROM workflows WHERE id = ? AND project_id = ?',
        [workflowId, projectId]
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [wfRows] = await pool.query(
          'SELECT id, project_id, code, name, description FROM workflows WHERE id = ? AND project_id = ?',
          [workflowId, projectId]
        );
      } else {
        throw colErr;
      }
    }
    if (!wfRows.length) return sendError(res, 404, 'Workflow not found');
    if (wfRows[0].deleted_at) return sendError(res, 404, 'Workflow not found');

    if (body.deleted === true) {
      try {
        await pool.query(
          'UPDATE workflows SET deleted_at = CURRENT_TIMESTAMP(3), updated_at = CURRENT_TIMESTAMP(3) WHERE id = ? AND project_id = ?',
          [workflowId, projectId]
        );
      } catch (updErr) {
        if (updErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(updErr.message)) {
          return sendError(res, 501, 'Workflow logical delete not available; run migration 08_workflows_deleted_at.sql');
        }
        throw updErr;
      }
      return res.json({ ok: true, id: workflowId });
    }

    if (name !== null) {
      await pool.query('UPDATE workflows SET name = ?, updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?', [name, workflowId]);
    }
    if (description !== null) {
      await pool.query('UPDATE workflows SET description = ?, updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?', [description || null, workflowId]);
    }

    if (stages !== null) {
      if (stages.length === 0) return sendError(res, 400, 'At least one stage is required');
      const validRoles = ['owner', 'dev', 'unit-test', 'integration-test', 'performance-test', 'devops'];
      for (let i = 0; i < stages.length; i++) {
        const s = stages[i];
        const sn = (s && s.stage_name != null) ? String(s.stage_name).trim() : '';
        const sr = (s && s.stage_role != null) ? String(s.stage_role) : '';
        if (!sn) return sendError(res, 400, `Stage ${i + 1}: stage name is required`);
        if (!validRoles.includes(sr)) return sendError(res, 400, `Stage ${i + 1}: invalid stage_role (use one of ${validRoles.join(', ')})`);
      }

      const [oldStages] = await pool.query(
        'SELECT id, stage_order FROM workflow_stages WHERE workflow_id = ? ORDER BY stage_order',
        [workflowId]
      );

      const conn = await pool.getConnection();
      try {
        await conn.beginTransaction();
        const orderOffset = 10000;
        const newStageIds = [];
        for (let i = 0; i < stages.length; i++) {
          const s = stages[i];
          const stageName = String(s.stage_name).trim();
          const stageRole = String(s.stage_role);
          const [ins] = await conn.query(
            'INSERT INTO workflow_stages (workflow_id, stage_order, stage_name, stage_role) VALUES (?, ?, ?, ?)',
            [workflowId, orderOffset + i, stageName, stageRole]
          );
          newStageIds.push(ins.insertId);
        }
        for (let i = 0; i < oldStages.length; i++) {
          const oldId = oldStages[i].id;
          const newIndex = Math.min(i, newStageIds.length - 1);
          const newId = newStageIds[newIndex];
          await conn.query('UPDATE stories SET workflow_stage_id = ? WHERE workflow_stage_id = ?', [newId, oldId]);
        }
        const oldIds = oldStages.map((s) => s.id);
        if (oldIds.length) {
          const placeholders = oldIds.map(() => '?').join(',');
          await conn.query(`DELETE FROM workflow_stages WHERE id IN (${placeholders})`, oldIds);
        }
        for (let i = 0; i < newStageIds.length; i++) {
          await conn.query('UPDATE workflow_stages SET stage_order = ? WHERE id = ?', [i + 1, newStageIds[i]]);
        }
        await conn.commit();
      } catch (e) {
        await conn.rollback();
        throw e;
      } finally {
        conn.release();
      }
    }

    const [updated] = await pool.query(
      'SELECT id, project_id, code, name, description, created_at, updated_at FROM workflows WHERE id = ?',
      [workflowId]
    );
    const [stageRows] = await pool.query(
      'SELECT id, workflow_id, stage_order, stage_name, stage_role, created_at FROM workflow_stages WHERE workflow_id = ? ORDER BY stage_order',
      [workflowId]
    );
    const result = updated[0];
    result.stages = stageRows;
    res.json(result);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to update workflow');
  }
});

// ----- PUT /api/projects/:projectId/workflows/order (reorder workflows) -----
router.put('/projects/:projectId/workflows/order', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  const orderedIds = Array.isArray(req.body && req.body.orderedWorkflowIds) ? req.body.orderedWorkflowIds : null;
  if (!orderedIds || orderedIds.length === 0) return sendError(res, 400, 'orderedWorkflowIds array is required');
  const workflowIds = orderedIds.map((id) => parseInt(id, 10)).filter((id) => !Number.isNaN(id));
  if (workflowIds.length !== orderedIds.length) return sendError(res, 400, 'Invalid workflow id in orderedWorkflowIds');
  try {
    const placeholders = workflowIds.map(() => '?').join(',');
    let rows;
    try {
      [rows] = await pool.query(
        `SELECT id FROM workflows WHERE project_id = ? AND (deleted_at IS NULL) AND id IN (${placeholders})`,
        [projectId, ...workflowIds]
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [rows] = await pool.query(
          `SELECT id FROM workflows WHERE project_id = ? AND id IN (${placeholders})`,
          [projectId, ...workflowIds]
        );
      } else {
        throw colErr;
      }
    }
    if (rows.length !== workflowIds.length) return sendError(res, 400, 'All workflow ids must belong to this project');
    const conn = await pool.getConnection();
    try {
      for (let i = 0; i < workflowIds.length; i++) {
        await conn.query(
          'UPDATE workflows SET display_order = ? WHERE id = ? AND project_id = ?',
          [i + 1, workflowIds[i], projectId]
        );
      }
      res.json({ ok: true });
    } catch (updateErr) {
      if (updateErr.code === 'ER_BAD_FIELD_ERROR' && /display_order/.test(updateErr.message)) {
        return sendError(
          res,
          501,
          'Workflow reordering requires the database migration. From the trace_hq directory run: ./scripts/mysql.sh < database/schema/02a_workflow_display_order.sql'
        );
      }
      throw updateErr;
    } finally {
      conn.release();
    }
  } catch (err) {
    if (err.code === 'ER_BAD_FIELD_ERROR' && /display_order/.test(err.message)) {
      return sendError(
        res,
        501,
        'Workflow reordering requires the database migration. From the trace_hq directory run: ./scripts/mysql.sh < database/schema/02a_workflow_display_order.sql'
      );
    }
    sendError(res, 500, err.message || 'Failed to reorder workflows');
  }
});

// ----- GET /api/projects/:projectId/stories -----
router.get('/projects/:projectId/stories', async (req, res) => {
  const projectId = parseInt(req.params.projectId, 10);
  if (Number.isNaN(projectId)) return sendError(res, 400, 'Invalid project id');
  try {
    let projectExists;
    try {
      const [projRows] = await pool.query(
        'SELECT id FROM projects WHERE id = ? AND (deleted_at IS NULL)',
        [projectId]
      );
      projectExists = projRows.length > 0;
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        const [projRows] = await pool.query('SELECT id FROM projects WHERE id = ?', [projectId]);
        projectExists = projRows.length > 0;
      } else {
        throw colErr;
      }
    }
    if (!projectExists) return sendError(res, 404, 'Project not found');

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
       WHERE s.workflow_id IN (${placeholders}) AND (s.deleted_at IS NULL)
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

// ----- GET /api/backlog (global backlog; stories not in any workflow) -----
router.get('/backlog', async (req, res) => {
  try {
    const [stories] = await pool.query(
      `SELECT s.id, s.title, s.description, s.type, s.priority, s.project_id, s.assignee_id, s.blocked, s.blocked_reason,
        s.created_at, s.last_updated_at, s.backlog_order,
        a.name AS assignee_name
       FROM stories s
       LEFT JOIN agents a ON s.assignee_id = a.id
       WHERE s.workflow_id IS NULL AND (s.deleted_at IS NULL)
       ORDER BY COALESCE(s.backlog_order, 999999), s.id`
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

// ----- PUT /api/backlog/order (reorder global backlog) -----
router.put('/backlog/order', async (req, res) => {
  const orderedStoryIds = req.body && Array.isArray(req.body.orderedStoryIds) ? req.body.orderedStoryIds : null;
  if (!orderedStoryIds || orderedStoryIds.length === 0) {
    return sendError(res, 400, 'Missing orderedStoryIds array');
  }
  const conn = await pool.getConnection();
  try {
    const placeholders = orderedStoryIds.map(() => '?').join(',');
    const [rows] = await conn.query(
      `SELECT id FROM stories WHERE workflow_id IS NULL AND (deleted_at IS NULL) AND id IN (${placeholders})`,
      orderedStoryIds
    );
    const validIds = new Set(rows.map((r) => r.id));
    const filteredOrder = orderedStoryIds.filter((id) => validIds.has(id));
    if (filteredOrder.length !== orderedStoryIds.length) {
      conn.release();
      return sendError(res, 400, 'Some story ids are not in the backlog');
    }
    for (let i = 0; i < filteredOrder.length; i++) {
      await conn.query(
        'UPDATE stories SET backlog_order = ?, last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ? AND workflow_id IS NULL',
        [i * 10, filteredOrder[i]]
      );
    }
    res.json({ ok: true, orderedStoryIds: filteredOrder });
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to reorder backlog');
  } finally {
    conn.release();
  }
});

// ----- GET /api/projects-with-workflows (for backlog "Add to workflow" dropdown: all projects + workflows) -----
router.get('/projects-with-workflows', async (req, res) => {
  try {
    let projects;
    try {
      [projects] = await pool.query(
        'SELECT id, name FROM projects WHERE (deleted_at IS NULL) ORDER BY id'
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [projects] = await pool.query('SELECT id, name FROM projects ORDER BY id');
      } else {
        throw colErr;
      }
    }
    for (const p of projects) {
      let workflows;
      try {
        [workflows] = await pool.query(
          'SELECT id, name FROM workflows WHERE project_id = ? AND (deleted_at IS NULL) ORDER BY id',
          [p.id]
        );
      } catch (colErr) {
        if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
          [workflows] = await pool.query(
            'SELECT id, name FROM workflows WHERE project_id = ? ORDER BY id',
            [p.id]
          );
        } else {
          throw colErr;
        }
      }
      for (const w of workflows) {
        const [stages] = await pool.query(
          'SELECT id FROM workflow_stages WHERE workflow_id = ? ORDER BY stage_order LIMIT 1',
          [w.id]
        );
        w.firstStageId = stages[0] ? stages[0].id : null;
      }
      p.workflows = workflows.filter((w) => w.firstStageId != null);
    }
    res.json(projects);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch projects with workflows');
  }
});
// ----- GET /api/roles (for workflow stage_role dropdown) -----
router.get('/roles', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT id, code FROM roles ORDER BY id');
    res.json(rows);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch roles');
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

// ----- GET /api/stories/next-id (suggested id for new story, e.g. S043) -----
router.get('/stories/next-id', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT id FROM stories WHERE id REGEXP '^S[0-9]+$' ORDER BY CAST(SUBSTRING(id, 2) AS UNSIGNED) DESC LIMIT 1`
    );
    const nextNum = rows[0] ? parseInt(rows[0].id.slice(1), 10) + 1 : 1;
    const nextId = 'S' + String(nextNum).padStart(3, '0');
    res.json({ nextId });
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to get next story id');
  }
});

// ----- POST /api/stories (create story; placement: backlog or workflow) -----
router.post('/stories', async (req, res) => {
  const body = req.body || {};
  const title = (body.title && String(body.title).trim()) || null;
  const type = (body.type && String(body.type).trim()) || 'dev';
  const priority = (body.priority && String(body.priority).trim()) || 'medium';
  if (!title) return sendError(res, 400, 'Title is required');

  const placement = body.placement;
  const toBacklog = placement === 'backlog' || (placement && placement.backlog === true);
  const workflowId = placement && placement.workflow_id != null ? parseInt(placement.workflow_id, 10) : null;
  const workflowStageId = placement && placement.workflow_stage_id != null ? parseInt(placement.workflow_stage_id, 10) : null;

  const conn = await pool.getConnection();
  try {
    const [idRows] = await conn.query(
      `SELECT id FROM stories WHERE id REGEXP '^S[0-9]+$' ORDER BY CAST(SUBSTRING(id, 2) AS UNSIGNED) DESC LIMIT 1`
    );
    const nextNum = idRows[0] ? parseInt(idRows[0].id.slice(1), 10) + 1 : 1;
    const id = body.id && String(body.id).trim() ? String(body.id).trim() : 'S' + String(nextNum).padStart(3, '0');

    const [existing] = await conn.query('SELECT id FROM stories WHERE id = ?', [id]);
    if (existing.length) return sendError(res, 400, 'Story id already exists: ' + id);

    let projectId = null;
    let wfId = null;
    let wfStageId = null;
    let backlogOrder = null;

    if (toBacklog || (!workflowId && !workflowStageId)) {
      const [maxOrder] = await conn.query(
        'SELECT COALESCE(MAX(backlog_order), 0) + 10 AS n FROM stories WHERE workflow_id IS NULL'
      );
      backlogOrder = maxOrder[0].n;
    } else if (workflowId && workflowStageId) {
      const [wf] = await conn.query('SELECT id, project_id FROM workflows WHERE id = ?', [workflowId]);
      if (!wf.length) {
        sendError(res, 400, 'Workflow not found');
        return;
      }
      const [stage] = await conn.query(
        'SELECT id FROM workflow_stages WHERE workflow_id = ? AND id = ?',
        [workflowId, workflowStageId]
      );
      if (!stage.length) {
        sendError(res, 400, 'Stage not found');
        return;
      }
      projectId = wf[0].project_id;
      wfId = workflowId;
      wfStageId = workflowStageId;
    }

    const ownerId = await getOwnerAgentId();
    const assigneeId = body.assignee_id === '' || body.assignee_id == null ? null : parseInt(body.assignee_id, 10);
    const blocked = body.blocked ? 1 : 0;

    await conn.query(
      `INSERT INTO stories (
        id, title, description, type, priority, project_id, workflow_id, workflow_stage_id, backlog_order,
        created_by_agent_id, assignee_id, blocked, blocked_reason, blocked_by,
        acceptance_criteria, implementation_notes, branch, review_reference, artifact,
        review_status, review_notes, rejection_count
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        id, title, body.description && String(body.description).trim() || null, type, priority,
        projectId, wfId, wfStageId, backlogOrder,
        ownerId, assigneeId, blocked, body.blocked_reason && String(body.blocked_reason).trim() || null,
        body.blocked_by && String(body.blocked_by).trim() || null,
        body.acceptance_criteria && String(body.acceptance_criteria).trim() || null,
        body.implementation_notes && String(body.implementation_notes).trim() || null,
        body.branch && String(body.branch).trim() || null,
        body.review_reference && String(body.review_reference).trim() || null,
        body.artifact && String(body.artifact).trim() || null,
        body.review_status && String(body.review_status).trim() || 'not-required',
        body.review_notes && String(body.review_notes).trim() || null,
        body.rejection_count != null ? parseInt(body.rejection_count, 10) || 0 : 0
      ]
    );

    if (wfId && wfStageId) {
      const [stageName] = await conn.query('SELECT stage_name FROM workflow_stages WHERE id = ?', [wfStageId]);
      await conn.query(
        `INSERT INTO story_stage_history (story_id, from_stage_name, to_stage_name, from_workflow_stage_id, to_workflow_stage_id, assignee_id, changed_by_agent_id)
         VALUES (?, NULL, ?, NULL, ?, ?, ?)`,
        [id, stageName[0].stage_name, wfStageId, assigneeId, ownerId]
      );
      await conn.query(
        'INSERT INTO story_audit_log (story_id, event_type, note) VALUES (?, ?, ?)',
        [id, 'add_to_workflow', 'Created in ' + stageName[0].stage_name]
      );
    }

    const depIds = Array.isArray(body.dependencies) ? body.dependencies.filter((d) => d && String(d).trim() && String(d).trim() !== id) : [];
    for (const depId of depIds) {
      const did = String(depId).trim();
      await conn.query(
        'INSERT IGNORE INTO story_dependencies (story_id, depends_on_story_id) VALUES (?, ?)',
        [id, did]
      );
    }
    const relatedIds = Array.isArray(body.related) ? body.related.filter((r) => r && String(r).trim() && String(r).trim() !== id) : [];
    for (const relId of relatedIds) {
      const rid = String(relId).trim();
      const [s, r] = id < rid ? [id, rid] : [rid, id];
      await conn.query(
        'INSERT IGNORE INTO story_related (story_id, related_story_id) VALUES (?, ?)',
        [s, r]
      );
    }

    const [created] = await conn.query(
      `SELECT s.id, s.title, s.type, s.priority, s.workflow_id, s.workflow_stage_id, s.project_id,
        ws.stage_name AS current_stage_name, w.name AS workflow_name
       FROM stories s
       LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id
       LEFT JOIN workflows w ON s.workflow_id = w.id
       WHERE s.id = ?`,
      [id]
    );
    res.status(201).json(created[0]);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to create story');
  } finally {
    conn.release();
  }
});

// ----- GET /api/stories/:storyId (full story for edit modal) -----
router.get('/stories/:storyId', async (req, res) => {
  const storyId = (req.params.storyId || '').trim();
  if (!storyId) return sendError(res, 400, 'Missing story id');
  try {
    const [rows] = await pool.query(
      `SELECT s.id, s.title, s.description, s.type, s.priority, s.project_id, s.workflow_id, s.workflow_stage_id,
        s.assignee_id, s.blocked, s.blocked_reason, s.blocked_by, s.acceptance_criteria, s.implementation_notes,
        s.branch, s.review_reference, s.artifact, s.review_status, s.review_notes, s.rejection_count,
        s.created_at, s.last_updated_at,
        a.name AS assignee_name,
        ws.stage_name AS current_stage_name, w.name AS workflow_name
       FROM stories s
       LEFT JOIN agents a ON s.assignee_id = a.id
       LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id
       LEFT JOIN workflows w ON s.workflow_id = w.id
       WHERE s.id = ? AND (s.deleted_at IS NULL)`,
      [storyId]
    );
    const story = rows[0];
    if (!story) return sendError(res, 404, 'Story not found');

    const [deps] = await pool.query(
      'SELECT depends_on_story_id FROM story_dependencies WHERE story_id = ? ORDER BY depends_on_story_id',
      [storyId]
    );
    const [relatedRows] = await pool.query(
      'SELECT story_id, related_story_id FROM story_related WHERE story_id = ? OR related_story_id = ?',
      [storyId, storyId]
    );
    const relatedIds = relatedRows.map((r) => (r.story_id === storyId ? r.related_story_id : r.story_id));

    story.dependencies = (deps || []).map((d) => d.depends_on_story_id);
    story.related = relatedIds;
    res.json(story);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch story');
  }
});

// ----- PATCH /api/stories/:storyId (update story fields, dependencies, related; logical delete via deleted: true) -----
router.patch('/stories/:storyId', async (req, res) => {
  const storyId = (req.params.storyId || '').trim();
  if (!storyId) return sendError(res, 400, 'Missing story id');
  const body = req.body || {};

  const conn = await pool.getConnection();
  try {
    const [existing] = await conn.query('SELECT id, deleted_at FROM stories WHERE id = ?', [storyId]);
    if (!existing.length) {
      sendError(res, 404, 'Story not found');
      return;
    }
    if (existing[0].deleted_at) {
      sendError(res, 404, 'Story not found');
      return;
    }

    // Logical delete: set deleted_at so story is hidden from lists
    if (body.deleted === true) {
      await conn.query(
        'UPDATE stories SET deleted_at = CURRENT_TIMESTAMP(3), last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
        [storyId]
      );
      res.json({ ok: true, id: storyId });
      return;
    }

    const allowed = [
      'title', 'description', 'type', 'priority', 'assignee_id',
      'blocked', 'blocked_reason', 'blocked_by', 'acceptance_criteria', 'implementation_notes',
      'branch', 'review_reference', 'artifact', 'review_status', 'review_notes', 'rejection_count'
    ];
    const updates = [];
    const values = [];
    for (const key of allowed) {
      if (!(key in body)) continue;
      if (key === 'blocked') {
        updates.push('`blocked` = ?');
        values.push(body[key] ? 1 : 0);
      } else if (key === 'assignee_id' || key === 'rejection_count') {
        const v = body[key];
        updates.push('`' + key + '` = ?');
        values.push(v === '' || v === null ? null : parseInt(v, 10));
      } else {
        updates.push('`' + key + '` = ?');
        values.push(body[key] === '' ? null : body[key]);
      }
    }
    if (updates.length) {
      values.push(storyId);
      await conn.query(
        'UPDATE stories SET ' + updates.join(', ') + ', last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
        values
      );
    }

    if (Array.isArray(body.dependencies)) {
      await conn.query('DELETE FROM story_dependencies WHERE story_id = ?', [storyId]);
      const depIds = body.dependencies.filter((id) => id && String(id).trim() && String(id).trim() !== storyId);
      for (const depId of depIds) {
        const id = String(depId).trim();
        if (!id) continue;
        await conn.query(
          'INSERT IGNORE INTO story_dependencies (story_id, depends_on_story_id) VALUES (?, ?)',
          [storyId, id]
        );
      }
    }
    if (Array.isArray(body.related)) {
      await conn.query(
        'DELETE FROM story_related WHERE story_id = ? OR related_story_id = ?',
        [storyId, storyId]
      );
      const relatedIds = body.related.filter((id) => id && String(id).trim() && String(id).trim() !== storyId);
      for (const relId of relatedIds) {
        const id = String(relId).trim();
        if (!id) continue;
        const [s, r] = storyId < id ? [storyId, id] : [id, storyId];
        await conn.query(
          'INSERT IGNORE INTO story_related (story_id, related_story_id) VALUES (?, ?)',
          [s, r]
        );
      }
    }

    const [updated] = await conn.query(
      `SELECT s.id, s.title, s.description, s.type, s.priority, s.assignee_id, s.blocked, s.blocked_reason,
        s.review_status, s.branch, s.review_reference, s.artifact, s.review_notes, s.rejection_count,
        a.name AS assignee_name
       FROM stories s LEFT JOIN agents a ON s.assignee_id = a.id WHERE s.id = ?`,
      [storyId]
    );
    const [deps] = await conn.query(
      'SELECT depends_on_story_id FROM story_dependencies WHERE story_id = ? ORDER BY depends_on_story_id',
      [storyId]
    );
    const [relRows] = await conn.query(
      'SELECT story_id, related_story_id FROM story_related WHERE story_id = ? OR related_story_id = ?',
      [storyId, storyId]
    );
    const out = updated[0];
    out.dependencies = (deps || []).map((d) => d.depends_on_story_id);
    out.related = relRows.map((r) => (r.story_id === storyId ? r.related_story_id : r.story_id));
    res.json(out);
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to update story');
  } finally {
    conn.release();
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
       WHERE s.id = ? AND (s.deleted_at IS NULL)`,
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

// ----- PATCH /api/stories/:storyId/workflow (add to workflow / move to backlog) -----
router.patch('/stories/:storyId/workflow', async (req, res) => {
  const storyId = (req.params.storyId || '').trim();
  const bodyWorkflowId = req.body && req.body.workflow_id;
  const bodyStageId = req.body && req.body.workflow_stage_id;
  const moveToBacklog = (bodyWorkflowId == null && bodyStageId == null);
  const workflowId = !moveToBacklog && bodyWorkflowId != null ? parseInt(bodyWorkflowId, 10) : null;
  const stageId = !moveToBacklog && bodyStageId != null ? parseInt(bodyStageId, 10) : null;

  if (!storyId) return sendError(res, 400, 'Missing story id');

  if (moveToBacklog) {
    const conn = await pool.getConnection();
    try {
      const [storyRows] = await conn.query(
        'SELECT id, workflow_id, workflow_stage_id FROM stories WHERE id = ? AND (deleted_at IS NULL)',
        [storyId]
      );
      const story = storyRows[0];
      if (!story) {
        sendError(res, 404, 'Story not found');
        return;
      }
      if (story.workflow_id == null) {
        sendError(res, 400, 'Story is already in the backlog');
        return;
      }
      const [stageRows] = await conn.query(
        'SELECT stage_name FROM workflow_stages WHERE id = ?',
        [story.workflow_stage_id]
      );
      const fromStageName = stageRows[0] ? stageRows[0].stage_name : null;
      const ownerId = await getOwnerAgentId();
      if (!ownerId) {
        sendError(res, 500, 'Owner agent not found');
        return;
      }
      // Shift existing backlog stories up so we can put the new one at 0 (front). backlog_order is UNSIGNED so we never use negatives.
      await conn.query(
        'UPDATE stories SET backlog_order = backlog_order + 10 WHERE workflow_id IS NULL'
      );
      await conn.query(
        'UPDATE stories SET workflow_id = NULL, workflow_stage_id = NULL, project_id = NULL, backlog_order = 0, last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
        [storyId]
      );
      await conn.query(
        `INSERT INTO story_stage_history (story_id, from_stage_name, to_stage_name, from_workflow_stage_id, to_workflow_stage_id, assignee_id, changed_by_agent_id)
         VALUES (?, ?, 'Backlog', ?, NULL, (SELECT assignee_id FROM stories WHERE id = ?), ?)`,
        [storyId, fromStageName, story.workflow_stage_id, storyId, ownerId]
      );
      await conn.query(
        'INSERT INTO story_audit_log (story_id, event_type, note) VALUES (?, ?, ?)',
        [storyId, 'move_to_backlog', `${fromStageName || 'Board'} → Backlog`]
      );
      const [updated] = await conn.query(
        'SELECT id, title, workflow_id, workflow_stage_id, NULL AS current_stage_name FROM stories WHERE id = ?',
        [storyId]
      );
      res.json(updated[0]);
    } catch (err) {
      sendError(res, 500, err.message || 'Failed to move story to backlog');
    } finally {
      conn.release();
    }
    return;
  }

  if (Number.isNaN(workflowId) || Number.isNaN(stageId)) return sendError(res, 400, 'Missing or invalid workflow_id or workflow_stage_id');

  const conn = await pool.getConnection();
  try {
    const [storyRows] = await conn.query(
      'SELECT id, project_id, workflow_id, workflow_stage_id FROM stories WHERE id = ? AND (deleted_at IS NULL)',
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
      'UPDATE stories SET workflow_id = ?, workflow_stage_id = ?, project_id = ?, backlog_order = NULL, last_updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?',
      [workflowId, stageId, workflow.project_id, storyId]
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

// ----- GET /api/dashboard (summary stats for the Home view) -----
router.get('/dashboard', async (req, res) => {
  try {
    let projects;
    try {
      [projects] = await pool.query(
        'SELECT id, name FROM projects WHERE (deleted_at IS NULL) ORDER BY id'
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [projects] = await pool.query('SELECT id, name FROM projects ORDER BY id');
      } else { throw colErr; }
    }

    let totalWorkflows = 0;
    try {
      const [wfCount] = await pool.query(
        'SELECT COUNT(*) AS cnt FROM workflows WHERE (deleted_at IS NULL)'
      );
      totalWorkflows = wfCount[0].cnt;
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        const [wfCount] = await pool.query('SELECT COUNT(*) AS cnt FROM workflows');
        totalWorkflows = wfCount[0].cnt;
      } else { throw colErr; }
    }

    let totalStories = 0;
    let blockedStories = 0;
    let backlogCount = 0;
    let storiesByType = [];
    let storiesByPriority = [];
    let storiesByStage = [];
    try {
      const [sc] = await pool.query(
        'SELECT COUNT(*) AS cnt FROM stories WHERE (deleted_at IS NULL)'
      );
      totalStories = sc[0].cnt;

      const [bc] = await pool.query(
        'SELECT COUNT(*) AS cnt FROM stories WHERE blocked = 1 AND (deleted_at IS NULL)'
      );
      blockedStories = bc[0].cnt;

      const [blc] = await pool.query(
        'SELECT COUNT(*) AS cnt FROM stories WHERE workflow_id IS NULL AND (deleted_at IS NULL)'
      );
      backlogCount = blc[0].cnt;

      [storiesByType] = await pool.query(
        'SELECT type, COUNT(*) AS cnt FROM stories WHERE (deleted_at IS NULL) GROUP BY type ORDER BY cnt DESC'
      );
      [storiesByPriority] = await pool.query(
        'SELECT priority, COUNT(*) AS cnt FROM stories WHERE (deleted_at IS NULL) GROUP BY priority ORDER BY FIELD(priority, "critical", "high", "medium", "low")'
      );
      [storiesByStage] = await pool.query(
        `SELECT COALESCE(ws.stage_name, 'Backlog') AS stage, COUNT(*) AS cnt
         FROM stories s LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id
         WHERE (s.deleted_at IS NULL) GROUP BY stage ORDER BY cnt DESC`
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        const [sc] = await pool.query('SELECT COUNT(*) AS cnt FROM stories');
        totalStories = sc[0].cnt;
        const [bc] = await pool.query('SELECT COUNT(*) AS cnt FROM stories WHERE blocked = 1');
        blockedStories = bc[0].cnt;
        const [blc] = await pool.query('SELECT COUNT(*) AS cnt FROM stories WHERE workflow_id IS NULL');
        backlogCount = blc[0].cnt;
        [storiesByType] = await pool.query('SELECT type, COUNT(*) AS cnt FROM stories GROUP BY type ORDER BY cnt DESC');
        [storiesByPriority] = await pool.query(
          'SELECT priority, COUNT(*) AS cnt FROM stories GROUP BY priority ORDER BY FIELD(priority, "critical", "high", "medium", "low")'
        );
        [storiesByStage] = await pool.query(
          `SELECT COALESCE(ws.stage_name, 'Backlog') AS stage, COUNT(*) AS cnt
           FROM stories s LEFT JOIN workflow_stages ws ON s.workflow_stage_id = ws.id GROUP BY stage ORDER BY cnt DESC`
        );
      } else { throw colErr; }
    }

    let recentActivity = [];
    try {
      [recentActivity] = await pool.query(
        `SELECT sal.story_id, sal.event_type, sal.note, sal.created_at
         FROM story_audit_log sal
         JOIN stories s ON sal.story_id = s.id AND (s.deleted_at IS NULL)
         ORDER BY sal.created_at DESC LIMIT 10`
      );
    } catch (colErr) {
      if (colErr.code === 'ER_BAD_FIELD_ERROR' && /deleted_at/.test(colErr.message)) {
        [recentActivity] = await pool.query(
          'SELECT story_id, event_type, note, created_at FROM story_audit_log ORDER BY created_at DESC LIMIT 10'
        );
      } else { throw colErr; }
    }

    const [agents] = await pool.query(
      'SELECT a.id, a.name, r.code AS role_code FROM agents a JOIN roles r ON a.role_id = r.id ORDER BY a.id'
    );

    res.json({
      projects: { count: projects.length, list: projects.map(p => ({ id: p.id, name: p.name })) },
      workflows: { count: totalWorkflows },
      stories: {
        total: totalStories,
        blocked: blockedStories,
        backlog: backlogCount,
        inWorkflow: totalStories - backlogCount,
        byType: storiesByType,
        byPriority: storiesByPriority,
        byStage: storiesByStage
      },
      agents: { count: agents.length, list: agents },
      recentActivity
    });
  } catch (err) {
    sendError(res, 500, err.message || 'Failed to fetch dashboard');
  }
});

module.exports = router;
