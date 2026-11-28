// The browser's audio channels: LEFT and RIGHT in stereo, or one channel in mono.  Each channel plays one call at a
// time, either a recording or live audio, through its own pan, mute and level nodes, and shows what it plays in a
// panel like the desktop application's audio panel.
import {API} from './api.js';
import {duration, el, identifier} from './format.js';

const HEADPHONES = '<svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path fill="currentColor" ' +
  'd="M12 3a9 9 0 0 0-9 9v7a2 2 0 0 0 2 2h2a1 1 0 0 0 1-1v-6a1 1 0 0 0-1-1H5a7 7 0 0 1 14 0h-2a1 1 0 0 0-1 1v6a1 1 ' +
  '0 0 0 1 1h2a2 2 0 0 0 2-2v-7a9 9 0 0 0-9-9z"/></svg>';
const MICROPHONE = '<svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true"><path fill="currentColor" ' +
  'd="M12 14a3 3 0 0 0 3-3V5a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3zm5-3a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.9V21h2v-3.1a7 7 0 0 0 ' +
  '6-6.9h-2z"/></svg>';
const METER_FLOOR_DB = -60;

function svg(markup) {
  const span = el('span', {class: 'audio-svg'});
  span.innerHTML = markup;
  return span;
}

// Minutes and seconds, as the desktop audio panel shows the elapsed time
function clock(ms) {
  const seconds = Math.max(0, Math.floor((ms || 0) / 1000));
  return Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0');
}

export class AudioChannel {
  // controller: {skip(channel), follow(channel), muteChanged(channel), recordingEnded(channel), canFollow(call)}
  constructor(index, name, controller) {
    this.index = index;
    this.name = name;
    this.controller = controller;
    this.current = null;   // {kind: 'recording' | 'live', key, callKey, call}
    this.lastCall = null;  // the call that played last, shown as LAST CALL
    this.lastPlayedKey = null;
    this.muted = false;
    this.pan = 0;
    this.context = null;
    this.input = null;     // live audio connects here
    this.element = null;   // plays recordings
    this.buildView();
  }

  // Builds the audio nodes in the player's audio context: input (level analyser) -> pan -> mute gain -> speakers
  connect(context) {
    if (this.context === context) {
      return;
    }
    this.context = context;
    this.panner = context.createStereoPanner();
    this.panner.pan.value = this.pan;
    this.gain = context.createGain();
    this.gain.gain.value = this.muted ? 0 : 1;
    this.analyser = context.createAnalyser();
    this.analyser.fftSize = 1024;
    this.samples = new Float32Array(this.analyser.fftSize);
    // The level is measured before the pan and mute, so a muted channel still shows its audio
    this.input = this.analyser;
    this.analyser.connect(this.panner);
    this.panner.connect(this.gain);
    this.gain.connect(context.destination);
    this.element = new Audio();
    this.element.preload = 'auto';
    this.element.addEventListener('ended', () => this.controller.recordingEnded(this));
    context.createMediaElementSource(this.element).connect(this.input);
  }

  setPan(pan) {
    this.pan = pan;
    if (this.panner) {
      this.panner.pan.value = pan;
    }
  }

  isIdle() {
    return !this.current;
  }

  isPlaying(key, callKey) {
    const current = this.current;
    return !!current && ((key && current.key === key) || (callKey && current.kind === 'live' &&
      current.callKey === callKey));
  }

  playRecording(key, call) {
    this.stop();
    this.current = {kind: 'recording', key, callKey: call.callKey, call};
    this.lastPlayedKey = key;
    this.element.src = API + '/calls/' + call.id + '/audio';
    this.element.play().catch(() => {
      // Autoplay can be refused before the first user action
    });
    this.update();
  }

  // The live player plays the call into this channel's input
  playLive(key, callKey, call) {
    this.stop();
    this.current = {kind: 'live', key, callKey, call, startedMs: Date.now()};
    this.update();
  }

  // Ends what plays.  Returns what it was, so the caller can release live audio.
  stop() {
    const current = this.current;
    if (current) {
      this.lastCall = current.call;
      this.current = null;
    }
    if (this.element) {
      this.element.pause();
      this.element.removeAttribute('src');
      this.element.load();
    }
    this.update();
    return current;
  }

  // The newer copy of the call that plays, for example with its stored id or final duration.  Returns true when it
  // is this channel's call.
  updateCall(key, callKey, call) {
    if (this.current && (this.current.key === key || (callKey && this.current.callKey === callKey))) {
      this.current.key = key;
      this.current.call = call;
      return true;
    }
    return false;
  }

  setMuted(muted) {
    this.muted = muted;
    if (this.gain) {
      this.gain.gain.value = muted ? 0 : 1;
    }
    this.update();
  }

