function integer(value, minimum, maximum) {
  if ((typeof value !== 'number' && typeof value !== 'string') ||
      (typeof value === 'string' && !value.trim())) return null;
  const number = Number(value);
  return Number.isInteger(number) && number >= minimum && number <= maximum ? number : null;
}

function text(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function p25ServingSystemKey(row, prefix = '') {
  const explicit = text(row?.radio_system_key) || text(row?.system_key) ||
    text(row?.playback_target?.radio_system_key);
  if (explicit) return explicit;
  const reference = prefix ? row?.[`${prefix}_entity_ref`] : row?.entity_ref;
  return text(reference?.kind).toLowerCase() === 'radio' ? text(reference.radio_system_key) : '';
}

function parseP25RadioIdentifier(value) {
  const match = /^([0-9a-f]{5})\.([0-9a-f]{3})\.([0-9]+)(?: \(Working ID ([0-9]+)\))?$/i.exec(text(value));
  if (!match) return null;
  const subscriberId = integer(match[3], 1, 0xFFFFFC);
  const workingId = match[4] === undefined ? null : integer(match[4], 1, 0xFFFFFC);
  if (subscriberId === null || match[4] !== undefined && workingId === null) return null;
  return { canonical_identity: { wacn: parseInt(match[1], 16), system_id: parseInt(match[2], 16),
    subscriber_id: subscriberId }, observed_working_id: workingId };
}

function formatP25RadioIdentifier(identity, { servingSystemKey = '', homeSystemName = '', workingId = null } = {}) {
  const wacn = integer(identity?.wacn, 0, 0xFFFFF);
  const systemId = integer(identity?.system_id, 0, 0xFFF);
  const subscriberId = integer(identity?.subscriber_id, 1, 0xFFFFFC);
  if (wacn === null || systemId === null || subscriberId === null) return '';

  const serving = /^p25:([0-9a-f]{1,5}):([0-9a-f]{1,3})$/i.exec(text(servingSystemKey));
  const local = serving && parseInt(serving[1], 16) === wacn && parseInt(serving[2], 16) === systemId;
  const observedId = integer(workingId, 1, 0xFFFFFC);
  const differentWorkingId = observedId !== null && observedId !== subscriberId;
  const name = text(homeSystemName);
  const fullIdentity = `${wacn.toString(16).toUpperCase().padStart(5, '0')}.` +
    `${systemId.toString(16).toUpperCase().padStart(3, '0')}.${subscriberId}`;
  const label = local && !differentWorkingId ? String(subscriberId) :
    name ? `${name} · ${subscriberId}` : fullIdentity;
  return differentWorkingId ? `${label} (Working ID ${observedId})` : label;
}

export { formatP25RadioIdentifier, p25ServingSystemKey, parseP25RadioIdentifier };
