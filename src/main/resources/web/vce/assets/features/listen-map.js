'use strict';

import { systemLabel, systemName, systemIdentity } from '../core/system-labels.js?v=1';
import { formatP25RadioIdentifier, parseP25RadioIdentifier, p25ServingSystemKey } from '../core/radio-labels.js?v=4';
import { createTableOverflow } from '../core/table-overflow.js?v=1';

const TILE_SIZE = 256;
const TILE_HOST = 'https://tile.openstreetmap.org';
const MIN_ZOOM = 2;
const MAX_ZOOM = 17;
const MAX_ENTITIES = 256;
const MAX_TRAIL_POINTS = 10;
const ICON_SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

function clamp(value, minimum, maximum) {
  return Math.max(minimum, Math.min(maximum, value));
}

function wrap(value, limit) {
  return ((value % limit) + limit) % limit;
}

function wrappedOffset(pointX, centerX, worldWidth) {
  return wrap(pointX - centerX + worldWidth / 2, worldWidth) - worldWidth / 2;
}

function project(latitude, longitude, zoom) {
  const lat = clamp(Number(latitude), -85.05112878, 85.05112878);
  const lon = Number(longitude);
  const scale = TILE_SIZE * 2 ** zoom;
  const sine = Math.sin(lat * Math.PI / 180);
  return {
    x: (lon + 180) / 360 * scale,
    y: (0.5 - Math.log((1 + sine) / (1 - sine)) / (4 * Math.PI)) * scale
  };
}

function unproject(x, y, zoom) {
  const scale = TILE_SIZE * 2 ** zoom;
  return {
    latitude: Math.atan(Math.sinh(Math.PI * (1 - 2 * y / scale))) * 180 / Math.PI,
    longitude: wrap(x / scale * 360, 360) - 180
  };
}

function visibleTiles(center, zoom, width, height) {
  const worldTiles = 2 ** zoom;
  const left = center.x - width / 2;
  const top = center.y - height / 2;
  const firstX = Math.floor(left / TILE_SIZE);
  const lastX = Math.floor((left + width - 1) / TILE_SIZE);
  const firstY = clamp(Math.floor(top / TILE_SIZE), 0, worldTiles - 1);
  const lastY = clamp(Math.floor((top + height - 1) / TILE_SIZE), 0, worldTiles - 1);
  const tiles = [];
  for (let y = firstY; y <= lastY; y += 1) {
    for (let x = firstX; x <= lastX; x += 1) {
      tiles.push({ zoom, worldX: x, x: ((x % worldTiles) + worldTiles) % worldTiles, y,
        left: x * TILE_SIZE - left, top: y * TILE_SIZE - top });
    }
  }
  return tiles;
}

function mapRadioIdentifier(entity, servingSystem) {
  const raw = String(entity.identifier || '');
  const identity = { wacn: entity.home_wacn, system_id: entity.home_system_id, subscriber_id: entity.radio_id };
  if (!formatP25RadioIdentifier(identity)) return raw;
  const parsed = parseP25RadioIdentifier(raw);
  const matchingText = parsed && Object.keys(identity).every((key) =>
    Number(identity[key]) === parsed.canonical_identity[key]);
  const homeKey = `p25:${Number(identity.wacn).toString(16).padStart(5, '0')}:` +
    Number(identity.system_id).toString(16).padStart(3, '0');
  return formatP25RadioIdentifier(identity, {
    servingSystemKey: p25ServingSystemKey(entity) || p25ServingSystemKey(servingSystem) || servingSystem?.key || '',
    homeSystemName: String(entity.home_system_name || '').trim() || systemName(entity.home_system) || systemName(homeKey),
    workingId: entity.observed_working_id ?? (matchingText ? parsed.observed_working_id : null)
  });
}

