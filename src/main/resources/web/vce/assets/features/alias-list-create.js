import { ALIAS_LIST_NAME_MAX_LENGTH } from './discovery-radioreference.js?v=8';

let creatorSequence = 0;

function normalizedFamily(value) {
  return String(value || '').trim().toUpperCase();
}

function familyLabel(value) {
  const family = normalizedFamily(value);
  return family === 'NBFM' ? 'Conventional Analog (AM/NBFM)' : family;
}

export async function createAliasList(requestJson, { name, family, revision }) {
  const normalizedName = String(name || '').trim();
  const normalized = normalizedFamily(family);
  const result = await requestJson('/api/v1/admin/alias-lists', {
    method: 'POST', body: {
      revision: Number(revision ?? 0), name: normalizedName, family: normalized.toLowerCase()
    }
  });
  const id = Number(result?.alias_list_id);
  if (!Number.isInteger(id) || id <= 0) throw new Error('The new Alias List did not return a usable identifier.');
  return {
    ...result,
    aliasList: { id, alias_list_id: id, name: normalizedName, family: normalized }
  };
}

export function createAliasListPopupTrigger(dependencies, options) {
  const { node, iconGlyph, uiPill, requestJson, openReadOnlyModal } = dependencies;
  const {
    select, family, getRevision, onCreated, triggerLabel = 'New list', submitLabel = 'Create and use',
    helperText = 'The list is created immediately and selected for this workflow.'
  } = options;
  const normalized = normalizedFamily(family);
  const host = node('div', 'ui-alias-list-create');
  const trigger = node('button', 'ui-button ui-button-secondary ui-alias-list-create-trigger');
  trigger.type = 'button';
  if (iconGlyph) trigger.append(iconGlyph('icon-plus'));
  trigger.append(node('span', '', triggerLabel));
  trigger.setAttribute('aria-haspopup', 'dialog');
  trigger.setAttribute('aria-expanded', 'false');
  host.append(trigger);

  trigger.addEventListener('click', () => {
    const form = node('form', 'admin-form');
    const name = node('input', 'ui-input');
    name.type = 'text';
    name.maxLength = ALIAS_LIST_NAME_MAX_LENGTH;
    name.autocomplete = 'off';
    const nameField = node('label', 'ui-field');
    nameField.append(node('span', 'ui-field-label', 'Name'), name);
    const context = node('div', 'ui-field-detail');
    context.append('Protocol family: ', uiPill(familyLabel(normalized), 'neutral'));
    const detail = node('div', 'ui-field-detail', helperText);
    const error = node('div');
    error.setAttribute('role', 'alert');
    const cancel = node('button', 'ui-button ui-button-secondary', 'Cancel');
    cancel.type = 'button';
    const submit = node('button', 'ui-button ui-button-primary', submitLabel);
    submit.type = 'submit';
    const actions = node('footer', 'ui-modal-footer ui-action-row');
    actions.append(cancel, submit);
    form.append(nameField, context, detail, error, actions);
    const modal = openReadOnlyModal('Create Alias List', form, {
      id: `create-alias-list-child-${++creatorSequence}`, className: 'modal-size-small alias-list-create-modal',
      stack: 'child', onClose: () => trigger.setAttribute('aria-expanded', 'false')
    });
    if (!modal) return;
    trigger.setAttribute('aria-expanded', 'true');
    cancel.addEventListener('click', () => modal.close());
    name.focus();
    let createdResult = null;
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const listName = name.value.trim();
      error.replaceChildren();
      if (!listName) {
        error.append(node('div', 'ui-notice ui-notice-danger', 'Enter a name for the Alias List.'));
        name.focus();
        return;
      }
      modal.setBusy(true);
      name.disabled = true;
      cancel.disabled = true;
      submit.disabled = true;
      try {
        if (!createdResult) {
          const revision = await getRevision();
          createdResult = await createAliasList(requestJson, { name: listName, family: normalized, revision });
        }
        await onCreated(createdResult);
        modal.setBusy(false);
        modal.close();
        (select && !select.disabled ? select : trigger).focus({ preventScroll: true });
      } catch (cause) {
        error.append(node('div', 'ui-notice ui-notice-danger',
          createdResult ? `${createdResult.aliasList.name} was created, but could not be selected. ` +
            `${cause?.message || 'Try again.'}` : cause?.message || 'The Alias List could not be created.'));
        modal.setBusy(false);
        name.disabled = Boolean(createdResult);
        cancel.disabled = false;
        submit.disabled = false;
        if (createdResult) {
          submit.textContent = 'Use created list';
          submit.focus();
        } else name.focus();
      }
    });
  });
  return host;
}
