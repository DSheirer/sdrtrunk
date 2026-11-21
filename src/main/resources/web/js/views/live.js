// Live: a tuner's spectrum.  Call audio is on the Calls page.
import {api} from '../api.js';
import {live} from '../live.js';
import {el, megahertz} from '../format.js';

export function mount(container, toast) {
  let spectrumSub = null;

  const tunerSelect = el('select');
  const spectrumButton = el('button', {type: 'button', text: 'Show spectrum'});
  const canvas = el('canvas', {width: 1024, height: 220, hidden: true});
  const info = el('p', {class: 'note'});

  container.append(
    el('section', {class: 'panel'}, el('h2', {text: 'Spectrum'}),
      el('div', {class: 'controls'}, el('label', {}, 'Tuner ', tunerSelect), spectrumButton), canvas, info));

  api('GET', '/tuners').then((page) => {
    for (const tuner of page.items.filter((t) => t.status === 'ENABLED')) {
      tunerSelect.append(el('option', {value: tuner.key, text: tuner.name}));
    }
    if (!tunerSelect.options.length) {
      tunerSelect.append(el('option', {value: '', text: 'No running tuner'}));
    }
  }).catch((e) => {
    tunerSelect.append(el('option', {value: '', text: 'Tuners unavailable'}));
    info.textContent = e.message;
  });

  spectrumButton.addEventListener('click', async () => {
    if (spectrumSub) {
      spectrumSub.close();
      spectrumSub = null;
      canvas.hidden = true;
      spectrumButton.textContent = 'Show spectrum';
      return;
    }
    if (!tunerSelect.value) {
      toast('No running tuner', true);
      return;
    }
    canvas.hidden = false;
    spectrumButton.textContent = 'Hide spectrum';
    spectrumSub = await live.subscribe('spectrum', {source: 'tuner:' + tunerSelect.value, fftSize: 4096,
      frameRate: 15}, {json: () => {}, binary: (view) => draw(view), error: (m) => toast(m.detail || m.title, true)});
  });

  function draw(view) {
    const center = Number(view.getBigInt64(16, true));
    const rate = view.getUint32(24, true);
    const count = view.getUint32(28, true);
    const minDb = view.getFloat32(32, true);
    const maxDb = view.getFloat32(36, true);
    const g = canvas.getContext('2d');
    g.fillStyle = '#0b0f14';
    g.fillRect(0, 0, canvas.width, canvas.height);
    g.strokeStyle = '#4fd1a5';
    g.beginPath();
    for (let x = 0; x < canvas.width; x++) {
      // Peak of the bins that fall in this pixel column
      const first = Math.floor(x * count / canvas.width);
      const last = Math.max(first + 1, Math.floor((x + 1) * count / canvas.width));
      let peak = 0;
      for (let b = first; b < last; b++) {
        peak = Math.max(peak, view.getUint8(40 + b));
      }
      const y = canvas.height - (peak / 255) * (canvas.height - 4) - 2;
      if (x === 0) {
        g.moveTo(x, y);
      } else {
        g.lineTo(x, y);
      }
    }
    g.stroke();
    info.textContent = megahertz(center) + ' center, ' + (rate / 1e6).toFixed(2) + ' MHz wide, ' +
      minDb.toFixed(0) + ' to ' + maxDb.toFixed(0) + ' dB';
  }

  return () => {
    if (spectrumSub) {
      spectrumSub.close();
    }
  };
}
