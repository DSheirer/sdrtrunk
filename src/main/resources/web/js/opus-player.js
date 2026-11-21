// Plays live call audio from the audio.live topic: Opus frames decoded with WebCodecs and scheduled with Web Audio.
//
// A call is either held (its frames are kept, up to a limit, while another call plays) or playing (decoded and
// scheduled with a small jitter buffer).  Playing a held call plays what was kept and then continues live, so a call
// that arrived during another one is heard in full afterwards.  onFinished(callKey) reports when a playing call has
// ended and its audio has played out.
const MAX_HELD_FRAMES = 3000; // 60 seconds of 20 ms frames

export class OpusPlayer {
  constructor() {
    this.context = null;
    this.calls = new Map();
    this.onFinished = () => {};
    this.onStateChange = () => {};
  }

  static supported() {
    return typeof AudioDecoder !== 'undefined';
  }

  // The audio context, created suspended until resume() is called from a user action
  ensureContext() {
    this.context = this.context || new AudioContext();
    return this.context;
  }

  // Call from a user action: browsers only allow audio to start from one
  async resume() {
    await this.ensureContext().resume();
  }

  close() {
    this.stopAll();
    if (this.context) {
      this.context.close().catch(() => {});
      this.context = null;
    }
  }

  // Starts keeping a call's frames without playing them
  hold(callKey) {
    if (!this.calls.has(callKey)) {
      this.calls.set(callKey, {state: 'held', frames: [], ended: false, nextTime: 0, decoder: null,
        destination: null});
    }
  }

  // Plays a call into the destination node (an audio channel's input): the kept frames, then its live frames
  play(callKey, destination) {
    this.hold(callKey);
    const call = this.calls.get(callKey);
    if (call.state === 'playing' || !this.context) {
      return;
    }
    call.state = 'playing';
    call.destination = destination || this.context.destination;
    call.decoder = new AudioDecoder({
      output: (audioData) => this.output(call, audioData),
      error: () => this.finish(callKey)
    });
    call.decoder.configure({codec: 'opus', sampleRate: 48000, numberOfChannels: 1});
    for (const frame of call.frames) {
      this.decode(call, frame.sequence, frame.data);
    }
    call.frames = [];
    this.onStateChange();
    if (call.ended) {
      this.drain(callKey);
    }
  }

  frame(view, buffer) {
    const call = this.calls.get(Number(view.getBigInt64(8, true)));
    if (!call) {
      return;
    }
    const sequence = view.getUint32(16, true);
    if (call.state === 'playing') {
      this.decode(call, sequence, new Uint8Array(buffer, 20));
    } else if (call.frames.length < MAX_HELD_FRAMES) {
      call.frames.push({sequence, data: new Uint8Array(buffer.slice(20))});
    }
  }

  decode(call, sequence, data) {
    if (call.decoder.state === 'configured') {
      call.decoder.decode(new EncodedAudioChunk({type: 'key', timestamp: sequence * 20000, data}));
    }
  }

  output(call, audioData) {
    const samples = new Float32Array(audioData.numberOfFrames);
    audioData.copyTo(samples, {planeIndex: 0, format: 'f32-planar'});
    const buffer = this.context.createBuffer(1, samples.length, audioData.sampleRate);
    buffer.copyToChannel(samples, 0);
    audioData.close();
    const source = this.context.createBufferSource();
    source.buffer = buffer;
    source.connect(call.destination);
    call.nextTime = Math.max(call.nextTime, this.context.currentTime + 0.15);
    source.start(call.nextTime);
    call.nextTime += buffer.duration;
  }

  // The call's audio ended (audio.end)
  ended(callKey) {
    const call = this.calls.get(callKey);
    if (call) {
      call.ended = true;
      if (call.state === 'playing') {
        this.drain(callKey);
      }
    }
  }

  // Waits for the decoded audio to play out, then reports the call finished
  drain(callKey) {
    const call = this.calls.get(callKey);
    call.decoder.flush().catch(() => {}).then(() => {
      const remaining = this.context ? Math.max(0, call.nextTime - this.context.currentTime) : 0;
      setTimeout(() => this.finish(callKey), remaining * 1000 + 50);
    });
  }

  finish(callKey) {
    if (this.discard(callKey)) {
      this.onFinished(callKey);
    }
  }

  // Stops and forgets a call.  Returns true if it was known.
  discard(callKey) {
    const call = this.calls.get(callKey);
    if (!call) {
      return false;
    }
    this.calls.delete(callKey);
    if (call.decoder && call.decoder.state !== 'closed') {
      call.decoder.close();
    }
    this.onStateChange();
    return true;
  }

  stopAll() {
    for (const callKey of [...this.calls.keys()]) {
      this.discard(callKey);
    }
  }

  isPlaying(callKey) {
    const call = this.calls.get(callKey);
    return !!call && call.state === 'playing';
  }

  isHeld(callKey) {
    const call = this.calls.get(callKey);
    return !!call && call.state === 'held';
  }
}
