'use strict';

  const MINIMUM_WIDTH = 48;
  const MAXIMUM_WIDTH = 1200;
  const STABLE_ID = /^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$/;

  function stableId(value, subject) {
    const id = typeof value === 'string' ? value.trim() : '';
    if (!id || id.length > 64 || !STABLE_ID.test(id)) {
      throw new Error(`${subject} requires a valid stable ID.`);
    }
    return id;
  }

  function columnId(column) {
    return stableId(column?.id, 'Every table column');
  }

  function tableId(value) {
    return stableId(value, 'Every table');
  }

  function schema(columns) {
    if (!Array.isArray(columns) || !columns.length) throw new Error('A table requires columns.');
    const ids = columns.map(columnId);
    if (new Set(ids).size !== ids.length) throw new Error('Table column IDs must be unique.');
    return ids;
  }

  function constrainedColumns(columns, property) {
    return columns.filter((column) => column?.[property] === true).map(columnId);
  }

  function registerSchema(registry, value, columns) {
    if (!registry || typeof registry.get !== 'function' || typeof registry.set !== 'function') {
      throw new TypeError('A table schema registry is required.');
    }
    const id = tableId(value);
    const next = schema(columns);
    const current = registry.get(id);
    if (current && (current.length !== next.length || current.some((column, index) => column !== next[index]))) {
      throw new Error(`Table ${id} must use one stable column schema.`);
    }
    if (!current) registry.set(id, next.slice());
    return next;
  }

  function width(value, fallback = null) {
    const numeric = Math.round(Number(value));
    return Number.isFinite(numeric) ? Math.max(MINIMUM_WIDTH, Math.min(MAXIMUM_WIDTH, numeric)) : fallback;
  }

  function normalize(columns, saved) {
    const exactSchema = schema(columns);
    const byId = new Map(columns.map((column) => [columnId(column), column]));
    const groups = Object.fromEntries(columns.map((column) => [columnId(column), String(column.group || '')]));
    const defaults = columns.map(columnId);
    const essentialColumns = constrainedColumns(columns, 'essential');
    const fixedColumns = constrainedColumns(columns, 'fixed');
    const fresh = (reset = false, resetReason = null) => ({
      schema: exactSchema.slice(),
      columns: columns.slice(),
      column_order: defaults.slice(),
      column_widths: {},
      hidden_columns: [],
      collapsed_groups: [],
      groups,
      essential_columns: essentialColumns.slice(),
      fixed_columns: fixedColumns.slice(),
      reset,
      reset_reason: resetReason
    });
    if (!saved) return fresh();
    const savedSchema = Array.isArray(saved?.schema) ? saved.schema : [];
    const schemaMatches = savedSchema.length === exactSchema.length &&
      savedSchema.every((id, index) => id === exactSchema[index]);
    if (typeof saved !== 'object' || Array.isArray(saved)) return fresh(true, 'invalid-layout');
    if (!schemaMatches) return fresh(true, 'schema-changed');
    if (!Array.isArray(saved.column_order) || saved.column_order.length !== defaults.length ||
        new Set(saved.column_order).size !== defaults.length ||
        saved.column_order.some((id) => !byId.has(id))) {
      return fresh(true, 'invalid-order');
    }
    if (fixedColumns.some((id) => saved.column_order.indexOf(id) !== defaults.indexOf(id))) {
      return fresh(true, 'fixed-column-moved');
    }
    const hidden = Array.isArray(saved.hidden_columns) ? saved.hidden_columns.slice() : [];
    if (new Set(hidden).size !== hidden.length || hidden.some((id) => !byId.has(id))) {
      return fresh(true, 'invalid-hidden-columns');
    }
    if (hidden.some((id) => essentialColumns.includes(id))) {
      return fresh(true, 'essential-column-hidden');
    }
    if (hidden.length === defaults.length) return fresh(true, 'all-columns-hidden');
    const widths = saved.column_widths && typeof saved.column_widths === 'object' &&
      !Array.isArray(saved.column_widths) ? saved.column_widths : {};
    if (Object.keys(widths).some((id) => !byId.has(id))) {
      return fresh(true, 'invalid-widths');
    }
    const widthEntries = Object.entries(widths);
    if (widthEntries.some(([, value]) => !Number.isInteger(value) || value < MINIMUM_WIDTH ||
        value > MAXIMUM_WIDTH)) return fresh(true, 'invalid-widths');
    const normalizedWidths = Object.fromEntries(widthEntries);
    const collapsedGroups = Array.isArray(saved.collapsed_groups) ? saved.collapsed_groups.slice() : [];
    if (collapsedGroups.length > 128 || new Set(collapsedGroups).size !== collapsedGroups.length ||
        collapsedGroups.some((id) => {
          try { stableId(id, 'Every collapsed row group'); return false; } catch (_error) { return true; }
        })) {
      return fresh(true, 'invalid-collapsed-groups');
    }
    const hiddenSet = new Set(hidden);
    return {
      schema: exactSchema.slice(),
      columns: saved.column_order.filter((id) => !hiddenSet.has(id)).map((id) => byId.get(id)),
      column_order: saved.column_order.slice(),
      column_widths: normalizedWidths,
      hidden_columns: hidden,
      collapsed_groups: collapsedGroups,
      groups,
      essential_columns: essentialColumns.slice(),
      fixed_columns: fixedColumns.slice(),
      reset: false,
      reset_reason: null
    };
  }

  function move(layout, id, beforeId = null) {
    const order = layout.column_order.slice();
    const from = order.indexOf(id);
    if (from < 0 || (beforeId !== null && !order.includes(beforeId))) throw new Error('Unknown table column.');
    if ((layout.fixed_columns || []).includes(id)) throw new Error('Fixed table columns cannot be moved.');
    if (beforeId === id) return { ...layout, column_order: order };
    const group = layout.groups?.[id] || '';
    if (beforeId !== null && (layout.groups?.[beforeId] || '') !== group) {
      throw new Error('Grouped columns can only be reordered within their group.');
    }
    order.splice(from, 1);
    let target = beforeId === null ? order.length : order.indexOf(beforeId);
    if (beforeId === null && group) {
      const lastInGroup = order.reduce((last, column, index) =>
        (layout.groups?.[column] || '') === group ? index : last, -1);
      target = lastInGroup + 1;
    }
    order.splice(target, 0, id);
    if ((layout.fixed_columns || []).some((column) =>
      order.indexOf(column) !== layout.schema.indexOf(column))) {
      throw new Error('Table columns cannot be moved across a fixed column.');
    }
    return { ...layout, column_order: order };
  }

  function canMove(layout, id, beforeId = null) {
    try {
      const moved = move(layout, id, beforeId);
      return moved.column_order.some((column, index) => column !== layout.column_order[index]);
    } catch (_error) {
      return false;
    }
  }

  function resize(layout, id, value) {
    if (!layout.column_order.includes(id)) throw new Error('Unknown table column.');
    return { ...layout, column_widths: { ...layout.column_widths, [id]: width(value, MINIMUM_WIDTH) } };
  }

  function setHidden(layout, id, hidden) {
    if (!layout.column_order.includes(id)) throw new Error('Unknown table column.');
    if (hidden && (layout.essential_columns || []).includes(id)) {
      throw new Error('Essential table columns cannot be hidden.');
    }
    const values = new Set(layout.hidden_columns);
    if (hidden) values.add(id);
    else values.delete(id);
    if (values.size === layout.column_order.length) throw new Error('A table must keep at least one visible column.');
    return { ...layout, hidden_columns: layout.column_order.filter((column) => values.has(column)) };
  }

  function setGroupCollapsed(layout, id, collapsed) {
    const groupId = stableId(id, 'Every collapsed row group');
    const values = new Set(layout.collapsed_groups || []);
    if (collapsed) values.add(groupId);
    else values.delete(groupId);
    if (values.size > 128) throw new Error('A table cannot store more than 128 collapsed row groups.');
    return { ...layout, collapsed_groups: [...values].sort() };
  }

  function persisted(layout) {
    return {
      schema: layout.schema.slice(),
      column_order: layout.column_order.slice(),
      column_widths: { ...layout.column_widths },
      hidden_columns: layout.hidden_columns.slice(),
      collapsed_groups: (layout.collapsed_groups || []).slice()
    };
  }

export {
  MINIMUM_WIDTH, MAXIMUM_WIDTH, columnId, tableId, schema, registerSchema,
  normalize, move, canMove, resize, setHidden, setGroupCollapsed, persisted
};
