'use strict';

  function text(value) {
    return typeof value === 'string' ? value.trim() : '';
  }

  function identityKey(value, kind) {
    const key = text(value);
    const match = /^v1-([grp])-(x|[0-9a-f]{5})-(x|[0-9a-f]{3})-([0-9]+)$/.exec(key);
    const expected = { talkgroup: 'g', patch_group: 'p', radio: 'r' }[kind];
    if (!match || match[1] !== expected) return '';
    const noHome = match[2] === 'x' && match[3] === 'x';
    if ((match[2] === 'x') !== (match[3] === 'x') || (kind === 'patch_group' && noHome)) return '';
    const identifier = Number(match[4]);
    if (!Number.isSafeInteger(identifier) || identifier < 1 || String(identifier) !== match[4]) return '';
    const maximum = kind === 'radio' ? (noHome ? 0xFFFFFF : 0xFFFFFC) :
      (noHome ? 0xFFFFFF : 0xFFFE);
    return identifier <= maximum ? key : '';
  }

  function exactKeys(value, keys) {
    const actual = Object.keys(value).sort();
    const expected = keys.slice().sort();
    return actual.length === expected.length && actual.every((key, index) => key === expected[index]);
  }

  function uuid(value) {
    const key = text(value);
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(key) ? key : '';
  }

  function radioSystemKey(value) {
    if (typeof value !== 'string' || value !== value.trim()) return '';
    if (/^p25:[0-9a-f]{5}:[0-9a-f]{3}$/.test(value)) return value;
    if (/^(?:dmr|nxdn-c|nxdn-d):channel:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(value)) {
      return value;
    }
    const dmr = /^dmr:tier3:(tiny|small|large|huge):(0|[1-9][0-9]*)$/.exec(value);
    if (dmr) {
      const network = Number(dmr[2]);
      const maximum = { tiny: 511, small: 127, large: 15, huge: 3 }[dmr[1]];
      return Number.isSafeInteger(network) && network <= maximum ? value : '';
    }
    const nxdn = /^nxdn-c:(global|regional|local):([1-9][0-9]*)$/.exec(value);
    if (nxdn) {
      const system = Number(nxdn[2]);
      const maximum = { global: 1022, regional: 16382, local: 131070 }[nxdn[1]];
      return Number.isSafeInteger(system) && system <= maximum ? value : '';
    }
    return '';
  }

  function href(reference) {
    if (!reference || typeof reference !== 'object' || Array.isArray(reference)) return null;
    const kind = text(reference.kind);
    const key = kind === 'radio_system' ? radioSystemKey(reference.key) : text(reference.key);
    if (kind === 'radio_system' && exactKeys(reference, ['kind', 'key']) && key) {
      return `/?${new URLSearchParams({ view: 'radio-system', radio_system_key: key })}`;
    }
    if (kind === 'channel' && exactKeys(reference, ['kind', 'key']) && uuid(key)) {
      return `/?${new URLSearchParams({ view: 'channel', configuration_id: key })}`;
    }
    if (['talkgroup', 'patch_group', 'radio'].includes(kind) &&
        exactKeys(reference, ['kind', 'radio_system_key', 'identity_key'])) {
      const systemKey = radioSystemKey(reference.radio_system_key);
      const key = identityKey(reference.identity_key, kind);
      if (!systemKey || !key) return null;
      const view = kind === 'radio' ? 'radio' : 'group-identity';
      const values = { view, radio_system_key: systemKey, identity_key: key };
      return `/?${new URLSearchParams(values)}`;
    }
    return null;
  }

export { href };
