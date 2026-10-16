// Default layouts for every data table. Tables not listed here use their declared
// column order, show every column, and use the shared widths below.
const COLUMN_WIDTHS = Object.freeze({
  action: 82, affiliation: 190, 'confirmed-channel': 220, alias: 170,
  band: 54, calls: 66, 'control-frequency': 120, count: 94,
  decoder: 78, encrypted: 52, encryption: 92, signaling: 134,
  event: 115, 'first-seen': 166, frequency: 94, group: 135,
  'group-identity-description': 240, 'group-identity-id': 90,
  'group-identity-name': 175, 'last-active': 166, 'last-seen': 166,
  lcn: 68, name: 175, 'neighbor-name': 175, radio: 82,
  'radio-alias': 165, recorded: 78, rfss: 66, signal: 82,
  'decode-health': 90, site: 66, source: 82, 'source-alias': 165,
  'source-ota-alias': 165, state: 82, status: 116, streamed: 82,
  system: 106, 'talker-alias': 160, 'talkgroup-name': 175,
  target: 82, 'target-alias': 165, time: 166, wacn: 108,
  select: 48
});

const ALIAS_WIDTHS = Object.freeze({
  select: 48, alias: 190, description: 260, identifier: 180,
  matcher: 120, group: 180, behavior: 145, overlap: 92
});
const ALIAS_GROW = ['alias', 'description', 'group', 'behavior'];
const ACTIVITY_DEFAULTS = {
  widths: { action: 110, event: 180, source: 220, target: 220, channel: 320,
    'source-alias': 190, 'source-ota-alias': 190, 'target-alias': 190 },
  grow: ['channel', 'source-alias', 'source-ota-alias', 'target-alias']
};
const NEIGHBOR_DEFAULTS = {
  widths: { 'neighbor-name': 240, system: 250 },
  grow: ['neighbor-name', 'system']
};
const PATCH_DEFAULTS = {
  widths: { 'patch-name': 190, 'member-talkgroup-ids': 180,
    'member-talkgroup-names': 240, 'member-radio-ids': 240, 'member-radio-names': 240 },
  grow: ['patch-name', 'member-talkgroup-names', 'member-radio-names']
};
const RETAINED_DEFAULTS = {
  widths: { item: 320, action: 112, 'alias-list': 210, 'alias-group': 190,
    'last-seen': 174, 'saved-records': 120 },
  grow: ['item', 'alias-list', 'alias-group']
};
const RETAINED_FREQUENCIES = {
  widths: { ...RETAINED_DEFAULTS.widths, item: 180, channel: 130, use: 160,
    uplink: 110, timeslots: 80, slots: 80, state: 100,
    observation_count: 110, voice_grant_observations: 110 },
  grow: ['item', 'use']
};
const RETAINED_BANDS = {
  widths: { ...RETAINED_DEFAULTS.widths, item: 180, base: 110, spacing: 110,
    offset: 120, timeslots: 80, state: 100, observation_count: 110 },
  grow: ['item']
};

