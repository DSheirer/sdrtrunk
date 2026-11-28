// Account: the signed-in user, password change, the user's API tokens and the server's system information.
import {api} from '../api.js';
import {el, revealable, time} from '../format.js';
import {ago} from './admin.js';

export function mount(container, toast, me) {
  const account = el('dl');
  const system = el('dl');
  const tokensBody = el('tbody');
  fill(account, [['Username', me.username], ['Role', me.role], ['Permissions', me.permissions.join(', ')]]);
  container.append(
    el('section', {class: 'panel'}, el('h2', {text: 'Account'}), account),
    el('section', {class: 'panel'}, el('h2', {text: 'Change password'}), passwordForm(toast, () => {}, me.username)),
    el('section', {class: 'panel'}, el('h2', {text: 'API tokens'}),
      el('p', {class: 'note', text: 'An administrator gives you API tokens.  Scripts send a token as the header ' +
        'Authorization: Bearer <token>, and act as your account.'}),
      el('div', {class: 'table-wrap'}, el('table', {class: 'calls'},
        el('thead', {}, el('tr', {}, ...['Name', 'Token', 'Created', 'Expires', 'Last used', '']
          .map((label) => el('th', {text: label})))),
        tokensBody))),
    el('section', {class: 'panel'}, el('h2', {text: 'System'}), system));

  const loadTokens = () => api('GET', '/auth/tokens').then((page) => tokensBody.replaceChildren(
    ...(page.items.length ? page.items.map((token) => el('tr', {},
      el('td', {text: token.name}),
      el('td', {class: 'mono', text: token.prefix + '…'}),
      el('td', {class: 'nowrap', text: time(Date.parse(token.created))}),
      el('td', {class: 'nowrap' + (token.expired ? ' warning' : ''), text: token.expires ?
        (token.expired ? 'Expired ' : '') + time(Date.parse(token.expires)) : 'Never'}),
      el('td', {class: 'nowrap', text: ago(token.lastUsed ? Date.parse(token.lastUsed) : null)}),
      el('td', {}, el('button', {type: 'button', class: 'secondary', text: 'Revoke', onclick: async () => {
        if (confirm('Revoke the token "' + token.name + '"?  Scripts that use it stop working.')) {
          try {
            await api('DELETE', '/auth/tokens/' + token.id);
          } catch (e) {
            toast(e.message, true);
          }
          loadTokens();
        }
      }})))) : [el('tr', {}, el('td', {colspan: 6, class: 'note', text: 'No API tokens'}))])))
    .catch((e) => toast(e.message, true));
  loadTokens();

  api('GET', '/system').then((s) => fill(system, [
    ['Node', s.nodeName], ['Instance', s.instanceId], ['Version', s.version],
    ['Uptime', Math.round(s.uptimeMs / 60000) + ' minutes'], ['Java', s.javaVersion], ['OS', s.os + ' ' + s.arch],
    ['Heap', Math.round(s.memory.heapUsedBytes / 1048576) + ' of ' + Math.round(s.memory.heapMaxBytes / 1048576) +
      ' MB'],
    ['Database', s.database ? s.database.type + (s.database.connected ? '' : ' (not connected)') : '-']
  ])).catch((e) => toast(e.message, true));
  return () => {};
}

// Laid out the way password managers expect a change password form: the username, then the current password, then
// the new password twice, each marked with its autocomplete purpose.  A browser's suggested password then fills both
// new password boxes and is saved for this username.
export function passwordForm(toast, changed, username) {
  const newPassword = el('input', {id: 'new-password', name: 'newPassword', type: 'password',
    autocomplete: 'new-password', minlength: 10, required: true});
  const confirmPassword = el('input', {id: 'confirm-password', name: 'confirmPassword', type: 'password',
    autocomplete: 'new-password', minlength: 10, required: true});
  const checkMatch = () => confirmPassword.setCustomValidity(
    confirmPassword.value && confirmPassword.value !== newPassword.value ? 'The new passwords don\'t match' : '');
  newPassword.addEventListener('input', checkMatch);
  confirmPassword.addEventListener('input', checkMatch);
  const form = el('form', {class: 'stack', method: 'post', action: '#'},
    el('label', {}, 'Username', el('input', {id: 'username', name: 'username', type: 'text', value: username,
      autocomplete: 'username', readonly: true})),
    el('label', {}, 'Current password', revealable(el('input', {id: 'current-password', name: 'currentPassword',
      type: 'password', autocomplete: 'current-password', required: true}))),
    el('label', {}, 'New password (10 or more characters)', revealable(newPassword)),
    el('label', {}, 'Confirm new password', revealable(confirmPassword)),
    el('button', {type: 'submit', text: 'Change password'}));
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    checkMatch();
    if (!form.reportValidity()) {
      return;
    }
    const data = new FormData(form);
    try {
      await api('POST', '/auth/password', {currentPassword: data.get('currentPassword'),
        newPassword: data.get('newPassword')});
      form.reset();
      toast('Password changed');
      changed();
    } catch (e) {
      toast(e.message, true);
    }
  });
  return form;
}

function fill(list, entries) {
  list.replaceChildren();
  for (const [name, value] of entries) {
    list.append(el('dt', {text: name}), el('dd', {text: value === null || value === undefined ? '-' :
      String(value)}));
  }
}
