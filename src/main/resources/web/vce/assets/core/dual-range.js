// Shared two-handle range control. Native sliders provide keyboard and
// assistive-technology interaction. A shared pointer track gives both handles
// a touch target and keeps each handle reachable when the endpoints meet.
export function createDualRange({ node, label, min = 0, max = 100, step = 1,
  lower = min, upper = max, format = String, lowerLabel = 'Minimum', upperLabel = 'Maximum',
  disabled = false, onChange } = {}) {
  const field = node('div', 'ui-field ui-dual-range-field');
  const heading = node('div', 'ui-dual-range-heading');
  const output = node('output', 'ui-dual-range-value');
  const track = node('div', 'ui-dual-range');
  track.setAttribute('role', 'group');
  track.setAttribute('aria-label', label);
  const rail = node('span', 'ui-dual-range-rail');
  rail.setAttribute('aria-hidden', 'true');
  const createHandle = (name, value) => {
    const input = node('input', 'ui-dual-range-handle');
    input.type = 'range';
    input.min = String(min);
    input.max = String(max);
    input.step = String(step);
    input.value = String(value);
    input.disabled = disabled;
    input.setAttribute('aria-label', `${label}: ${name}`);
    return input;
  };
  const lowerInput = createHandle(lowerLabel, lower);
  const upperInput = createHandle(upperLabel, upper);
  const values = () => ({ lower: Number(lowerInput.value), upper: Number(upperInput.value) });
  const render = () => {
    const range = values();
    lowerInput.setAttribute('aria-valuetext', format(range.lower, 'lower'));
    upperInput.setAttribute('aria-valuetext', format(range.upper, 'upper'));
    lowerInput.setAttribute('aria-valuemax', String(range.upper));
    upperInput.setAttribute('aria-valuemin', String(range.lower));
    output.textContent = `${format(range.lower, 'lower')} – ${format(range.upper, 'upper')}`;
    track.style.setProperty('--range-lower', `${100 * (range.lower - min) / (max - min)}%`);
    track.style.setProperty('--range-upper', `${100 * (range.upper - min) / (max - min)}%`);
    // When both handles meet, keep the lower handle reachable by pointer at
    // the upper end and the upper handle reachable at the lower end.
    track.classList.toggle('ui-dual-range-at-end', range.lower === max);
  };
  [lowerInput, upperInput].forEach((input) => input.addEventListener('input', () => {
    if (input === lowerInput) lowerInput.value = String(Math.min(Number(input.value), Number(upperInput.value)));
    else upperInput.value = String(Math.max(Number(input.value), Number(lowerInput.value)));
    render();
    onChange?.(values());
  }));
  let dragging = null;
  const pointerValue = (event) => {
    const bounds = rail.getBoundingClientRect();
    const fraction = Math.max(0, Math.min(1, (event.clientX - bounds.left) / bounds.width));
    return Math.max(min, Math.min(max, min + Math.round(fraction * (max - min) / step) * step));
  };
  const moveHandle = (event) => {
    if (!dragging) return;
    dragging.value = String(pointerValue(event));
    dragging.dispatchEvent(new Event('input', { bubbles: true }));
  };
  track.addEventListener('pointerdown', (event) => {
    if (disabled || event.button > 0) return;
    event.preventDefault();
    const position = pointerValue(event);
    const range = values();
    const lowerDistance = Math.abs(position - range.lower);
    const upperDistance = Math.abs(position - range.upper);
    dragging = lowerDistance < upperDistance ||
      (lowerDistance === upperDistance && position <= range.lower) ? lowerInput : upperInput;
    dragging.focus();
    track.setPointerCapture(event.pointerId);
    moveHandle(event);
  });
  track.addEventListener('pointermove', moveHandle);
  const stopDragging = () => { dragging = null; };
  track.addEventListener('pointerup', stopDragging);
  track.addEventListener('pointercancel', stopDragging);
  track.addEventListener('lostpointercapture', stopDragging);
  heading.append(node('span', 'ui-field-label', label), output);
  track.append(rail, lowerInput, upperInput);
  field.append(heading, track);
  field.classList.toggle('ui-dual-range-disabled', disabled);
  render();
  return { field, lowerInput, upperInput, output, values };
}
