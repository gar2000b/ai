/**
 * TRACE H.Q. — Server entry point.
 * Serves API under /api and static files from public/.
 * Do not log or pass DB credentials on the command line.
 * Warms up DB before listening to avoid "Failed to load stories" on first request after cold start.
 */

const path = require('path');
const express = require('express');
const api = require('./api');
const { pool } = require('./config/db');

const app = express();
const PORT = process.env.PORT || 3000;
const publicDir = path.join(__dirname, 'public');

const WARMUP_RETRIES = 5;
const WARMUP_DELAY_MS = 1000;

async function waitForDb() {
  for (let attempt = 1; attempt <= WARMUP_RETRIES; attempt++) {
    try {
      await pool.query('SELECT 1');
      return true;
    } catch (err) {
      if (attempt === WARMUP_RETRIES) {
        console.error('DB warmup failed after %d attempts: %s', WARMUP_RETRIES, err.message);
        return false;
      }
      console.warn('DB not ready (attempt %d/%d), retrying in %dms…', attempt, WARMUP_RETRIES, WARMUP_DELAY_MS);
      await new Promise((r) => setTimeout(r, WARMUP_DELAY_MS));
    }
  }
  return false;
}

app.use(express.json());
app.use('/api', api);
app.use(express.static(publicDir));

app.get('/', (req, res) => {
  res.sendFile(path.join(publicDir, 'index.html'));
});

(async () => {
  const dbReady = await waitForDb();
  if (!dbReady) {
    console.warn('Starting server anyway; first requests may fail until MySQL is ready. Refresh the page to retry.');
  } else {
    console.log('DB connection ready.');
  }
  app.listen(PORT, () => {
    console.log(`TRACE H.Q. listening on http://localhost:${PORT}`);
  });
})();