function normalizeSnapshot(value) {
  if (!value || !Array.isArray(value.entities)) throw new Error('The receiver returned an invalid map snapshot.');
  const entities = value.entities.slice(0, MAX_ENTITIES).flatMap((entity) => {
    const positions = (Array.isArray(entity?.positions) ? entity.positions : [])
      .slice(0, MAX_TRAIL_POINTS).filter((position) =>
        Number.isFinite(Number(position?.latitude)) && Math.abs(Number(position.latitude)) <= 90 &&
        Number.isFinite(Number(position?.longitude)) && Math.abs(Number(position.longitude)) <= 180)
      .map((position) => ({ latitude: Number(position.latitude), longitude: Number(position.longitude),
        timestamp_ms: Number(position.timestamp_ms) || 0 })).reverse();
    if (!positions.length || typeof entity?.id !== 'string' || !entity.id) return [];
    const servingSystem = entity.serving_system || entity.system_identity;
    const rawIdentifier = String(entity.identifier || '');
    const identifier = mapRadioIdentifier(entity, servingSystem);
    const rawLabel = String(entity.label || rawIdentifier || entity.id);
    const label = rawLabel.trim() === rawIdentifier.trim() && identifier ? identifier : rawLabel;
    return [{ id: entity.id, label, identifier, raw_identifier: rawIdentifier,
      alias_list: String(entity.alias_list || ''),
      system: systemLabel(servingSystem) || systemLabel(entity),
      system_name: systemName(servingSystem) || systemName(entity),
      serving_system: servingSystem && typeof servingSystem === 'object' ? servingSystem : null,
      home_system_name: String(entity.home_system_name || systemName(entity.home_system)),
      home_system: entity.home_system && typeof entity.home_system === 'object' ? entity.home_system : null,
      icon: ICON_SLUG.test(String(entity.icon || '')) ? entity.icon : 'no-icon',
      color: /^#[0-9a-fA-F]{6}$/.test(String(entity.color || '')) ? entity.color : null,
      heading: Number(entity.heading), speed_kph: Number(entity.speed_kph), positions }];
  });
  return { entities, generated_at_ms: Number(value.generated_at_ms) || 0,
    dropped_observations: Number(value.dropped_observations) || 0,
    evicted_entities: Number(value.evicted_entities) || 0 };
}

function mapSystemFacts(entity, radioSystemLink = (_reference, label) => label) {
  const reference = (system) => system?.entity_ref || system?.radio_system_entity_ref;
  const nativeIdentity = (system) => {
    const key = system?.radio_system_key || system?.system_key || system?.key || '';
    return /^(?:dmr|nxdn-[cd]):channel:/i.test(key) ? '' : systemIdentity(system);
  };
  const servingIdentity = entity.system_name ? nativeIdentity(entity.serving_system) : '';
  const homeIdentity = entity.home_system_name ? nativeIdentity(entity.home_system) : '';
  return [
    ['System', radioSystemLink(reference(entity.serving_system), entity.system || '(no system name)')],
    ...(servingIdentity ? [['Radio identity', servingIdentity]] : []),
    ...(entity.home_system || entity.home_system_name ?
      [['Home system', radioSystemLink(reference(entity.home_system),
        entity.home_system_name || systemLabel(entity.home_system))]] : []),
    ...(homeIdentity ? [['Home radio identity', homeIdentity]] : [])
  ];
}

function positionSignature(position) {
  return position ? `${position.timestamp_ms}:${position.latitude}:${position.longitude}` : '';
}

function positionsAfterCutoff(positions, cutoffSignature) {
  if (!cutoffSignature) return positions;
  const index = positions.findIndex((position) => positionSignature(position) === cutoffSignature);
  return index < 0 ? positions : positions.slice(index + 1);
}

function localTimestampParts(timestampMs) {
  if (!(timestampMs > 0)) return { date: 'Unknown', time: '' };
  const value = new Date(timestampMs);
  const part = (number) => String(number).padStart(2, '0');
  return {
    date: `${value.getFullYear()}-${part(value.getMonth() + 1)}-${part(value.getDate())}`,
    time: `${part(value.getHours())}:${part(value.getMinutes())}:${part(value.getSeconds())}`
  };
}

function fitPoints(points, width, height) {
  if (!points.length) return null;
  const xs = points.map((point) => wrap((point.longitude + 180) / 360, 1)).sort((a, b) => a - b);
  let largestGap = -1;
  let arcStart = xs[0];
  for (let index = 0; index < xs.length; index += 1) {
    const next = index + 1 < xs.length ? xs[index + 1] : xs[0] + 1;
    const gap = next - xs[index];
    if (gap > largestGap) {
      largestGap = gap;
      arcStart = wrap(next, 1);
    }
  }
  const longitudeSpan = 1 - largestGap;
  const longitudeCenter = wrap(arcStart + longitudeSpan / 2, 1);
  const ys = points.map((point) => project(point.latitude, 0, 0).y);
  const yMinimum = Math.min(...ys);
  const yMaximum = Math.max(...ys);
  for (let zoom = 14; zoom >= MIN_ZOOM; zoom -= 1) {
    const worldWidth = TILE_SIZE * 2 ** zoom;
    if ((longitudeSpan * worldWidth <= Math.max(1, width - 96) &&
        (yMaximum - yMinimum) * 2 ** zoom <= Math.max(1, height - 96)) || zoom === MIN_ZOOM) {
      return { zoom, center: { x: longitudeCenter * worldWidth,
        y: (yMinimum + yMaximum) / 2 * 2 ** zoom } };
    }
  }
  return null;
}

