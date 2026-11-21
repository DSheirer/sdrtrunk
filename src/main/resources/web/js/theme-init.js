// Applies the chosen theme before the page draws, so it doesn't flash in the wrong colors.  A classic script, loaded
// in the head.  The themes match the desktop application's; 'system' follows the computer's light or dark setting.
(function () {
  var THEMES = [['system', 'System'], ['light', 'Light'], ['dark', 'Dark'], ['nord', 'Nord'], ['dracula', 'Dracula'],
    ['solarized-light', 'Solarized Light'], ['solarized-dark', 'Solarized Dark'],
    ['material-deep-ocean', 'Material Deep Ocean'], ['arc-dark', 'Arc Dark']];
  var KEY = 'sdrtrunk.theme';
  var darkQuery = window.matchMedia('(prefers-color-scheme: dark)');
  var choice = 'system';

  try {
    choice = JSON.parse(localStorage.getItem(KEY)) || 'system';
  } catch (e) {
    // Storage unavailable: follow the system
  }

  function apply() {
    var theme = choice === 'system' ? (darkQuery.matches ? 'dark' : 'light') : choice;
    var known = THEMES.some(function (entry) { return entry[0] === theme; });
    document.documentElement.setAttribute('data-theme', known ? theme : 'light');
  }

  window.sdrtrunkTheme = {
    themes: THEMES,
    get: function () { return choice; },
    set: function (value) {
      choice = value;
      try {
        localStorage.setItem(KEY, JSON.stringify(value));
      } catch (e) {
        // Not remembered
      }
      apply();
    }
  };

  darkQuery.addEventListener('change', function () {
    if (choice === 'system') {
      apply();
    }
  });
  apply();
})();