const TABLE_DEFAULTS = Object.freeze({
  'issi-current-assignments': {
    widths: { radio: 220, 'home-system': 230, 'working-id': 120, confirmed: 180,
      expires: 185, evidence: 130, 'observed-on': 260 },
    grow: ['radio', 'home-system', 'observed-on']
  },
  'issi-recent-changes': {
    widths: { changed: 180, radio: 220, 'home-system': 230, change: 165,
      'working-id': 120, 'previous-working-id': 160, 'observed-on': 260 },
    grow: ['radio', 'home-system', 'observed-on']
  },
  'issi-subscribers': {
    widths: { 'canonical-subscriber': 220, 'working-id': 170, 'home-system': 230,
      'talker-alias': 220, evidence: 150, affiliation: 220, 'observed-on': 260,
      'last-mapping-seen': 180, 'last-seen': 180 },
    grow: ['canonical-subscriber', 'home-system', 'talker-alias', 'observed-on']
  },
  'issi-foreign-systems': {
    widths: { 'home-system': 250, subscribers: 115, 'wuid-observed-subscribers': 120,
      registrations: 85, affiliations: 85, calls: 100, bands: 70, 'first-seen': 180, 'last-seen': 180 },
    grow: ['home-system']
  },
  'issi-frequency-bands': {
    widths: { 'home-system': 250, band: 90, 'observed-on': 260, state: 160, 'last-seen': 180 },
    grow: ['home-system', 'observed-on']
  },
  'retained-statistics-v3': RETAINED_DEFAULTS,
  'retained-statistics-v3.radio_system.frequencies': RETAINED_FREQUENCIES,
  'retained-statistics-v3.saved_channel.frequencies': RETAINED_FREQUENCIES,
  'retained-statistics-v3.radio_system.band_plans': RETAINED_BANDS,
  'retained-statistics-v3.radio_system.foreign_band_plans': RETAINED_BANDS,
  activity: ACTIVITY_DEFAULTS,
  'activity-conventional-analog-v1': ACTIVITY_DEFAULTS,
  'streaming-aliases': {
    widths: { selected: 88, name: 220, identifier: 145, list: 230 },
    grow: ['name', 'list']
  },
  'channel-frequencies-p25': {
    widths: { descriptor: 104, callsign: 110, tags: 130, downlink: 94, uplink: 94,
      tdma: 60, slots: 56, state: 86, 'voice-observations': 80,
      'data-observations': 70, 'last-seen': 116 },
    hidden: ['callsign', 'tdma', 'data-observations'],
    grow: ['descriptor', 'callsign', 'tags']
  },
  'channel-frequency-bands': {
    widths: { band: 64, base: 94, spacing: 68, bandwidth: 68, offset: 92,
      tdma: 60, slots: 56, state: 82, observations: 76, 'last-seen': 116 },
    hidden: ['tdma'], grow: ['base', 'last-seen']
  },
  'channel-frequency-bands-override': {
    widths: { band: 64, base: 94, spacing: 68, bandwidth: 68, offset: 92,
      tdma: 60, slots: 56 },
    hidden: ['tdma'], grow: ['base', 'offset']
  },
  'channel-catalog-admin-v1': {
    widths: { select: 48, name: 300, frequency: 260, protocol: 160,
      status: 112, 'auto-start': 144, 'alias-list': 190 },
    grow: ['name', 'frequency', 'alias-list']
  },
  'channel-catalog-admin-v2': {
    widths: { select: 48, name: 320, frequency: 280,
      status: 112, 'auto-start': 144, 'alias-list': 210 },
    grow: ['name', 'frequency', 'alias-list']
  },
  'channel-catalog-readonly-v1': {
    widths: { name: 300, frequency: 260, protocol: 160, status: 112, 'alias-list': 190 },
    grow: ['name', 'frequency', 'alias-list']
  },
  'group-identities': {
    widths: { 'group-identity-id': 64, 'group-identity-kind': 60,
      'group-identity-name': 160, 'group-identity-description': 240,
      'alias-group': 200, 'logical-calls': 90, 'recorded-logical-calls': 65,
      'stream-submitted-logical-calls': 95, 'encrypted-logical-calls': 80,
      signaling: 100, 'last-seen': 174 },
    grow: ['group-identity-name', 'group-identity-description', 'alias-group']
  },
  radios: {
    widths: { radio: 220, alias: 175, 'talker-alias': 175,
      affiliation: 210, 'confirmed-channel': 260, 'logical-calls': 90,
      'encrypted-logical-calls': 90, 'last-seen': 174 },
    grow: ['alias', 'talker-alias', 'affiliation', 'confirmed-channel']
  },
  'group-identity-radios': {
    widths: { radio: 220, alias: 175, 'talker-alias': 175,
      'confirmed-channel': 260, 'logical-calls': 90,
      'encrypted-logical-calls': 90, 'last-seen': 174 },
    grow: ['alias', 'talker-alias', 'confirmed-channel']
  },
  'talker-aliases': {
    widths: { radio: 220, 'talker-alias': 190, 'radio-alias': 190,
      'logical-calls': 90, 'encrypted-logical-calls': 90, 'last-seen': 174 },
    grow: ['talker-alias', 'radio-alias']
  },
  'channel-radios': {
    widths: { radio: 220, 'radio-alias': 190, 'talkgroup-name': 190,
      'last-peer': 220, 'peer-alias': 190, 'last-talkgroup': 100 },
    grow: ['radio-alias', 'talkgroup-name', 'peer-alias']
  },
  'channel-group-identities': {
    widths: { source: 220, 'source-alias': 190 },
    grow: ['group-identity-name', 'group-identity-description', 'source-alias']
  },
  'channel-top-groups': {
    widths: { group: 190, 'group-identity-name': 190 },
    grow: ['group', 'group-identity-name']
  },
  'alias-observed-group-identities': {
    widths: { 'group-identity-id': 190, source: 280, calls: 80,
      signaling: 110, 'last-seen': 174, action: 112 },
    grow: ['group-identity-id', 'source']
  },
  'alias-coverage-unassigned-v1': {
    widths: { identity: 230, system: 260, calls: 80, first: 190, last: 190 },
    grow: ['identity', 'system']
  },
  'alias-coverage-configured-v1': {
    widths: { name: 220, identity: 180, calls: 80, signals: 85, first: 190, heard: 190 },
    grow: ['name', 'identity']
  },
  'action-counts': {
    widths: { action: 160, count: 110 }, grow: ['action']
  },
  'system-action-observations': {
    widths: { action: 160, observations: 110 }, grow: ['action']
  },
  'radio-system-channels': {
    widths: { name: 180, details: 260, 'control-frequency': 120,
      channels: 56, neighbors: 56, 'last-seen': 174 },
    grow: ['name', 'details']
  },
  'channel-neighbors-p25': NEIGHBOR_DEFAULTS,
  'channel-neighbors-trunked': NEIGHBOR_DEFAULTS,
  'channel-patches': PATCH_DEFAULTS,
  'channel-patches-versioned': PATCH_DEFAULTS,
  'channel-foreign-frequency-bands': {
    widths: { system: 250 }, grow: ['system']
  },
  'dashboard-activity-radios': {
    widths: { system: 280, radio: 220, alias: 220,
      observations: 110, 'last-seen': 174 },
    grow: ['system', 'alias']
  },
  'dashboard-call-sources': {
    widths: { receiver: 300, mode: 80, 'logical-calls': 85,
      'recorded-logical-calls': 70, 'stream-submitted-logical-calls': 90 },
    grow: ['receiver']
  },
  'dashboard-destinations': {
    widths: { identity: 190, system: 200, mode: 80, 'logical-calls': 85,
      'recorded-logical-calls': 70, 'stream-submitted-logical-calls': 90 },
    grow: ['identity', 'system']
  },
  'dashboard-sources': {
    widths: { identity: 190, system: 200, mode: 80, 'logical-calls': 85,
      'recorded-logical-calls': 70, 'stream-submitted-logical-calls': 90 },
    grow: ['identity', 'system']
  },
  'dashboard-receivers': { widths: {
    name: 442, mode: 180, context: 315, frequency: 237, 'last-seen': 419
  } },
  'live-events': { widths: {
    time: 92, duration: 78, event: 181, from: 220, to: 220,
    channel: 260, details: 600
  }, grow: ['from', 'to', 'channel', 'details'] },
  'live-messages': { widths: { time: 95, channel: 220, context: 110, message: 1200 } },
  'alias-editor-source-breakdown': { widths: {
    source: 230, 'source-calls': 100, 'source-signaling': 110, 'last-seen': 166
  } },
  'alias-editor-activity': { widths: ALIAS_WIDTHS, grow: ALIAS_GROW },
  'alias-editor-custom': {
    widths: ALIAS_WIDTHS, grow: ALIAS_GROW,
    visible: ['select', 'alias', 'description', 'identifier', 'matcher', 'group',
      'calls', 'signaling', 'last-seen']
  },
  'alias-scan-list-members': {
    widths: { ...ALIAS_WIDTHS, 'alias-list': 210, family: 120 },
    grow: [...ALIAS_GROW, 'alias-list']
  },
  'live-channels': { widths: {
    status: 105, tags: 100, channel: 150, frequency: 96, signal: 95,
    'decode-health': { normal: 105, detailed: 260 }, 'source-alias': 220,
    source: 90, 'target-alias': 220, target: 90, decoder: 78
  }, grow: ['channel', 'source-alias', 'target-alias'] },
  'alias-editor-configure': {
    widths: ALIAS_WIDTHS, grow: ALIAS_GROW
  },
  'admin-users': { widths: {
    username: 230, 'access-tier': 150, 'password-changed': 190, actions: 230
  } },
  'admin-access': { widths: {
    capability: 310, 'required-tier': 170, 'default-tier': 120, 'policy-status': 130
  } },
  'call-matching-duplicates': {
    widths: { time: 160, talkgroup: 165, radio: 125, site: 160, copies: 82,
      match: 185, outputs: 130, action: 115 },
    grow: ['talkgroup', 'site', 'match']
  },
  'radioreference-sites': {
    widths: { site: 340, system: 200, frequencies: 150 }, grow: ['site'], stretchSaved: true
  },
  'radioreference-talkgroups': { widths: {
    selected: 54, talkgroup: 90, 'alpha-tag': 190, description: 260,
    category: 180, status: 150
  }, grow: ['alpha-tag', 'description', 'category'], stretchSaved: true },
  'radioreference-conventional': { widths: {
    frequency: 125, 'alpha-tag': 220, description: 260, mode: 120
  }, grow: ['alpha-tag', 'description'], stretchSaved: true }
});

