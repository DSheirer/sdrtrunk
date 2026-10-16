// Reuse map: catalog filters share one disclosure/sheet lifecycle, pagination
// shares count/action placement, and visibility choices use neutral tag pills.
// Controls, focus return, modal sizing, themes and phone layout remain shared.

export function pageRangeText({ offset = 0, visible = 0, total = null, label = 'Rows',
  format = (value) => Number(value).toLocaleString() }) {
  const range = visible ? `${format(offset + 1)}–${format(offset + visible)}` : '0';
  return `${label ? `${label} ` : ''}${range}${Number.isInteger(total) ? ` of ${format(total)}` : ''}`;
}

export function createBrowsingPager({ node, className = '', ariaLabel = 'Results pages',
  countText = '', previous = {}, next = {} }) {
  const pager = node('nav', `pager ui-pager ui-browse-pager ${className}`.trim());
  pager.setAttribute('aria-label', ariaLabel);
  const count = node('span', 'muted ui-browse-pager-count');
  const actions = node('div', 'ui-action-row ui-browse-pager-actions');
  pager.append(count, actions);
  let settings = { countText, previous, next };
  const controls = new Map();
  const update = (values) => {
    settings = { ...settings, ...values };
    count.textContent = settings.countText || '';
    count.hidden = !count.textContent;
    for (const [key, fallback] of [['previous', 'Previous'], ['next', 'Next']]) {
      const option = settings[key] || {};
      const enabled = option.enabled === true;
      const tag = enabled && option.href ? 'A' : 'BUTTON';
      let control = controls.get(key);
      if (!control || control.tagName !== tag) {
        const replacement = node(tag.toLowerCase(), 'ui-button ui-button-secondary');
        replacement.dataset.pageAction = key;
        if (control) control.replaceWith(replacement);
        else actions.append(replacement);
        controls.set(key, replacement);
        control = replacement;
      }
      control.textContent = option.label || fallback;
      if (tag === 'A') control.href = option.href;
      else { control.type = 'button'; control.disabled = !enabled; }
      control.setAttribute('aria-disabled', String(!enabled));
      control.onclick = enabled && option.onClick ? option.onClick : null;
      if (option.focusKey) control.dataset.receiverHealthFocus = option.focusKey;
    }
  };
  pager.update = update;
  update(settings);
  return pager;
}

export function createFilterDisclosure({ node, openReadOnlyModal, form, panel, fields = [panel],
  button, clearAction = null, applyLabel = 'Apply filters', initialExpanded = false,
  returnFocusSelector = null, id = 'browse-filters', media = '(max-width: 700px)' }) {
  button.type = 'button';
  button.classList.add('ui-filter-toggle');
  panel.classList.add('ui-filter-panel');
  if (!panel.id) panel.id = `${id}-panel`;
  if (!form.id) form.id = `${id}-form`;
  const isPhone = () => window.matchMedia(media).matches;
  panel.hidden = isPhone() || !initialExpanded;
  const synchronize = () => {
    button.setAttribute('aria-controls', panel.id);
    button.setAttribute('aria-expanded', String(!isPhone() && !panel.hidden));
  };
  synchronize();
  let modal = null;
  button.addEventListener('click', () => {
    if (!isPhone()) {
      panel.hidden = !panel.hidden;
      synchronize();
      return;
    }
    if (modal) return;
    // Mark exact locations before moving controls; their values and listeners
    // remain intact when closing without applying the draft.
    const locations = fields.map((field) => {
      const marker = field.ownerDocument.createComment('filter location');
      field.before(marker);
      const controls = [...field.querySelectorAll('input, select, textarea, button')]
        .map((control) => ({ control, owner: control.getAttribute('form') }));
      controls.forEach(({ control }) => control.setAttribute('form', form.id));
      return { field, marker, hidden: field.hidden, controls };
    });
    const sheet = node('div', 'ui-filter-sheet');
    const sheetFields = node('div', 'ui-filter-sheet-fields');
    for (const { field } of locations) {
      if (field === panel) field.hidden = false;
      sheetFields.append(field);
    }
    const actions = node('footer', 'ui-modal-footer ui-action-row ui-filter-sheet-actions' +
      (clearAction ? ' ui-modal-footer-primary-row' : ''));
    const cancel = node('button', 'ui-button ui-button-secondary', 'Cancel');
    cancel.type = 'button';
    cancel.addEventListener('click', () => modal?.close());
    if (clearAction) {
      const clear = node('button', 'ui-button ui-button-secondary', 'Clear filters');
      clear.type = 'button';
      clear.addEventListener('click', () => { modal?.close(); clearAction.click(); });
      actions.append(clear);
    }
    const apply = node('button', 'ui-button ui-button-primary', applyLabel);
    apply.type = 'button';
    apply.addEventListener('click', () => { modal?.close(); form.requestSubmit(); });
    actions.append(cancel, apply);
    sheet.append(node('p', 'ui-field-detail',
      `Closing keeps your choices. ${applyLabel} updates the results.`), sheetFields, actions);
    const restore = () => {
      for (const { field, marker, hidden, controls } of locations) {
        marker.replaceWith(field);
        if (field === panel) field.hidden = hidden;
        controls.forEach(({ control, owner }) => owner === null ? control.removeAttribute('form') :
          control.setAttribute('form', owner));
      }
      modal = null;
      synchronize();
    };
    modal = openReadOnlyModal('Filters', sheet, { id, className: 'ui-filter-modal',
      returnFocusSelector, onClose: restore });
    if (!modal) { restore(); return; }
    modal.dialog.id = `${id}-dialog`;
    button.setAttribute('aria-controls', modal.dialog.id);
    button.setAttribute('aria-expanded', 'true');
  });
  form.addEventListener('submit', () => modal?.close());
  return { synchronize };
}

export function scanListAvailabilityLabel(row) {
  return row?.published === false ? 'Hidden from listeners' : 'Available to listeners';
}

export function scanListAvailabilityPill(node, row) {
  return node('span', 'ui-pill', scanListAvailabilityLabel(row));
}