  // ---- Panel ----
  buildView() {
    const side = this.name === 'RIGHT' ? 'right' : 'left';
    this.tag = el('span', {class: 'audio-channel-tag ' + side, text: this.name});
    this.status = el('span', {class: 'audio-status'});
    this.elapsed = el('span', {class: 'audio-elapsed'});
    this.followButton = el('button', {type: 'button', class: 'secondary audio-button', text: 'Follow',
      title: 'Follow this talkgroup: list only its calls and play them in order',
      onclick: () => this.controller.follow(this)});
    this.skipButton = el('button', {type: 'button', class: 'secondary audio-button', text: 'Skip',
      title: 'Stop this call and play the next waiting call on this channel',
      onclick: () => this.controller.skip(this)});
    this.muteButton = el('button', {type: 'button', class: 'secondary audio-button', text: 'Mute',
      'aria-pressed': 'false', onclick: () => {
        this.setMuted(!this.muted);
        this.controller.muteChanged(this);
      }});
    this.to = el('span', {class: 'audio-to'});
    this.toDetail = el('span', {class: 'audio-to-detail'});
    this.from = el('span', {class: 'audio-detail'});
    this.fromBox = el('span', {class: 'audio-from'}, svg(MICROPHONE), this.from);
    this.context_ = el('span', {class: 'audio-context'});
    this.meterBar = el('span', {class: 'audio-meter-bar'});
    this.progressBar = el('span', {class: 'audio-progress-bar'});
    this.progress = el('span', {class: 'audio-progress', title: 'Click to move in the recording',
      onclick: (event) => this.seek(event)}, this.progressBar);
    this.view = el('div', {class: 'audio-channel ' + side},
      el('span', {class: 'audio-accent'}),
      el('span', {class: 'audio-icon'}, svg(HEADPHONES)),
      el('div', {class: 'audio-content'},
        el('div', {class: 'audio-header'}, this.tag, this.status, el('span', {class: 'audio-spacer'}),
          this.elapsed, this.followButton, this.skipButton, this.muteButton),
        el('div', {class: 'audio-to-row'}, this.to, this.toDetail),
        el('div', {class: 'audio-detail-row'}, this.fromBox, this.context_),
        el('span', {class: 'audio-meter'}, this.meterBar),
        this.progress));
    this.update();
  }

  seek(event) {
    const element = this.element;
    if (!this.current || this.current.kind !== 'recording' || !element || !isFinite(element.duration)) {
      return;
    }
    const box = this.progress.getBoundingClientRect();
    element.currentTime = Math.min(1, Math.max(0, (event.clientX - box.left) / box.width)) * element.duration;
  }

  // Redraws the panel from what plays
  update() {
    const current = this.current;
    const call = current ? current.call : this.lastCall;
    const active = !!current;
    this.view.classList.toggle('active', active);
    this.view.classList.toggle('muted', this.muted);
    let status = active ? (current.kind === 'recording' ? 'REPLAY' : 'PLAYING') : (call ? 'LAST CALL' : 'IDLE');
    if (this.muted) {
      status = 'MUTED';
    }
    this.status.textContent = status;
    this.muteButton.textContent = this.muted ? 'Unmute' : 'Mute';
    this.muteButton.classList.toggle('pressed', this.muted);
    this.muteButton.setAttribute('aria-pressed', String(this.muted));
    this.muteButton.title = (this.muted ? 'Unmute' : 'Mute') + ' the ' + this.name.toLowerCase() + ' channel';
    this.skipButton.disabled = !active;
    this.followButton.hidden = !active || !this.controller.canFollow(call);
    this.progress.hidden = !active || current.kind !== 'recording';
    if (!call) {
      this.to.textContent = 'No audio';
      this.toDetail.textContent = '';
      this.fromBox.hidden = true;
      this.context_.textContent = '';
      this.elapsed.textContent = '';
    } else {
      const to = call.to;
      this.to.textContent = to ? (to.alias || to.formatted || String(to.value ?? '')) || 'Unknown' : 'Unknown';
      this.toDetail.textContent = to && to.alias ? to.formatted || '' : '';
      const fromName = (call.from && call.from.alias) || call.talkerAlias;
      const fromId = call.from ? call.from.formatted || String(call.from.value ?? '') : '';
      const from = [fromName, fromId].filter(Boolean).join('  ');
      this.from.textContent = from;
      this.fromBox.hidden = !from;
      this.from.title = call.from ? identifier(call.from) : '';
      this.context_.textContent = [call.system, call.site, call.channel,
        call.protocol && call.protocol !== 'UNKNOWN' ? call.protocol : null]
        .filter((part, index, parts) => part && parts.indexOf(part) === index).join('  ·  ');
    }
    this.tick();
  }

  // Elapsed time, recording progress and level meter.  Called a few times a second.
  tick() {
    const current = this.current;
    const call = current ? current.call : this.lastCall;
    let text = '';
    if (current && current.kind === 'recording' && this.element) {
      const total = isFinite(this.element.duration) ? this.element.duration * 1000 : call.durationMs;
      text = clock(this.element.currentTime * 1000) + ' / ' + clock(total);
      this.progressBar.style.width = total ? Math.min(100, this.element.currentTime * 100000 / total) + '%' : '0';
    } else if (current) {
      // A call that waited plays from its start, so the time is how long it has played
      text = clock(Date.now() - current.startedMs);
    } else if (call) {
      text = clock(call.durationMs ?? call.audioMs);
    }
    if (this.elapsed.textContent !== text) {
      this.elapsed.textContent = text;
      this.elapsed.title = call && call.durationMs ? 'Call duration ' + duration(call.durationMs) : '';
    }
    let level = 0;
    if (current && this.analyser) {
      this.analyser.getFloatTimeDomainData(this.samples);
      let peak = 0;
      for (const sample of this.samples) {
        peak = Math.max(peak, Math.abs(sample));
      }
      const db = peak > 0 ? 20 * Math.log10(peak) : METER_FLOOR_DB;
      level = Math.max(0, Math.min(1, (db - METER_FLOOR_DB) / -METER_FLOOR_DB));
    }
    this.meterBar.style.width = (level * 100).toFixed(1) + '%';
  }
}