function createListenMap({ node, iconGlyph, iconButton, fetchSnapshot, iconUrl,
  radioSystemLink = (_reference, label) => label }) {
  const root = node('div', 'listen-map-workspace data-workspace');
  const toolbar = node('div', 'listen-map-toolbar ui-toolbar');
  const status = node('span', 'listen-map-status', 'Loading locations…');
  status.setAttribute('role', 'status');
  const refresh = node('button', 'ui-button ui-button-secondary', 'Refresh');
  refresh.type = 'button';
  refresh.prepend(iconGlyph('icon-refresh'));
  const fit = node('button', 'ui-button ui-button-secondary', 'Show all');
  fit.type = 'button';
  fit.disabled = true;
  toolbar.append(status, refresh, fit);

  const layout = node('div', 'listen-map-layout');
  const mapPanel = node('section', 'section ui-section listen-map-panel');
  const mapTitle = node('div', 'section-title ui-section-title');
  mapTitle.append(node('span', '', 'Map'));
  const mapActions = node('div', 'ui-section-actions listen-map-map-actions');
  const trailControl = node('label', 'listen-map-trail-control');
  trailControl.append(node('span', '', 'Trail length'));
  const trailSelect = node('select', 'ui-select listen-map-trail-select');
  trailSelect.setAttribute('aria-label', 'Trail length');
  for (let length = 1; length <= MAX_TRAIL_POINTS; length += 1) {
    const option = node('option', '', String(length));
    option.value = String(length);
    option.selected = length === 3;
    trailSelect.append(option);
  }
  trailControl.append(trailSelect);
  const clearMap = node('button', 'ui-button ui-button-secondary', 'Clear map');
  clearMap.type = 'button';
  clearMap.title = 'Hide tracks until selected, replotted, or updated';
  const replotAll = node('button', 'ui-button ui-button-secondary', 'Replot all');
  replotAll.type = 'button';
  replotAll.title = 'Show all tracks in this browser';
  mapActions.append(trailControl, clearMap, replotAll);
  mapTitle.append(mapActions);
  mapPanel.append(mapTitle);

  const viewport = node('div', 'listen-map-viewport');
  viewport.tabIndex = 0;
  viewport.setAttribute('role', 'region');
  viewport.setAttribute('aria-label', 'Location map. Drag to pan; use the zoom buttons to change scale.');
  const tilesHost = node('div', 'listen-map-tiles');
  tilesHost.setAttribute('aria-hidden', 'true');
  const trails = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  trails.classList.add('listen-map-trails');
  trails.setAttribute('aria-hidden', 'true');
  const markers = node('div', 'listen-map-markers');
  const zoomControls = node('div', 'listen-map-zoom ui-action-row');
  const zoomIn = node('button', 'ui-button ui-button-secondary ui-icon-button');
  zoomIn.type = 'button';
  zoomIn.setAttribute('aria-label', 'Zoom in');
  zoomIn.append(iconGlyph('icon-plus'));
  const zoomOut = node('button', 'ui-button ui-button-secondary ui-icon-button');
  zoomOut.type = 'button';
  zoomOut.setAttribute('aria-label', 'Zoom out');
  zoomOut.append(iconGlyph('icon-zoom-out'));
  zoomControls.append(zoomIn, zoomOut);
  const attribution = node('div', 'listen-map-attribution');
  const attributionLink = node('a', '', '© OpenStreetMap contributors');
  attributionLink.href = 'https://www.openstreetmap.org/copyright';
  attributionLink.target = '_blank';
  attributionLink.rel = 'noopener noreferrer';
  attribution.append(attributionLink);
  viewport.append(tilesHost, trails, markers, zoomControls, attribution);
  mapPanel.append(viewport);

  const sidebar = node('section', 'section ui-section listen-map-sidebar');
  const sidebarTitle = node('div', 'section-title ui-section-title');
  sidebarTitle.append(node('span', '', 'Tracks'));
  const removeAll = node('button', 'ui-button ui-button-danger-quiet', 'Remove all');
  removeAll.type = 'button';
  removeAll.title = 'Remove tracks until their next location update';
  sidebarTitle.append(removeAll);
  const catalogControls = node('div', 'listen-map-catalog-controls');
  const centerToggle = node('label', 'listen-map-center-toggle');
  centerToggle.append(node('span', '', 'Center on selection'));
  const centerSwitch = node('span', 'ui-toggle');
  const centerInput = node('input');
  centerInput.type = 'checkbox';
  centerInput.checked = true;
  centerInput.setAttribute('aria-label', 'Center on selection');
  const centerTrack = node('span', 'ui-toggle-track');
  centerTrack.append(node('span', 'ui-toggle-thumb'));
  const centerState = node('span', 'ui-toggle-state', 'On');
  centerSwitch.append(centerInput, centerTrack, centerState);
  centerToggle.append(centerSwitch);
  const followStatus = node('span', 'listen-map-follow-status');
  followStatus.hidden = true;
  catalogControls.append(centerToggle, followStatus);
  const list = node('div', 'listen-map-list');
  const selectedActions = node('div', 'listen-map-selected-actions ui-action-row');
  const follow = node('button', 'ui-button ui-button-primary', 'Follow');
  follow.type = 'button';
  const centerSelected = node('button', 'ui-button ui-button-secondary', 'Center');
  centerSelected.type = 'button';
  const removeSelected = node('button', 'ui-button ui-button-danger-quiet', 'Remove');
  removeSelected.type = 'button';
  removeSelected.title = 'Remove this track until its next location update';
  selectedActions.append(follow, centerSelected, removeSelected);
  const details = node('div', 'listen-map-details');
  sidebar.append(sidebarTitle, catalogControls, list, selectedActions, details);
  layout.append(mapPanel, sidebar);
  root.append(toolbar, layout);

  let snapshot = { entities: [], generated_at_ms: 0, dropped_observations: 0, evicted_entities: 0 };
  let selectedId = '';
  let followingId = '';
  let followedSignature = '';
  let trailLength = 3;
  let center = project(39.5, -98.35, 4);
  let zoom = 4;
  let initialized = false;
  let closed = false;
  let inFlight = false;
  let frame = 0;
  let drag = null;
  let historyOverflow = null;
  const tileNodes = new Map();
  const deletedCutoffs = new Map();
  const historyCutoffs = new Map();
  const plotCutoffs = new Map();

  const dimensions = () => ({ width: Math.max(1, viewport.clientWidth), height: Math.max(1, viewport.clientHeight) });
  const screenPoint = (position) => {
    const point = project(position.latitude, position.longitude, zoom);
    const world = TILE_SIZE * 2 ** zoom;
    const offsetX = wrappedOffset(point.x, center.x, world);
    const { width, height } = dimensions();
    return { x: width / 2 + offsetX, y: height / 2 + point.y - center.y };
  };
  const latestSignature = (entity) => positionSignature(entity?.positions.at(-1));

  function catalogEntities() {
    return snapshot.entities.flatMap((entity) => {
      if (deletedCutoffs.has(entity.id)) return [];
      const positions = positionsAfterCutoff(entity.positions, historyCutoffs.get(entity.id));
      return positions.length ? [{ ...entity, positions }] : [];
    }).sort((left, right) => left.label.localeCompare(right.label));
  }

  function plottedEntities() {
    return catalogEntities().filter((entity) => !plotCutoffs.has(entity.id));
  }

  function selectedEntity() {
    return catalogEntities().find((entity) => entity.id === selectedId);
  }

  function centerOn(position) {
    if (!position) return;
    center = project(position.latitude, position.longitude, zoom);
    scheduleDraw();
  }

  function draw() {
    frame = 0;
    if (closed || !viewport.isConnected) return;
    const { width, height } = dimensions();
    const current = new Set();
    for (const tile of visibleTiles(center, zoom, width, height)) {
      const key = `${tile.zoom}/${tile.worldX}/${tile.y}`;
      current.add(key);
      let image = tileNodes.get(key);
      if (!image) {
        image = document.createElement('img');
        image.alt = '';
        image.draggable = false;
        image.decoding = 'async';
        image.referrerPolicy = 'origin';
        image.src = `${TILE_HOST}/${tile.zoom}/${tile.x}/${tile.y}.png`;
        tileNodes.set(key, image);
        tilesHost.append(image);
      }
      image.style.left = `${tile.left}px`;
      image.style.top = `${tile.top}px`;
    }
    for (const [key, image] of tileNodes) {
      if (!current.has(key)) { image.remove(); tileNodes.delete(key); }
    }

    trails.setAttribute('viewBox', `0 0 ${width} ${height}`);
    trails.replaceChildren();
    const focusedMarkerId = markers.contains(document.activeElement) ? document.activeElement.dataset.entityId : null;
    markers.replaceChildren();
    for (const entity of plottedEntities()) {
      const points = entity.positions.slice(-trailLength).map(screenPoint);
      if (points.length > 1) {
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', points.map((point, index) => `${index ? 'L' : 'M'}${point.x} ${point.y}`).join(' '));
        path.setAttribute('stroke', entity.color || 'var(--accent)');
        path.classList.add(entity.id === selectedId ? 'listen-map-trail-selected' : 'listen-map-trail');
        trails.append(path);
      }
      const point = points.at(-1);
      if (!point || point.x < -32 || point.y < -32 || point.x > width + 32 || point.y > height + 32) continue;
      const marker = node('button', `listen-map-marker ui-button ${entity.id === selectedId ?
        'ui-button-primary' : 'ui-button-secondary'}`);
      marker.type = 'button';
      marker.setAttribute('aria-label', `Select ${entity.label}`);
      marker.setAttribute('aria-pressed', String(entity.id === selectedId));
      marker.dataset.entityId = entity.id;
      marker.title = entity.label;
      marker.style.left = `${point.x}px`;
      marker.style.top = `${point.y}px`;
      const icon = document.createElement('img');
      icon.alt = '';
      icon.loading = 'lazy';
      icon.src = iconUrl(entity.icon);
      marker.append(icon);
      marker.addEventListener('pointerdown', (event) => event.stopPropagation());
      marker.addEventListener('click', () => select(entity.id));
      markers.append(marker);
    }
    if (focusedMarkerId) [...markers.querySelectorAll('button')]
      .find((marker) => marker.dataset.entityId === focusedMarkerId)?.focus({ preventScroll: true });
    zoomIn.disabled = zoom >= MAX_ZOOM;
    zoomOut.disabled = zoom <= MIN_ZOOM;
  }

  function scheduleDraw() {
    if (!frame && !closed) frame = window.requestAnimationFrame(draw);
  }

  function setFollowState(id = '') {
    followingId = id;
    followedSignature = id ? latestSignature(catalogEntities().find((entity) => entity.id === id)) : '';
    centerInput.disabled = Boolean(id);
    follow.textContent = id ? 'Unfollow' : 'Follow';
    const entity = catalogEntities().find((candidate) => candidate.id === id);
    followStatus.hidden = !entity;
    followStatus.textContent = entity ? `Following: ${entity.label}` : '';
    if (entity) centerOn(entity.positions.at(-1));
  }

  function updateControls(entities) {
    const selected = entities.find((entity) => entity.id === selectedId);
    const plotted = plottedEntities();
    const count = entities.length;
    const shown = plotted.length;
    const dropped = snapshot.dropped_observations ? ` · ${snapshot.dropped_observations} dropped` : '';
    const nextStatus = `${count} track${count === 1 ? '' : 's'} · ${shown} shown${dropped}`;
    if (status.textContent !== nextStatus) status.textContent = nextStatus;
    fit.disabled = !shown;
    clearMap.disabled = !shown;
    replotAll.disabled = !count || shown === count;
    removeAll.disabled = !count;
    follow.disabled = !selected && !followingId;
    centerSelected.disabled = !selected;
    removeSelected.disabled = !selected;
    centerState.textContent = centerInput.checked ? 'On' : 'Off';
    follow.textContent = followingId ? 'Unfollow' : 'Follow';
    const followed = entities.find((entity) => entity.id === followingId);
    followStatus.hidden = !followed;
    followStatus.textContent = followed ? `Following: ${followed.label}` : '';
  }

  function historyTable(entity, actionsHost) {
    const wrapper = node('div', 'ui-table-wrap listen-map-history-wrap');
    wrapper.dataset.entityId = entity.id;
    const table = node('table', 'ui-data-table ui-data-table-quiet listen-map-history');
    const head = node('thead');
    const headingRow = node('tr');
    for (const label of ['Time', 'Latitude', 'Longitude']) headingRow.append(node('th', '', label));
    head.append(headingRow);
    const body = node('tbody');
    for (const position of entity.positions.slice().reverse()) {
      const row = node('tr', 'listen-map-history-item');
      row.dataset.positionSignature = positionSignature(position);
      const timeCell = node('td');
      const timestamp = localTimestampParts(position.timestamp_ms);
      const choose = node('button', 'listen-map-history-center');
      choose.type = 'button';
      choose.append(node('span', '', timestamp.date));
      if (timestamp.time) choose.append(node('span', '', timestamp.time));
      choose.setAttribute('aria-label', `Center history position from ${timestamp.date} ${timestamp.time}`.trim());
      const centerHistory = () => {
        if (centerInput.checked && !followingId) centerOn(position);
      };
      choose.addEventListener('click', centerHistory);
      row.addEventListener('click', (event) => {
        if (event.target !== choose) centerHistory();
      });
      timeCell.append(choose);
      row.append(timeCell, node('td', 'numeric', position.latitude.toFixed(5)),
        node('td', 'numeric', position.longitude.toFixed(5)));
      body.append(row);
    }
    table.append(head, body);
    wrapper.append(table);
    historyOverflow = createTableOverflow({ wrapper, table, actionsHost, iconButton,
      label: 'Position history' });
    return wrapper;
  }

  function renderSelection() {
    const entities = catalogEntities();
    if (!entities.some((entity) => entity.id === selectedId)) selectedId = entities[0]?.id || '';
    if (followingId && !entities.some((entity) => entity.id === followingId)) setFollowState();
    const focusedId = list.contains(document.activeElement) ? document.activeElement.dataset.entityId : null;
    const focusedHistorySignature = details.contains(document.activeElement) ?
      document.activeElement.closest('.listen-map-history-item')?.dataset.positionSignature : null;
    const focusedHistoryScroll = [...details.querySelectorAll('.ui-table-overflow-controls button')]
      .indexOf(document.activeElement);
    const scrollTop = list.scrollTop;
    list.replaceChildren();
    if (!entities.length) list.append(node('div', 'empty', 'No mapped locations yet.'));
    for (const entity of entities) {
      const button = node('button', `listen-map-list-item ui-button ${entity.id === selectedId ?
        'ui-button-primary' : 'ui-button-secondary'}`);
      button.type = 'button';
      button.setAttribute('aria-pressed', String(entity.id === selectedId));
      button.dataset.entityId = entity.id;
      const icon = document.createElement('img');
      icon.alt = '';
      icon.loading = 'lazy';
      icon.src = iconUrl(entity.icon);
      const copy = node('span', 'listen-map-list-copy');
      const context = [entity.identifier, entity.alias_list].filter(Boolean).join(' · ');
      copy.append(node('strong', '', entity.label),
        node('small', '', context || 'Unknown'));
      button.append(icon, copy);
      button.addEventListener('click', () => select(entity.id, true));
      list.append(button);
    }
    list.scrollTop = scrollTop;
    if (focusedId) [...list.querySelectorAll('button')]
      .find((button) => button.dataset.entityId === focusedId)?.focus({ preventScroll: true });

    const previousHistory = details.querySelector('.listen-map-history-wrap');
    const historyPosition = previousHistory ? { entityId: previousHistory.dataset.entityId,
      left: previousHistory.scrollLeft, top: previousHistory.scrollTop } : null;
    historyOverflow?.destroy();
    historyOverflow = null;
    details.replaceChildren();
    const selected = entities.find((entity) => entity.id === selectedId);
    if (selected) {
      const latest = selected.positions.at(-1);
      const facts = node('dl', 'ui-facts listen-map-facts');
      const identifier = node('span', '', selected.identifier || '—');
      if (selected.raw_identifier !== selected.identifier) identifier.title = selected.raw_identifier;
      for (const [label, value] of [
        ...mapSystemFacts(selected, radioSystemLink),
        ['Identifier', identifier],
        ['Alias list', selected.alias_list || '—'],
        ['Position', `${latest.latitude.toFixed(5)}, ${latest.longitude.toFixed(5)}`],
        ['Heading', Number.isFinite(selected.heading) ? `${Math.round(selected.heading)}°` : '—'],
        ['Speed', Number.isFinite(selected.speed_kph) ? `${Math.round(selected.speed_kph)} km/h` : '—'],
        ['Last seen', latest.timestamp_ms > 0 ? new Date(latest.timestamp_ms).toLocaleString() : '—']
      ]) {
        const fact = node('div', 'ui-fact');
        const description = node('dd');
        description.append(value);
        fact.append(node('dt', '', label), description);
        facts.append(fact);
      }
      const historyHeader = node('div', 'listen-map-history-heading');
      const historyActions = node('div', 'ui-section-actions');
      historyActions.append(node('span', '', String(selected.positions.length)));
      historyHeader.append(node('h4', '', 'History'), historyActions);
      const history = historyTable(selected, historyActions);
      details.append(node('h3', '', selected.label), facts, historyHeader, history);
      if (historyPosition?.entityId === selected.id) {
        history.scrollLeft = historyPosition.left;
        history.scrollTop = historyPosition.top;
      }
      historyOverflow.refresh();
      if (historyPosition?.entityId === selected.id && focusedHistoryScroll >= 0) {
        const scrollButtons = [...historyActions.querySelectorAll('.ui-table-overflow-controls button')];
        const focusTarget = !scrollButtons[focusedHistoryScroll]?.disabled ? scrollButtons[focusedHistoryScroll] :
          scrollButtons.find((button) => !button.disabled) || (history.tabIndex === 0 ? history : null);
        focusTarget?.focus({ preventScroll: true });
      }
    }
    if (focusedHistorySignature) {
      [...details.querySelectorAll('.listen-map-history-item')]
        .find((row) => row.dataset.positionSignature === focusedHistorySignature)
        ?.querySelector('.listen-map-history-center')?.focus({ preventScroll: true });
    }
    updateControls(entities);
  }

  function select(id, centerOnEntity = false) {
    selectedId = id;
    plotCutoffs.delete(id);
    const selected = catalogEntities().find((entity) => entity.id === id);
    if (centerOnEntity && centerInput.checked && !followingId) centerOn(selected?.positions.at(-1));
    renderSelection();
    scheduleDraw();
  }

  function fitAll() {
    const points = plottedEntities().map((entity) => entity.positions.at(-1)).filter(Boolean);
    const { width, height } = dimensions();
    const fitted = fitPoints(points, width, height);
    if (!fitted) return;
    ({ zoom, center } = fitted);
    scheduleDraw();
  }

  function removeEntity(id) {
    const entity = catalogEntities().find((candidate) => candidate.id === id);
    if (!entity) return;
    deletedCutoffs.set(id, latestSignature(entity));
    historyCutoffs.delete(id);
    plotCutoffs.delete(id);
    if (followingId === id) setFollowState();
  }

  function reconcileSnapshot(next) {
    const incomingIds = new Set(next.entities.map((entity) => entity.id));
    for (const state of [deletedCutoffs, historyCutoffs, plotCutoffs]) {
      for (const id of state.keys()) {
        if (!incomingIds.has(id)) state.delete(id);
      }
    }
    for (const entity of next.entities) {
      const signature = latestSignature(entity);
      const deleted = deletedCutoffs.get(entity.id);
      if (deleted && deleted !== signature) {
        deletedCutoffs.delete(entity.id);
        historyCutoffs.set(entity.id, deleted);
      }
      const cleared = plotCutoffs.get(entity.id);
      if (cleared && cleared !== signature) plotCutoffs.delete(entity.id);
      const cutoff = historyCutoffs.get(entity.id);
      if (cutoff && !entity.positions.some((position) => positionSignature(position) === cutoff)) {
        historyCutoffs.delete(entity.id);
      }
    }
    snapshot = next;
    const followed = catalogEntities().find((entity) => entity.id === followingId);
    if (followingId && !followed) setFollowState();
    else if (followed) {
      const signature = latestSignature(followed);
      if (signature !== followedSignature) {
        followedSignature = signature;
        centerOn(followed.positions.at(-1));
      }
    }
  }

  async function poll() {
    if (closed || inFlight || document.hidden) return;
    inFlight = true;
    refresh.disabled = true;
    try {
      const next = normalizeSnapshot(await fetchSnapshot());
      if (closed) return;
      reconcileSnapshot(next);
      const entities = catalogEntities();
      if (!initialized && entities.length) {
        initialized = true;
        const latest = entities[0].positions.at(-1);
        zoom = 12;
        center = project(latest.latitude, latest.longitude, zoom);
      }
      renderSelection();
      scheduleDraw();
    } catch (error) {
      if (!closed) status.textContent = error.message || 'Map locations are unavailable.';
    } finally {
      inFlight = false;
      if (!closed) refresh.disabled = false;
    }
  }

  function changeZoom(delta) {
    const next = clamp(zoom + delta, MIN_ZOOM, MAX_ZOOM);
    if (next === zoom) return;
    const geo = unproject(center.x, center.y, zoom);
    zoom = next;
    center = project(geo.latitude, geo.longitude, zoom);
    scheduleDraw();
  }

  centerInput.addEventListener('change', () => { centerState.textContent = centerInput.checked ? 'On' : 'Off'; });
  trailSelect.addEventListener('change', () => {
    trailLength = clamp(Number(trailSelect.value) || 3, 1, MAX_TRAIL_POINTS);
    scheduleDraw();
  });
  refresh.addEventListener('click', () => void poll());
  fit.addEventListener('click', fitAll);
  clearMap.addEventListener('click', () => {
    for (const entity of catalogEntities()) plotCutoffs.set(entity.id, latestSignature(entity));
    renderSelection();
    scheduleDraw();
  });
  replotAll.addEventListener('click', () => {
    plotCutoffs.clear();
    renderSelection();
    scheduleDraw();
  });
  removeAll.addEventListener('click', () => {
    for (const entity of catalogEntities()) removeEntity(entity.id);
    selectedId = '';
    renderSelection();
    scheduleDraw();
  });
  follow.addEventListener('click', () => {
    if (followingId) setFollowState();
    else if (selectedId) setFollowState(selectedId);
    renderSelection();
  });
  centerSelected.addEventListener('click', () => centerOn(selectedEntity()?.positions.at(-1)));
  removeSelected.addEventListener('click', () => {
    removeEntity(selectedId);
    selectedId = '';
    renderSelection();
    scheduleDraw();
  });
  zoomIn.addEventListener('click', () => changeZoom(1));
  zoomOut.addEventListener('click', () => changeZoom(-1));
  viewport.addEventListener('keydown', (event) => {
    const movements = { ArrowLeft: [80, 0], ArrowRight: [-80, 0], ArrowUp: [0, 80], ArrowDown: [0, -80] };
    if (event.key === '+' || event.key === '=') { changeZoom(1); event.preventDefault(); }
    else if (event.key === '-') { changeZoom(-1); event.preventDefault(); }
    else if (movements[event.key]) {
      center.x = wrap(center.x - movements[event.key][0], TILE_SIZE * 2 ** zoom);
      center.y = clamp(center.y - movements[event.key][1], 0, TILE_SIZE * 2 ** zoom);
      scheduleDraw();
      event.preventDefault();
    }
  });
  viewport.addEventListener('pointerdown', (event) => {
    if (event.button !== 0 || event.target.closest('button, a')) return;
    drag = { x: event.clientX, y: event.clientY, center: { ...center } };
    viewport.setPointerCapture(event.pointerId);
  });
  viewport.addEventListener('pointermove', (event) => {
    if (!drag) return;
    center = { x: wrap(drag.center.x - (event.clientX - drag.x), TILE_SIZE * 2 ** zoom),
      y: clamp(drag.center.y - (event.clientY - drag.y), 0, TILE_SIZE * 2 ** zoom) };
    scheduleDraw();
  });
  const endDrag = () => { drag = null; };
  viewport.addEventListener('pointerup', endDrag);
  viewport.addEventListener('pointercancel', endDrag);
  const resize = typeof ResizeObserver === 'function' ? new ResizeObserver(scheduleDraw) : null;
  resize?.observe(viewport);
  if (!resize) window.addEventListener('resize', scheduleDraw);
  const onVisibility = () => { if (!document.hidden) void poll(); };
  document.addEventListener('visibilitychange', onVisibility);
  const timer = window.setInterval(() => void poll(), 5000);
  window.requestAnimationFrame(() => { scheduleDraw(); void poll(); });

  return { element: root, close() {
    closed = true;
    historyOverflow?.destroy();
    historyOverflow = null;
    window.clearInterval(timer);
    if (frame) window.cancelAnimationFrame(frame);
    resize?.disconnect();
    if (!resize) window.removeEventListener('resize', scheduleDraw);
    document.removeEventListener('visibilitychange', onVisibility);
    tileNodes.clear();
  } };
}

export { project, unproject, visibleTiles, normalizeSnapshot, fitPoints, wrappedOffset,
  positionSignature, positionsAfterCutoff, mapSystemFacts, createListenMap };