function defaultsFor(tableType) {
  return TABLE_DEFAULTS[tableType] || TABLE_DEFAULTS[tableType.split('.')[0]] || {};
}

function layout(tableType, columns) {
  const schema = columns.map((column) => column.id);
  const defaults = defaultsFor(tableType);
  const namedOrder = (defaults.order || []).filter((id) => schema.includes(id));
  const order = [...namedOrder, ...schema.filter((id) => !namedOrder.includes(id))];
  const visible = defaults.visible ? new Set(defaults.visible) : null;
  return {
    schema,
    column_order: order,
    column_widths: {},
    hidden_columns: columns.filter((column) => !column.essential &&
      (visible ? !visible.has(column.id) : (defaults.hidden || []).includes(column.id)))
      .map((column) => column.id),
    collapsed_groups: []
  };
}

function width(tableType, column, variant = 'normal') {
  const specified = defaultsFor(tableType).widths?.[column.id];
  const tableWidth = typeof specified === 'object' ? specified[variant] ?? specified.normal : specified;
  if (Number.isFinite(tableWidth)) return tableWidth;
  const semanticWidth = COLUMN_WIDTHS[column.id];
  if (semanticWidth) return semanticWidth;
  if (String(column.className || '').includes('alias-cell')) return 190;
  if (String(column.className || '').includes('numeric')) return 100;
  return Math.max(90, Math.min(220, String(column.label || '').length * 9 + 34));
}

