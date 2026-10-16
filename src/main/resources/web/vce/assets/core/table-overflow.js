// Shared overflow affordances preserve the existing table scroll container.
// Title actions hold explicit controls; zero-height sticky cues never change row
// or Live pane geometry, and keyboard scrolling belongs to the wrapper itself.
const attachmentTrackers = new WeakMap();

function trackAttachment(wrapper, onDetached) {
  const document = wrapper.ownerDocument;
  const MutationObserver = document.defaultView?.MutationObserver;
  if (!MutationObserver || !document.documentElement) return () => {};
  let tracker = attachmentTrackers.get(document);
  if (!tracker) {
    const entries = new Set();
    const observer = new MutationObserver(() => {
      for (const entry of entries) {
        if (entry.wrapper.isConnected) entry.connected = true;
        else if (entry.connected) entry.onDetached();
      }
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
    tracker = { entries, observer };
    attachmentTrackers.set(document, tracker);
  }
  const entry = { wrapper, onDetached, connected: wrapper.isConnected };
  tracker.entries.add(entry);
  return () => {
    tracker.entries.delete(entry);
    if (!tracker.entries.size) {
      tracker.observer.disconnect();
      attachmentTrackers.delete(document);
    }
  };
}

export function createTableOverflow({ wrapper, table, actionsHost, iconButton, signal,
  label = 'Table' }) {
  if (!wrapper || !table || !actionsHost || typeof iconButton !== 'function') {
    throw new Error('Table overflow controls require a wrapper, table, action host and icon buttons');
  }
  const document = wrapper.ownerDocument;
  const view = document.defaultView;
  const controls = document.createElement('div');
  controls.className = 'ui-table-overflow-controls';
  controls.setAttribute('role', 'group');
  controls.setAttribute('aria-label', `${label} scrolling`);
  controls.hidden = true;
  const buttonClass = 'ui-button ui-button-secondary ui-icon-button ui-icon-button-compact';
  const leftButton = iconButton('icon-chevron-left', 'Scroll table left', buttonClass);
  const rightButton = iconButton('icon-chevron-right', 'Scroll table right', buttonClass);
  leftButton.disabled = true;
  rightButton.disabled = true;
  controls.append(leftButton, rightButton);
  actionsHost.append(controls);

  const cues = document.createElement('div');
  cues.className = 'ui-table-overflow-cues';
  cues.setAttribute('aria-hidden', 'true');
  cues.hidden = true;
  const leftCue = document.createElement('span');
  leftCue.className = 'ui-table-overflow-edge ui-table-overflow-edge-left';
  const rightCue = document.createElement('span');
  rightCue.className = 'ui-table-overflow-edge ui-table-overflow-edge-right';
  cues.append(leftCue, rightCue);
  wrapper.prepend(cues);
  wrapper.classList.add('ui-table-overflow');

  const ownedAttributes = ['tabindex', 'role', 'aria-label', 'aria-keyshortcuts'];
  const originalAttributes = new Map(ownedAttributes.map((name) => [name, wrapper.getAttribute(name)]));
  const restoreAttributes = () => {
    for (const [name, value] of originalAttributes) {
      if (value === null) wrapper.removeAttribute(name);
      else wrapper.setAttribute(name, value);
    }
  };
  let destroyed = false;
  let frame = null;
  let overflowing = false;
  let resizeObserver = null;
  let contentObserver = null;
  let untrackAttachment = () => {};

  const geometry = () => {
    const maximum = Math.max(0, wrapper.scrollWidth - wrapper.clientWidth);
    const rtl = view?.getComputedStyle(wrapper).direction === 'rtl';
    const position = Math.max(0, Math.min(maximum, rtl ? maximum + wrapper.scrollLeft : wrapper.scrollLeft));
    return { maximum, position, rtl };
  };
  const refresh = () => {
    if (destroyed) return;
    const { maximum, position } = geometry();
    const overflowX = view?.getComputedStyle(wrapper).overflowX;
    overflowing = wrapper.isConnected && wrapper.clientWidth > 0 && wrapper.clientHeight > 0 &&
      maximum > 1 && overflowX !== 'hidden' && overflowX !== 'clip';
    controls.hidden = !overflowing;
    cues.hidden = !overflowing;
    const hasLeft = overflowing && position > 1;
    const hasRight = overflowing && maximum - position > 1;
    leftButton.disabled = !hasLeft;
    rightButton.disabled = !hasRight;
    leftCue.hidden = !hasLeft;
    rightCue.hidden = !hasRight;
    if (overflowing) {
      cues.style.width = `${wrapper.clientWidth}px`;
      leftCue.style.height = `${wrapper.clientHeight}px`;
      rightCue.style.height = `${wrapper.clientHeight}px`;
      wrapper.tabIndex = 0;
      if (!originalAttributes.get('role')) wrapper.setAttribute('role', 'region');
      if (!originalAttributes.get('aria-label')) wrapper.setAttribute('aria-label', `${label}, scroll horizontally for more columns`);
      wrapper.setAttribute('aria-keyshortcuts', 'ArrowLeft ArrowRight Home End');
    } else restoreAttributes();
  };
  const scheduleRefresh = () => {
    if (destroyed || frame !== null) return;
    frame = view.requestAnimationFrame(() => {
      frame = null;
      refresh();
    });
  };
  const scroll = (direction, boundary = false) => {
    if (!overflowing || destroyed) return;
    const { maximum, position, rtl } = geometry();
    const next = boundary ? (direction < 0 ? 0 : maximum) :
      Math.max(0, Math.min(maximum, position + direction * Math.max(1, wrapper.clientWidth * 0.8)));
    const reducedMotion = view.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    wrapper.scrollTo({ left: rtl ? next - maximum : next,
      behavior: reducedMotion ? 'instant' : 'smooth' });
    scheduleRefresh();
  };
  const scrollLeft = () => scroll(-1);
  const scrollRight = () => scroll(1);
  const keydown = (event) => {
    if (event.target !== wrapper || event.altKey || event.ctrlKey || event.metaKey || !overflowing) return;
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    scroll(event.key === 'ArrowLeft' || event.key === 'Home' ? -1 : 1,
      event.key === 'Home' || event.key === 'End');
  };
  const destroy = () => {
    if (destroyed) return;
    destroyed = true;
    if (frame !== null) view.cancelAnimationFrame(frame);
    frame = null;
    resizeObserver?.disconnect();
    contentObserver?.disconnect();
    untrackAttachment();
    signal?.removeEventListener('abort', destroy);
    wrapper.removeEventListener('scroll', scheduleRefresh);
    wrapper.removeEventListener('keydown', keydown);
    view.removeEventListener('resize', scheduleRefresh);
    leftButton.removeEventListener('click', scrollLeft);
    rightButton.removeEventListener('click', scrollRight);
    controls.remove();
    cues.remove();
    wrapper.classList.remove('ui-table-overflow');
    restoreAttributes();
  };
  leftButton.addEventListener('click', scrollLeft);
  rightButton.addEventListener('click', scrollRight);
  wrapper.addEventListener('scroll', scheduleRefresh, { passive: true });
  wrapper.addEventListener('keydown', keydown);
  view.addEventListener('resize', scheduleRefresh, { passive: true });
  if (typeof view.ResizeObserver === 'function') {
    resizeObserver = new view.ResizeObserver(scheduleRefresh);
    resizeObserver.observe(wrapper);
    resizeObserver.observe(table);
  }
  if (typeof view.MutationObserver === 'function') {
    contentObserver = new view.MutationObserver(scheduleRefresh);
    contentObserver.observe(table, { childList: true, subtree: true, characterData: true,
      attributes: true, attributeFilter: ['style', 'class', 'hidden'] });
  }
  untrackAttachment = trackAttachment(wrapper, destroy);
  signal?.addEventListener('abort', destroy, { once: true });
  if (signal?.aborted) destroy();
  else {
    refresh();
    scheduleRefresh();
  }
  return { refresh, destroy };
}
