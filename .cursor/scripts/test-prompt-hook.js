const fs = require('fs');
const path = require('path');

// Log file: project root so it's easy to find (works regardless of Cursor cwd)
const logPath = path.join(__dirname, '..', '..', 'hook-test.log');
const now = new Date().toISOString();

// Test text to stderr so you can see the hook ran
console.error('[test-prompt-hook] Ran at', now, '— beforeSubmitPrompt');

// Read stdin (prompt/context from Cursor), then log it and pass through to stdout
const chunks = [];
process.stdin.on('data', (chunk) => chunks.push(chunk));
process.stdin.on('end', () => {
  const raw = Buffer.concat(chunks).toString('utf8');
  const logLine = now + ' — prompt: ' + (raw.trim() || '(empty)') + '\n';
  try {
    fs.appendFileSync(logPath, logLine);
  } catch (e) {
    console.error('[test-prompt-hook] Could not write log:', e.message);
  }
  process.stdout.write(raw, 'utf8');
});
process.stdin.resume();