function fit(tableType) {
  const defaults = defaultsFor(tableType);
  return Array.isArray(defaults.grow) ? {
    grow: defaults.grow,
    maxWidth: defaults.maxWidth || null,
    stretchSaved: defaults.stretchSaved === true
  } : null;
}

function fittedWidths(tableType, columns, widths, savedWidths, availableWidth) {
  const profile = fit(tableType);
  if (!profile) return widths;
  const target = Math.min(Math.max(0, availableWidth), profile.maxWidth || Infinity);
  const extra = Math.max(0, target - widths.reduce((sum, value) => sum + value, 0));
  const configuredGrow = columns.map((column, index) => ({ id: column.id, index }))
    .filter(({ id }) => profile.grow.includes(id));
  const semanticGrow = configuredGrow.filter(({ id }) =>
    profile.stretchSaved || !Object.hasOwn(savedWidths, id));
  // Saved widths are preferred baselines, not a reason for a fitted table to leave
  // unused panel space. Prefer untouched semantic grow columns, then the table's
  // configured variable-content columns. Only fall back to all visible columns
  // when every configured grow column is hidden.
  const grow = semanticGrow.length ? semanticGrow : (configuredGrow.length ? configuredGrow :
    columns.map((column, index) => ({ id: column.id, index })));
  if (!extra || !grow.length) return widths;
  const growTotal = grow.reduce((sum, { index }) => sum + widths[index], 0);
  let distributed = 0;
  const fitted = widths.slice();
  grow.forEach(({ index }, position) => {
    const share = position === grow.length - 1 ? extra - distributed :
      Math.round(extra * widths[index] / growTotal);
    fitted[index] += share;
    distributed += share;
  });
  return fitted;
}

export { layout, width, fit, fittedWidths };
