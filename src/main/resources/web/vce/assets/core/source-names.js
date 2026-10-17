/** Choose a source's displayed name without changing its identity or stored aliases. */
export function formatSourceName(configuredAliases, talkerAlias, mode = 'talker_alias') {
  const names = [];
  const seen = new Set();
  const values = Array.isArray(configuredAliases) ? configuredAliases : [configuredAliases];
  values.forEach((value) => {
    const name = String(value ?? '').trim();
    const key = name.toLowerCase();
    if (name && !seen.has(key)) {
      names.push(name);
      seen.add(key);
    }
  });
  const configured = names.join(', ');
  const talker = String(talkerAlias ?? '').trim();
  if (mode === 'source_alias') return configured || talker;
  if (mode === 'both') {
    return [configured, talker && !seen.has(talker.toLowerCase()) ? talker : '']
      .filter(Boolean).join(' · ');
  }
  return talker || configured;
}
