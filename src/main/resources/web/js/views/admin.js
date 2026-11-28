// Admin (users.manage): account requests waiting for approval, the accounts with who is active and when each one last
// used the web page or the API, password resets, API tokens for an account, and the recent audit log.
import {api, query} from '../api.js';
import {el, time} from '../format.js';

const REFRESH_MS = 15000;
const ROLE_LABELS = {ADMIN: 'Admin', OPERATOR: 'Operator', LISTENER: 'Listener', VIEWER: 'Viewer'};

// "just now", "4 min ago", "3 h ago", or the date and time
export function ago(ms) {
  if (!ms) {
    return 'Never';
  }
  const seconds = Math.max(0, (Date.now() - ms) / 1000);
  if (seconds < 60) {
    return 'Just now';
  }
  if (seconds < 3600) {
    return Math.floor(seconds / 60) + ' min ago';
  }
  if (seconds < 86400) {
    return Math.floor(seconds / 3600) + ' h ago';
  }
  return time(ms);
}

function isoMs(text) {
  return text ? Date.parse(text) : null;
}

// Text that is shown once, such as a temporary password or an API token, with a Copy button
export function secretBox(label, secret, note) {
  const input = el('input', {class: 'secret-value', readonly: true, value: secret, 'aria-label': label,
    onfocus: (event) => event.target.select()});
  const copy = el('button', {type: 'button', class: 'secondary', text: 'Copy', onclick: async () => {
    try {
      await navigator.clipboard.writeText(secret);
      copy.textContent = 'Copied';
    } catch (e) {
      input.focus();
      input.select();
    }
  }});
  return el('div', {class: 'secret-box'}, el('strong', {text: label}), el('div', {class: 'reveal-field'}, input,
    copy), note ? el('p', {class: 'note', text: note}) : null);
}

function roleSelect(value) {
  const select = el('select', {'aria-label': 'Role'});
  for (const [name, label] of Object.entries(ROLE_LABELS)) {
    select.append(el('option', {value: name, text: label, selected: name === value}));
  }
  return select;
}

function status(user) {
  if (user.pendingApproval) {
    return {text: 'Needs approval', cls: 'pending'};
  }
  if (!user.enabled) {
    return {text: 'Disabled', cls: 'disabled'};
  }
  if (user.locked) {
    return {text: 'Locked', cls: 'disabled'};
  }
  if (user.active) {
    return {text: 'Active now', cls: 'active'};
  }
  return {text: user.mustChangePassword ? 'Temporary password' : 'Enabled', cls: ''};
}

