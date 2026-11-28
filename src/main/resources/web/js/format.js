// Display formatting shared by the views
const timeFormat = new Intl.DateTimeFormat(undefined, {hour: '2-digit', minute: '2-digit', second: '2-digit',
  hour12: false});
const dateFormat = new Intl.DateTimeFormat(undefined, {month: 'short', day: 'numeric'});

export function time(ms) {
  if (!ms) {
    return '';
  }
  const date = new Date(ms);
  const today = new Date();
  const sameDay = date.toDateString() === today.toDateString();
  return (sameDay ? '' : dateFormat.format(date) + ' ') + timeFormat.format(date);
}

export function duration(ms) {
  if (ms === null || ms === undefined) {
    return '';
  }
  const seconds = ms / 1000;
  if (seconds < 60) {
    return seconds.toFixed(1) + ' s';
  }
  const minutes = Math.floor(seconds / 60);
  return minutes + ':' + String(Math.floor(seconds % 60)).padStart(2, '0');
}

// Talkgroup or radio: alias with the value, or the value
export function identifier(id) {
  if (!id) {
    return '';
  }
  return id.alias ? id.alias + ' (' + id.formatted + ')' : (id.formatted || String(id.value ?? ''));
}

export function megahertz(hz) {
  return hz ? (hz / 1e6).toFixed(4) + ' MHz' : '';
}

// Builds an element: el('td', {class: 'x', text: 'y'}, child...)
export function el(tag, attributes = {}, ...children) {
  const element = document.createElement(tag);
  for (const [name, value] of Object.entries(attributes)) {
    if (value === undefined || value === null || value === false) {
      continue;
    }
    if (name === 'text') {
      element.textContent = value;
    } else if (name.startsWith('on')) {
      element.addEventListener(name.substring(2), value);
    } else {
      element.setAttribute(name, value === true ? '' : value);
    }
  }
  for (const child of children) {
    if (child !== null && child !== undefined) {
      element.append(child);
    }
  }
  return element;
}

// Password box with a Show/Hide button that unmasks the password.  The password is masked again when the form is
// submitted or reset, so that the browser recognizes the box as a password when it offers to save it.
export function revealable(input) {
  const button = el('button', {type: 'button', class: 'secondary reveal', text: 'Show', title: 'Show the password',
    'aria-label': 'Show password', 'aria-pressed': 'false'});
  const set = (shown) => {
    input.type = shown ? 'text' : 'password';
    button.textContent = shown ? 'Hide' : 'Show';
    button.title = shown ? 'Hide the password' : 'Show the password';
    button.setAttribute('aria-label', shown ? 'Hide password' : 'Show password');
    button.setAttribute('aria-pressed', String(shown));
  };
  let formListening = false;
  button.addEventListener('click', () => {
    set(input.type === 'password');
    input.focus();
    if (!formListening && input.form) {
      formListening = true;
      input.form.addEventListener('submit', () => set(false), true);
      input.form.addEventListener('reset', () => set(false));
    }
  });
  return el('span', {class: 'reveal-field'}, input, button);
}

// Per-browser convenience settings; storage can be unavailable
export const prefs = {
  get(name, fallback) {
    try {
      const value = localStorage.getItem('sdrtrunk.' + name);
      return value === null ? fallback : JSON.parse(value);
    } catch (e) {
      return fallback;
    }
  },
  set(name, value) {
    try {
      localStorage.setItem('sdrtrunk.' + name, JSON.stringify(value));
    } catch (e) {
      // Not stored
    }
  }
};
