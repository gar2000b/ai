/**
 * TRACE H.Q. — Settings UI: theme picker, persistence (localStorage).
 */

const THEME_KEY = 'tracehq_theme';
const THEMES = ['light', 'dark', 'medium'];

function getStoredTheme() {
  try {
    const t = localStorage.getItem(THEME_KEY);
    return THEMES.includes(t) ? t : 'light';
  } catch {
    return 'light';
  }
}

function setStoredTheme(theme) {
  if (!THEMES.includes(theme)) return;
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch (_) {}
  applyTheme(theme);
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
}

function renderSettingsPanel(container) {
  const current = getStoredTheme();
  container.innerHTML = `
    <div class="settings-panel">
      <h2>Settings</h2>
      <div class="settings-row">
        <label>Theme</label>
        <div class="theme-options">
          ${THEMES.map(
            (t) =>
              `<label><input type="radio" name="theme" value="${t}" ${t === current ? 'checked' : ''}> ${t.charAt(0).toUpperCase() + t.slice(1)}</label>`
          ).join(' ')}
        </div>
      </div>
    </div>
  `;
  container.querySelectorAll('input[name="theme"]').forEach((radio) => {
    radio.addEventListener('change', (e) => setStoredTheme(e.target.value));
  });
}

// Apply theme on load (before any view renders)
applyTheme(getStoredTheme());

window.TraceHqSettings = { getStoredTheme, setStoredTheme, applyTheme, renderSettingsPanel };
