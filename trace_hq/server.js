/**
 * TRACE H.Q. — Server entry point.
 * Serves API under /api and static files from public/.
 * Do not log or pass DB credentials on the command line.
 */

const path = require('path');
const express = require('express');
const api = require('./api');

const app = express();
const PORT = process.env.PORT || 3000;
const publicDir = path.join(__dirname, 'public');

app.use(express.json());
app.use('/api', api);
app.use(express.static(publicDir));

app.get('/', (req, res) => {
  res.sendFile(path.join(publicDir, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`TRACE H.Q. listening on http://localhost:${PORT}`);
});