export function mount(container, toast, me) {
  let users = [];
  let selectedId = null;
  let message = null;   // {userId, node}: a temporary password or token shown once
  let timer = null;
  let disposed = false;

  const pendingPanel = el('section', {class: 'panel'});
  const usersBody = el('tbody');
  const summary = el('span', {class: 'note'});
  const detailPanel = el('section', {class: 'panel'});
  const auditBody = el('tbody');

  const createForm = el('form', {class: 'admin-create'},
    el('label', {}, 'Username', el('input', {name: 'username', required: true, autocomplete: 'off'})),
    el('label', {}, 'Display name', el('input', {name: 'displayName', autocomplete: 'off'})),
    el('label', {}, 'Role', roleSelect('LISTENER')),
    el('button', {type: 'submit', text: 'Create account'}));
  createForm.querySelector('select').name = 'role';
  createForm.addEventListener('submit', (event) => {
    event.preventDefault();
    const data = new FormData(createForm);
    act(async () => {
      const created = await api('POST', '/users', {username: data.get('username'),
        displayName: data.get('displayName') || null, role: data.get('role')});
      createForm.reset();
      selectedId = created.user.id;
      message = {userId: created.user.id, node: secretBox('Temporary password for ' + created.user.username,
        created.temporaryPassword, 'Give it to the user.  It is shown only now and must be changed at the first ' +
        'sign-in.')};
      toast('Created ' + created.user.username);
    });
  });

  container.append(
    pendingPanel,
    el('section', {class: 'panel'},
      el('h2', {}, 'Accounts ', summary),
      el('p', {class: 'note', text: 'Click an account to change it, reset its password or give it API tokens.  ' +
        'Active means a request in the last 5 minutes.'}),
      el('div', {class: 'table-wrap'}, el('table', {class: 'calls admin-users'},
        el('thead', {}, el('tr', {},
          ...['', 'Username', 'Name', 'Role', 'Status', 'Sessions', 'Web page', 'API', 'Last sign-in']
            .map((name) => el('th', {text: name})))),
        usersBody)),
      el('h3', {class: 'admin-subtitle', text: 'New account'}),
      createForm),
    detailPanel,
    el('section', {class: 'panel'}, el('h2', {text: 'Recent activity'}),
      el('div', {class: 'table-wrap'}, el('table', {class: 'calls'},
        el('thead', {}, el('tr', {}, ...['Time', 'User', 'Action', 'Target', 'Result', 'Address', 'Detail']
          .map((name) => el('th', {text: name})))),
        auditBody))));

  async function act(work) {
    try {
      await work();
    } catch (e) {
      toast(e.message, true);
    }
    await load();
  }

  async function load() {
    try {
      const all = [];
      let cursor = null;
      do {
        const page = await api('GET', '/users' + query({limit: 500, cursor}));
        all.push(...page.items);
        cursor = page.nextCursor;
      } while (cursor);
      users = all;
      const audit = await api('GET', '/audit' + query({limit: 25}));
      if (disposed) {
        return;
      }
      render();
      renderAudit(audit.items);
    } catch (e) {
      if (!disposed) {
        toast(e.message, true);
      }
    }
  }

  function render() {
    const pending = users.filter((user) => user.pendingApproval);
    const accounts = users.filter((user) => !user.pendingApproval);
    const active = accounts.filter((user) => user.active).length;
    summary.textContent = accounts.length + (accounts.length === 1 ? ' account' : ' accounts') + ', ' + active +
      ' active now';
    renderPending(pending);
    usersBody.replaceChildren(...accounts.map((user) => {
      const state = status(user);
      const row = el('tr', {class: user.id === selectedId ? 'selected' : null, onclick: () => select(user.id)},
        el('td', {class: 'state'}, el('span', {class: 'dot' + (user.active ? ' on' : ''),
          title: user.active ? 'Active in the last 5 minutes' : ''})),
        el('td', {text: user.username + (user.id === me.id ? ' (you)' : '')}),
        el('td', {text: user.displayName || ''}),
        el('td', {text: ROLE_LABELS[user.role] || user.role}),
        el('td', {}, el('span', {class: 'user-status ' + state.cls, text: state.text})),
        el('td', {class: 'num', text: String(user.sessions)}),
        el('td', {class: 'nowrap', text: ago(user.lastWebAccessMs), title: user.lastWebAccess || ''}),
        el('td', {class: 'nowrap', text: ago(user.lastApiAccessMs), title: user.lastApiAccess || ''}),
        el('td', {class: 'nowrap', text: ago(isoMs(user.lastLogin)), title: user.lastLogin || ''}));
      return row;
    }));
    renderDetail();
  }

  function renderPending(pending) {
    pendingPanel.hidden = pending.length === 0;
    pendingPanel.replaceChildren(el('h2', {}, 'Account requests ', el('span', {class: 'pending-count',
      text: String(pending.length)})),
    el('p', {class: 'note', text: 'These users asked for an account on the sign-in page.  Approve to let them ' +
      'sign in with the role you choose, or reject to delete the request.'}),
    el('div', {class: 'table-wrap'}, el('table', {class: 'calls'},
      el('thead', {}, el('tr', {}, ...['Username', 'Name', 'Requested', 'Role', ''].map((name) =>
        el('th', {text: name})))),
      el('tbody', {}, ...pending.map((user) => {
        const role = roleSelect('LISTENER');
        return el('tr', {},
          el('td', {text: user.username}),
          el('td', {text: user.displayName || ''}),
          el('td', {class: 'nowrap', text: ago(isoMs(user.created)), title: user.created}),
          el('td', {}, role),
          el('td', {}, el('div', {class: 'admin-actions inline'},
            el('button', {type: 'button', text: 'Approve', onclick: () => act(async () => {
              await api('POST', '/users/' + user.id + '/approve', {role: role.value});
              toast('Approved ' + user.username + ' as ' + ROLE_LABELS[role.value]);
            })}),
            el('button', {type: 'button', class: 'secondary', text: 'Reject', onclick: () => {
              if (confirm('Reject and delete the account request from ' + user.username + '?')) {
                act(async () => {
                  await api('DELETE', '/users/' + user.id);
                  toast('Rejected ' + user.username);
                });
              }
            }}))));
      })))));
  }

  function select(id) {
    selectedId = selectedId === id ? null : id;
    if (message && message.userId !== selectedId) {
      message = null;
    }
    render();
  }

  async function renderDetail() {
    const user = users.find((candidate) => candidate.id === selectedId && !candidate.pendingApproval);
    detailPanel.hidden = !user;
    if (!user) {
      detailPanel.replaceChildren();
      return;
    }
    const self = user.id === me.id;
    const role = roleSelect(user.role);
    role.addEventListener('change', () => act(async () => {
      await api('PATCH', '/users/' + user.id, {role: role.value});
      toast(user.username + ' is now ' + ROLE_LABELS[role.value]);
    }));
    const name = el('input', {value: user.displayName || '', 'aria-label': 'Display name'});
    name.addEventListener('change', () => act(() => api('PATCH', '/users/' + user.id,
      {displayName: name.value})));

    const buttons = el('div', {class: 'admin-actions'},
      el('button', {type: 'button', class: 'secondary', text: 'Reset password', onclick: () => {
        if (confirm('Give ' + user.username + ' a new temporary password?  Their sessions end and they must ' +
          'choose a new password at the next sign-in.')) {
          act(async () => {
            const result = await api('POST', '/users/' + user.id + '/reset-password');
            message = {userId: user.id, node: secretBox('Temporary password for ' + user.username,
              result.temporaryPassword, 'Give it to the user.  It is shown only now.')};
          });
        }
      }}),
      el('button', {type: 'button', class: 'secondary', text: user.enabled ? 'Disable' : 'Enable',
        disabled: self && user.enabled, title: self ? 'You can\'t disable your own account' : '',
        onclick: () => act(() => api('PATCH', '/users/' + user.id, {enabled: !user.enabled}))}),
      el('button', {type: 'button', class: 'secondary', text: 'Sign out everywhere',
        title: 'End all browser sessions and delete all API tokens of this account', onclick: () => {
          if (confirm('End all of ' + user.username + '\'s sessions and delete all of their API tokens?')) {
            act(async () => {
              const result = await api('DELETE', '/users/' + user.id + '/sessions');
              toast('Revoked ' + result.revoked);
            });
          }
        }}),
      el('button', {type: 'button', class: 'secondary danger', text: 'Delete account', disabled: self,
        onclick: () => {
          if (confirm('Delete the account ' + user.username + ' and its API tokens?')) {
            act(async () => {
              await api('DELETE', '/users/' + user.id);
              selectedId = null;
              toast('Deleted ' + user.username);
            });
          }
        }}));

    const tokenName = el('input', {name: 'name', required: true, placeholder: 'For example: scanner script',
      'aria-label': 'Token name'});
    const expiry = el('select', {'aria-label': 'Expires'},
      ...[['', 'Never'], ['30', '30 days'], ['90', '90 days'], ['365', '1 year']].map(([value, label]) =>
        el('option', {value, text: label})));
    const tokenForm = el('form', {class: 'admin-token-form'}, tokenName,
      el('label', {}, 'Expires ', expiry), el('button', {type: 'submit', text: 'Create API token'}));
    tokenForm.addEventListener('submit', (event) => {
      event.preventDefault();
      act(async () => {
        const created = await api('POST', '/users/' + user.id + '/tokens', {name: tokenName.value,
          expiresInDays: expiry.value ? Number(expiry.value) : null});
        message = {userId: user.id, node: secretBox('API token for ' + user.username, created.token,
          'Give it to the user, who sends it as the header  Authorization: Bearer <token>.  It is shown only now; ' +
          'sdrtrunk stores only its hash.')};
      });
    });
    const tokensBody = el('tbody', {}, el('tr', {}, el('td', {colspan: 6, class: 'note', text: 'Loading…'})));

    detailPanel.replaceChildren(
      el('h2', {}, user.username, el('span', {class: 'user-status ' + status(user).cls, text: status(user).text})),
      el('dl', {},
        el('dt', {text: 'Display name'}), el('dd', {}, name),
        el('dt', {text: 'Role'}), el('dd', {}, role),
        el('dt', {text: 'Created'}), el('dd', {text: time(isoMs(user.created))}),
        el('dt', {text: 'Web page'}), el('dd', {text: ago(user.lastWebAccessMs) + (user.sessions ? '  ·  ' +
          user.sessions + (user.sessions === 1 ? ' open session' : ' open sessions') : '')}),
        el('dt', {text: 'API'}), el('dd', {text: ago(user.lastApiAccessMs)})),
      buttons,
      message && message.userId === user.id ? message.node : null,
      el('h3', {class: 'admin-subtitle', text: 'API tokens'}),
      el('p', {class: 'note', text: 'A token acts as this account, with the permissions of its role.'}),
      el('div', {class: 'table-wrap'}, el('table', {class: 'calls'},
        el('thead', {}, el('tr', {}, ...['Name', 'Token', 'Created', 'Expires', 'Last used', ''].map((label) =>
          el('th', {text: label})))),
        tokensBody)),
      tokenForm);

    try {
      const tokens = (await api('GET', '/users/' + user.id + '/tokens')).items;
      tokensBody.replaceChildren(...(tokens.length ? tokens.map((token) => el('tr', {},
        el('td', {text: token.name}),
        el('td', {class: 'mono', text: token.prefix + '…'}),
        el('td', {class: 'nowrap', text: time(isoMs(token.created))}),
        el('td', {class: 'nowrap' + (token.expired ? ' warning' : ''), text: token.expires ?
          (token.expired ? 'Expired ' : '') + time(isoMs(token.expires)) : 'Never'}),
        el('td', {class: 'nowrap', text: ago(isoMs(token.lastUsed))}),
        el('td', {}, el('button', {type: 'button', class: 'secondary', text: 'Revoke', onclick: () => {
          if (confirm('Revoke the token "' + token.name + '"?  Scripts that use it stop working.')) {
            act(() => api('DELETE', '/users/' + user.id + '/tokens/' + token.id));
          }
        }})))) : [el('tr', {}, el('td', {colspan: 6, class: 'note', text: 'No API tokens'}))]));
    } catch (e) {
      tokensBody.replaceChildren(el('tr', {}, el('td', {colspan: 6, class: 'warning', text: e.message})));
    }
  }

  function renderAudit(entries) {
    auditBody.replaceChildren(...entries.map((entry) => el('tr', {},
      el('td', {class: 'nowrap', text: time(entry.timeMs)}),
      el('td', {text: entry.username || ''}),
      el('td', {text: entry.action}),
      el('td', {text: entry.target || ''}),
      el('td', {class: entry.success ? '' : 'warning', text: entry.success ? 'OK' : 'Refused'}),
      el('td', {text: entry.address || ''}),
      el('td', {text: entry.detail || ''}))));
  }

  load();
  // Keep the tokens list stable while typing in the detail panel: only refresh when nothing there has focus
  timer = setInterval(() => {
    if (!detailPanel.contains(document.activeElement) && !createForm.contains(document.activeElement)) {
      load();
    }
  }, REFRESH_MS);
  return () => {
    disposed = true;
    clearInterval(timer);
  };
}
