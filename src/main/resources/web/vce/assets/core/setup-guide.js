// Spotlight existing actions without moving or duplicating their controls.
// The shared modal owns dismissal, keyboard focus, replacement and cleanup.
export function openSetupGuide({ node, openReadOnlyModal }, { title, choices, onDismiss }) {
  const body = node('div', 'ui-setup-guide-choices');
  const originals = [];
  const buttons = choices.map(choice => choice.button);
  let automaticClose = false;
  let closed = false;
  let frame = null;
  let observer = null;
  let update = () => {};
  const schedule = () => {
    if (closed || frame !== null) return;
    frame = requestAnimationFrame(() => { frame = null; update(); });
  };
  const restore = () => {
    closed = true;
    if (frame !== null) cancelAnimationFrame(frame);
    observer?.disconnect();
    window.removeEventListener('resize', schedule);
    document.removeEventListener('scroll', schedule, true);
    window.visualViewport?.removeEventListener('resize', schedule);
    window.visualViewport?.removeEventListener('scroll', schedule);
    originals.forEach(({ button, id, describedBy, choose }) => {
      button.removeEventListener('click', choose, true);
      if (describedBy === null) button.removeAttribute('aria-describedby');
      else button.setAttribute('aria-describedby', describedBy);
      if (id === null) button.removeAttribute('id');
      else button.id = id;
    });
  };
  const modal = openReadOnlyModal(title, body, {
    id: 'setup-guide', className: 'ui-setup-guide', closeLabel: 'Dismiss setup guide',
    focusTargets: buttons, lockScroll: false, cleanup: restore,
    onDismiss: () => { if (!automaticClose) onDismiss?.(); }
  });
  if (!modal) return null;
  const backdrop = modal.state.backdrop;
  backdrop.classList.add('ui-setup-guide-backdrop');
  void modal.ready.then((shown) => {
    if (!shown || closed) return;
    const prefix = modal.dialog.querySelector('h2').id;
    const svgNode = tag => document.createElementNS('http://www.w3.org/2000/svg', tag);
    const shade = svgNode('svg');
    shade.classList.add('ui-setup-guide-shade');
    shade.setAttribute('aria-hidden', 'true');
    const dimmer = svgNode('path');
    dimmer.classList.add('ui-setup-guide-dimmer');
    dimmer.setAttribute('fill-rule', 'evenodd');
    dimmer.setAttribute('data-modal-dismiss-surface', '');
    shade.append(dimmer);
    const highlights = choices.map(({ button, description }, index) => {
      const id = button.getAttribute('id');
      const describedBy = button.getAttribute('aria-describedby');
      if (!id) button.id = `${prefix}-action-${index}`;
      const detail = node('p', 'ui-setup-guide-description', description);
      detail.id = `${prefix}-choice-${index}`;
      button.setAttribute('aria-describedby', [describedBy, detail.id].filter(Boolean).join(' '));
      const choice = node('div', 'ui-setup-guide-choice');
      choice.append(node('strong', '', button.textContent.trim()), detail);
      body.append(choice);
      const choose = () => modal.close();
      originals.push({ button, id, describedBy, choose });
      button.addEventListener('click', choose, true);
      const highlight = svgNode('rect');
      highlight.classList.add('ui-setup-guide-highlight');
      highlight.setAttribute('data-setup-guide-target', button.id);
      shade.append(highlight);
      return highlight;
    });
    modal.dialog.setAttribute('aria-owns', buttons.map(button => button.id).join(' '));
    backdrop.prepend(shade);
    const styles = getComputedStyle(backdrop);
    const inset = parseFloat(styles.getPropertyValue('--space-1'));
    const gap = parseFloat(styles.getPropertyValue('--space-3'));
    const gutter = parseFloat(styles.getPropertyValue('--space-4'));
    update = () => {
      if (closed) return;
      if (buttons.some(button => !button.isConnected)) {
        automaticClose = true;
        modal.close();
        return;
      }
      const width = document.documentElement.clientWidth;
      const height = document.documentElement.clientHeight;
      shade.setAttribute('viewBox', `0 0 ${width} ${height}`);
      const bounds = buttons.map(button => button.getBoundingClientRect());
      let path = `M0 0H${width}V${height}H0Z`;
      bounds.forEach((rect, index) => {
        const left = Math.max(0, rect.left - inset);
        const top = Math.max(0, rect.top - inset);
        const right = Math.min(width, rect.right + inset);
        const bottom = Math.min(height, rect.bottom + inset);
        const visible = right > left && bottom > top;
        highlights[index].toggleAttribute('hidden', !visible);
        if (!visible) return;
        path += `M${left} ${top}H${right}V${bottom}H${left}Z`;
        for (const [key, value] of Object.entries({ x: left, y: top, width: right - left, height: bottom - top })) {
          highlights[index].setAttribute(key, String(value));
        }
      });
      dimmer.setAttribute('d', path);
      // Keep the explanation beside the toolbar, choosing the side with room.
      modal.dialog.style.maxHeight = `${height - gutter * 2}px`;
      const panel = modal.dialog.getBoundingClientRect();
      const top = Math.min(...bounds.map(rect => rect.top)) - inset;
      const bottom = Math.max(...bounds.map(rect => rect.bottom)) + inset;
      const below = height - gutter - bottom - gap;
      const above = top - gap - gutter;
      const useBelow = below >= panel.height || below >= above;
      const room = Math.max(0, useBelow ? below : above);
      modal.dialog.style.maxHeight = `${room}px`;
      const panelHeight = Math.min(panel.height, room);
      modal.dialog.style.left = `${Math.max(gutter, Math.min(Math.min(...bounds.map(rect => rect.left)),
        width - gutter - panel.width))}px`;
      modal.dialog.style.top = `${Math.max(gutter, Math.min(useBelow ? bottom + gap : top - gap - panelHeight,
        height - gutter - panelHeight))}px`;
    };
    // Summary cards can put the real toolbar below the fold on a phone.
    const visible = buttons.every(button => {
      const rect = button.getBoundingClientRect();
      return rect.top >= gutter && rect.bottom <= document.documentElement.clientHeight - gutter;
    });
    if (!visible) buttons[0].scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'instant' });
    update();
    window.addEventListener('resize', schedule);
    document.addEventListener('scroll', schedule, true);
    window.visualViewport?.addEventListener('resize', schedule);
    window.visualViewport?.addEventListener('scroll', schedule);
    observer = new ResizeObserver(schedule);
    observer.observe(modal.dialog);
    buttons.forEach(button => observer.observe(button));
    modal.focus(buttons[0]);
  });
  return {
    ready: modal.ready,
    isOpen: () => !closed && backdrop.isConnected,
    close: () => { automaticClose = true; modal.close(); }
  };
}
