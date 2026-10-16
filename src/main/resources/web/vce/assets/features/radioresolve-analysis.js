'use strict';

const RADIORESOLVE_PLANNER_URL = 'https://radioresolve.com/rf-planner/';
const MAXIMUM_PLANNER_URL_LENGTH = 8192;

function buildRadioResolvePlannerUrl(snapshot) {
  if (!Array.isArray(snapshot?.tuners) || !snapshot.tuners.length) {
    throw new Error('No supported physical tuners are available for external analysis.');
  }
  if (!Array.isArray(snapshot?.frequencies_hz)) {
    throw new Error('Running channel frequencies are unavailable.');
  }
  const url = new URL(RADIORESOLVE_PLANNER_URL);
  for (const tuner of snapshot.tuners) {
    const model = String(tuner?.model || '');
    const rate = Number(tuner?.rate_hz);
    if (!/^[a-z0-9-]{1,40}$/.test(model) || !Number.isSafeInteger(rate) || rate <= 0) {
      throw new Error('A tuner has no supported RF analysis profile.');
    }
    url.searchParams.append('model', model);
    url.searchParams.append('rate', String(rate));
  }
  const frequencies = snapshot.frequencies_hz.map(Number)
    .filter((frequency) => Number.isSafeInteger(frequency) && frequency > 0)
    .map((frequency) => (frequency / 1_000_000).toFixed(6).replace(/0+$/, '').replace(/\.$/, ''));
  url.searchParams.set('version', 'sdrtrunk-vce');
  if (frequencies.length) url.searchParams.set('frequencies', frequencies.join('\n'));
  if (url.href.length > MAXIMUM_PLANNER_URL_LENGTH) {
    throw new Error('Too many running frequencies for an external analysis link.');
  }
  return url.href;
}

export { buildRadioResolvePlannerUrl };
