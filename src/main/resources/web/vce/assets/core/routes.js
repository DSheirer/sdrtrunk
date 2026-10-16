'use strict';

  const definitions = Object.freeze([
    { id: 'dashboard', label: 'Main', title: 'Main', parent: 'dashboard', capability: 'dashboard', databaseNotice: false },
    { id: 'live', label: 'Live', title: 'Live', parent: 'live', capability: 'live', databaseNotice: false },
    { id: 'recordings', label: 'Recordings', title: 'Recordings', parent: 'recordings', capability: 'recordings', databaseNotice: false },
    { id: 'map', label: 'Map', title: 'Map', parent: 'map', capability: 'call-audio', databaseNotice: false },
    { id: 'network-visualizer', label: 'P25 Visualizer', title: 'P25 Visualizer', parent: 'network-visualizer', capability: 'radio', databaseNotice: false },
    { id: 'scanner', label: 'Scanner', title: 'Scanner', parent: 'scanner', capability: 'call-audio', databaseNotice: false },
    { id: 'tuner-spectrum', label: 'Browser Spectrum', title: 'Browse Spectrum', parent: 'tuner-spectrum', access: 'admin-tuner', databaseNotice: false },
    { id: 'radio-system', label: 'Radio System Details', title: 'Radio System Details', parent: 'dashboard', capability: 'radio', databaseNotice: true },
    { id: 'group-identity', label: 'Group Identity Details', title: 'Group Identity Details', parent: 'dashboard', capability: 'radio', databaseNotice: true },
    { id: 'radio', label: 'Radio Details', title: 'Radio Details', parent: 'dashboard', capability: 'radio', databaseNotice: true },
    { id: 'channel-setup', label: 'Channels', title: 'Channels', parent: 'channel-setup', access: 'admin-channels', databaseNotice: false },
    { id: 'channel', label: 'Channel Details', title: 'Channel Details', parent: 'dashboard', capability: 'radio', databaseNotice: true },
    { id: 'aliases', label: 'Aliases', title: 'Aliases', parent: 'aliases', access: 'admin-aliases', databaseNotice: true },
    { id: 'scan-lists', label: 'Scan Lists', title: 'Scan Lists', parent: 'scan-lists', access: 'admin-configuration', databaseNotice: false },
    { id: 'radioreference', label: 'RadioReference', title: 'RadioReference', parent: 'radioreference', access: 'admin-radioreference', databaseNotice: false },
    { id: 'streaming', label: 'Streaming', title: 'Streaming', parent: 'streaming', access: 'admin-streaming', databaseNotice: false },
    { id: 'tuners', label: 'Tuners', title: 'Tuners', parent: 'tuners', access: 'admin-tuner', databaseNotice: false },
    { id: 'admin', label: 'Administration', title: 'Administration', parent: 'admin', access: 'admin', databaseNotice: false },
    { id: 'settings', label: 'My Settings', title: 'My Settings', parent: null, capability: 'user-settings', databaseNotice: false },
    { id: 'credits', label: 'About', title: 'About', parent: 'credits', capability: 'credits', databaseNotice: false }
  ].map((definition) => Object.freeze({ ...definition })));

  function createRegistry(handlers, isAllowed) {
    if (!handlers || typeof handlers !== 'object' || Array.isArray(handlers)) {
      throw new TypeError('Route handlers are required.');
    }
    if (typeof isAllowed !== 'function') throw new TypeError('A route access function is required.');
    const expected = new Set(definitions.map(({ id }) => id));
    Object.keys(handlers).forEach((id) => {
      if (!expected.has(id)) throw new Error(`Unknown route handler: ${id}`);
    });
    const entries = Object.fromEntries(definitions.map((definition) => {
      const handler = handlers[definition.id];
      if (typeof handler !== 'function') throw new Error(`Missing route handler: ${definition.id}`);
      return [definition.id, Object.freeze({
        ...definition,
        handler,
        allowed: () => isAllowed(definition)
      })];
    }));
    return Object.freeze(entries);
  }

  function requestedView(search) {
    const parameters = search instanceof URLSearchParams ? search : new URLSearchParams(search || '');
    return parameters.get('view') || 'dashboard';
  }

  function resolve(registry, search) {
    return registry?.[requestedView(search)] || null;
  }

  function localTarget(location, value) {
    const target = value instanceof URL ? value : new URL(String(value || ''), location.href);
    if (target.origin !== location.origin || target.pathname !== '/') return null;
    return target;
  }

  function navigate(windowValue, value, onNavigate, options = {}) {
    const target = localTarget(windowValue.location, value);
    if (!target) return false;
    const relative = `${target.pathname}${target.search}${target.hash}`;
    if (options.replace === true) windowValue.history.replaceState({}, '', relative);
    else windowValue.history.pushState({}, '', relative);
    onNavigate?.(new URLSearchParams(target.search));
    return true;
  }

export { definitions, createRegistry, requestedView, resolve, localTarget, navigate };
