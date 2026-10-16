const ICON_CONTROL_SELECTOR = [
  '.ui-icon-button', '.icon-button', '.ui-header-indicator', '.playback-command',
  '.playback-control-menu > summary', '.channels-tab-close', '.ui-segmented-option-icon', '[data-ui-hint]'
].join(', ');

export function installIconHints(root = document) {
  const hint = root.createElement('div');
  hint.className = 'ui-icon-hint';
  hint.setAttribute('role', 'tooltip');
  hint.setAttribute('popover', 'manual');
  root.body.append(hint);
  let active = null;
  let timer = null;

  const hide = () => {
    clearTimeout(timer);
    timer = null;
    active = null;
    if (hint.matches(':popover-open')) hint.hidePopover();
  };
  const position = (control) => {
    const rect = control.getBoundingClientRect();
    const width = hint.offsetWidth;
    const height = hint.offsetHeight;
    const gap = 8;
    const left = Math.max(gap, Math.min(rect.left + rect.width / 2 - width / 2,
      window.innerWidth - width - gap));
    const below = rect.bottom + gap + height <= window.innerHeight - gap;
    const top = below ? rect.bottom + gap : Math.max(gap, rect.top - height - gap);
    hint.style.left = `${Math.round(left)}px`;
    hint.style.top = `${Math.round(top)}px`;
  };
  const show = (control) => {
    if (active !== control || !control.isConnected) return;
    const title = control.getAttribute('title');
    if (title) {
      control.dataset.uiHint = title;
      control.removeAttribute('title');
    }
    const label = control.dataset.uiHint || control.getAttribute('aria-label');
    if (!label) return;
    hint.textContent = label;
    if (!hint.matches(':popover-open')) hint.showPopover();
    position(control);
  };
  const controlFor = (target) => target instanceof Element ? target.closest(ICON_CONTROL_SELECTOR) : null;
  const activate = (control, delay) => {
    if (!control || control === active) return;
    hide();
    active = control;
    if (delay) timer = window.setTimeout(() => show(control), delay);
    else show(control);
  };

  root.addEventListener('pointerover', (event) => activate(controlFor(event.target), 350));
  root.addEventListener('pointermove', (event) => {
    if (!active) activate(controlFor(event.target), 350);
  });
  root.addEventListener('pointerout', (event) => {
    if (active && active.contains(event.target) && !active.contains(event.relatedTarget)) hide();
  });
  root.addEventListener('focusin', (event) => {
    const control = controlFor(event.target);
    if (!control?.matches(':focus-visible')) return;
    if (control === active) {
      clearTimeout(timer);
      timer = null;
      show(control);
      return;
    }
    activate(control, 0);
  });
  root.addEventListener('focusout', (event) => {
    if (active && active.contains(event.target) && !active.contains(event.relatedTarget)) hide();
  });
  root.addEventListener('click', (event) => {
    const control = controlFor(event.target);
    if (control !== active || !hint.matches(':popover-open')) return;
    window.queueMicrotask(() => {
      if (active === control && hint.matches(':popover-open')) show(control);
    });
  });
  root.addEventListener('pointerdown', hide);
  root.addEventListener('scroll', () => {
    if (active === root.activeElement && hint.matches(':popover-open')) position(active);
    else hide();
  }, true);
  root.addEventListener('keydown', (event) => { if (event.key === 'Escape') hide(); });
  window.addEventListener('resize', hide);
}
