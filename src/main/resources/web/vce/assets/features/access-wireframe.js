/** A decorative, data-free wireframe field shared by the access page and sign-in dialog. */
export function mountAccessWireframe(canvas, { compact = false } = {}) {
  const context = canvas?.getContext?.('2d');
  if (!context) return () => {};

  const host = canvas.parentElement;
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  let width = 0;
  let height = 0;
  let frameId = 0;
  let lastFrame = 0;
  let visible = false;
  let pointerX = 0;
  let pointerY = 0;
  let cameraX = 0;
  let cameraY = 0;
  let removed = false;
  let seed = compact ? 3169 : 74291;
  const random = () => ((seed = (1664525 * seed + 1013904223) >>> 0) / 4294967296);
  const colors = { wire: '', grid: '', link: '', glow: '' };

  const cubeVertices = [
    [-.5, -.5, -.5], [.5, -.5, -.5], [.5, .5, -.5], [-.5, .5, -.5],
    [-.5, -.5, .5], [.5, -.5, .5], [.5, .5, .5], [-.5, .5, .5]
  ];
  const cubeEdges = [[0, 1], [1, 2], [2, 3], [3, 0], [4, 5], [5, 6], [6, 7], [7, 4],
    [0, 4], [1, 5], [2, 6], [3, 7]];
  const cubeBraces = [[0, 2], [4, 6], [0, 5], [3, 6]];
  const tetraVertices = [[0, -.68, 0], [-.72, .52, -.62], [.72, .52, -.62], [0, .52, .75]];
  const tetraEdges = [[0, 1], [0, 2], [0, 3], [1, 2], [2, 3], [3, 1]];
  const shapes = compact ? [
    { kind: 'cube', x: -2.2, y: -2.1, z: 1.6, size: 1, phase: .4, spin: .4, bright: true },
    { kind: 'tetra', x: 2.25, y: 1.4, z: 1.3, size: 1.08, phase: .8, spin: -.45, bright: true }
  ] : [
    { kind: 'cube', x: -5.5, y: 2.25, z: 1.1, size: 1.45, phase: .25, spin: .65, bright: true },
    { kind: 'cube', x: 5.05, y: -2.6, z: 1.6, size: 1.18, phase: .8, spin: -.42, bright: true },
    { kind: 'tetra', x: -4.9, y: -2.1, z: 2.6, size: 1.34, phase: .4, spin: .5, bright: true },
    { kind: 'cube', x: 3.9, y: 2.8, z: 2.6, size: 1.1, phase: 1.4, spin: .35, bright: true }
  ];
  for (let index = 0; index < (compact ? 34 : 82); index += 1) {
    const z = 1.2 + random() * 12;
    shapes.push({
      kind: random() < .52 ? 'cube' : 'tetra',
      x: (random() - .5) * 15,
      y: (random() - .5) * 8.3,
      z,
      size: .37 + random() * .88,
      phase: random() * Math.PI * 2,
      spin: (random() - .5) * 1.2,
      bright: random() < .11
    });
  }

  function project(x, y, z) {
    const distance = Math.max(2.8, z + 5.2);
    const focal = Math.min(width * 1.12, height * 1.95) / distance;
    return [width * .5 + (x - cameraX) * focal, height * .52 + (y - cameraY) * focal];
  }

  function segment(left, right) {
    context.moveTo(left[0], left[1]);
    context.lineTo(right[0], right[1]);
  }

  function drawGrid() {
    context.strokeStyle = colors.grid;
    context.globalAlpha = compact ? .085 : .09;
    context.lineWidth = .75;
    context.beginPath();
    for (let x = -16; x <= 16; x += 2) segment(project(x, 3.65, .5), project(x, 3.65, 22));
    for (let z = 1; z <= 22; z += 1.7) segment(project(-16, 3.65, z), project(16, 3.65, z));
    context.stroke();
    context.globalAlpha *= .65;
    context.beginPath();
    for (let x = -14; x <= 14; x += 3.5) segment(project(x, -4.1, .5), project(x, -4.1, 23));
    for (let z = 2; z <= 22; z += 2.8) segment(project(-14, -4.1, z), project(14, -4.1, z));
    context.stroke();
  }

  function drawConnections() {
    context.strokeStyle = colors.link;
    context.globalAlpha = compact ? .055 : .07;
    context.lineWidth = .8;
    context.beginPath();
    for (let index = 6; index < shapes.length - 7; index += 3) {
      const left = shapes[index];
      const right = shapes[index + 5];
      segment(project(left.x, left.y, left.z), project(right.x, right.y, right.z));
    }
    context.stroke();
  }

  function drawShape(shape, time) {
    const vertices = shape.kind === 'cube' ? cubeVertices : tetraVertices;
    const edges = shape.kind === 'cube' ? cubeEdges : tetraEdges;
    const angleY = shape.phase + time * .00011 * shape.spin;
    const angleX = shape.phase * .46 + time * .000065 * shape.spin;
    const cosY = Math.cos(angleY);
    const sinY = Math.sin(angleY);
    const cosX = Math.cos(angleX);
    const sinX = Math.sin(angleX);
    const points = vertices.map((vertex) => {
      const x = vertex[0] * cosY + vertex[2] * sinY;
      const z = -vertex[0] * sinY + vertex[2] * cosY;
      const y = vertex[1] * cosX - z * sinX;
      const rotatedZ = vertex[1] * sinX + z * cosX;
      return project(shape.x + x * shape.size, shape.y + y * shape.size,
        shape.z + rotatedZ * shape.size);
    });
    const depth = Math.max(0, Math.min(1, 1 - shape.z / 16));
    const alpha = (shape.bright ? .64 : .43) * (.27 + depth * .73) * (compact ? .9 : 1);
    context.strokeStyle = colors.wire;
    context.globalAlpha = alpha;
    context.lineWidth = shape.bright ? 1.12 : .85;
    context.shadowBlur = shape.bright ? 9 : 0;
    context.shadowColor = colors.glow;
    context.beginPath();
    for (const [left, right] of edges) segment(points[left], points[right]);
    context.stroke();
    context.shadowBlur = 0;
    if (shape.kind === 'cube' && (shape.bright || shape.z < 6)) {
      context.globalAlpha = alpha * .46;
      context.beginPath();
      for (const [left, right] of cubeBraces) segment(points[left], points[right]);
      context.stroke();
    }
  }

  function draw(time) {
    if (!width || !height) return;
    context.clearRect(0, 0, width, height);
    cameraX += (pointerX - cameraX) * .035;
    cameraY += (pointerY - cameraY) * .035;
    drawGrid();
    drawConnections();
    for (let index = shapes.length - 1; index >= 0; index -= 1) {
      if (width < 600 && !compact && index > 3 && index % 2) continue;
      drawShape(shapes[index], time);
    }
    context.globalAlpha = 1;
  }

  function resize() {
    if (removed) return;
    const rectangle = canvas.getBoundingClientRect();
    width = rectangle.width;
    height = rectangle.height;
    if (!width || !height) return;
    const scale = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = Math.round(width * scale);
    canvas.height = Math.round(height * scale);
    context.setTransform(scale, 0, 0, scale, 0, 0);
    const style = getComputedStyle(canvas);
    colors.wire = style.getPropertyValue('--access-wire-color').trim();
    colors.grid = style.getPropertyValue('--access-grid-color').trim();
    colors.link = style.getPropertyValue('--access-link-color').trim();
    colors.glow = style.getPropertyValue('--access-wire-glow').trim();
    draw(0);
  }

  function frame(time) {
    frameId = 0;
    if (removed || !visible || document.hidden || reducedMotion.matches) return;
    if (time - lastFrame >= 40) {
      draw(time);
      lastFrame = time;
    }
    frameId = requestAnimationFrame(frame);
  }

  function synchronizeMotion() {
    const animate = visible && !document.hidden && !reducedMotion.matches;
    if (!animate && frameId) cancelAnimationFrame(frameId);
    if (!animate) frameId = 0;
    if (visible) resize();
    if (animate && !frameId) frameId = requestAnimationFrame(frame);
  }

  function pointerMove(event) {
    const rectangle = host.getBoundingClientRect();
    pointerX = ((event.clientX - rectangle.left) / rectangle.width - .5) * .65;
    pointerY = ((event.clientY - rectangle.top) / rectangle.height - .5) * .42;
  }

  function pointerLeave() {
    pointerX = 0;
    pointerY = 0;
  }

  const sizeObserver = new ResizeObserver(resize);
  const visibilityObserver = new IntersectionObserver(([entry]) => {
    visible = entry?.isIntersecting === true;
    synchronizeMotion();
  });
  sizeObserver.observe(host);
  visibilityObserver.observe(canvas);
  host.addEventListener('pointermove', pointerMove);
  host.addEventListener('pointerleave', pointerLeave);
  document.addEventListener('visibilitychange', synchronizeMotion);
  reducedMotion.addEventListener('change', synchronizeMotion);
  resize();

  return () => {
    removed = true;
    if (frameId) cancelAnimationFrame(frameId);
    sizeObserver.disconnect();
    visibilityObserver.disconnect();
    host.removeEventListener('pointermove', pointerMove);
    host.removeEventListener('pointerleave', pointerLeave);
    document.removeEventListener('visibilitychange', synchronizeMotion);
    reducedMotion.removeEventListener('change', synchronizeMotion);
  };
}
