// Table columns the user can show, hide and reorder, like the desktop application's column editor.  The choice is
// kept per browser.  Column headers can be dragged to change the order.
import {el, prefs} from './format.js';

export class ColumnModel {
  // definitions: [{id, name, heading?, class?, hidden?, render}] in the default order
  constructor(definitions, prefName) {
    this.definitions = definitions;
    this.prefName = prefName;
    this.onChange = null;
    this.order = [];   // [{column, visible}]
    this.load();
  }

  load() {
    const byId = new Map(this.definitions.map((column) => [column.id, column]));
    const saved = prefs.get(this.prefName, null);
    this.order = [];
    if (Array.isArray(saved)) {
      for (const item of saved) {
        const column = item && byId.get(item.id);
        if (column) {
          this.order.push({column, visible: !!item.visible});
          byId.delete(item.id);
        }
      }
    }
    // Columns added since the choice was saved go at their default position
    for (const column of byId.values()) {
      const index = this.definitions.indexOf(column);
      const before = this.order.findIndex((item) => this.definitions.indexOf(item.column) > index);
      const entry = {column, visible: !column.hidden};
      if (before < 0) {
        this.order.push(entry);
      } else {
        this.order.splice(before, 0, entry);
      }
    }
    if (!this.order.some((item) => item.visible)) {
      this.order = this.defaults();
    }
  }

  defaults() {
    return this.definitions.map((column) => ({column, visible: !column.hidden}));
  }

  visible() {
    return this.order.filter((item) => item.visible).map((item) => item.column);
  }

  visibleCount() {
    return this.order.filter((item) => item.visible).length;
  }

  isDefault() {
    const defaults = this.defaults();
    return this.order.every((item, index) => item.column === defaults[index].column &&
      item.visible === defaults[index].visible);
  }

  setVisible(id, visible) {
    const item = this.order.find((entry) => entry.column.id === id);
    // Keep at least one column
    if (item && item.visible !== visible && (visible || this.visibleCount() > 1)) {
      item.visible = visible;
      this.changed();
    }
  }

  // Moves a column by step places in the editor's list.  Returns true when it moved.
  move(id, step) {
    const index = this.order.findIndex((entry) => entry.column.id === id);
    const target = index + step;
    if (index < 0 || target < 0 || target >= this.order.length) {
      return false;
    }
    const [item] = this.order.splice(index, 1);
    this.order.splice(target, 0, item);
    this.changed();
    return true;
  }

  // Moves a column next to another one, before it or after it
  place(id, targetId, after) {
    if (id === targetId) {
      return;
    }
    const index = this.order.findIndex((entry) => entry.column.id === id);
    if (index < 0) {
      return;
    }
    const [item] = this.order.splice(index, 1);
    let target = this.order.findIndex((entry) => entry.column.id === targetId);
    if (target < 0) {
      this.order.splice(index, 0, item);
      return;
    }
    this.order.splice(after ? target + 1 : target, 0, item);
    this.changed();
  }

  reset() {
    this.order = this.defaults();
    this.changed();
  }

  changed() {
    prefs.set(this.prefName, this.order.map((item) => ({id: item.column.id, visible: item.visible})));
    if (this.onChange) {
      this.onChange();
    }
    for (const listener of this.listeners || []) {
      listener();
    }
  }

  addListener(listener) {
    (this.listeners ||= []).push(listener);
  }
}

// The Columns button and its editor panel.  It also renders the table header row and makes its headers draggable.
export class ColumnEditor {
  constructor(model, title, headerRow) {
    this.model = model;
    this.title = title;
    this.headerRow = headerRow;
    this.selected = null;
    this.panel = null;
    this.button = el('button', {type: 'button', class: 'secondary columns-button', text: 'Columns',
      title: 'Choose the columns to show and the column order', 'aria-expanded': 'false',
      onclick: (event) => {
        event.stopPropagation();
        this.toggle();
      }});
    this.onDocumentClick = (event) => {
      if (this.panel && !this.panel.contains(event.target) && event.target !== this.button) {
        this.close();
      }
    };
    this.onKey = (event) => {
      if (event.key === 'Escape') {
        this.close();
        this.button.focus();
      }
    };
    model.addListener(() => {
      this.renderHeader();
      this.renderList();
    });
    this.renderHeader();
  }

