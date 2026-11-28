// sdrtrunk web client: sign-in and account requests, navigation between views (#/calls, #/live, #/account,
// #/admin) and the shared page parts.
import {api, setCsrfToken} from './api.js';
import {live} from './live.js';
import {el, revealable} from './format.js';
import * as calls from './views/calls.js';
import * as liveView from './views/live.js';
import * as account from './views/account.js';
import * as admin from './views/admin.js';

const VIEWS = {'#/calls': calls, '#/live': liveView, '#/account': account, '#/admin': admin};
// Views that need a permission
const VIEW_PERMISSIONS = {'#/admin': 'users.manage'};
const $ = (id) => document.getElementById(id);
let me = null;
let unmount = null;
let toastTimer = null;

function toast(text, isError) {
  const element = $('toast');
  element.textContent = text || '';
  element.className = isError ? 'error' : '';
  element.hidden = !text;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { element.hidden = true; }, isError ? 8000 : 4000);
}

function showView() {
  if (unmount) {
    unmount();
    unmount = null;
  }
  const container = $('view');
  container.replaceChildren();
  if (!me) {
    container.append(loginView());
    return;
  }
  if (me.mustChangePassword) {
    container.append(el('section', {class: 'panel narrow'}, el('h2', {text: 'Choose a new password'}),
      el('p', {class: 'warning', text: 'Your password is temporary.  Choose a new password to continue.'}),
      account.passwordForm(toast, refresh, me.username)));
    return;
  }
  const allowed = (hash) => VIEWS[hash] && (!VIEW_PERMISSIONS[hash] || me.permissions.includes(VIEW_PERMISSIONS[hash]));
  const hash = allowed(location.hash) ? location.hash : '#/calls';
  document.querySelectorAll('#nav a[href^="#"]').forEach((link) =>
    link.classList.toggle('current', link.getAttribute('href') === hash));
  unmount = VIEWS[hash].mount(container, toast, me);
}

function loginView() {
  if (location.hash === '#/register') {
    return registerView();
  }
  const requestLink = el('p', {class: 'note', hidden: true}, 'No account? ',
    el('a', {href: '#/register', text: 'Request an account'}));
  api('GET', '/auth/options').then((options) => {
    requestLink.hidden = !options.registration;
  }).catch(() => {});
  const form = el('form', {class: 'stack'},
    el('label', {}, 'Username', el('input', {name: 'username', autocomplete: 'username', required: true})),
    el('label', {}, 'Password', revealable(el('input', {name: 'password', type: 'password',
      autocomplete: 'current-password', required: true}))),
    el('button', {type: 'submit', text: 'Sign in'}));
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const data = new FormData(form);
    try {
      setUser(await api('POST', '/auth/login', {username: data.get('username'), password: data.get('password')}));
    } catch (e) {
      toast(e.message, true);
    }
  });
  return el('section', {class: 'panel narrow'}, el('h2', {text: 'Sign in'}), form, requestLink);
}

// Account request: the account is created disabled and an administrator approves it
function registerView() {
  const password = el('input', {name: 'password', type: 'password', autocomplete: 'new-password', minlength: 10,
    required: true});
  const confirmPassword = el('input', {name: 'confirm', type: 'password', autocomplete: 'new-password',
    minlength: 10, required: true});
  const checkMatch = () => confirmPassword.setCustomValidity(
    confirmPassword.value && confirmPassword.value !== password.value ? 'The passwords don\'t match' : '');
  password.addEventListener('input', checkMatch);
  confirmPassword.addEventListener('input', checkMatch);
  const form = el('form', {class: 'stack', method: 'post', action: '#'},
    el('label', {}, 'Username', el('input', {name: 'username', autocomplete: 'username', required: true,
      minlength: 2, maxlength: 64, pattern: '[A-Za-z0-9][A-Za-z0-9._@\\-]{1,63}',
      title: 'Letters, digits and . _ @ -, starting with a letter or digit'})),
    el('label', {}, 'Display name (optional)', el('input', {name: 'displayName', autocomplete: 'name',
      maxlength: 128})),
    el('label', {}, 'Password (10 or more characters)', revealable(password)),
    el('label', {}, 'Confirm password', revealable(confirmPassword)),
    el('button', {type: 'submit', text: 'Request account'}));
  const section = el('section', {class: 'panel narrow'}, el('h2', {text: 'Request an account'}),
    el('p', {class: 'note', text: 'An administrator approves new accounts.  You can sign in after approval.'}),
    form, el('p', {class: 'note'}, el('a', {href: '#/calls', text: 'Back to sign in'})));
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    checkMatch();
    if (!form.reportValidity()) {
      return;
    }
    const data = new FormData(form);
    try {
      const result = await api('POST', '/auth/register', {username: data.get('username'),
        displayName: data.get('displayName') || null, password: data.get('password')});
      section.replaceChildren(el('h2', {text: 'Request sent'}), el('p', {text: result.message}),
        el('p', {}, el('a', {href: '#/calls', text: 'Back to sign in'})));
    } catch (e) {
      toast(e.message, true);
    }
  });
  return section;
}

function setUser(user) {
  me = user;
  setCsrfToken(user ? user.csrfToken : null);
  $('nav').hidden = !user || user.mustChangePassword;
  $('nav-admin').hidden = !user || !user.permissions.includes('users.manage');
  $('logout').hidden = !user;
  $('who').textContent = user ? (user.displayName || user.username) + ' · ' + user.role : '';
  if (!user) {
    live.closeAll();
  }
  showView();
}

async function refresh() {
  try {
    setUser(await api('GET', '/auth/me'));
  } catch (e) {
    setUser(null);
    if (e.status !== 401) {
      toast(e.message, true);
    }
  }
}

$('logout').addEventListener('click', async () => {
  try {
    await api('POST', '/auth/logout');
  } catch (e) {
    // Already signed out on the server
  }
  setUser(null);
});

// Theme menu: the themes come from theme-init.js, which applied the chosen one before the page drew
const themeSelect = $('theme-select');
for (const [value, label] of window.sdrtrunkTheme.themes) {
  themeSelect.append(el('option', {value, text: label, selected: value === window.sdrtrunkTheme.get()}));
}
themeSelect.addEventListener('change', () => window.sdrtrunkTheme.set(themeSelect.value));

live.onStatus((connected) => $('connection').classList.toggle('offline', !connected && live.subscriptions.size > 0));
window.addEventListener('hashchange', showView);
refresh();
