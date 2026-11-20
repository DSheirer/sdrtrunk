// sdrtrunk site: latest release download links and the animated hero spectrum.
(function () {
  'use strict';

  var REPO = 'DSheirer/sdrtrunk';

  // ---- Highlight the visitor's operating system ----
  function detectOs() {
    var ua = (navigator.userAgentData && navigator.userAgentData.platform) || navigator.platform || navigator.userAgent || '';
    ua = ua.toLowerCase();
    if (ua.indexOf('win') >= 0) return 'windows';
    if (ua.indexOf('mac') >= 0) return 'mac';
    if (ua.indexOf('linux') >= 0 || ua.indexOf('x11') >= 0) return 'linux';
    return null;
  }
  var os = detectOs();
  if (os) {
    var card = document.querySelector('.dl[data-os="' + os + '"]');
    if (card) card.classList.add('is-you');
  }

  // ---- Fill in links to the latest release assets ----
  function formatSize(bytes) {
    return Math.round(bytes / 1048576) + ' MB';
  }
  function formatDate(iso) {
    try {
      return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
    } catch (e) { return ''; }
  }

  function applyRelease(release) {
    if (!release || !release.tag_name) return;
    var version = release.tag_name;
    document.querySelectorAll('[data-latest-version]').forEach(function (el) { el.textContent = version; });
    document.querySelectorAll('[data-latest-date]').forEach(function (el) {
      el.textContent = 'Released ' + formatDate(release.published_at);
    });
    var assets = release.assets || [];
    document.querySelectorAll('[data-asset]').forEach(function (link) {
      var key = 'sdr-trunk-' + link.getAttribute('data-asset') + '-';
      for (var i = 0; i < assets.length; i++) {
        var a = assets[i];
        if (a.name.indexOf(key) === 0 && /\.zip$/.test(a.name)) {
          link.href = a.browser_download_url;
          link.title = a.name;
          var size = document.createElement('span');
          size.className = 'size';
          size.textContent = ' · ' + formatSize(a.size);
          link.appendChild(size);
          break;
        }
      }
    });
  }

  // Cache the API response for an hour so repeat visits don't use up the GitHub rate limit.
  var CACHE_KEY = 'sdrtrunk-latest-release';
  var cached = null;
  try { cached = JSON.parse(localStorage.getItem(CACHE_KEY) || 'null'); } catch (e) { cached = null; }
  if (cached && cached.time && Date.now() - cached.time < 3600000) {
    applyRelease(cached.release);
  } else if (window.fetch) {
    fetch('https://api.github.com/repos/' + REPO + '/releases/latest', { headers: { Accept: 'application/vnd.github+json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (release) {
        if (!release) return;
        var slim = {
          tag_name: release.tag_name,
          published_at: release.published_at,
          assets: (release.assets || []).map(function (a) {
            return { name: a.name, size: a.size, browser_download_url: a.browser_download_url };
          })
        };
        applyRelease(slim);
        try { localStorage.setItem(CACHE_KEY, JSON.stringify({ time: Date.now(), release: slim })); } catch (e) { /* ignore */ }
      })
      .catch(function () { /* links already point at the releases page */ });
  }

  // ---- Animated spectrum behind the hero ----
  var canvas = document.querySelector('.hero-spectrum');
  if (!canvas || !canvas.getContext) return;
  var ctx = canvas.getContext('2d');
  var reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var peaks = [
    { f: 0.12, w: 0.006, h: 0.55 }, { f: 0.27, w: 0.004, h: 0.75 }, { f: 0.31, w: 0.004, h: 0.4 },
    { f: 0.46, w: 0.007, h: 0.85 }, { f: 0.58, w: 0.004, h: 0.5 }, { f: 0.63, w: 0.005, h: 0.65 },
    { f: 0.79, w: 0.006, h: 0.45 }, { f: 0.91, w: 0.004, h: 0.7 }
  ];
  var noise = [];
  var W = 0, H = 0, dpr = 1;

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    W = canvas.clientWidth; H = canvas.clientHeight;
    canvas.width = W * dpr; canvas.height = H * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    noise = new Array(Math.ceil(W / 3) + 2).fill(0);
  }

  function draw(t) {
    ctx.clearRect(0, 0, W, H);
    var n = noise.length;
    var pts = [];
    for (var i = 0; i < n; i++) {
      noise[i] = noise[i] * 0.7 + Math.random() * 0.3;
      var x = i / (n - 1);
      var y = 0.06 + noise[i] * 0.06;
      for (var p = 0; p < peaks.length; p++) {
        var pk = peaks[p];
        var on = 0.55 + 0.45 * Math.sin(t / 900 * (0.6 + p * 0.17) + p * 2.1);
        var d = (x - pk.f) / pk.w;
        y += pk.h * on * Math.exp(-d * d * 0.5);
      }
      pts.push([x * W, H - y * H * 0.9]);
    }
    var grad = ctx.createLinearGradient(0, 0, W, 0);
    grad.addColorStop(0, '#22d3ee'); grad.addColorStop(0.55, '#2ee6a6'); grad.addColorStop(1, '#7cf05a');
    ctx.beginPath();
    ctx.moveTo(0, H);
    pts.forEach(function (pt) { ctx.lineTo(pt[0], pt[1]); });
    ctx.lineTo(W, H);
    ctx.closePath();
    var fill = ctx.createLinearGradient(0, H * 0.2, 0, H);
    fill.addColorStop(0, 'rgba(46,230,166,0.22)'); fill.addColorStop(1, 'rgba(46,230,166,0)');
    ctx.fillStyle = fill;
    ctx.fill();
    ctx.beginPath();
    pts.forEach(function (pt, i) { if (i) ctx.lineTo(pt[0], pt[1]); else ctx.moveTo(pt[0], pt[1]); });
    ctx.strokeStyle = grad; ctx.lineWidth = 1.5; ctx.stroke();
  }

  var last = 0;
  function loop(t) {
    if (t - last > 60) { draw(t); last = t; }
    requestAnimationFrame(loop);
  }
  resize();
  window.addEventListener('resize', resize);
  if (reduced) draw(0); else requestAnimationFrame(loop);
})();