  // ---- Table header ----
  renderHeader() {
    this.headerRow.replaceChildren(...this.model.visible().map((column) => {
      const th = el('th', {text: column.heading ?? column.name, class: column.class || null, draggable: 'true',
        title: column.heading === '' ? column.name : null, 'data-column': column.id});
      th.addEventListener('dragstart', (event) => {
        event.dataTransfer.setData('text/x-sdrtrunk-column', column.id);
        event.dataTransfer.effectAllowed = 'move';
        th.classList.add('dragging');
      });
      th.addEventListener('dragend', () => {
        th.classList.remove('dragging');
        this.clearDropMarks();
      });
      th.addEventListener('dragover', (event) => {
        if (event.dataTransfer.types.includes('text/x-sdrtrunk-column')) {
          event.preventDefault();
          this.clearDropMarks();
          th.classList.add(this.afterHalf(event, th, true) ? 'drop-after' : 'drop-before');
        }
      });
      th.addEventListener('dragleave', () => th.classList.remove('drop-before', 'drop-after'));
      th.addEventListener('drop', (event) => {
        const id = event.dataTransfer.getData('text/x-sdrtrunk-column');
        if (id) {
          event.preventDefault();
          this.clearDropMarks();
          this.model.place(id, column.id, this.afterHalf(event, th, true));
        }
      });
      return th;
    }));
  }

  afterHalf(event, element, horizontal) {
    const box = element.getBoundingClientRect();
    return horizontal ? event.clientX > box.left + box.width / 2 : event.clientY > box.top + box.height / 2;
  }

  clearDropMarks() {
    for (const element of document.querySelectorAll('.drop-before, .drop-after')) {
      element.classList.remove('drop-before', 'drop-after');
    }
  }

  // ---- Editor panel ----
  toggle() {
    if (this.panel) {
      this.close();
    } else {
      this.open();
    }
  }

  open() {
    this.selected = null;
    this.list = el('ul', {class: 'column-list', role: 'listbox', tabindex: '0', 'aria-label': 'Columns'});
    this.list.addEventListener('keydown', (event) => this.listKey(event));
    this.moveUp = el('button', {type: 'button', class: 'secondary', text: 'Move Up',
      title: 'Move the selected column toward the left side of the table', onclick: () => this.moveSelected(-1)});
    this.moveDown = el('button', {type: 'button', class: 'secondary', text: 'Move Down',
      title: 'Move the selected column toward the right side of the table', onclick: () => this.moveSelected(1)});
    this.resetButton = el('button', {type: 'button', class: 'secondary', text: 'Reset',
      title: 'Restore the default columns and column order', onclick: () => {
        this.selected = null;
        this.model.reset();
      }});
    const closeButton = el('button', {type: 'button', text: 'Close', onclick: () => this.close()});
    this.panel = el('div', {class: 'column-editor', role: 'dialog', 'aria-label': this.title},
      el('h3', {text: this.title}),
      el('p', {class: 'note', text: 'Check the columns to show.  You can also drag the columns here or the column ' +
        'headers in the table to change the column order.'}),
      el('div', {class: 'column-editor-body'}, this.list,
        el('div', {class: 'column-editor-buttons'}, this.moveUp, this.moveDown, this.resetButton,
          el('span', {class: 'spacer'}), closeButton)));
    this.panel.addEventListener('click', (event) => event.stopPropagation());
    this.button.after(this.panel);
    this.button.setAttribute('aria-expanded', 'true');
    this.renderList();
    this.position();
    document.addEventListener('click', this.onDocumentClick);
    document.addEventListener('keydown', this.onKey);
    this.list.focus();
  }

  close() {
    if (this.panel) {
      this.panel.remove();
      this.panel = null;
      this.button.setAttribute('aria-expanded', 'false');
      document.removeEventListener('click', this.onDocumentClick);
      document.removeEventListener('keydown', this.onKey);
    }
  }

  // Opens below the button, aligned to its right edge, and inside the window
  position() {
    const parent = this.button.offsetParent || document.body;
    const button = this.button.getBoundingClientRect();
    const origin = parent.getBoundingClientRect();
    const width = Math.min(this.panel.offsetWidth, window.innerWidth - 16);
    const left = Math.max(8 - origin.left, Math.min(button.right - origin.left - width,
      window.innerWidth - 8 - origin.left - width));
    this.panel.style.left = left + 'px';
    this.panel.style.top = (button.bottom - origin.top + 4) + 'px';
  }

  renderList() {
    if (!this.panel) {
      return;
    }
    const hadFocus = this.panel.contains(document.activeElement);
    const lastVisible = this.model.visibleCount() === 1;
    this.list.replaceChildren(...this.model.order.map((item) => {
      const id = item.column.id;
      const box = el('input', {type: 'checkbox', checked: item.visible, tabindex: '-1',
        disabled: item.visible && lastVisible,
        title: item.visible && lastVisible ? 'At least one column is shown' : null});
      box.addEventListener('change', () => this.model.setVisible(id, box.checked));
      const li = el('li', {role: 'option', draggable: 'true', 'data-column': id,
        'aria-selected': String(this.selected === id), class: this.selected === id ? 'selected' : null},
        el('label', {}, box, item.column.name));
      li.addEventListener('click', () => {
        this.selected = id;
        this.updateSelection();
      });
      li.addEventListener('dragstart', (event) => {
        event.dataTransfer.setData('text/x-sdrtrunk-column', id);
        event.dataTransfer.effectAllowed = 'move';
        this.selected = id;
        this.updateSelection();
      });
      li.addEventListener('dragend', () => this.clearDropMarks());
      li.addEventListener('dragover', (event) => {
        if (event.dataTransfer.types.includes('text/x-sdrtrunk-column')) {
          event.preventDefault();
          this.clearDropMarks();
          li.classList.add(this.afterHalf(event, li, false) ? 'drop-after' : 'drop-before');
        }
      });
      li.addEventListener('drop', (event) => {
        const dragged = event.dataTransfer.getData('text/x-sdrtrunk-column');
        if (dragged) {
          event.preventDefault();
          this.clearDropMarks();
          this.model.place(dragged, id, this.afterHalf(event, li, false));
        }
      });
      return li;
    }));
    this.updateSelection();
    // Rebuilding the list drops the focus of a clicked checkbox; keep the keyboard on the list
    if (hadFocus && !this.panel.contains(document.activeElement)) {
      this.list.focus();
    }
  }

  updateSelection() {
    for (const li of this.list.children) {
      const selected = li.dataset.column === this.selected;
      li.classList.toggle('selected', selected);
      li.setAttribute('aria-selected', String(selected));
      if (selected) {
        li.scrollIntoView({block: 'nearest'});
      }
    }
    const index = this.model.order.findIndex((item) => item.column.id === this.selected);
    this.moveUp.disabled = index <= 0;
    this.moveDown.disabled = index < 0 || index >= this.model.order.length - 1;
    this.resetButton.disabled = this.model.isDefault();
  }

  moveSelected(step) {
    if (this.selected) {
      this.model.move(this.selected, step);
    }
  }

  // Up and down select, Alt+Up and Alt+Down move, Space shows or hides the selected column
  listKey(event) {
    const order = this.model.order;
    const index = order.findIndex((item) => item.column.id === this.selected);
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const step = event.key === 'ArrowDown' ? 1 : -1;
      if (event.altKey) {
        this.moveSelected(step);
      } else {
        const next = Math.max(0, Math.min(order.length - 1, index < 0 ? 0 : index + step));
        this.selected = order[next].column.id;
        this.updateSelection();
      }
    } else if (event.key === ' ' && index >= 0) {
      event.preventDefault();
      this.model.setVisible(this.selected, !order[index].visible);
    }
  }
}
